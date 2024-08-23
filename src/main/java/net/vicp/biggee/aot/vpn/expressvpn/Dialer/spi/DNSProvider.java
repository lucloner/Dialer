package net.vicp.biggee.aot.vpn.expressvpn.Dialer.spi;

import lombok.extern.slf4j.Slf4j;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.util.RunShell;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;

import java.net.*;
import java.net.spi.InetAddressResolver;
import java.net.spi.InetAddressResolverProvider;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class DNSProvider extends InetAddressResolverProvider implements InetAddressResolver {
    @SuppressWarnings("unused")
    public static final ProxySelector sysDefault = RunShell.getDefault();
    @SuppressWarnings("FieldCanBeLocal")
    private Configuration configuration;
    @SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal", "SpellCheckingInspection"})
    private String name = "Biggee's DNS Config";
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private LookupPolicy lookupPolicy;
    private static final Cache innerCache = new Cache();
    private static final Map<String, String> cache = new HashMap<>();

    @Override
    public InetAddressResolver get(Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    @Override
    public String name() {
        return name;
    }

    public static Set<InetAddress> nsLookup(String host, String dns) {
        try {
            Lookup lookup = new Lookup(host, Type.A);
            lookup.setResolver(new SimpleResolver(dns));
            lookup.setCache(innerCache);
            //noinspection UnusedAssignment
            Record[] records = lookup.run();
            if (Lookup.SUCCESSFUL == lookup.getResult()) {
                records = lookup.getAnswers();
                return Arrays.stream(records)
                        .parallel()
                        .map(r -> {
                            try {
                                if (r instanceof ARecord) {
                                    return Inet4Address.getByAddress(host, ((ARecord) r).getAddress().getAddress());
                                } else if (r instanceof AAAARecord) {
                                    return Inet6Address.getByAddress(host, ((AAAARecord) r).getAddress().getAddress());
                                }
                            } catch (UnknownHostException e) {
                                log.error("lookup resolve error: {} address: {} from {}", host, r, dns, e);
                            }
                            return null;
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
            }
            log.warn("lookup resolver not reached: {} from {}", host, dns);
        } catch (TextParseException | UnknownHostException e) {
            log.error("lookup resolve failure: {} from {}", host, dns, e);
        }
        return null;
    }

    @Override
    public Stream<InetAddress> lookupByName(String host, LookupPolicy lookupPolicy) throws UnknownHostException {
        this.lookupPolicy = lookupPolicy;

        Set<InetAddress> addresses = new HashSet<>();
        RunShell zero = RunShell.getZero();
        if (zero != null
                && Arrays.stream(zero.getUrls())
                .anyMatch(url -> url.contains(host))) {

            Arrays.stream(zero.getDns())
                    .parallel()
                    .map(dns -> nsLookup(host, dns))
                    .filter(Objects::nonNull)
                    .forEach(addresses::addAll);
        }
        if (addresses.isEmpty()) {
            cache.entrySet()
                    .stream()
                    .parallel()
                    .filter(e -> e.getValue().equals(host))
                    .forEach(e -> {
                        String ip = e.getKey();
                        try {
                            addresses.add(InetAddress.getByAddress(host, Address.getByAddress(ip).getAddress()));
                            log.info("lookup load cache: {} from {}", host, ip);
                        } catch (UnknownHostException ex) {
                            log.warn("lookup load cache but useless: {} from {}", host, ip, ex);
                        }
                    });
        } else {
            cache.clear();
        }

        Arrays.stream(Address.getAllByName(host))
                .parallel()
                .forEach(ip -> {
                    try {
                        addresses.add(InetAddress.getByAddress(host, ip.getAddress()));
                    } catch (UnknownHostException e) {
                        log.warn("lookup dnsjava but useless: {} from {}", host, ip, e);
                    }
                });

        configuration.builtinResolver()
                .lookupByName(host, lookupPolicy)
                .parallel()
                .forEach(ip -> {
                    try {
                        addresses.add(InetAddress.getByAddress(host, ip.getAddress()));
                    } catch (UnknownHostException e) {
                        log.warn("lookup builtin dns but useless: {} from {}", host, ip, e);
                    }
                });

        if (zero == null || !zero.isSupportIPv6()) {
            addresses.removeIf(ip -> ip instanceof Inet6Address);
        }
        addresses.stream()
                .parallel()
                .peek(ip -> log.debug("dns: {} -> {}", host, ip))
                .forEach(ip -> cache.put(Address.toDottedQuad(ip.getAddress()), host));
        return addresses.stream();
    }

    @Override
    public String lookupByAddress(byte[] addr) throws UnknownHostException {
        return cache.getOrDefault(Address.toDottedQuad(addr),
                configuration.builtinResolver().lookupByAddress(addr));
    }
}

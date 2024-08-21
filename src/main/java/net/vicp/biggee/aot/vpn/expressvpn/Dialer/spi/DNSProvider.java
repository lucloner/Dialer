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
                Stream<InetAddress> addressStream = Arrays.stream(records)
                        .parallel()
                        .filter(r -> r instanceof ARecord)
                        .map(r -> {
                            try {
                                return Inet4Address.getByAddress(host, ((ARecord) r).getAddress().getAddress());
                            } catch (UnknownHostException e) {
                                log.error("lookup resolve error: {} address: {} from {}", host, r, dns, e);
                            }
                            return null;
                        })
                        .filter(Objects::nonNull);
                Stream<InetAddress> addressStream6 = Arrays.stream(records)
                        .parallel()
                        .filter(r -> r instanceof AAAARecord)
                        .map(r -> {
                            try {
                                return Inet6Address.getByAddress(host, ((AAAARecord) r).getAddress().getAddress());
                            } catch (UnknownHostException e) {
                                log.error("lookup6 resolve error: {} is address {} from {}", host, r, dns, e);
                            }
                            return null;
                        })
                        .filter(Objects::nonNull);
                return Stream.concat(addressStream, addressStream6).collect(Collectors.toSet());
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

        Stream<InetAddress> result = Stream.empty();
        RunShell zero = RunShell.getZero();
        if (zero != null
                && Arrays.stream(zero.getUrls()).anyMatch(url -> url.contains(host))) {
            Set<InetAddress> addresses = new HashSet<>();
            Arrays.stream(zero.getDns())
                    .parallel()
                    .map(dns -> nsLookup(host, dns))
                    .filter(Objects::nonNull)
                    .forEach(addresses::addAll);
            if (!addresses.isEmpty()) {
                result = addresses.stream();
            }
        }
        if (result.findAny().isEmpty()) {
            result = Arrays.stream(Address.getAllByName(host));
        }

        //noinspection DataFlowIssue
        return result.peek(ip -> log.debug("dns: {} -> {}", host, ip))
                .peek(ip -> cache.put(Address.toDottedQuad(ip.getAddress()), host));
    }

    @Override
    public String lookupByAddress(byte[] addr) throws UnknownHostException {
        return cache.getOrDefault(Address.toDottedQuad(addr),
                configuration.builtinResolver().lookupByAddress(addr));
    }
}

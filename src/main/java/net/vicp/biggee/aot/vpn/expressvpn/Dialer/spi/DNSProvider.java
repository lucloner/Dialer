package net.vicp.biggee.aot.vpn.expressvpn.Dialer.spi;

import net.vicp.biggee.aot.vpn.expressvpn.Dialer.util.RunShell;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.spi.InetAddressResolver;
import java.net.spi.InetAddressResolverProvider;
import java.util.Arrays;
import java.util.stream.Stream;

public class DNSProvider extends InetAddressResolverProvider implements InetAddressResolver {
    public static Stream<InetAddress> dns;
    @SuppressWarnings("FieldCanBeLocal")
    private Configuration configuration;
    @SuppressWarnings({"FieldCanBeLocal", "FieldMayBeFinal", "SpellCheckingInspection"})
    private String name = "Biggee's DNS Config";
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private LookupPolicy lookupPolicy;

    @Override
    public InetAddressResolver get(Configuration configuration) {
        this.configuration = configuration;
        return this;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Stream<InetAddress> lookupByName(String host, LookupPolicy lookupPolicy) throws UnknownHostException {
        this.lookupPolicy = lookupPolicy;
        Stream<InetAddress> inetAddressStream = Stream.empty();
        if (dns != null) {
            inetAddressStream = dns;
        }
        if (configuration != null) {
            inetAddressStream = Stream.concat(inetAddressStream, configuration.builtinResolver().lookupByName(host, lookupPolicy));
        }
        Stream<InetAddress> base = Stream.of(InetAddress.getByName("202.96.209.133"),
                InetAddress.getByName("114.114.114.114"));
        if (inetAddressStream.findAny().isEmpty()) {
            inetAddressStream = base;
        }
        RunShell zero = RunShell.getZero();
        //noinspection HttpUrlsUsage
        if (zero != null && Arrays.stream(zero.getUrls())
                .map(u -> u.replace("https://", "")
                        .replace("http://", ""))
                .anyMatch(host::contains)) {
            return inetAddressStream.distinct();
        }

        return Stream.concat(base, inetAddressStream).distinct();
    }

    @Override
    public String lookupByAddress(byte[] addr) throws UnknownHostException {
        if (configuration == null) {
            return InetAddress.getByAddress(addr).toString();
        }
        return configuration.builtinResolver().lookupByAddress(addr);
    }
}

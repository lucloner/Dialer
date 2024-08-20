package net.vicp.biggee.aot.vpn.expressvpn.Dialer.spi;

import lombok.extern.slf4j.Slf4j;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.util.RunShell;

import java.io.IOException;
import java.net.*;
import java.net.spi.InetAddressResolver;
import java.net.spi.InetAddressResolverProvider;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
public class DNSProvider extends InetAddressResolverProvider implements InetAddressResolver {
    public static final ProxySelector sysDefault = ProxySelector.getDefault();
    public static List<Proxy> dnsList;
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
        ProxySelector.setDefault(getProxyList());
        return Arrays.stream(InetAddress.getAllByName(host)).distinct();
    }

    @Override
    public String lookupByAddress(byte[] addr) throws UnknownHostException {
        if (configuration == null) {
            return InetAddress.getByAddress(addr).toString();
        }
        return configuration.builtinResolver().lookupByAddress(addr);
    }

    public ProxySelector getProxyList() {
        if (dnsList == null || dnsList.isEmpty()) {
            return sysDefault;
        }
        return new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                RunShell zero = RunShell.getZero();
                if (zero != null && Arrays.stream(zero.getUrls()).anyMatch(u -> u.contains(uri.getHost()))) {
                    log.info("select custom dns: {}", dnsList);
                    return dnsList;
                }
                log.info("select normal dns");
                return sysDefault.select(uri);
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                log.error("ProxySelector error from {} by {}", uri, sa, ioe);
            }
        };
    }
}

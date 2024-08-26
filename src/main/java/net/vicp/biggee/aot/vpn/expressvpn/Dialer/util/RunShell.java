package net.vicp.biggee.aot.vpn.expressvpn.Dialer.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.Host;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.Node;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.NodesDao;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus.*;

@Slf4j
@Component
@Data
@ConfigurationProperties(prefix = "info")
public class RunShell extends ProxySelector {
    private String[] urls;
    private int tolerance;
    private Host[] hosts;
    private boolean upgradeable = false;
    private boolean connected = false;
    private String location = "";
    public int index = 0;
    public static RunShell[] mesh;
    private ExpressvpnStatus status = Not_Connected;
    private int interval = 1;
    private LocalDateTime lastCheck = LocalDateTime.now().minusYears(1);
    private String[] dns;
    private boolean supportIPv6 = false;

    public RunShell() {

    }

    public boolean isSupportIPv6() {
        RunShell zero = getZero();
        zero = zero == null ? this : zero;
        return zero.supportIPv6;
    }

    public void setSupportIPv6(boolean supportIPv6) {
        RunShell zero = getZero();
        zero = zero == null ? this : zero;
        zero.supportIPv6 = supportIPv6;
    }

    public String[] getDns() {
        RunShell zero = getZero();
        zero = zero == null ? this : zero;
        return zero.dns;
    }

    public void setDns(String... dns) {
        RunShell zero = getZero();
        zero = zero == null ? this : zero;
        zero.dns = dns;
    }

    public int getInterval() {
        RunShell zero = getZero();
        zero = zero == null ? this : zero;
        return zero.interval + getUrls().length;
    }

    public void setInterval(int interval) {
        RunShell zero = getZero();
        zero = zero == null ? this : zero;
        zero.interval = interval;
    }

    public static RunShell getZero() {
        if (mesh != null && mesh.length > 0) {
            return mesh[0];
        }
        return null;
    }

    public Host[] getHosts() {
        RunShell zero = getZero();
        zero = zero == null ? this : zero;
        return zero.hosts;
    }

    public void setHosts(Host... hosts) {
        RunShell zero = getZero();
        zero = zero == null ? this : zero;
        zero.hosts = hosts;
    }

    public String[] getUrls() {
        RunShell zero = getZero();
        zero = zero == null ? this : zero;
        return zero.urls;
    }

    public void setUrls(String... urls) {
        RunShell zero = getZero();
        zero = zero == null ? this : zero;
        zero.urls = urls;
    }

    public int getTolerance() {
        RunShell zero = getZero();
        zero = zero == null ? this : zero;
        return zero.tolerance;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createRunners() {
        log.info("Spring Boot 应用启动完成，装填操作组。");
        ArrayList<RunShell> meshList = new ArrayList<>();
        meshList.add(this);
        log.info("read config zero: {}", this);
        int bound = getHosts().length;
        for (int i = 1; i < bound; i++) {
            RunShell r = new RunShell();
            r.index = i;
            meshList.add(r);
            log.info("read config: {}", r);
        }
        mesh = meshList.toArray(new RunShell[0]);
    }

    public void setTolerance(int tolerance) {
        RunShell zero = getZero();
        zero = zero == null ? this : zero;
        zero.tolerance = tolerance;
    }

    public RunShell getNext() {
        int offset = 1;
        RunShell runShell = mesh[(index + offset++) % getHosts().length];
        while (!runShell.getHost().enabled) {
            runShell = mesh[(index + offset++) % getHosts().length];
        }
        return runShell;
    }

    public Host getHost() {
        return getHosts()[index];
    }

    public List<String> getCommand() {
        return Arrays.stream(getHost().command.split(" ")).toList();
    }

    public List<String> getCommand(String... params) {
        List<String> cmd = new ArrayList<>(getCommand());
        cmd.addAll(List.of(params));
        return cmd;
    }

    public boolean checkWebs() {
        assert getZero() != null;
        int passLine = getUrls().length - getTolerance();
        long padded = Arrays.stream(getZero().urls).parallel().map(u -> {
            try {
                int code = checkUrl(u);
                log.info("[{}]checkWebs {} checked {} ok", getIndex(), u, code);
                return code;
            } catch (IOException | InterruptedException | RuntimeException e) {
                log.error("url: {}", u, e);
            }
            return 500;
        }).filter(i -> i >= 200).filter(i -> i < 300).count();
        if (padded > passLine) {
            log.info("[{}]checkWebs done {}/{}", getIndex(), padded, passLine);
            lastCheck = LocalDateTime.now();
            return true;
        }
        log.info("[{}]checkWebs failed {}/{}", getIndex(), padded, passLine);
        lastCheck = lastCheck.minusHours(1);
        return false;
    }

    public int checkUrl(String url) throws IOException, InterruptedException {
        HttpClient.Builder builder = HttpClient.newBuilder();
        if (!getHost().isLocalHost) {
            builder.proxy(this);
        }
        log.info("checking url: {}", url);
        //noinspection resource
        HttpClient build = builder.followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofMinutes(1))
                .build();
        return build
                .send(HttpRequest.newBuilder(URI.create(url))
                        .GET()
                        .timeout(Duration.ofMinutes(1))
                        .build(), HttpResponse.BodyHandlers.ofString())
                .statusCode();
    }

    public String getLocation(NodesDao nodesDao) {
        Optional<Node> node = nodesDao.findOne((r, q, b) -> b.or(b.equal(r.get("location"), location), b.equal(r.get("alias"), location)));
        if (node.isEmpty()) {
            node = nodesDao.findOne((r, q, b) -> b.or(b.like(b.literal(location), b.concat("%", b.concat(r.get("location"), "%"))),
                    b.like(b.literal(location), b.concat("%", b.concat(r.get("location"), "%")))));
        }
        return node.orElse(new Node("", location, false)).alias;
    }

    public Process connect(String location) throws IOException {
        location = location == null ? "" : location;
        var builder = initCommand(getCommand("connect", location));
        lastCheck = LocalDateTime.now();
        Process start = builder.start();
        status=Connected;
        return start;
    }

    public List<String> flush() {
        run(getCommand("refresh"));
        return getList();
    }

    @SuppressWarnings("UnusedReturnValue")
    public String disconnect() {
        if (getInterval() <= 0
                || getTolerance() <= 0
                || getTolerance() >= getUrls().length
                || Duration.between(lastCheck, LocalDateTime.now()).toMinutes() < getInterval()) {
            log.info("[{}]disconnect disabled {}/{} {}", getIndex(), getInterval(),getTolerance(),lastCheck);
            return Connecting.key;
        }

        String disconnect = run(getCommand("disconnect")).toString();
        status=Not_Connected;
        return disconnect;
    }

    public ExpressvpnStatus status() {
        var returns = run(getCommand("status"));
        return status(returns.toString());
    }

    public ExpressvpnStatus checkStatus(String returns, ExpressvpnStatus... statuses) {
        if (DISABLED.key.equals(returns)) {
            return DISABLED;
        }
        ExpressvpnStatus base = null;
        for (ExpressvpnStatus status : statuses) {
            var k = status.key;
            if (k == null) {
                base = status;
                //noinspection UnnecessaryContinue
                continue;
            } else if (returns.contains(k)) {
                return status;
            }
        }
        return base;
    }

    public ExpressvpnStatus status(String returns) {
        upgradeable = returns.contains(Upgradeable.key) || returns.contains(Upgradeable_Arch.key);
        ExpressvpnStatus base = Not_Connected;
        if (upgradeable) {
            base = Upgradeable;
        }
        connected = returns.contains(Connected.key);
        if (connected) {
            //location = returns.split(Connected.key)[1].trim().split("\\n")[0].trim();
            return Connected;
        }

        //noinspection UnnecessaryLocalVariable
        ExpressvpnStatus expressvpnStatus = checkStatus(returns, Halt, Connected, Connecting, Reconnecting, Unable_Connect, base, Unknown_Error, Busy);
        return expressvpnStatus;
    }

    public List<String> getList() {
        List<String> result = run(getCommand("list", "all"));
        if (Halt.equals(checkStatus(result.toString(), Halt))) {
            log.error("expressvpnd is halt! {}", result);
            return List.of("xv", "smart");
        }

        var start = false;
        var newList = new ArrayList<String>();

        for (String n : result) {
            if (n.startsWith("--")) {
                continue;
            } else if (n.startsWith("ALIAS")) {
                start = true;
                continue;
            } else if (!start) {
                continue;
            }

            newList.add(n);
        }

        return newList;
    }

    public List<String> run(List<String> commands) {
        if (!getHost().enabled) {
            return Collections.singletonList("DISABLED");
        }
        var builder = initCommand(commands);
        Process start = null;
        try {
            log.debug("run command: {}", commands);
            start = builder.start();
            start.waitFor(status.timeout, TimeUnit.SECONDS);
            return new BufferedReader(
                    new InputStreamReader(
                            start.getInputStream()))
                    .lines().toList();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (start != null && !commands.contains("kill")) {
                String pid = String.valueOf(start.pid());
                start.destroy();
                //noinspection resource
                Executors.newSingleThreadScheduledExecutor()
                                .scheduleWithFixedDelay(start::destroyForcibly,15,15,TimeUnit.SECONDS);
                Process finalStart = start;
                //noinspection resource
                Executors.newSingleThreadScheduledExecutor()
                        .scheduleWithFixedDelay(() -> {
                            if(finalStart.isAlive()){
                                log.warn("Commands {} troubled Destroy: {}, force killing",commands,pid);
                                run(List.of("kill","-9",pid));
                            }
                        },30,30,TimeUnit.SECONDS);
            }
        }
    }

    public ProcessBuilder initCommand(List<String> commands) {
        commands.removeIf(Objects::isNull);
        return new ProcessBuilder(commands);
    }

    @Override
    public List<Proxy> select(URI uri) {
        InetSocketAddress proxy = new InetSocketAddress(getHost().proxyHost, getHost().proxyPort);
        log.info("checking url from proxy: {}", proxy);
        Proxy proxyConfig = new Proxy(Proxy.Type.HTTP, proxy);
        return List.of(proxyConfig);
    }

    @Override
    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
        throw new RuntimeException("proxy error: " + sa, ioe);
    }

    @Override
    public String toString() {
        return "RunShell{" +
                "urls=" + Arrays.toString(urls) +
                ", tolerance=" + getTolerance() +
                ", hosts=" + Arrays.toString(hosts) +
                ", upgradeable=" + upgradeable +
                ", connected=" + connected +
                ", location='" + location + '\'' +
                ", index=" + index +
                ", status=" + status +
                ", interval=" + interval +
                ", lastCheck=" + lastCheck +
                ", dns=" + Arrays.toString(dns) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        return toString().equals(o.toString());
    }

    @Override
    public int hashCode() {
        long result = super.hashCode();
        result = 31 * result + getTolerance();
        result = 31 * result + (isUpgradeable() ? 0 : 1);
        result = 2 * result + (isConnected() ? 0 : 1);
        result = 2 * result + getLocation().hashCode();
        result = 31 * result + Integer.hashCode(getIndex());
        result = 31 * result + status.hashCode();
        result = 31 * result + Integer.hashCode(getInterval());
        result = 31 * result + lastCheck.hashCode();
        return (int) result;
    }
}

package net.vicp.biggee.aot.vpn.expressvpn.Dialer.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Transient;
import lombok.Data;
import lombok.Setter;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    public RunShell(){

    }

    public Host[] getHosts() {
        RunShell zero = getZero();
        zero=zero==null?this:zero;
        return zero.hosts;
    }
    public void setHosts(Host...hosts) {
        RunShell zero = getZero();
        zero=zero==null?this:zero;
        zero.hosts= hosts;
    }

    public String[] getUrls() {
        RunShell zero = getZero();
        zero=zero==null?this:zero;
        return zero.urls;
    }

    public void setUrls(String...urls) {
        RunShell zero = getZero();
        zero=zero==null?this:zero;
        zero.urls = urls;
    }

    public int getTolerance() {
        RunShell zero = getZero();
        zero=zero==null?this:zero;
        return zero.tolerance;
    }

    @SuppressWarnings("unused")
    public void getTolerance(int tolerance) {
        RunShell zero = getZero();
        zero=zero==null?this:zero;
        zero.tolerance=tolerance;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void createRunners() {
        log.info("Spring Boot 应用启动完成，装填操作组。");
        ArrayList<RunShell> meshList = new ArrayList<>();
        meshList.add(this);
        int bound = getHosts().length;
        for (int i = 1; i < bound; i++) {
            RunShell r = new RunShell();
            r.index = i;
            meshList.add(r);
            log.info("read config: {}", r);
        }
        mesh = meshList.toArray(new RunShell[0]);
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

    public static RunShell getZero() {
        if(mesh!=null&&mesh.length>0){
            return mesh[0];
        }
        return null;
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
        return Arrays.stream(getZero().urls).parallel().map(u -> {
            try {
                return checkUrl(u);
            } catch (IOException | InterruptedException | RuntimeException e) {
                log.error("url: {}", u, e);
            }
            return 500;
        }).filter(i -> i >= 200).filter(i -> i < 300).count() >= getUrls().length - getTolerance();
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
        return node.orElse(new Node(location, location, false)).alias;
    }

    public Process connect(String location) throws IOException {
        location = location == null ? "" : location;
        var builder = initCommand(getCommand("connect", location));
        return builder.start();
    }

    public String[] flush() {
        run(getCommand("refresh"));
        return getList();
    }

    public String disconnect() {
        return run(getCommand("disconnect"));
    }

    public ExpressvpnStatus status() {
        var returns = run(getCommand("status"));
        return status(returns);
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
            location = returns.split(Connected.key)[1].trim().split("\\n")[0].trim();
            return Connected;
        }

        return checkStatus(returns, Halt, Connected, Connecting, Reconnecting, Unable_Connect, base, Unknown_Error, Busy);
    }

    public String[] getList() {
        String result = run(getCommand("list", "all"));
        if(Halt.equals(checkStatus(result,Halt))){
            log.error("expressvpnd is halt! {}", result);
            return new String[]{"xv","smart"};
        }
        var run = result.split("\\n");

        var start = false;
        var newList = new ArrayList<String>();

        for (String n : run) {
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

        return newList.toArray(new String[]{});
    }

    public String run(List<String> commands) {
        if (!getHost().enabled) {
            return "DISABLED";
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
                    .lines()
                    .collect(Collectors.joining("\n"));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (start != null) {
                start.destroy();
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
        RunShell main = getZero();

        main=main==null?this:main;
        return "RunShell{" +
                "urls=" + Arrays.toString(main.urls) +
                ", tolerance=" + main.tolerance +
                ", hosts=" + Arrays.toString(main.hosts) +
                ", upgradeable=" + upgradeable +
                ", connected=" + connected +
                ", location='" + location + '\'' +
                ", index=" + index +
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
        int result = super.hashCode();
        result = 31 * result + getUrls().hashCode();
        result = 31 * result + getTolerance();
        result = 31 * result + Arrays.hashCode(getHosts());
        result = 31 * result + Boolean.hashCode(isUpgradeable());
        result = 31 * result + Boolean.hashCode(isConnected());
        result = 31 * result + getLocation().hashCode();
        result = 31 * result + getIndex();
        return result;
    }
}

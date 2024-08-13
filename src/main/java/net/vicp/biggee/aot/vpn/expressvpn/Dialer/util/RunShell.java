package net.vicp.biggee.aot.vpn.expressvpn.Dialer.util;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.Nodes;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.NodesDao;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus.*;

@Configuration
@ConfigurationProperties(prefix = "info")
public class RunShell {
    String command;
    List<String> urls;
    int tolerance;
    static String CMD="expressvpn";
    static boolean upgradeable=false;
    static boolean connected = false;
    private static String location = "";

    public RunShell() {
        CMD = command;
    }

    public boolean checkWebs() {
        return urls.stream().parallel().map(u -> {
            try {
                return checkUrl(u);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
            return 500;
        }).filter(i -> i >= 200).filter(i -> i < 300).count() >= urls.size() - tolerance;
    }

    public int checkUrl(String url) throws IOException, InterruptedException {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build()
                .send(HttpRequest.newBuilder(URI.create(url))
                        .GET()
                        .build(), HttpResponse.BodyHandlers.ofString())
                .statusCode();
    }

    public String getLocation(NodesDao nodesDao) {
        Optional<Nodes> node = nodesDao.findOne((r, q, b) -> b.or(b.equal(r.get("location"), location), b.equal(r.get("alias"), location)));
        return node.orElse(new Nodes(location, location, false)).alias;
    }

    public Process connect(String location) throws IOException {
        var builder=initCommand(new String[]{CMD,"connect",location});
        return builder.start();
    }

    public static String[] flush() {
        run(new String[]{CMD,"refresh"});
        return getList();
    }

    public static String disconnect() {
        return run(new String[]{CMD, "disconnect"});
    }

    public ExpressvpnStatus status(){
        var returns=run(new String[]{CMD,"status"});
        return status(returns);
    }

    public static ExpressvpnStatus checkStatus(String returns,ExpressvpnStatus... statuses){
        ExpressvpnStatus base=null;
        for (ExpressvpnStatus status : statuses) {
            var k=status.key;
            if(k==null){
                base=status;
                //noinspection UnnecessaryContinue
                continue;
            } else if(returns.contains(k)){
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

    public static String[] getList(){
        var run = run(new String[]{CMD, "list","all"}).split("\\n");
        var start=false;
        var newList=new ArrayList<String>();

        for (String n : run) {
            if(n.startsWith("--")){
                continue;
            }
            else if(n.startsWith("ALIAS")){
                start=true;
                continue;
            }
            else if(!start){
                continue;
            }

            newList.add(n);
        }

        return newList.toArray(new String[]{});
    }

    public static String run(String[] commands){
        var builder = initCommand(commands);
        PtyProcess start=null;
        try{
            start = builder.start();
            start.waitFor(1, TimeUnit.MINUTES);
            return new String(start.getInputStream().readAllBytes());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        finally {
            if(start!=null){
                start.destroy();
            }
        }
    }

    public static PtyProcessBuilder initCommand(String[] commands){
        var builder = new PtyProcessBuilder();
        builder.setCommand(commands);
        return builder;
    }
}

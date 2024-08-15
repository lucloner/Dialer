package net.vicp.biggee.aot.vpn.expressvpn.Dialer.util;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.Nodes;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.NodesDao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus.*;

@Slf4j
@Component
@Data
@ConfigurationProperties(prefix = "info")
public class RunShell {
    public List<String> urls;
    public int tolerance;
    public String[] CMD;
    public boolean upgradeable=false;
    public boolean connected = false;
    public String location = "";
    public int index=0;
    public static RunShell[] mesh;

    @EventListener(ApplicationReadyEvent.class)
    public void createRunners() {
        log.info("Spring Boot 应用启动完成，装填操作组。");
        ArrayList<RunShell> meshList = new ArrayList<>();
        meshList.add(this);
        IntStream.range(1,CMD.length).forEach(i->{
            RunShell r = new RunShell();
            r.index=i;
            meshList.add(r);
        });
        mesh=meshList.toArray(new RunShell[CMD.length]);
    }

    public RunShell getNext(){
        return mesh[(index+1)%CMD.length];
    }

    public String[] getCommand(){
        return CMD[0].split(" ");
    }

    public String[] getCommand(String...params){
        String[] cmd=getCommand();
        String[] fullCommand=Arrays.copyOf(cmd, cmd.length + cmd.length);
        System.arraycopy(params, 0, fullCommand, cmd.length, params.length);
        return fullCommand;
    }

    public boolean checkWebs() {
        return urls.stream().parallel().map(u -> {
            try {
                return checkUrl(u);
            } catch (IOException | InterruptedException e) {
                log.error("url: "+u,e);
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
        var builder=initCommand(getCommand("connect",location));
        return builder.start();
    }

    public String[] flush() {
        run(getCommand("refresh"));
        return getList();
    }

    public String disconnect() {
        return run(getCommand("disconnect"));
    }

    public ExpressvpnStatus status(){
        var returns=run(getCommand("status"));
        return status(returns);
    }

    public ExpressvpnStatus checkStatus(String returns,ExpressvpnStatus... statuses){
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

    public String[] getList(){
        var run = run(getCommand("list","all")).split("\\n");
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

    public String run(String[] commands){
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

    public PtyProcessBuilder initCommand(String[] commands){
        var builder = new PtyProcessBuilder();
        builder.setCommand(commands);
        return builder;
    }
}

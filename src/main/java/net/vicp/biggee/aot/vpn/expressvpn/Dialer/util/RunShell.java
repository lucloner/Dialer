package net.vicp.biggee.aot.vpn.expressvpn.Dialer.util;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus.*;

public class RunShell {
    static String CMD="expressvpn";
    static String[] nodes;
    static boolean upgradeable=false;
    boolean connected=false;
    String location="";

    public Process connect(String location) throws IOException {
        var builder=initCommand(new String[]{CMD,"connect",location});
        return builder.start();
    }

    public ExpressvpnStatus status(){
        var returns=run(new String[]{CMD,"status"});
        upgradeable=returns.contains(Upgradeable.key)||returns.contains(Upgradeable_Arch.key);
        ExpressvpnStatus base=Not_Connected;
        if(upgradeable){
            base=Upgradeable;
        }
        connected=returns.contains(Connected.key);
        if(connected){
            location= returns.split(Connected.key)[1].trim().split("\\n")[0].trim();
            return Connected;
        }

        return checkStatus(returns,Halt, Connected,Connecting,Reconnecting,Unable_Connect,base,Unknown_Error);
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

    public static void flush(){
        run(new String[]{CMD,"refresh"});
        nodes= getList();
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

            newList.add(n.split(" ")[0]);
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

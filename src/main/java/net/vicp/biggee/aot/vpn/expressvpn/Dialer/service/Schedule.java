package net.vicp.biggee.aot.vpn.expressvpn.Dialer.service;

import lombok.extern.slf4j.Slf4j;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.History;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.HistoryDao;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.NodesDao;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.PlanDao;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.util.RunShell;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus.*;

@Slf4j
@Component
public class Schedule {
    final
    Connect connect;
    final
    HistoryDao historyDao;
    final
    PlanDao planDao;
    final
    NodesDao nodesDao;
    final
    Recycle recycle;

    public Schedule(Connect connect, HistoryDao historyDao, PlanDao planDao, NodesDao nodesDao, Recycle recycle) {
        this.connect = connect;
        this.historyDao = historyDao;
        this.planDao = planDao;
        this.nodesDao = nodesDao;
        this.recycle = recycle;
    }

    @Async
    @Scheduled(initialDelay = 1, fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    public void checkStatus() {
        for (RunShell runShell : RunShell.mesh) {
            if (!runShell.getHost().enabled) {
                continue;
            }
            log.info("ready to check: {}", runShell);

            int meshIndex = runShell.index;
            ExpressvpnStatus status = connect.status.status(meshIndex);
            runShell.setStatus(status);
            String location=runShell.getLocation();
            historyDao.save(new History(location,status,meshIndex));

            if(List.of(Connecting,Reconnecting,Busy).contains(status)){
                LocalDateTime since = runShell.getLastCheck();
                int timeout = runShell.getInterval();
                if(timeout>0&&Duration.between(since,LocalDateTime.now()).toMinutes()>10+timeout){
                    runShell.disconnect();
                    runShell.setStatus(Not_Connected);
                    status=Not_Connected;
                    historyDao.save(new History(location,Not_Connected,meshIndex));
                    log.info("[{}]checkStatus timeout to {} since {}", meshIndex, location, since);
                }
            }

            if(List.of(Not_Connected,Unable_Connect,Unknown_Error,Upgradeable,Upgradeable_Arch).contains(status)){
                connect.autoConnect(meshIndex);
                location = runShell.getLocation();
                historyDao.save(new History(location,Connecting,meshIndex));
                log.info("[{}]checkStatus Connect to {}", meshIndex,location);
                continue;
            }
            log.info("[{}]checkStatus is {} to {}", meshIndex,status,location);
        }
    }

    @Async
    @Scheduled(initialDelay = 10, fixedRate = 15, timeUnit = TimeUnit.MINUTES)
    public void watchCat() {
        for (RunShell runShell : RunShell.mesh) {
            if (!runShell.getHost().enabled) {
                continue;
            }

            int meshIndex = runShell.index;
            ExpressvpnStatus lastStatus= runShell.getStatus();
            ExpressvpnStatus status = connect.status.status(meshIndex);
            String location=runShell.getLocation();
            historyDao.save(new History(location,status,meshIndex));
            log.info("[{}]watchCat checked status from {} to {} around {}", meshIndex,lastStatus,status,location);

            if(!status.equals(lastStatus)){
                continue;
            }

            runShell.setStatus(status);

            if(Connected.equals(status)){
                String newLocation = runShell.getLocation(nodesDao);
                runShell.setLocation(newLocation);
                historyDao.save(new History(newLocation,Connected,meshIndex));
                log.info("[{}]watchCat corrected location from {} to {}", meshIndex,location,newLocation);
                continue;
            }

            Future<Process> future = connect.connect(meshIndex, location);
            List<String> stdout=new ArrayList<>();
            try {
                Process process = future.get(5, TimeUnit.MINUTES);
                stdout = new BufferedReader(
                        new InputStreamReader(
                                process.getInputStream()))
                        .lines().toList();
            } catch (InterruptedException|ExecutionException|TimeoutException e) {
                log.error("[{}]watchCat sleep error {}", meshIndex,location,e);
            }

            List<ExpressvpnStatus> statusList = stdout.stream()
                    .parallel()
                    .map(l -> runShell.checkStatus(l, Not_Connected, Connected, Connecting, Reconnecting, Unable_Connect, Unknown_Error, Busy))
                    .toList();

            if(statusList.contains(Busy)){
                log.info("[{}]watchCat checked working {}", meshIndex,location);
                return;
            }

            if(statusList.contains(Connected)){
                log.info("[{}]watchCat checked and reconnected {}", meshIndex,location);
                historyDao.save(new History(location,Connected,meshIndex));
                return;
            }

            if(statusList.contains(Connecting)||statusList.contains(Unable_Connect)){
                log.info("[{}]watchCat failed to reconnected {}", meshIndex,location);
                runShell.disconnect();
            }
            historyDao.save(new History(location,status,meshIndex));

            connect.autoConnect(meshIndex);
            location = runShell.getLocation();
            historyDao.save(new History(location,Connecting,meshIndex));
            log.info("[{}]watchCat Connect to {}", meshIndex,location);
        }
    }
}
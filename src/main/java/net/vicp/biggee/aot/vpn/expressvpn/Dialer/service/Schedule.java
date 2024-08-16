package net.vicp.biggee.aot.vpn.expressvpn.Dialer.service;

import lombok.extern.slf4j.Slf4j;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.History;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.Plan;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.HistoryDao;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.NodesDao;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.PlanDao;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.util.RunShell;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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

    @SuppressWarnings("UnusedReturnValue")
    @Async
    public CompletableFuture<ExpressvpnStatus> scheduleConnect() {
        int meshIndex = connect.runShell.index;
        log.info("scheduleConnect launched: "+meshIndex);

        Future<Process> autoconnect = connect.autoconnect();
        String returns = null;
        int returnCode = -1;
        try {
            Process process = autoconnect.get();
            returnCode = process.waitFor();
            returns = new String(process.getInputStream().readAllBytes());
            log.info("scheduleConnect return: " + returnCode + " message: " + returns);
            process.destroy();
        } catch (InterruptedException | ExecutionException | IOException e) {
            log.warn("scheduleConnect launched error", e);
            throw new RuntimeException(e);
        }
        ExpressvpnStatus expressvpnStatus = connect.runShell.status(returns);

        History last = historyDao.findTopByMeshIndexOrderByTimeDesc(meshIndex);
        last.status = expressvpnStatus;
        last.meshIndex=meshIndex;
        historyDao.save(new History(last));

        if (Connected.equals(expressvpnStatus)) {
            log.info("schedule Connect done: " + expressvpnStatus + " message: " + returns);
            return CompletableFuture.completedFuture(Connected);
        } else if (Busy.equals(expressvpnStatus)) {
            log.warn("schedule Connect is busy: " + expressvpnStatus + " message: " + returns);
            return CompletableFuture.completedFuture(Busy);
        }

        log.info("schedule Connect run again: " + expressvpnStatus + " message: " + returns);

        Connect.executor.execute(connect::init);

        return CompletableFuture.completedFuture(Connecting);
    }

    @Scheduled(initialDelay = 1, fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    public void checkStatus() {
        int meshIndex = connect.runShell.index;
        log.info("checkStatus run [{}]: {}",meshIndex, LocalDateTime.now());
        ExpressvpnStatus expressvpnStatus = connect.status.status();
        History last = historyDao.findTopByMeshIndexOrderByTimeDesc(meshIndex);
        if (last == null) {
            //new init
            connect.plan();
            last = new History();
        }
        String alias = last.location;
        String runShellLocation = connect.runShell.getLocation(nodesDao);
        if (Connected.equals(expressvpnStatus) && !Objects.equals(alias, runShellLocation)) {
            log.warn("checkStatus location sync: " + alias + " <++ " + runShellLocation);
            last.location = runShellLocation;
        } else if (!Reconnecting.equals(last.status)) {
            last.time = LocalDateTime.now();
        }
        last.status = expressvpnStatus;
        if (Arrays.asList(Not_Connected, Unable_Connect, Unknown_Error).contains(expressvpnStatus)) {
            String disconnect = connect.runShell.disconnect();
            log.info("checkStatus disconnect: " + disconnect);
            last.status = Not_Connected;
        } else if (Arrays.asList(Connecting, Reconnecting).contains(expressvpnStatus)) {
            long minutes = Duration.between(last.time, LocalDateTime.now()).toMinutes();
            if (minutes > 10) {
                String disconnect = connect.runShell.disconnect();
                log.info("checkStatus timeout: " + minutes + " message: " + disconnect);
                last.status = Not_Connected;
            } else {
                last.meshIndex=meshIndex;
                historyDao.save(new History(last));
                return;
            }
        } else if (Connected.equals(expressvpnStatus)) {
            log.info("checkStatus connected: [{}] {} <==> {}",meshIndex, last.location, runShellLocation);
            historyDao.save(new History(runShellLocation, Connected,meshIndex));
            recycle.clearAndRePlan();

            return;
        }
        last.id=0;
        last.meshIndex=meshIndex;
        historyDao.save(new History(last));
        scheduleConnect();
    }

    @Scheduled(initialDelay = 10, fixedRate = 15, timeUnit = TimeUnit.MINUTES)
    public void watchCat() {
        int meshIndex = connect.runShell.index;
        ExpressvpnStatus expressvpnStatus = connect.status.status();
        log.info("watchCat run [{}]: {}",meshIndex, expressvpnStatus);
        History history = historyDao.findTopByMeshIndexOrderByTimeDesc(meshIndex);
        if (Duration.between(history.time, LocalDateTime.now()).toMinutes() > 20) {
            connect.runShell.disconnect();
            history.status = Not_Connected;
            history.meshIndex=meshIndex;
            historyDao.save(new History(history));
            ExecutorService executor = Connect.executor;
            Connect.executor = Executors.newCachedThreadPool();
            executor.shutdown();
            Executors.newCachedThreadPool().submit(this::scheduleConnect);

            log.warn("watchCat restart connection! "+connect.runShell.index);

        }else {
            history.status = expressvpnStatus;
            history.meshIndex=meshIndex;
            historyDao.save(new History(history));
            Executors.newCachedThreadPool().submit(this::checkStatus);
            log.warn("watchCat done! ");
        }

        connect.runShell=connect.runShell.getNext();
    }
}

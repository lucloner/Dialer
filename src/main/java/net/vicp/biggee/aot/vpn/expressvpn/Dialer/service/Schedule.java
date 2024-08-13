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
    final
    RunShell runShell;

    public Schedule(Connect connect, HistoryDao historyDao, PlanDao planDao, NodesDao nodesDao, Recycle recycle, RunShell runShell) {
        this.connect = connect;
        this.historyDao = historyDao;
        this.planDao = planDao;
        this.nodesDao = nodesDao;
        this.recycle = recycle;
        this.runShell = runShell;
    }

    @SuppressWarnings("UnusedReturnValue")
    @Async
    public CompletableFuture<ExpressvpnStatus> scheduleConnect() {
        log.info("scheduleConnect launched");
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
        ExpressvpnStatus expressvpnStatus = runShell.status(returns);

        History last = historyDao.findFirstByIdAfterOrderByIdDesc(-1);
        last.status = expressvpnStatus;
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
        log.info("checkStatus run: " + LocalDateTime.now());
        ExpressvpnStatus expressvpnStatus = connect.status.status();
        History last = historyDao.findFirstByIdAfterOrderByIdDesc(-1);
        if (last == null) {
            //new init
            connect.plan();
            last = new History();
        }
        String alias = last.location;
        String runShellLocation = runShell.getLocation(nodesDao);
        if (Connected.equals(expressvpnStatus) && !Objects.equals(alias, runShellLocation)) {
            log.warn("checkStatus location sync: " + alias + " <++ " + runShellLocation);
            last.location = runShellLocation;
        } else if (!Reconnecting.equals(last.status)) {
            last.time = LocalDateTime.now();
        }
        last.status = expressvpnStatus;
        if (Arrays.asList(Not_Connected, Unable_Connect, Unknown_Error).contains(expressvpnStatus)) {
            String disconnect = RunShell.disconnect();
            log.info("checkStatus disconnect: " + disconnect);
            last.status = Not_Connected;
        } else if (Arrays.asList(Connecting, Reconnecting).contains(expressvpnStatus)) {
            long minutes = Duration.between(last.time, LocalDateTime.now()).toMinutes();
            if (minutes > 10) {
                String disconnect = RunShell.disconnect();
                log.info("checkStatus timeout: " + minutes + " message: " + disconnect);
                last.status = Not_Connected;
            } else {
                historyDao.save(new History(last));
                return;
            }
        } else if (Connected.equals(expressvpnStatus)) {
            log.info("checkStatus connected: " + last.location + " <==> " + runShellLocation);
            if (runShellLocation != null) {
                historyDao.save(new History(runShellLocation, Connected));
                if (planDao.exists((r, q, b) -> b.equal(r.get("alias"), runShellLocation))) {
                    planDao.save(new Plan(runShellLocation));
                }
            } else {
                historyDao.save(new History(last.location, Connected));
            }

            recycle.clearAndRePlan();
            return;
        }
        historyDao.save(new History(last));

        scheduleConnect();
    }

    @Scheduled(initialDelay = 10, fixedRate = 15, timeUnit = TimeUnit.MINUTES)
    public void watchCat() {
        ExpressvpnStatus expressvpnStatus = connect.status.status();
        log.info("watchCat run: " + expressvpnStatus);
        List<History> all = historyDao.findAll();
        History history = all.get(all.size() - 1);
        if (Duration.between(history.time, LocalDateTime.now()).toMinutes() > 20) {
            RunShell.disconnect();
            history.status = Not_Connected;
            historyDao.save(new History(history));
            ExecutorService executor = Connect.executor;
            Connect.executor = Executors.newCachedThreadPool();
            executor.shutdown();
            Executors.newCachedThreadPool().submit(this::scheduleConnect);

            log.warn("watchCat restart connection! ");
            return;
        }

        history.status = expressvpnStatus;
        historyDao.save(new History(history));
        Executors.newCachedThreadPool().submit(this::checkStatus);
        log.warn("watchCat done! ");
    }
}

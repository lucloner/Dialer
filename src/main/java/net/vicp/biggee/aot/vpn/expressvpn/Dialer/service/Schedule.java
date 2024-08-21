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

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
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
    private int checkIndex = 0;

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
        String tag=Integer.toHexString(new Object().hashCode());
        RunShell runShell = connect.getRunShell();
        while (!runShell.getHost().enabled){
            runShell=connect.setRunShell(runShell.getNext());
        }
        int meshIndex = runShell.index;
        log.info("[{}]scheduleConnect launched: {}",tag, meshIndex);

        Future<Process> autoconnect = connect.autoconnect();
        //noinspection UnusedAssignment
        String returns = null;
        //noinspection UnusedAssignment
        int returnCode = -1;
        try {
            Process process = autoconnect.get();
            returnCode = process.waitFor();
            returns = new String(process.getInputStream().readAllBytes());
            log.info("[{}]scheduleConnect return: {} message: {}",tag, returnCode, returns);
            process.destroy();
        } catch (InterruptedException | ExecutionException | IOException e) {
            log.warn("[{}]scheduleConnect launched error", tag, e);
            throw new RuntimeException(e);
        }
        ExpressvpnStatus expressvpnStatus = connect.status.status();

        History last = historyDao.findTopByMeshIndexOrderByTimeDesc(meshIndex);
        last.status = expressvpnStatus;
        last.meshIndex = meshIndex;
        historyDao.save(new History(last));

        if (Connected.equals(expressvpnStatus)) {
            log.info("[{}]schedule Connect done: {} message: {}",tag, expressvpnStatus, returns);
            return CompletableFuture.completedFuture(Connected);
        } else if (Busy.equals(expressvpnStatus)) {
            log.warn("[{}]schedule Connect is busy: {} message: {}",tag, expressvpnStatus, returns);
            return CompletableFuture.completedFuture(Busy);
        }

        log.info("[{}]schedule Connect run again: {} message: {}",tag, expressvpnStatus, returns);

        Connect.executor.execute(connect::init);

        return CompletableFuture.completedFuture(Connecting);
    }

    @Async
    @Scheduled(initialDelay = 1, fixedRate = 1, timeUnit = TimeUnit.MINUTES)
    public void checkStatus() {
        for (RunShell runShell : RunShell.mesh) {
            if(!runShell.getHost().enabled){
                log.info("skiped: {}", runShell);
                continue;
            }
            log.info("ready to check: {}", runShell);
            //noinspection resource
            Executors.newCachedThreadPool().execute(() -> {
                String tag=Integer.toHexString(new Object().hashCode());
                connect.setRunShell(runShell);
                int meshIndex = runShell.index;
                log.info("[{}]checkStatus run [{}]: {}",tag, meshIndex, LocalDateTime.now());
                ExpressvpnStatus expressvpnStatus = connect.status.status();
                History last = historyDao.findTopByMeshIndexOrderByTimeDesc(meshIndex);
                if (last == null) {
                    if(!connect.plan()){
                        throw new RuntimeException("no plan list to pick!");
                    }
                    last = new History();
                }
                String alias = last.location;
                String runShellLocation = runShell.getLocation(nodesDao);
                if (Connected.equals(expressvpnStatus) && !Objects.equals(alias, runShellLocation)) {
                    log.warn("[{}]checkStatus location sync: {} <++ {}",tag, alias, runShellLocation);
                    last.location = runShellLocation;
                } else if (!Reconnecting.equals(last.status)) {
                    last.time = LocalDateTime.now();
                }
                last.status = expressvpnStatus;
                if (Arrays.asList(Not_Connected, Unable_Connect, Unknown_Error).contains(expressvpnStatus)) {
                    String disconnect = runShell.disconnect();
                    log.info("[{}]checkStatus disconnect: {}",tag, disconnect);
                    last.status = Not_Connected;
                } else if (Arrays.asList(Connecting, Reconnecting).contains(expressvpnStatus)) {
                    LocalDateTime since = LocalDateTime.now();
                    for (History h : historyDao.findAllByMeshIndexOrderByTimeDesc(meshIndex)) {
                        ExpressvpnStatus status = h.status;
                        if (String.valueOf(alias).equals(h.location)) {
                            if (!status.equals(expressvpnStatus)) {
                                break;
                            }
                            since = h.time;
                            log.info("[{}]checkStatus lasts: {} {}", tag, status, since);
                            continue;
                        }
                        break;
                    }

                    long minutes = Duration.between(since, LocalDateTime.now()).toMinutes();
                    if (minutes > 10) {
                        String disconnect = runShell.disconnect();
                        log.info("[{}]checkStatus timeout: {} message: {}",tag, minutes, disconnect);
                        last.status = Not_Connected;
                    } else {
                        last.meshIndex = meshIndex;
                        historyDao.save(new History(last));
                        return;
                    }
                } else if (Connected.equals(expressvpnStatus)) {
                    log.info("[{}]checkStatus connected: [{}] {} <==> {}",tag, meshIndex, last.location, runShellLocation);
                    historyDao.save(new History(runShellLocation, Connected, meshIndex));
                    recycle.clearAndRePlan();
                    return;
                }

                last.meshIndex = meshIndex;
                historyDao.save(new History(last));
                scheduleConnect();
            });
        }
    }

    @Scheduled(initialDelay = 10, fixedRate = 15, timeUnit = TimeUnit.MINUTES)
    public void watchCat() {
        int meshIndex = checkIndex;
        RunShell runShell = RunShell.mesh[meshIndex];

        synchronized (connect) {
            connect.setRunShell(runShell);
            ExpressvpnStatus expressvpnStatus = connect.status.status();
            log.info("watchCat run [{}]: {}", meshIndex, expressvpnStatus);
            History history = historyDao.findTopByMeshIndexOrderByTimeDesc(meshIndex);
            if (Duration.between(history.time, LocalDateTime.now()).toMinutes() > 20) {
                runShell.disconnect();
                history.status = Not_Connected;
                history.meshIndex = meshIndex;
                historyDao.save(new History(history));
                ExecutorService executor = Connect.executor;
                Connect.executor = Executors.newCachedThreadPool();
                executor.shutdown();
                //noinspection resource
                Executors.newCachedThreadPool().submit(this::scheduleConnect);
                log.warn("watchCat restart connection! {}", runShell.index);

            } else {
                history.status = expressvpnStatus;
                history.meshIndex = meshIndex;
                historyDao.save(new History(history));
                //noinspection resource
                Executors.newCachedThreadPool().submit(this::checkStatus);
                log.warn("watchCat done! ");
            }

            checkIndex = connect.setRunShell(connect.getRunShell().getNext()).index;
            log.info("watchCat next run at index: [{}]", meshIndex);
        }
    }
}
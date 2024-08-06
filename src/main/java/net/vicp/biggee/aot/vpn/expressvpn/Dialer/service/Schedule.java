package net.vicp.biggee.aot.vpn.expressvpn.Dialer.service;

import lombok.extern.slf4j.Slf4j;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.History;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.HistoryDao;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.util.RunShell;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus.*;

@Slf4j
@Component
public class Schedule {
    final
    Connect connect;
    final
    HistoryDao historyDao;

    public Schedule(Connect connect, HistoryDao historyDao) {
        this.connect = connect;
        this.historyDao = historyDao;
    }

    @SuppressWarnings("UnusedReturnValue")
    @Async
    public CompletableFuture<ExpressvpnStatus> scheduleConnect() {
        log.info("scheduleConnect launched");
        Future<Process> autoconnect = connect.autoconnect();
        RunShell runShell = new RunShell();
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
        RunShell runShell = new RunShell();
        ExpressvpnStatus expressvpnStatus = runShell.status();
        History last = historyDao.findFirstByIdAfterOrderByIdDesc(-1);
        if (last == null) {
            //new init
            connect.plan();
            last = new History();
        }
        String alias = last.location;
        if (Connected.equals(expressvpnStatus) && !Objects.equals(alias, runShell.location)) {
            log.warn("checkStatus location sync: " + alias + " <++ " + runShell.location);
            last.location = runShell.location;
        } else if (Reconnecting.equals(last.status)) {
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
            }
        }
        historyDao.save(new History(last));

        scheduleConnect();
    }

}

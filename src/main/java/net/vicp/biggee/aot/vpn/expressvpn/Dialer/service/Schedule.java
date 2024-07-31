package net.vicp.biggee.aot.vpn.expressvpn.Dialer.service;

import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.History;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.HistoryDao;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.NodesDao;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.PlanDao;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.util.RunShell;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus.Connected;
import static net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus.Connecting;

@Component
public class Schedule {
    @Autowired
    Connect connect;
    @Autowired
    HistoryDao historyDao;
    @Autowired
    NodesDao nodesDao;
    @Autowired
    PlanDao planDao;

    @Async
    public ExpressvpnStatus scheduleConnect() {
        Future<Process> autoconnect = connect.autoconnect();
        RunShell runShell = new RunShell();
        String returns = null;
        int returnCode = -1;
        try {
            Process process = autoconnect.get();
            returnCode = process.waitFor();
            returns = new String(process.getInputStream().readAllBytes());
            process.destroy();
        } catch (InterruptedException | ExecutionException | IOException e) {
            throw new RuntimeException(e);
        }
        ExpressvpnStatus expressvpnStatus = runShell.status(returns);

        History last = historyDao.findFirstByIdAfterOrderByIdDesc(-1);
        last.status = expressvpnStatus;
        historyDao.save(last);

        if (expressvpnStatus.equals(Connected)) {
            return expressvpnStatus;
        }

        Connect.executor.execute(connect::init);

        return Connecting;
    }


}

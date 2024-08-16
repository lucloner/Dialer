package net.vicp.biggee.aot.vpn.expressvpn.Dialer.service;

import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.History;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.Node;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.HistoryDao;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.NodesDao;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus.Connected;
import static net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus.Unknown_Error;

@RestController
@RequestMapping("/status")
public class Status {
    final
    HistoryDao historyDao;
    final
    NodesDao nodesDao;
    Supplier<Connect> getConnect;

    public Status(HistoryDao historyDao, NodesDao nodesDao) {
        this.historyDao = historyDao;
        this.nodesDao = nodesDao;
    }

    @RequestMapping("/history")
    public List<History> getHistory() {
        return historyDao.findAll();
    }

    @RequestMapping("/nodes")
    public List<Node> getNodes() {
        return nodesDao.findAll();
    }

    @RequestMapping("/refresh")
    public String refresh() {
        String[] list = getConnect.get().runShell.flush();
        for (String s : list) {
            s = s.trim();
            String[] c = s.split(" ", 2);
            String alias = c[0].trim();
            if (nodesDao.existsById(alias)) {
                continue;
            }
            c[1] = c[1].trim();
            boolean recommended = c[1].endsWith("Y");
            String location = recommended ? c[1].substring(0, c[1].length() - 1).trim() : c[1];
            if (location.contains(")")) {
                location = location.split("\\)", 2)[1].trim();
            } else if (location.startsWith("Smart Location")) {
                location = "Smart Location " + location.split("Smart Location", 2)[1].trim();
            }
            nodesDao.save(new Node(alias, location, recommended));
        }
        return Arrays.toString(list);
    }

    @RequestMapping("/status")
    public ExpressvpnStatus status() {
        ExpressvpnStatus expressvpnStatus = getConnect.get().runShell.status();
        if (Connected.equals(expressvpnStatus)) {
            return getConnect.get().runShell.checkWebs() ? Connected : Unknown_Error;
        }
        return expressvpnStatus;
    }
}

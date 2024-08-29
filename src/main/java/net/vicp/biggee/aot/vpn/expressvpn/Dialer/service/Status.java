package net.vicp.biggee.aot.vpn.expressvpn.Dialer.service;

import lombok.extern.slf4j.Slf4j;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.History;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.Host;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.Node;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.HistoryDao;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.NodesDao;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.util.RunShell;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.function.Supplier;

import static net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus.Connected;
import static net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus.Unknown_Error;

@Slf4j
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
        List<String> list = getConnect.get().getRunShell().flush();
        log.info("refresh read: {}", list);
        for (String s : list) {
            s = s.trim();
            String[] c = s.split(" ", 2);
            String alias = c[0].trim();
            //noinspection ConstantValue
            if(alias==null||alias.isEmpty()){
                continue;
            }
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
            try {
                nodesDao.save(new Node(alias.trim(), location.trim(), recommended));
            } catch (Exception e) {
                log.error("nodesDao save error",e);
            }

//            log.info("read list: {}", s);
        }
        return list.toString();
    }

    @RequestMapping("/status")
    public ExpressvpnStatus status(@RequestParam(defaultValue = "0") int meshIndex) {
        RunShell runShell = RunShell.mesh[meshIndex];
        ExpressvpnStatus expressvpnStatus = runShell.status();
        if (Connected.equals(expressvpnStatus)) {
            return runShell.checkWebs() ? Connected : Unknown_Error;
        }
        return expressvpnStatus;
    }

    @RequestMapping("/listHost")
    public Host[] listHost() {
        return getConnect.get().getRunShell().getHosts();
    }

    @RequestMapping("/setHost")
    public Host setHost(@RequestParam(defaultValue = "0") int meshIndex, Host host) {
        host.enabled=true;
        listHost()[meshIndex]=host;
        return  listHost()[meshIndex];
    }

    @RequestMapping("/switchHost")
    public boolean switchHost(@RequestParam(defaultValue = "0") int meshIndex) {
        Host host =  listHost()[meshIndex];
        host.enabled=!host.enabled;
        return host.enabled;
    }
}

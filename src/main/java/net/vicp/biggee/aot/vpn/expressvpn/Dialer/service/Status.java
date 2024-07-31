package net.vicp.biggee.aot.vpn.expressvpn.Dialer.service;

import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.History;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.Nodes;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.HistoryDao;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.NodesDao;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.util.RunShell;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/status")
public class Status {
    @Autowired
    HistoryDao historyDao;
    @Autowired
    NodesDao nodesDao;

    @RequestMapping("/history")
    public List<History> getHistory() {
        return historyDao.findAll();
    }

    @RequestMapping("/nodes")
    public List<Nodes> getNodes() {
        return nodesDao.findAll();
    }

    @RequestMapping("/refresh")
    public String refresh() {
        String[] list = RunShell.flush();
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
            nodesDao.save(new Nodes(alias, location, recommended));
        }
        return Arrays.toString(list);
    }


}

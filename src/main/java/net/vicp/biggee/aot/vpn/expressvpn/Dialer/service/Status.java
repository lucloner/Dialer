package net.vicp.biggee.aot.vpn.expressvpn.Dialer.service;

import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.History;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.HistoryDao;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.NodesDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class Status {
    @Autowired
    HistoryDao historyDao;
    @Autowired
    NodesDao nodesDao;

    public void getData(){


    }

}

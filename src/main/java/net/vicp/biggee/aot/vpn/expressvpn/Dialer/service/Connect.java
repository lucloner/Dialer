package net.vicp.biggee.aot.vpn.expressvpn.Dialer.service;

import lombok.extern.slf4j.Slf4j;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.History;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.Node;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.Plan;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.HistoryDao;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.NodesDao;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.PlanDao;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.util.BashProcess;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.util.RunShell;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus.Connecting;

@Slf4j
@RestController
@RequestMapping("/connect")
public class Connect {
    final
    Status status;
    final
    HistoryDao historyDao;
    final
    NodesDao nodesDao;
    final
    PlanDao planDao;
    private RunShell runShell;

    public synchronized RunShell getRunShell() {
        return runShell;
    }

    @SuppressWarnings("unused")
    public synchronized RunShell setRunShell(RunShell runShell) {
        this.runShell = runShell;
        return runShell;
    }

    public Connect(Status status, HistoryDao historyDao, NodesDao nodesDao, PlanDao planDao, RunShell runShell) {
        this.status = status;
        this.historyDao = historyDao;
        this.nodesDao = nodesDao;
        this.planDao = planDao;
        this.runShell = runShell;
        status.getConnect=()->this;
    }

    @RequestMapping("/plan")
    public boolean plan() {
        List<Node> all = nodesDao.findAll();
        if (all.isEmpty()) {
            status.refresh();
            all = nodesDao.findAll();
        }
        Collections.shuffle(all);

        List<Node> recommended = nodesDao.findAll((r, q, b) -> b.equal(r.get("recommended"), true));
        Collections.shuffle(recommended);

        long count = planDao.count();
        Arrays.asList(recommended, all).forEach(l -> l.forEach(n -> {
            var alias = n.alias;
            if (planDao.exists((r, q, b) -> b.equal(r.get("alias"), alias))) {
                return;
            }
            planDao.save(new Plan(alias));
//            log.info("save plan: {}", alias);
        }));

        count=planDao.count()-count;
        return count>0 || planDao.count()>0;
    }

    @RequestMapping("/listPlan")
    public List<Plan> listPlan() {
        return planDao.findAll();
    }

    @RequestMapping("/pick")
    public Plan pick() {
        planDao.deleteAll(planDao.findAll((r, q, b) -> b.or(b.isNull(r.get("alias")), b.equal(r.get("alias"), ""))));
        long count = planDao.count();
        if (count == 0) {
            return null;
        }
        return planDao.findFirstBy();
    }

    public BashProcess connect(String alias) {
        return connect(runShell.index,alias);
    }

    @RequestMapping("/connect")
    public BashProcess connect(@RequestParam(defaultValue = "0") int meshIndex, @RequestParam(defaultValue = "") String alias) {
        RunShell rs= RunShell.mesh[meshIndex];
        ExpressvpnStatus expressvpnStatus = status.status(meshIndex);
        BashProcess connect = null;
        try {
            connect = rs.connect(alias);
        } catch (IOException e) {
            log.warn("[{}]run connect error to {}", meshIndex, alias);
        }
        if (connect != null) {
            planDao.deleteAll(planDao.findAll((r, q, b) -> b.equal(r.get("alias"), alias)));
            historyDao.save(new History(alias, Connecting, meshIndex));
        }
        return connect;
    }

    @RequestMapping("/autoConnect")
    public BashProcess autoConnect(@RequestParam(defaultValue = "0") int meshIndex) {
        Plan pick = pick();
        while (pick == null) {
            if(plan()){
                pick = pick();
            } else {
              throw new RuntimeException("no plan list to pick!");
            }
        }

        return connect(meshIndex,pick.alias);
    }

    @RequestMapping("/init")
    public ExpressvpnStatus init(@RequestParam(defaultValue = "0") int meshIndex) {
        plan();
        autoConnect(meshIndex);
        return status.status(meshIndex);
    }

    @RequestMapping("/switch")
    public int switchMesh(@RequestParam(defaultValue = "0") int meshIndex) {
        if(meshIndex<0||meshIndex>=RunShell.mesh.length){
            return runShell.index;
        }
        runShell=RunShell.mesh[meshIndex];
        return meshIndex;
    }
}

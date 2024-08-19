package net.vicp.biggee.aot.vpn.expressvpn.Dialer.service;

import lombok.extern.slf4j.Slf4j;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.History;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.Node;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.Plan;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.HistoryDao;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.NodesDao;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.PlanDao;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.util.RunShell;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus.*;

@Slf4j
@RestController
@RequestMapping("/connect")
public class Connect {

    public static ExecutorService executor = Executors.newCachedThreadPool();
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
        return planDao.findFirstBy();
    }

    @RequestMapping("/connect")
    public Future<Process> connect(String alias) {
        ExpressvpnStatus expressvpnStatus = status.status();
        if (Arrays.asList(Not_Connected, Unable_Connect, Unknown_Error).contains(expressvpnStatus)) {
            Future<Process> submit = executor.submit(() -> runShell.connect(alias));
            planDao.deleteAll(planDao.findAll((r, q, b) -> b.equal(r.get("alias"), alias)));
            historyDao.save(new History(alias, Connecting, runShell.index));
            return submit;
        }

        return null;
    }

    @RequestMapping("/autoconnect")
    public Future<Process> autoconnect() {
        Plan pick = pick();
        if (pick == null) {
            if(plan()){
                pick = pick();
            } else {
              throw new RuntimeException("no plan list to pick!");
            }
        }

        return connect(pick.alias);
    }

    @RequestMapping("/init")
    public ExpressvpnStatus init() {
        plan();
        autoconnect();
        return status.status();
    }

    @RequestMapping("/switch")
    public int switchMesh(int meshIndex) {
        if(meshIndex<0||meshIndex>=RunShell.mesh.length){
            return runShell.index;
        }
        runShell=RunShell.mesh[meshIndex];
        return meshIndex;
    }
}

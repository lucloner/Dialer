package net.vicp.biggee.aot.vpn.expressvpn.Dialer.service;

import lombok.extern.slf4j.Slf4j;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.History;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.Nodes;
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

    public Connect(Status status, HistoryDao historyDao, NodesDao nodesDao, PlanDao planDao) {
        this.status = status;
        this.historyDao = historyDao;
        this.nodesDao = nodesDao;
        this.planDao = planDao;
    }

    @RequestMapping("/plan")
    public boolean plan() {
        List<Nodes> all = nodesDao.findAll();
        if (all.isEmpty()) {
            status.refresh();
            all = nodesDao.findAll();
        }
        Collections.shuffle(all);

        List<Nodes> recommended = nodesDao.findAll((r, q, b) -> b.equal(r.get("recommended"), true));
        Collections.shuffle(recommended);

        Arrays.asList(recommended, all).forEach(l -> l.forEach(n -> {
            var alias = n.alias;
            if (planDao.exists((r, q, b) -> b.equal(r.get("alias"), alias))) {
                return;
            }
            planDao.save(new Plan(alias));
        }));

        return true;
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
        RunShell runShell = new RunShell();
        ExpressvpnStatus expressvpnStatus = status.status();
        if (Arrays.asList(Not_Connected, Unable_Connect, Unknown_Error).contains(expressvpnStatus)) {
            Future<Process> submit = executor.submit(() -> runShell.connect(alias));
            planDao.deleteAll(planDao.findAll((r, q, b) -> b.equal(r.get("alias"), alias)));
            historyDao.save(new History(alias, Connecting));
            return submit;
        }

        return null;
    }

    @RequestMapping("/autoconnect")
    public Future<Process> autoconnect() {
        Plan pick = pick();
        if (pick == null) {
            plan();
            pick = pick();
        }

        RunShell runShell = new RunShell();
        List<History> allByStatusOrderByTimeDesc = historyDao.findAllByStatusOrderByTimeDesc(Connecting);

        if (!allByStatusOrderByTimeDesc.isEmpty()) {
            allByStatusOrderByTimeDesc.subList(1, allByStatusOrderByTimeDesc.size()).forEach(c -> {
                c.status = Unable_Connect;
                historyDao.save(new History(c));
            });
            History first = allByStatusOrderByTimeDesc.get(0);
            first.status = status.status();
        }

        return connect(pick.alias);
    }

    @RequestMapping("/init")
    public ExpressvpnStatus init() {
        plan();
        autoconnect();
        return status.status();
    }
}

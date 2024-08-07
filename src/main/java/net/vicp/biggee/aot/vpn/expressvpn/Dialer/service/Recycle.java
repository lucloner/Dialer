package net.vicp.biggee.aot.vpn.expressvpn.Dialer.service;

import lombok.extern.slf4j.Slf4j;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.History;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.Plan;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.HistoryDao;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.PlanDao;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.concurrent.Executors;

import static net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus.Connected;

@Slf4j
@RestController
@RequestMapping("/recycle")
public class Recycle {
    final
    HistoryDao historyDao;
    final
    PlanDao planDao;

    public Recycle(HistoryDao historyDao, PlanDao planDao) {
        this.historyDao = historyDao;
        this.planDao = planDao;
    }

    @RequestMapping("/clear")
    public void clear() {
        List<History> all = historyDao.findAll();
        Set<String> removed = new HashSet<>();
        for (int i = all.size() - 1; i >= 0; i--) {
            History history = all.get(i);
            String location = history.location;
            if (removed.contains(location)) {
                historyDao.deleteById(history.id);
                log.info("Recycle: " + location + " is Duplicated, deleted id: " + history.id);
                continue;
            }
            removed.add(location);
            ExpressvpnStatus expressvpnStatus = history.status;
            if (Connected.equals(expressvpnStatus)) {
                long deleted = historyDao.delete((r, q, b) -> b.and(b.equal(r.get("location"), location),
                        b.notEqual(r.get("status"), Connected)));
                log.info("Recycle: " + location + " is Good, deleted: " + deleted);
            } else {
                long deleted = historyDao.delete((r, q, b) -> b.and(b.equal(r.get("location"), location),
                        b.equal(r.get("status"), Connected)));
                log.info("Recycle: " + location + " is Bad, deleted: " + deleted);
            }
        }

        log.info("Recycle Finished, Count" + historyDao.count());
    }

    @RequestMapping("/rePlan")
    public void rePlan() {
        List<History> all = historyDao.findAll((r, q, b) -> b.equal(r.get("status"), Connected));
        List<Plan> goodPlan = all.stream().map(h -> new Plan(h.location)).distinct().sorted(Comparator.comparing(p -> p.connectTime)).toList();
        for (int i = 1; i <= goodPlan.size(); i++) {
            Plan plan = goodPlan.get(goodPlan.size() - i);
            plan.id = i;
            Optional<Plan> orig = planDao.findById((long) i);
            orig.ifPresent(p -> planDao.save(new Plan(p.alias)));
            planDao.save(plan);
            log.info("rePlan insert: " + plan.alias + " to: " + i);
        }

        all = historyDao.findAll((r, q, b) -> b.notEqual(r.get("status"), Connected));
        for (History history : all) {
            long deleted = planDao.delete((r, q, b) -> b.equal(r.get("alias"), history.location));
            log.info("rePlan delete: " + history.location + " count: " + deleted);
        }

        planDao.findAll().forEach(p ->
                planDao.delete((r, q, b) ->
                        b.and(b.equal(r.get("alias"), p.alias),
                                b.greaterThan(r.get("id"), p.id))));

    }

    @RequestMapping("/clearAndRePlan")
    public void clearAndRePlan() {
        Executors.newSingleThreadExecutor().execute(() -> {
            clear();
            rePlan();
        });
    }
}

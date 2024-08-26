package net.vicp.biggee.aot.vpn.expressvpn.Dialer.service;

import lombok.extern.slf4j.Slf4j;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.History;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.Plan;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.HistoryDao;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo.PlanDao;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    final
    Connect connect;

    public Recycle(HistoryDao historyDao, PlanDao planDao, Connect connect) {
        this.historyDao = historyDao;
        this.planDao = planDao;
        this.connect = connect;
    }

    @RequestMapping("/clear")
    public void clear() {
        int meshIndex = connect.getRunShell().index;
        List<History> all = historyDao.findAll();
        Set<String> removed = new HashSet<>();
        for (int i = all.size() - 1; i >= 0; i--) {
            History history = all.get(i);
            String location = history.location;
            ExpressvpnStatus expressvpnStatus = history.status;
            if (removed.contains(location) && !Connected.equals(history.status)) {
                historyDao.deleteById(history.id);
                log.info("Recycle [{}]: {} is Duplicated, deleted id: {}", meshIndex, location, history.id);
                continue;
            }
            removed.add(location);
            if (Connected.equals(expressvpnStatus)) {
                long deleted = historyDao.delete((r, q, b) -> b.and(b.equal(r.get("location"), location),
                        b.notEqual(r.get("status"), Connected)));

                deleted += historyDao.delete((r, q, b) -> b.and(b.equal(r.get("location"), location),
                        b.equal(r.get("status"), Connected),b.lt(r.get("id"),history.id)));

                if(deleted>0){
                    log.info("Recycle [{}]: {} is Good, deleted: {}",meshIndex, location, deleted);
                }
            } else {
                History last = historyDao.findTopByMeshIndexOrderByTimeDesc(meshIndex);
                long deleted = historyDao.delete((r, q, b) -> b.and(b.equal(r.get("location"), location),
                        b.notEqual(r.get("status"), Connected),
                        b.equal(r.get("meshIndex"), meshIndex),
                        b.lt(r.get("id"),last.id)));
                if(deleted>0){
                    log.info("Recycle [{}]: {} is Bad, deleted: {}",meshIndex, location, deleted);
                }
            }
        }

        log.info("Recycle Finished, Count {}", historyDao.count());
    }

    @RequestMapping("/rePlan")
    public void rePlan() {
        List<Plan> all = planDao.findAll();
        for (Plan p : all) {
            long deleted = planDao.delete((r, q, b) -> b.and(b.gt(r.get("id"), p.id), b.equal(r.get("alias"), p.alias)));
            if (deleted > 0) {
                log.info("Recycle Plan: {} is Duplicated, deleted: {}", p.alias, deleted);
            }
        }

        historyDao.findAll((r, q, b) -> b.equal(r.get("status"), Connected))
                .stream()
                .parallel()
                .map(History::getLocation)
                .forEach(l->planDao.save(new Plan(l)));
    }

    @RequestMapping("/clearAndRePlan")
    public void clearAndRePlan() {
        //noinspection resource
        Executors.newSingleThreadExecutor().execute(() -> {
            clear();
            rePlan();
        });
    }
}

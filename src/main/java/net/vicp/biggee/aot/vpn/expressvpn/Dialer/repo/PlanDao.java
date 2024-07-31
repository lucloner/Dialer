package net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo;

import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.Plan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PlanDao extends JpaRepository<Plan, Long>, JpaSpecificationExecutor<Plan> {
    Plan findFirstBy();
}

package net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo;

import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.History;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistoryDao extends JpaRepository<History,Long>, JpaSpecificationExecutor<History> {
    List<History> findAllByStatusOrderByTimeDesc(ExpressvpnStatus status);

    History findFirstByIdAfterOrderByIdDesc(long id);
}

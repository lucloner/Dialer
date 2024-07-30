package net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo;

import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.History;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface HistoryDao extends JpaRepository<History,Long>, JpaSpecificationExecutor<History> {
}

package net.vicp.biggee.aot.vpn.expressvpn.Dialer.repo;

import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.History;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.data.Nodes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface NodesDao extends JpaRepository<Nodes,String>, JpaSpecificationExecutor<Nodes> {
}

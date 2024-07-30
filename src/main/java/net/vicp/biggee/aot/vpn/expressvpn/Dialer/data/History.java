package net.vicp.biggee.aot.vpn.expressvpn.Dialer.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus;

import java.time.LocalDateTime;

@Entity
public class History {
    @Id
    public long id=System.nanoTime();
    @Column
    public String location;
    @Column
    public LocalDateTime time=LocalDateTime.now();
    @Column
    public ExpressvpnStatus status=ExpressvpnStatus.Not_Connected;
}

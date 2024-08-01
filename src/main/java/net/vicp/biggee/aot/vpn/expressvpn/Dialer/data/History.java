package net.vicp.biggee.aot.vpn.expressvpn.Dialer.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus;

import java.time.LocalDateTime;

@Entity
public class History {
    @Id
    @GeneratedValue
    public long id;
    @Column
    public String location;
    @Column
    public LocalDateTime time=LocalDateTime.now();
    @Column
    public ExpressvpnStatus status=ExpressvpnStatus.Not_Connected;

    public History() {
    }

    public History(String location, ExpressvpnStatus status) {
        this.location = location;
        this.status = status;
    }

    public History(History history) {
        this.id = history.id;
        this.location = history.location;
        this.time = history.time;
        this.status = history.status;
    }
}

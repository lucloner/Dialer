package net.vicp.biggee.aot.vpn.expressvpn.Dialer.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.Getter;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus;

import java.time.LocalDateTime;

@Entity
public class History {
    @Id
    @GeneratedValue
    public long id;
    @Getter
    @Column
    public String location;
    @Column
    public LocalDateTime time=LocalDateTime.now();
    @Column
    public ExpressvpnStatus status=ExpressvpnStatus.Not_Connected;
    @Column
    public int meshIndex;

    public History() {
    }

    public History(String location, ExpressvpnStatus status,int meshIndex) {
        this.location = location;
        this.status = status;
        this.meshIndex=meshIndex;
    }

    public History(History history) {
        this.id = -1;
        this.location = history.location;
        this.time = history.time;
        this.status = history.status;
        this.meshIndex= history.meshIndex;
    }
}

package net.vicp.biggee.aot.vpn.expressvpn.Dialer.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums.ExpressvpnStatus;

import java.time.LocalDateTime;

@Entity
public class Plan {
    @Id
    @GeneratedValue
    public long id;
    @Column
    public String alias;
    @Column
    public LocalDateTime planTime = LocalDateTime.now();
    @Column
    public LocalDateTime connectTime = LocalDateTime.now();
    @Column
    public ExpressvpnStatus status = ExpressvpnStatus.Not_Connected;

    public Plan() {
    }

    public Plan(String alias) {
        this.alias = alias;
    }

    public Plan(Plan plan) {
        this.id = plan.id;
        this.alias = plan.alias;
        this.planTime = plan.planTime;
        this.connectTime = plan.connectTime;
        this.status = plan.status;
    }
}

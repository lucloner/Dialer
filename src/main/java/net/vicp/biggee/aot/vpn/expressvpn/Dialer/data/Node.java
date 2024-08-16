package net.vicp.biggee.aot.vpn.expressvpn.Dialer.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Node {
    @Id
    public String alias;
    @Column
    public String location;
    @Column
    public boolean recommended=false;

    public Node(){}
    public Node(String alias, String location, String recommended) {
        this.alias = alias;
        this.location = location;
        this.recommended= recommended.contains("Y");
    }

    public Node(String alias, String location, boolean recommended) {
        this.alias = alias;
        this.location = location;
        this.recommended= recommended;
    }
}

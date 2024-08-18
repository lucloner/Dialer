package net.vicp.biggee.aot.vpn.expressvpn.Dialer.data;

import lombok.Data;

@Data
public class Host {
    public String name="unnamed";
    public String command="expressvpn";
    public boolean isLocalHost=true;
    public String proxyHost="127.0.0.1";
    public int proxyPort=1080;
    public boolean enabled=true;

    @SuppressWarnings("unused")
    public Host(){}

    @SuppressWarnings("unused")
    public Host(int proxyPort, String proxyHost, boolean isLocalHost, String command, String name) {
        this.proxyPort = proxyPort;
        this.proxyHost = proxyHost;
        this.isLocalHost = isLocalHost;
        this.command = command;
        this.name = name;
    }
}

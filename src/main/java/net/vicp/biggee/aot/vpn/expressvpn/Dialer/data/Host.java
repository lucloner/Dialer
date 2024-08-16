package net.vicp.biggee.aot.vpn.expressvpn.Dialer.data;

import lombok.Data;

@Data
public class Host {
    public String name="unnamed";
    public String command="expressvpn";
    public boolean isLocalHost=true;
    public String proxyHost="127.0.0.1";
    public int proxyPort=1080;
}

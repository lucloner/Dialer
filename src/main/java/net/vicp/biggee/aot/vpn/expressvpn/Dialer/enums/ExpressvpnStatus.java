package net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums;

public enum ExpressvpnStatus {
    Not_Connected("Not connected",1),
    Connected("Connected to",0),
    Connecting("Connecting to",2),
    Reconnecting("Reconnecting...",3),
    Unable_Connect("Unable to connect",4),
    Unknown_Error(null,99),
    Upgradeable("update ExpressVPN",6),
    Upgradeable_Arch("new version",7),
    Halt("expressvpnd", 8),
    Busy("Please disconnect", 9);

    public String key;
    public int code;

    ExpressvpnStatus(String key, int code){
        this.key=key;
        this.code=code;
    }
}

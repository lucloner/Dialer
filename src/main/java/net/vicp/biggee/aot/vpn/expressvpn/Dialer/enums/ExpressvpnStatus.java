package net.vicp.biggee.aot.vpn.expressvpn.Dialer.enums;

@SuppressWarnings("SpellCheckingInspection")
public enum ExpressvpnStatus {
    Not_Connected("Not connected",1),
    Connected("Connected to",0),
    Connecting("Connecting",2,300),
    Reconnecting("Reconnecting...",3,600),
    Unable_Connect("Unable to connect",4),
    Unknown_Error(null,99),
    Upgradeable("update ExpressVPN",6),
    Upgradeable_Arch("new version",7),
    Halt("expressvpnd", 8),
    Busy("Please disconnect", 9,60);

    public final String key;
    public final int code;
    public int timeout=1800;

    ExpressvpnStatus(String key, int code){
        this.key=key;
        this.code=code;
    }

    ExpressvpnStatus(String key, int code,int timeout){
        this(key,code);
        this.timeout=timeout;
    }
}

package net.vicp.biggee.aot.vpn.expressvpn.Dialer.util;

import org.junit.jupiter.api.Test;

import java.util.List;

class BashProcessTest {

    @Test
    void run() throws InterruptedException {
        BashProcess p = new BashProcess();
        List<String> l = p.runSync("ls");
        l.addAll(p.runSync("ssdsfsdf"));
        l.addAll(p.runSync("expressvpn list all"));
        l.addAll(p.runSync("ssh 192.168.4.90 ls"));
        Thread.sleep(10000);
        l.addAll(p.readAll());
        l.forEach(System.out::println);
    }
}
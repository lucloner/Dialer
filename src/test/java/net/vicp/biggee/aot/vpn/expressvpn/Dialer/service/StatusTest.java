package net.vicp.biggee.aot.vpn.expressvpn.Dialer.service;

import net.vicp.biggee.aot.vpn.expressvpn.Dialer.util.RunShell;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class StatusTest {
    @Test
    public void test1(){
        var l=new RunShell().getList();
        System.out.println(Arrays.toString(l));
    }
}
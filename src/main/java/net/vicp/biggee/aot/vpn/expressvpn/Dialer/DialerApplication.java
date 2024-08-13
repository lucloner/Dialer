package net.vicp.biggee.aot.vpn.expressvpn.Dialer;

import net.vicp.biggee.aot.vpn.expressvpn.Dialer.util.RunShell;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(RunShell.class)
public class DialerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DialerApplication.class, args);
	}

}

package net.vicp.biggee.aot.vpn.expressvpn.Dialer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DialerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DialerApplication.class, args);
	}

}

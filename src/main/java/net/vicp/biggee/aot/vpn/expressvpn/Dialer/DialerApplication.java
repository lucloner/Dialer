package net.vicp.biggee.aot.vpn.expressvpn.Dialer;

import net.vicp.biggee.aot.vpn.expressvpn.Dialer.util.RunShell;
import org.apache.zookeeper.server.quorum.QuorumPeer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(RunShell.class)
public class DialerApplication {

	public static void main(String[] args) {
		System.setProperty(QuorumPeer.CONFIG_KEY_MULTI_ADDRESS_ENABLED, "true");
		SpringApplication.run(DialerApplication.class, args);
	}

}

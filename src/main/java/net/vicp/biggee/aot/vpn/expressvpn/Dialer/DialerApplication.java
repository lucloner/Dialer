package net.vicp.biggee.aot.vpn.expressvpn.Dialer;

import net.vicp.biggee.aot.vpn.expressvpn.Dialer.util.RunShell;
import org.apache.zookeeper.server.quorum.QuorumPeer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(RunShell.class)
@Configuration
public class DialerApplication implements AsyncConfigurer {

	public static ExecutorService executors=Executors.newCachedThreadPool();

	public static void main(String[] args) {
		System.setProperty(QuorumPeer.CONFIG_KEY_MULTI_ADDRESS_ENABLED, "true");
		SpringApplication.run(DialerApplication.class, args);
	}

	@Override
	public Executor getAsyncExecutor() {
		return task -> executors.execute(task);
	}
}

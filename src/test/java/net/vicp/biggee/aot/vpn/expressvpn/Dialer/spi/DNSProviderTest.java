package net.vicp.biggee.aot.vpn.expressvpn.Dialer.spi;

import com.google.common.collect.Range;
import net.vicp.biggee.aot.vpn.expressvpn.Dialer.util.RunShell;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.Assert;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class DNSProviderTest {

    private static int dnsCheck(String dns) {
        String url = "https://www.baidu.com";
        RunShell zero = new RunShell();
        ReflectionTestUtils.setField(RunShell.class, "mesh", new RunShell[]{zero});
        zero.setDns(dns);
        zero.setUrls("https://www.youtube.com");
        zero.setUrls(url);

        HttpClient.Builder builder = HttpClient.newBuilder();
        HttpClient build = builder.followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofMinutes(1))
                .build();
        try {
            return build.send(HttpRequest.newBuilder(URI.create(url))
                            .GET()
                            .timeout(Duration.ofMinutes(1))
                            .build(), HttpResponse.BodyHandlers.ofString())
                    .statusCode();
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testGet() {
        Range<Integer> range = Range.closedOpen(200, 300);
        Assert.isTrue(range.test(dnsCheck("114.114.114.114")), "test baidu from dns 114");
    }
}
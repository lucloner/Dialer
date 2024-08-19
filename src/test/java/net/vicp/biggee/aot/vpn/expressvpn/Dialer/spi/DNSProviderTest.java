package net.vicp.biggee.aot.vpn.expressvpn.Dialer.spi;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class DNSProviderTest {

    @Test
    public void testGet() {
        HttpClient.Builder builder = HttpClient.newBuilder();
        HttpClient build = builder.followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofMinutes(1))
                .build();
        try {
            System.out.println(build
                    .send(HttpRequest.newBuilder(URI.create("https://www.baidu.com"))
                            .GET()
                            .timeout(Duration.ofMinutes(1))
                            .build(), HttpResponse.BodyHandlers.ofString())
                    .statusCode());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
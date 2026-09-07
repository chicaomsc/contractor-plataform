package io.chicaodw.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Bean;

import java.net.http.HttpClient;
import java.time.Clock;
import java.time.Duration;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlatformApplication.class, args);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Single shared client for outbound HTTP calls to third-party APIs — today only
     * {@code ResendEmailService}. A short connect timeout so a provider outage fails
     * fast instead of tying up the calling thread; the per-request timeout (Resend
     * call) is set separately on each {@code HttpRequest}.
     */
    @Bean
    public HttpClient httpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }
}

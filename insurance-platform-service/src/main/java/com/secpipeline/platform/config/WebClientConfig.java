package com.secpipeline.platform.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableConfigurationProperties(PlatformProperties.class)
public class WebClientConfig {
    @Bean
    WebClient aiGatewayWebClient(PlatformProperties properties) {
        Duration responseTimeout = Duration.ofSeconds(properties.aiGatewayResponseTimeoutSeconds());
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(responseTimeout);

        return WebClient.builder()
                .baseUrl(properties.aiGatewayBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeaders(headers -> {
                    headers.setBearerAuth(properties.aiGatewayToken());
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                })
                .build();
    }
}

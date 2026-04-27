package com.secpipeline.platform.config;

import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(PlatformProperties.class)
public class WebClientConfig {
    @Bean
    WebClient aiGatewayWebClient(PlatformProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.aiGatewayBaseUrl())
                .defaultHeaders(headers -> {
                    headers.setBearerAuth(properties.aiGatewayToken());
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
                })
                .build();
    }
}

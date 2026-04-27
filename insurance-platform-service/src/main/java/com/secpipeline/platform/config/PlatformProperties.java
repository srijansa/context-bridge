package com.secpipeline.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "platform")
public record PlatformProperties(
        String aiGatewayBaseUrl,
        String aiGatewayToken
) {
}

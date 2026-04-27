package com.secpipeline.platform.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "platform")
public record PlatformProperties(
        @NotBlank
        String aiGatewayBaseUrl,
        @NotBlank
        String aiGatewayToken
) {
}

package com.secpipeline.platform.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "platform")
public record PlatformProperties(
        @NotBlank
        @Pattern(regexp = "https?://.+", message = "must start with http:// or https://")
        String aiGatewayBaseUrl,
        @NotBlank
        String aiGatewayToken,
        @Min(1)
        @Max(300)
        Integer aiGatewayResponseTimeoutSeconds
) {
    // Default timeout for testing
    private static final int DEFAULT_AI_GATEWAY_RESPONSE_TIMEOUT_SECONDS = 60;

    public PlatformProperties {
        if (aiGatewayResponseTimeoutSeconds == null) {
            aiGatewayResponseTimeoutSeconds = DEFAULT_AI_GATEWAY_RESPONSE_TIMEOUT_SECONDS;
        }
    }
}

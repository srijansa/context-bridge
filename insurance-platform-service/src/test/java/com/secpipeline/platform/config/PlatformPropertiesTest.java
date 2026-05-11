package com.secpipeline.platform.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class PlatformPropertiesTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void defaultsGatewayResponseTimeoutWhenMissing() {
        PlatformProperties properties = new PlatformProperties(
                "http://localhost:8081",
                "internal-token",
                null);

        assertThat(properties.aiGatewayResponseTimeoutSeconds()).isEqualTo(60);
    }

    @Test
    void validatesGatewayBaseUrlRequiresHttpScheme() {
        PlatformProperties properties = new PlatformProperties(
                "localhost:8081",
                "internal-token",
                60);

        assertThat(validator.validate(properties))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("aiGatewayBaseUrl"));
    }

    @Test
    void validatesGatewayResponseTimeoutRange() {
        PlatformProperties properties = new PlatformProperties(
                "http://localhost:8081",
                "internal-token",
                301);

        assertThat(validator.validate(properties))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("aiGatewayResponseTimeoutSeconds"));
    }
}

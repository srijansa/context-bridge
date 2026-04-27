package com.secpipeline.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record AiAssistRequest(
        @NotBlank String caseId,
        @NotBlank String policyNumber,
        @NotBlank String workflowType,
        @NotNull @NotEmpty Map<String, Object> businessContext
) {
}

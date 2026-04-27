package com.secpipeline.platform.dto;

import java.util.List;
import java.util.Map;

public record McpGatewayRequest(
        String correlationId,
        String workflowType,
        String userRole,
        String caseId,
        String policyNumber,
        List<String> allowedTools,
        String redactionPolicy,
        Map<String, Object> businessContext
) {
}

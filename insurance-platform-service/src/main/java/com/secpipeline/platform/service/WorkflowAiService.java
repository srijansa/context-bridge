package com.secpipeline.platform.service;

import com.secpipeline.platform.dto.AiAssistRequest;
import com.secpipeline.platform.dto.AiAssistResponse;
import com.secpipeline.platform.dto.McpGatewayRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class WorkflowAiService {
    private final WebClient aiGatewayWebClient;
    private final AuditLogger auditLogger;

    public WorkflowAiService(WebClient aiGatewayWebClient, AuditLogger auditLogger) {
        this.aiGatewayWebClient = aiGatewayWebClient;
        this.auditLogger = auditLogger;
    }

    public AiAssistResponse invokeGateway(AiAssistRequest request, String userRole) {
        String correlationId = UUID.randomUUID().toString();
        auditLogger.workflowRequested(correlationId, userRole, request.workflowType(), request.caseId());

        McpGatewayRequest gatewayRequest = new McpGatewayRequest(
                correlationId,
                request.workflowType(),
                userRole,
                request.caseId(),
                request.policyNumber(),
                allowedToolsFor(request.workflowType()),
                "INSURANCE_STANDARD",
                request.businessContext());

        AiAssistResponse response = aiGatewayWebClient.post()
                .uri("/internal/mcp/execute")
                .bodyValue(gatewayRequest)
                .retrieve()
                .bodyToMono(AiAssistResponse.class)
                .block();

        auditLogger.workflowCompleted(correlationId, request.workflowType(), "SUCCESS");
        return response;
    }

    private List<String> allowedToolsFor(String workflowType) {
        return switch (workflowType.toLowerCase()) {
            case "underwriting" -> List.of("risk-score", "policy-rules", "document-summary");
            case "claims" -> List.of("claim-summary", "fraud-signal-review", "settlement-guidance");
            default -> List.of("document-summary");
        };
    }
}

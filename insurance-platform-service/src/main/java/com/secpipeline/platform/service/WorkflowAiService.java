package com.secpipeline.platform.service;

import com.secpipeline.platform.dto.AiAssistRequest;
import com.secpipeline.platform.dto.AiAssistResponse;
import com.secpipeline.platform.dto.McpGatewayRequest;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class WorkflowAiService {
    private final WebClient aiGatewayWebClient;
    private final AuditLogger auditLogger;

    public WorkflowAiService(WebClient aiGatewayWebClient, AuditLogger auditLogger) {
        this.aiGatewayWebClient = aiGatewayWebClient;
        this.auditLogger = auditLogger;
    }

    public AiAssistResponse invokeGateway(AiAssistRequest request, String userRole, String routeWorkflowType) {
        validateWorkflowType(request.workflowType(), routeWorkflowType);

        String correlationId = UUID.randomUUID().toString();
        auditLogger.workflowRequested(correlationId, userRole, routeWorkflowType, request.caseId());

        McpGatewayRequest gatewayRequest = new McpGatewayRequest(
                correlationId,
                routeWorkflowType,
                userRole,
                request.caseId(),
                request.policyNumber(),
                allowedToolsFor(routeWorkflowType),
                "INSURANCE_STANDARD",
                request.businessContext());

        try {
            AiAssistResponse response = aiGatewayWebClient.post()
                    .uri("/internal/mcp/execute")
                    .header("X-Correlation-Id", correlationId)
                    .bodyValue(gatewayRequest)
                    .retrieve()
                    .bodyToMono(AiAssistResponse.class)
                    .block();

            auditLogger.workflowCompleted(correlationId, routeWorkflowType, "SUCCESS");
            return response;
        } catch (RuntimeException exception) {
            auditLogger.workflowFailed(correlationId, routeWorkflowType, exception.getClass().getSimpleName());
            throw exception;
        }
    }

    private List<String> allowedToolsFor(String workflowType) {
        return switch (workflowType.toLowerCase(Locale.ROOT)) {
            case "underwriting" -> List.of("risk-score", "policy-rules", "document-summary");
            case "claims" -> List.of("claim-summary", "fraud-signal-review", "settlement-guidance");
            default -> List.of("document-summary");
        };
    }

    private void validateWorkflowType(String requestedWorkflowType, String routeWorkflowType) {
        if (!routeWorkflowType.equalsIgnoreCase(requestedWorkflowType)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "workflowType must match the secured API route");
        }
    }
}

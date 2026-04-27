package com.secpipeline.platform.service;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AuditLogger {
    private static final Logger audit = LoggerFactory.getLogger("AUDIT");

    public void workflowRequested(String correlationId, String role, String workflowType, String caseId) {
        audit.info("event=workflow_ai_requested correlationId={} role={} workflowType={} caseId={} timestamp={}",
                correlationId, role, workflowType, caseId, Instant.now());
    }

    public void workflowCompleted(String correlationId, String workflowType, String status) {
        audit.info("event=workflow_ai_completed correlationId={} workflowType={} status={} timestamp={}",
                correlationId, workflowType, status, Instant.now());
    }

    public void workflowFailed(String correlationId, String workflowType, String reason) {
        audit.warn("event=workflow_ai_failed correlationId={} workflowType={} reason={} timestamp={}",
                correlationId, workflowType, reason, Instant.now());
    }
}

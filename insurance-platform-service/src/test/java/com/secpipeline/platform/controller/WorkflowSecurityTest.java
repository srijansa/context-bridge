package com.secpipeline.platform.controller;

import com.secpipeline.platform.dto.AiAssistResponse;
import com.secpipeline.platform.dto.AiAssistRequest;
import com.secpipeline.platform.config.SecurityConfig;
import com.secpipeline.platform.service.WorkflowAiService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ClaimsController.class, UnderwritingController.class})
@ContextConfiguration(classes = {
        ClaimsController.class,
        UnderwritingController.class,
        SecurityConfig.class,
        WorkflowSecurityTest.TestBeans.class
})
class WorkflowSecurityTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FakeWorkflowAiService workflowAiService;

    @Test
    void claimsEndpointRejectsUnauthenticatedRequest() throws Exception {
        mockMvc.perform(post("/api/claims/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(claimsPayload()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "UNDERWRITER")
    void claimsEndpointRejectsWrongRole() throws Exception {
        mockMvc.perform(post("/api/claims/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(claimsPayload()))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CLAIMS_ADJUSTER")
    void claimsEndpointUsesServerControlledWorkflowType() throws Exception {
        mockMvc.perform(post("/api/claims/summarize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(claimsPayload()))
                .andExpect(status().isOk());

        assertThat(workflowAiService.lastUserRole).isEqualTo("CLAIMS_ADJUSTER");
        assertThat(workflowAiService.lastRouteWorkflowType).isEqualTo("claims");
    }

    @Test
    @WithMockUser(roles = "CLAIMS_ADJUSTER")
    void underwritingEndpointRejectsWrongRole() throws Exception {
        mockMvc.perform(post("/api/underwriting/assist")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(underwritingPayload()))
                .andExpect(status().isForbidden());
    }

    private String claimsPayload() {
        return """
                {
                  "caseId": "CASE-1",
                  "policyNumber": "POL-1",
                  "workflowType": "claims",
                  "businessContext": {
                    "claimType": "auto",
                    "lossDescription": "Rear-end collision",
                    "damageEstimate": 30000
                  }
                }
                """;
    }

    private String underwritingPayload() {
        return """
                {
                  "caseId": "CASE-2",
                  "policyNumber": "POL-2",
                  "workflowType": "underwriting",
                  "businessContext": {
                    "coverageAmount": 250000,
                    "priorClaims": 1,
                    "policyType": "home"
                  }
                }
                """;
    }

    private AiAssistResponse response() {
        return new AiAssistResponse(
                "correlation-id",
                "summary",
                "recommendation",
                List.of(),
                List.of("claim-summary"),
                Instant.now());
    }

    @TestConfiguration
    static class TestBeans {
        @Bean
        FakeWorkflowAiService workflowAiService() {
            return new FakeWorkflowAiService();
        }
    }

    static class FakeWorkflowAiService extends WorkflowAiService {
        private String lastUserRole;
        private String lastRouteWorkflowType;

        FakeWorkflowAiService() {
            super(null, null);
        }

        @Override
        public AiAssistResponse invokeGateway(
                AiAssistRequest request,
                String userRole,
                String routeWorkflowType) {
            this.lastUserRole = userRole;
            this.lastRouteWorkflowType = routeWorkflowType;
            return new AiAssistResponse(
                    "correlation-id",
                    "summary",
                    "recommendation",
                    List.of(),
                    List.of("claim-summary"),
                    Instant.now());
        }
    }
}

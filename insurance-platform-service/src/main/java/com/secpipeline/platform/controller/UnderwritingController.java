package com.secpipeline.platform.controller;

import com.secpipeline.platform.dto.AiAssistRequest;
import com.secpipeline.platform.dto.AiAssistResponse;
import com.secpipeline.platform.service.WorkflowAiService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/underwriting")
public class UnderwritingController {
    private final WorkflowAiService workflowAiService;

    public UnderwritingController(WorkflowAiService workflowAiService) {
        this.workflowAiService = workflowAiService;
    }

    @PostMapping("/assist")
    @PreAuthorize("hasRole('UNDERWRITER')")
    public AiAssistResponse assist(@Valid @RequestBody AiAssistRequest request) {
        return workflowAiService.invokeGateway(request, "UNDERWRITER");
    }
}

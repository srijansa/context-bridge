package com.secpipeline.platform.dto;

import java.time.Instant;
import java.util.List;

public record AiAssistResponse(
        String correlationId,
        String summary,
        String recommendation,
        List<String> warnings,
        List<String> toolCalls,
        Instant completedAt
) {
}

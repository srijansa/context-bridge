from __future__ import annotations

import logging
import time

from app.models import McpExecutionResult, McpGatewayRequest, SanitizedPayload

telemetry = logging.getLogger("prompt_telemetry")


def log_execution(
    request: McpGatewayRequest,
    sanitized: SanitizedPayload,
    result: McpExecutionResult,
    started_at: float,
) -> None:
    telemetry.info(
        "event=mcp_execution correlationId=%s workflowType=%s userRole=%s "
        "redactionCount=%s removedFields=%s toolCalls=%s latencyMs=%s",
        request.correlationId,
        request.workflowType,
        request.userRole,
        sanitized.redactionCount,
        sanitized.removedFields,
        result.toolCalls,
        round((time.monotonic() - started_at) * 1000),
    )

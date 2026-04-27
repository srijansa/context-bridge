from __future__ import annotations

from datetime import datetime, timezone
from typing import Any

from pydantic import BaseModel, Field


class McpGatewayRequest(BaseModel):
    correlationId: str
    workflowType: str
    userRole: str
    caseId: str
    policyNumber: str
    allowedTools: list[str] = Field(min_length=1)
    redactionPolicy: str
    businessContext: dict[str, Any] = Field(min_length=1)


class SanitizedPayload(BaseModel):
    context: dict[str, Any]
    redactionCount: int
    removedFields: list[str]


class McpExecutionResult(BaseModel):
    summary: str
    recommendation: str
    warnings: list[str]
    toolCalls: list[str]


class AiAssistResponse(BaseModel):
    correlationId: str
    summary: str
    recommendation: str
    warnings: list[str]
    toolCalls: list[str]
    completedAt: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))

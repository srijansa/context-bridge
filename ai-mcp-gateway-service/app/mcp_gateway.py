from __future__ import annotations

from app.models import McpExecutionResult, McpGatewayRequest, SanitizedPayload
from app.tools import execute_allowed_tools


def build_prompt(request: McpGatewayRequest, sanitized: SanitizedPayload) -> str:
    instruction = {
        "underwriting": "Create an underwriting risk summary and decision-support recommendation.",
        "claims": "Create a claims summary with fraud indicators and settlement-support guidance.",
    }.get(
        request.workflowType.lower(),
        "Summarize the workflow context and provide decision-support guidance.",
    )

    return (
        "You are assisting an insurance operations workflow.\n"
        f"Workflow: {request.workflowType}\n"
        f"User role: {request.userRole}\n"
        f"Case ID: {request.caseId}\n"
        f"Policy number: {request.policyNumber}\n"
        f"Instruction: {instruction}\n"
        "Use only sanitized context and approved tool outputs. Do not expose PII.\n"
        f"Sanitized context: {sanitized.context}\n"
    )


def execute_mcp_workflow(
    request: McpGatewayRequest,
    sanitized: SanitizedPayload,
) -> McpExecutionResult:
    prompt = build_prompt(request, sanitized)
    tool_results = execute_allowed_tools(request.allowedTools, sanitized.context)

    summary = (
        f"Generated {request.workflowType} assistance using "
        f"{len(sanitized.context)} approved context fields and {len(tool_results)} MCP tools."
    )
    recommendation = _recommendation_for(request.workflowType, tool_results)
    warnings = ["Claude API call is stubbed; MCP tools are executed locally through the Python SDK layer."]

    if not tool_results:
        warnings.append("No allowed MCP tools were available for this request.")

    if prompt:
        warnings.append("Prompt assembled from sanitized context only.")

    return McpExecutionResult(
        summary=summary,
        recommendation=recommendation,
        warnings=warnings,
        toolCalls=list(tool_results.keys()),
    )


def _recommendation_for(workflow_type: str, tool_results: dict[str, object]) -> str:
    if workflow_type.lower() == "underwriting":
        risk = tool_results.get("risk-score", {})
        if isinstance(risk, dict):
            return f"Route underwriting case for {risk.get('riskBand', 'STANDARD')} risk review."
        return "Route underwriting case for standard review."

    if workflow_type.lower() == "claims":
        fraud = tool_results.get("fraud-signal-review", {})
        if isinstance(fraud, dict) and fraud.get("requiresReview"):
            return "Escalate claim for fraud review before settlement action."
        return "Proceed with adjuster review using generated claim summary."

    return "Review AI output with a workflow owner before final action."

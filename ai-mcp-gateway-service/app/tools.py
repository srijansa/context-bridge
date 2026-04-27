from __future__ import annotations

from typing import Any, Callable

from mcp.server.fastmcp import FastMCP

mcp = FastMCP("insurance-mcp-gateway")


@mcp.tool(name="risk-score")
def risk_score(coverage_amount: float = 0, prior_claims: int = 0) -> dict[str, Any]:
    score = min(95, 35 + int(coverage_amount / 10000) + prior_claims * 12)
    band = "LOW" if score < 45 else "MEDIUM" if score < 75 else "HIGH"
    return {
        "riskScore": score,
        "riskBand": band,
        "reason": "Estimated from coverage amount and prior claim count.",
    }


@mcp.tool(name="policy-rules")
def policy_rules(policy_type: str = "standard") -> dict[str, Any]:
    return {
        "policyType": policy_type,
        "rulesApplied": ["coverage-limit-check", "prior-loss-review", "manual-approval-threshold"],
    }


@mcp.tool(name="document-summary")
def document_summary(documents: list[str] | None = None) -> dict[str, Any]:
    documents = documents or []
    return {
        "documentCount": len(documents),
        "summary": "Reviewed sanitized document metadata and extracted workflow-relevant signals.",
    }


@mcp.tool(name="claim-summary")
def claim_summary(claim_type: str = "unknown", loss_description: str = "") -> dict[str, Any]:
    return {
        "claimType": claim_type,
        "summary": loss_description[:240] or "No loss description supplied.",
    }


@mcp.tool(name="fraud-signal-review")
def fraud_signal_review(damage_estimate: float = 0, loss_description: str = "") -> dict[str, Any]:
    signals = []
    if damage_estimate > 25000:
        signals.append("high-damage-estimate")
    if "stolen" in loss_description.lower():
        signals.append("theft-related-loss")
    return {
        "requiresReview": bool(signals),
        "signals": signals,
    }


@mcp.tool(name="settlement-guidance")
def settlement_guidance(damage_estimate: float = 0) -> dict[str, Any]:
    reserve = round(damage_estimate * 0.85, 2)
    return {
        "suggestedReserve": reserve,
        "guidance": "Use as decision support only; final settlement requires adjuster approval.",
    }


ToolFn = Callable[[dict[str, Any]], dict[str, Any]]


def execute_allowed_tools(allowed_tools: list[str], context: dict[str, Any]) -> dict[str, Any]:
    registry: dict[str, ToolFn] = {
        "risk-score": lambda ctx: risk_score(
            coverage_amount=float(ctx.get("coverageAmount", 0) or 0),
            prior_claims=int(ctx.get("priorClaims", 0) or 0),
        ),
        "policy-rules": lambda ctx: policy_rules(policy_type=str(ctx.get("policyType", "standard"))),
        "document-summary": lambda ctx: document_summary(documents=ctx.get("documents", [])),
        "claim-summary": lambda ctx: claim_summary(
            claim_type=str(ctx.get("claimType", "unknown")),
            loss_description=str(ctx.get("lossDescription", "")),
        ),
        "fraud-signal-review": lambda ctx: fraud_signal_review(
            damage_estimate=float(ctx.get("damageEstimate", 0) or 0),
            loss_description=str(ctx.get("lossDescription", "")),
        ),
        "settlement-guidance": lambda ctx: settlement_guidance(
            damage_estimate=float(ctx.get("damageEstimate", 0) or 0)
        ),
    }

    results: dict[str, Any] = {}
    for tool_name in allowed_tools:
        tool = registry.get(tool_name)
        if tool:
            results[tool_name] = tool(context)
    return results

from __future__ import annotations

import re
from typing import Any

from app.models import McpGatewayRequest, SanitizedPayload

SSN = re.compile(r"\b\d{3}-\d{2}-\d{4}\b")
EMAIL = re.compile(r"\b[\w.%+-]+@[\w.-]+\.[A-Za-z]{2,}\b")
PHONE = re.compile(r"\b(?:\+1[-.\s]?)?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}\b")

SENSITIVE_KEYS = {
    "ssn",
    "socialsecuritynumber",
    "dob",
    "dateofbirth",
    "phone",
    "email",
    "address",
    "name",
    "policyholdername",
    "bankaccount",
    "routingnumber",
    "medicalnotes",
}

ALLOWED_FIELDS = {
    "underwriting": {
        "riskFactors",
        "coverageAmount",
        "policyType",
        "priorClaims",
        "propertyDetails",
    },
    "claims": {
        "claimType",
        "lossDescription",
        "incidentDate",
        "damageEstimate",
        "documents",
    },
}


def sanitize_payload(request: McpGatewayRequest) -> SanitizedPayload:
    allowed = ALLOWED_FIELDS.get(
        request.workflowType.lower(),
        {"documents", "lossDescription", "riskFactors"},
    )
    context: dict[str, Any] = {}
    removed_fields: list[str] = []
    redaction_count = 0

    for key, value in request.businessContext.items():
        if key not in allowed:
            removed_fields.append(key)
            continue

        redacted, count = _redact_value(key, value)
        context[key] = redacted
        redaction_count += count

    return SanitizedPayload(
        context=context,
        redactionCount=redaction_count,
        removedFields=removed_fields,
    )


def _redact_value(key: str, value: Any) -> tuple[Any, int]:
    normalized_key = re.sub(r"[^A-Za-z0-9]", "", key).lower()
    if normalized_key in SENSITIVE_KEYS:
        return f"[REDACTED_{normalized_key.upper()}]", 1

    if isinstance(value, str):
        return _redact_text(value)

    if isinstance(value, dict):
        output: dict[str, Any] = {}
        total = 0
        for nested_key, nested_value in value.items():
            redacted, count = _redact_value(str(nested_key), nested_value)
            output[str(nested_key)] = redacted
            total += count
        return output, total

    if isinstance(value, list):
        output = []
        total = 0
        for item in value:
            redacted, count = _redact_value(key, item)
            output.append(redacted)
            total += count
        return output, total

    return value, 0


def _redact_text(text: str) -> tuple[str, int]:
    count = 0

    def replace_ssn(_: re.Match[str]) -> str:
        nonlocal count
        count += 1
        return "[REDACTED_SSN]"

    def replace_email(_: re.Match[str]) -> str:
        nonlocal count
        count += 1
        return "[REDACTED_EMAIL]"

    def replace_phone(_: re.Match[str]) -> str:
        nonlocal count
        count += 1
        return "[REDACTED_PHONE]"

    redacted = SSN.sub(replace_ssn, text)
    redacted = EMAIL.sub(replace_email, redacted)
    redacted = PHONE.sub(replace_phone, redacted)
    return redacted, count

from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_gateway_rejects_missing_bearer_token():
    response = client.post("/internal/mcp/execute", json=_claims_request())

    assert response.status_code == 401


def test_gateway_rejects_invalid_bearer_token():
    response = client.post(
        "/internal/mcp/execute",
        headers={"Authorization": "Bearer invalid"},
        json=_claims_request(),
    )

    assert response.status_code == 403


def test_gateway_redacts_and_executes_only_allowed_tools():
    response = client.post(
        "/internal/mcp/execute",
        headers={"Authorization": "Bearer dev-internal-token"},
        json=_claims_request(),
    )

    body = response.json()
    assert response.status_code == 200
    assert body["correlationId"] == "test-correlation-id"
    assert body["toolCalls"] == ["claim-summary", "fraud-signal-review"]
    assert "Prompt assembled from sanitized context only." in body["warnings"]


def _claims_request():
    return {
        "correlationId": "test-correlation-id",
        "workflowType": "claims",
        "userRole": "CLAIMS_ADJUSTER",
        "caseId": "CASE-1001",
        "policyNumber": "POL-2001",
        "allowedTools": ["claim-summary", "fraud-signal-review"],
        "redactionPolicy": "INSURANCE_STANDARD",
        "businessContext": {
            "claimType": "auto",
            "lossDescription": "Contact test@example.com after collision.",
            "damageEstimate": 30000,
            "ssn": "123-45-6789",
        },
    }

from __future__ import annotations

import base64
import json
import os

from fastapi import Header, HTTPException, status


def require_internal_token(authorization: str | None = Header(default=None)) -> None:
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing bearer token",
        )

    token = authorization.removeprefix("Bearer ").strip()
    expected_token = os.getenv("INTERNAL_SERVICE_TOKEN", "dev-internal-token")
    if token == expected_token:
        return

    scopes = _unsafe_read_jwt_scopes(token)
    if "ai.invoke" in scopes:
        return

    raise HTTPException(
        status_code=status.HTTP_403_FORBIDDEN,
        detail="Token must include ai.invoke scope",
    )


def _unsafe_read_jwt_scopes(token: str) -> set[str]:
    """Dev-only JWT scope extraction. Production should verify signature and issuer."""
    parts = token.split(".")
    if len(parts) < 2:
        return set()

    try:
        padded = parts[1] + "=" * (-len(parts[1]) % 4)
        payload = json.loads(base64.urlsafe_b64decode(padded))
    except (ValueError, json.JSONDecodeError):
        return set()

    scope = payload.get("scope", "")
    scopes = set(str(scope).split())
    scopes.update(map(str, payload.get("scp", [])))
    return scopes

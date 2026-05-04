from __future__ import annotations

import hmac
import os

from fastapi import Header, HTTPException, status


def require_internal_token(authorization: str | None = Header(default=None)) -> None:
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Missing bearer token",
        )

    token = authorization.removeprefix("Bearer ").strip()
    expected_token = _expected_internal_token()
    if hmac.compare_digest(token, expected_token):
        return

    raise HTTPException(
        status_code=status.HTTP_403_FORBIDDEN,
        detail="Invalid internal service token",
    )


def _expected_internal_token() -> str:
    token = os.getenv("INTERNAL_SERVICE_TOKEN")
    app_env = os.getenv("APP_ENV", "local").lower()

    if token:
        return token

    if app_env == "local":
        return "dev-internal-token"

    raise RuntimeError("INTERNAL_SERVICE_TOKEN must be configured outside local environments")

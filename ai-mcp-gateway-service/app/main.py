from __future__ import annotations

import logging
import time

from fastapi import Depends, FastAPI, Request
from fastapi.responses import JSONResponse

from app.mcp_gateway import execute_mcp_workflow
from app.models import AiAssistResponse, McpGatewayRequest
from app.sanitizer import sanitize_payload
from app.security import require_internal_token
from app.telemetry import log_execution
from app.tools import mcp

logging.basicConfig(level=logging.INFO)

app = FastAPI(title="SecPipeline AI/MCP Gateway", version="0.1.0")


@app.get("/actuator/health")
def health() -> dict[str, str]:
    return {"status": "UP"}


@app.exception_handler(Exception)
async def unhandled_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    correlation_id = request.headers.get("X-Correlation-Id")
    logging.getLogger("gateway_errors").exception(
        "event=gateway_unhandled_exception correlationId=%s path=%s",
        correlation_id,
        request.url.path,
    )
    body = {
        "detail": "Request could not be processed",
        "path": request.url.path,
    }
    if correlation_id:
        body["correlationId"] = correlation_id
    return JSONResponse(status_code=500, content=body)


@app.post(
    "/internal/mcp/execute",
    response_model=AiAssistResponse,
    dependencies=[Depends(require_internal_token)],
)
def execute(request: McpGatewayRequest) -> AiAssistResponse:
    started_at = time.monotonic()
    sanitized = sanitize_payload(request)
    result = execute_mcp_workflow(request, sanitized)
    log_execution(request, sanitized, result, started_at)

    return AiAssistResponse(
        correlationId=request.correlationId,
        summary=result.summary,
        recommendation=result.recommendation,
        warnings=result.warnings,
        toolCalls=result.toolCalls,
    )


if __name__ == "__main__":
    mcp.run()

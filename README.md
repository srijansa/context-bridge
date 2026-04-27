# SecPipeline

SecPipeline is a two-service reference architecture for AI-assisted insurance underwriting and claims workflows. It uses a Java Spring Boot platform for enterprise workflow security and a Python AI/MCP gateway for Model Context Protocol tools, redaction, filtering, prompt assembly, and telemetry.

## What Is Here

This repository contains two application services:

- `insurance-platform-service`: Java Spring Boot service for underwriting and claims APIs. It uses Spring Security RBAC, validates JWT bearer tokens, creates workflow correlation IDs, emits audit logs, and calls the AI/MCP gateway.
- `ai-mcp-gateway-service`: Python FastAPI service that exposes the internal AI gateway endpoint, implements MCP-style insurance tools using the official MCP Python SDK, filters payloads, redacts PII, assembles prompts, executes approved tools, and emits prompt telemetry.

Supporting files:

- `pom.xml`: root Maven parent for the Java service.
- `.github/workflows/ci.yml`: CI pipeline for Java compile/test and Python checks.
- `.gitignore`: ignores Java build output, Python virtualenv/build artifacts, generated packaging files, logs, IDE files, and secrets.
- `docs/architecture.md`: shorter architecture reference.

## High-Level Architecture

```text
User or workflow client
  -> Java Spring Boot Insurance Platform
      -> Spring Security validates JWT
      -> RBAC checks UNDERWRITER / CLAIMS_ADJUSTER / ADMIN
      -> platform creates correlation ID
      -> platform chooses allowed MCP tools for workflow
      -> platform calls Python AI/MCP Gateway
          -> gateway validates internal bearer token
          -> gateway filters business payload by workflow allowlist
          -> gateway redacts PII
          -> gateway builds controlled prompt
          -> gateway executes only approved MCP tools
          -> gateway logs prompt telemetry
      -> platform returns structured AI assistance response
```

Claude is not implemented as a local service in this repo. The current Python gateway has the MCP tool layer and a stubbed model-response path. A real Claude adapter would be added inside the Python gateway after redaction, filtering, and allowed-tool execution.

## Final System Design

```text
                         User / Frontend / API Client
                                  |
                                  | HTTPS + JWT bearer token
                                  v
          +------------------------------------------------+
          | Java Spring Boot Insurance Platform Service    |
          |------------------------------------------------|
          | Port: 8080                                     |
          | Uses:                                          |
          | - Spring Boot Web                              |
          | - Spring Security                              |
          | - OAuth2 Resource Server / JWT                 |
          | - RBAC: UNDERWRITER, CLAIMS_ADJUSTER, ADMIN    |
          | - WebClient for service-to-service HTTP        |
          | - Actuator health checks                       |
          | - Maven and Docker                             |
          |                                                |
          | Owns:                                          |
          | - Underwriting APIs                            |
          | - Claims APIs                                  |
          | - User authorization                           |
          | - Correlation IDs                              |
          | - Allowed MCP tool selection                   |
          | - Audit logging                                |
          +------------------------------------------------+
                                  |
                                  | Internal HTTP POST
                                  | Authorization: Bearer AI_GATEWAY_TOKEN
                                  v
          +------------------------------------------------+
          | Python AI/MCP Gateway Service                  |
          |------------------------------------------------|
          | Port: 8081                                     |
          | Uses:                                          |
          | - Python                                       |
          | - FastAPI                                      |
          | - Pydantic                                     |
          | - Uvicorn                                      |
          | - MCP Python SDK                               |
          | - Docker                                       |
          |                                                |
          | Owns:                                          |
          | - Internal AI endpoint                         |
          | - Internal token validation                    |
          | - Payload allowlisting                         |
          | - PII redaction                                |
          | - Prompt construction                          |
          | - MCP tool execution                           |
          | - Prompt telemetry                             |
          +------------------------------------------------+
                                  |
                                  | Future provider adapter
                                  v
          +------------------------------------------------+
          | Claude / External AI Provider                  |
          |------------------------------------------------|
          | Status: not implemented yet                     |
          | Future use:                                    |
          | - receive sanitized prompt/context             |
          | - use approved MCP tool outputs                |
          | - return summary/recommendation                |
          +------------------------------------------------+
```

Current implementation status:

```text
Implemented:
- Java Spring Boot platform service
- Spring Security RBAC configuration
- Python FastAPI AI/MCP gateway
- MCP Python SDK tool definitions
- PII redaction and payload filtering
- Service-to-service request contract
- Dockerfiles for both services
- GitHub Actions CI

Not implemented yet:
- real Claude API/model invocation
- Terraform infrastructure
- AWS ECR image publishing
- ECS/EKS runtime deployment
- CloudWatch/OpenSearch integration
- production-grade JWT validation in the Python gateway
```

## How The Services Communicate

The Java service communicates with the Python service over HTTP.

Local addresses:

```text
Java Insurance Platform: http://localhost:8080
Python AI/MCP Gateway:   http://localhost:8081
```

The Java service calls:

```text
POST http://localhost:8081/internal/mcp/execute
```

The base URL comes from:

```text
AI_GATEWAY_BASE_URL=http://localhost:8081
```

The bearer token comes from:

```text
AI_GATEWAY_TOKEN=dev-internal-token
```

In Java, the `WebClient` is configured in `WebClientConfig.java`:

```java
WebClient.builder()
    .baseUrl(properties.aiGatewayBaseUrl())
    .defaultHeaders(headers -> headers.setBearerAuth(properties.aiGatewayToken()))
    .build();
```

The platform service then sends the gateway request in `WorkflowAiService.java`:

```java
aiGatewayWebClient.post()
    .uri("/internal/mcp/execute")
    .bodyValue(gatewayRequest)
    .retrieve()
    .bodyToMono(AiAssistResponse.class)
    .block();
```

The Python gateway receives that request in `app/main.py`:

```python
@app.post("/internal/mcp/execute", response_model=AiAssistResponse)
def execute(request: McpGatewayRequest) -> AiAssistResponse:
    sanitized = sanitize_payload(request)
    result = execute_mcp_workflow(request, sanitized)
    return AiAssistResponse(...)
```

Communication sequence:

```text
1. User calls Java underwriting or claims API with a JWT bearer token.
2. Spring Security validates the token and checks the user's role.
3. Java creates a correlation ID.
4. Java maps the workflow to allowed MCP tools.
5. Java sends a JSON request to the Python gateway.
6. Java includes `Authorization: Bearer <AI_GATEWAY_TOKEN>`.
7. Python validates the internal bearer token.
8. Python filters and redacts the payload.
9. Python executes only the allowed MCP tools.
10. Python returns a structured JSON response.
11. Java returns that response to the caller.
```

In production, this call should happen on a private network path, such as service discovery inside ECS/EKS, an internal load balancer, or a service mesh. The Python gateway should not be directly exposed to the public internet.

## Current AWS And Deployment Position

This repo does not currently provision or deploy AWS resources. AWS is part of the recommended deployment architecture, not active runtime code.

Currently included:

```text
- Local Java service
- Local Python service
- Dockerfile for each service
- GitHub Actions CI
- AWS deployment design documentation
```

Recommended AWS deployment:

```text
GitHub
  -> GitHub Actions
      -> build Java and Python services
      -> build Docker images
      -> push images to Amazon ECR
          -> deploy to ECS Fargate or EKS
              -> public/internal ALB routes to Java platform
                  -> private call to Python AI/MCP gateway
                      -> future outbound call to Claude / AI provider
```

Recommended AWS services:

```text
Amazon ECR:
- Stores Docker images for both services.

Amazon ECS Fargate or Amazon EKS:
- Runs the Java and Python services.

Application Load Balancer:
- Exposes the Java insurance platform.
- The Python gateway should stay private.

VPC and private subnets:
- Isolate the Python AI/MCP gateway from public traffic.

AWS Secrets Manager or SSM Parameter Store:
- Stores JWT issuer config, internal service tokens, and future Claude API keys.

CloudWatch Logs:
- Stores application logs, audit logs, and telemetry logs.

OpenSearch or ELK:
- Searches audit logs and prompt telemetry.

IAM roles:
- Controls service permissions.

Route 53:
- DNS and failover routing for multi-region deployments.

Terraform:
- Recommended for provisioning the infrastructure.

AWS CodeBuild / CodeDeploy:
- Optional if the enterprise deployment process requires AWS-native build or release stages.
```

## Why Two Services

The Java service owns the enterprise workflow boundary:

- user authentication
- RBAC authorization
- underwriting and claims APIs
- audit logging
- service-to-service gateway calls

The Python service owns the AI/MCP boundary:

- MCP tool definitions
- payload filtering
- PII redaction
- prompt construction
- tool execution
- prompt telemetry
- future Claude integration

This keeps insurance workflow security separate from AI execution logic.

## Service 1: Insurance Platform

Path:

```text
insurance-platform-service/
```

Main responsibilities:

- Exposes underwriting and claims workflow APIs.
- Uses Spring Security as an OAuth2 resource server.
- Converts JWT `roles`, `groups`, and `scope` claims into Spring Security authorities.
- Protects endpoints using route-level and method-level authorization.
- Calls the Python AI/MCP gateway using `WebClient`.
- Emits audit logs with correlation IDs.

Important files:

- `InsurancePlatformApplication.java`: Spring Boot entry point.
- `SecurityConfig.java`: JWT resource server and RBAC rules.
- `WorkflowAiService.java`: creates correlation IDs, selects allowed tools, calls the gateway.
- `AuditLogger.java`: structured audit logging.
- `UnderwritingController.java`: underwriting AI endpoint.
- `ClaimsController.java`: claims AI endpoint.
- `McpGatewayRequest.java`: request contract sent to the Python gateway.

Current endpoints:

```text
POST /api/underwriting/assist
POST /api/claims/summarize
GET  /actuator/health
```

RBAC mapping:

```text
/api/underwriting/** requires ROLE_UNDERWRITER
/api/claims/**        requires ROLE_CLAIMS_ADJUSTER
/api/admin/**         requires ROLE_ADMIN
```

## Service 2: Python AI/MCP Gateway

Path:

```text
ai-mcp-gateway-service/
```

Main responsibilities:

- Receives internal requests from the Java platform.
- Validates the internal bearer token.
- Filters fields using workflow-specific allowlists.
- Redacts PII before any AI/model/tool path.
- Defines MCP tools with the Python MCP SDK.
- Executes only the tools allowed by the Java platform.
- Emits prompt telemetry.

Important files:

- `app/main.py`: FastAPI app and `/internal/mcp/execute` endpoint.
- `app/models.py`: request/response models.
- `app/security.py`: internal bearer-token check.
- `app/sanitizer.py`: PII redaction and payload filtering.
- `app/tools.py`: MCP tool definitions and tool registry.
- `app/mcp_gateway.py`: prompt assembly and workflow execution.
- `app/telemetry.py`: prompt/tool telemetry logging.

Current endpoints:

```text
POST /internal/mcp/execute
GET  /actuator/health
```

Implemented MCP tools:

```text
risk-score
policy-rules
document-summary
claim-summary
fraud-signal-review
settlement-guidance
```

## Request Contract

The Java platform sends this shape to the Python gateway:

```json
{
  "correlationId": "2d25c6f2-8d08-44c8-b3b6-705c22d55df0",
  "workflowType": "claims",
  "userRole": "CLAIMS_ADJUSTER",
  "caseId": "CASE-1001",
  "policyNumber": "POL-2001",
  "allowedTools": ["claim-summary", "fraud-signal-review", "settlement-guidance"],
  "redactionPolicy": "INSURANCE_STANDARD",
  "businessContext": {
    "claimType": "auto",
    "lossDescription": "Rear-end collision. Contact test@example.com.",
    "damageEstimate": 30000
  }
}
```

The gateway returns:

```json
{
  "correlationId": "2d25c6f2-8d08-44c8-b3b6-705c22d55df0",
  "summary": "Generated claims assistance using approved context and MCP tools.",
  "recommendation": "Escalate claim for fraud review before settlement action.",
  "warnings": ["Prompt assembled from sanitized context only."],
  "toolCalls": ["claim-summary", "fraud-signal-review"],
  "completedAt": "2026-04-26T12:00:00Z"
}
```

## Security Model

User-facing security is handled by Spring Security in the Java service.

Expected JWT claims:

```json
{
  "sub": "user-123",
  "roles": ["UNDERWRITER"],
  "scope": "profile.read case.read"
}
```

The Java service converts:

```text
UNDERWRITER -> ROLE_UNDERWRITER
ai.invoke   -> SCOPE_ai.invoke
```

Service-to-service security is handled by a bearer token between Java and Python.

Local development default:

```text
dev-internal-token
```

Production recommendation:

- Use OAuth2 client credentials, signed JWT, or mTLS.
- Validate issuer, audience, signature, expiry, and `ai.invoke` scope in the Python gateway.
- Store secrets in AWS Secrets Manager, Parameter Store, Vault, or the platform equivalent.

## Payload Filtering And PII Redaction

The Python gateway filters payloads before tool/model execution.

For underwriting, allowed fields include:

```text
riskFactors
coverageAmount
policyType
priorClaims
propertyDetails
```

For claims, allowed fields include:

```text
claimType
lossDescription
incidentDate
damageEstimate
documents
```

Sensitive values are redacted, including:

```text
SSN
email
phone number
date of birth
address
policyholder name
bank account
routing number
medical notes
```

Example:

```text
test@example.com -> [REDACTED_EMAIL]
123-45-6789     -> [REDACTED_SSN]
```

## Tool Authorization Flow

The Java platform decides which tools are allowed.

In underwriting workflows:

```text
risk-score
policy-rules
document-summary
```

In claims workflows:

```text
claim-summary
fraud-signal-review
settlement-guidance
```

The Python gateway executes only the tools included in `allowedTools`. This prevents a user with claims access from invoking underwriting-only tools through the AI path.

## Local Setup

Prerequisites:

- Java 21 or newer
- Maven
- Python 3.11 or newer

Build the Java service:

```bash
mvn -Dmaven.repo.local=.m2/repository verify
```

Set up the Python gateway:

```bash
cd ai-mcp-gateway-service
python3 -m venv .venv
source .venv/bin/activate
pip install -e .
```

## Run Locally

Terminal 1, start the Python AI/MCP gateway:

```bash
cd ai-mcp-gateway-service
source .venv/bin/activate
INTERNAL_SERVICE_TOKEN=dev-internal-token uvicorn app.main:app --host 0.0.0.0 --port 8081
```

Terminal 2, start the Java insurance platform:

```bash
mvn -Dmaven.repo.local=.m2/repository -pl insurance-platform-service spring-boot:run
```

Health checks:

```bash
curl http://127.0.0.1:8081/actuator/health
curl http://127.0.0.1:8080/actuator/health
```

Expected response:

```json
{"status":"UP"}
```

## Configuration

Java platform environment variables:

```text
JWT_ISSUER_URI=http://localhost:9000/realms/secpipeline
AI_GATEWAY_BASE_URL=http://localhost:8081
AI_GATEWAY_TOKEN=dev-internal-token
```

Python gateway environment variables:

```text
INTERNAL_SERVICE_TOKEN=dev-internal-token
```

## Testing The Gateway Directly

You can call the Python gateway directly with the local development token:

```bash
curl -X POST http://127.0.0.1:8081/internal/mcp/execute \
  -H "Authorization: Bearer dev-internal-token" \
  -H "Content-Type: application/json" \
  -d '{
    "correlationId": "local-test-1",
    "workflowType": "claims",
    "userRole": "CLAIMS_ADJUSTER",
    "caseId": "CASE-1001",
    "policyNumber": "POL-2001",
    "allowedTools": ["claim-summary", "fraud-signal-review", "settlement-guidance"],
    "redactionPolicy": "INSURANCE_STANDARD",
    "businessContext": {
      "claimType": "auto",
      "lossDescription": "Rear-end collision. Contact test@example.com.",
      "damageEstimate": 30000,
      "ssn": "123-45-6789"
    }
  }'
```

The `ssn` field is not on the claims allowlist, so it is removed. The email inside `lossDescription` is redacted before tool execution.

## CI/CD

The GitHub Actions workflow:

- checks out the repository
- installs Java
- runs Maven verification for the Java service
- installs Python
- installs the Python gateway dependencies
- runs Python compile checks
- runs Ruff linting

In a production pipeline, the next steps would be:

- build Docker images for both services
- push images to Amazon ECR
- provision infrastructure with Terraform
- deploy to ECS Fargate or EKS
- publish logs to CloudWatch
- ship audit and telemetry logs to ELK/OpenSearch

## Deployment Model

Recommended AWS deployment:

```text
Route 53 / ALB
  -> insurance-platform-service on ECS Fargate or EKS
      -> private network call
          -> ai-mcp-gateway-service on ECS Fargate or EKS
              -> Claude / external AI provider
```

Recommended production controls:

- Keep the Python gateway private, not internet-facing.
- Use IAM roles and least privilege.
- Store tokens and API keys in a secrets manager.
- Enforce TLS for all service-to-service traffic.
- Validate JWTs fully in both services.
- Avoid logging raw prompts with PII.
- Log correlation IDs, tool calls, redaction counts, latency, and outcomes.

## Implementation Steps

1. Create the Java Spring Boot insurance platform service.
2. Add Spring Web, Spring Security, OAuth2 Resource Server, Validation, Actuator, and WebClient.
3. Configure Spring Security route-level RBAC.
4. Enable method-level security with `@PreAuthorize`.
5. Define underwriting and claims controllers.
6. Define request/response DTOs.
7. Add `WorkflowAiService` to create correlation IDs and call the gateway.
8. Add `AuditLogger` for structured workflow audit events.
9. Create the Python FastAPI AI/MCP gateway.
10. Add the official MCP Python SDK dependency.
11. Define the internal `/internal/mcp/execute` endpoint.
12. Add internal bearer-token validation.
13. Mirror the Java gateway request contract with Pydantic models.
14. Add workflow-specific payload allowlists.
15. Add PII redaction for sensitive keys and patterns.
16. Define MCP tools in Python with `@mcp.tool`.
17. Create a tool registry that maps allowed tool names to implementations.
18. Execute only tools provided in the Java request allowlist.
19. Build prompts using sanitized context and tool outputs only.
20. Return structured AI assistance responses to Java.
21. Emit prompt telemetry with correlation ID, redaction count, removed fields, tool calls, and latency.
22. Add Dockerfiles for both services.
23. Add CI to build Java and check Python.
24. Add deployment documentation for AWS ECS/EKS.
25. Replace the stubbed Claude path with a real model adapter when credentials and provider choice are finalized.

## Current Limitations

- Claude/model invocation is stubbed.
- Python gateway token validation supports a local shared token and dev-only JWT scope parsing. Production should perform full JWT validation.
- Java protected workflow endpoints require a real JWT issuer and valid user token.
- No database is included yet for policies, claims, users, or persistent audit storage.

## Resume Summary

This repo demonstrates a two-service AI integration pattern:

```text
Java Spring Boot + Spring Security RBAC
+ Python FastAPI MCP Gateway
+ PII redaction and payload filtering
+ MCP insurance tools
+ audit logging and prompt telemetry
```

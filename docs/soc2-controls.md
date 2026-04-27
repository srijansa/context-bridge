# SOC 2 Control Mapping

This document maps the current SecPipeline implementation to practical SOC 2 readiness controls. It is not a SOC 2 report or legal/compliance certification; it is an engineering control inventory that supports a future audit.

## Current Implemented Controls

### Access Control

- Java workflow APIs require JWT bearer-token authentication.
- Spring Security RBAC restricts underwriting endpoints to `UNDERWRITER`.
- Spring Security RBAC restricts claims endpoints to `CLAIMS_ADJUSTER`.
- Admin route pattern is reserved for `ADMIN`.
- Python AI/MCP Gateway requires an internal bearer token.

### Tool Authorization

- Java controllers provide a server-controlled workflow type.
- `WorkflowAiService` rejects mismatches between the request body and secured route.
- Allowed MCP tools are selected from trusted route context, not untrusted client input.
- Python gateway executes only tools listed in the Java allowlist.

### Data Protection

- Python gateway filters payloads by workflow-specific allowlists.
- Python gateway redacts SSNs, emails, phone numbers, and sensitive field names.
- Raw sensitive payloads are not emitted in normal audit or telemetry logs.

### Auditability

- Java platform emits workflow request, success, and failure audit events.
- Java platform propagates `X-Correlation-Id` to the Python gateway.
- Python gateway emits telemetry with correlation ID, workflow type, redaction count, removed fields, tool calls, and latency.

### Safe Failure Handling

- Java service has centralized error handling with safe `ProblemDetail` responses.
- Python gateway has a generic exception handler that returns a safe 500 response.
- Error responses avoid stack traces and raw request payloads.

### CI / Security Checks

- Java security behavior is covered by controller tests.
- Python gateway token and tool execution behavior is covered by tests.
- CI runs Java tests, Python tests, Ruff linting, `pip-audit`, and Trivy filesystem scanning.
- CI upgrades `pip` before dependency auditing to avoid known tooling vulnerabilities.

## Remaining Production Controls

### Identity And Service Authentication

- Replace local `dev-internal-token` with OAuth2 client credentials, signed JWT, or mTLS.
- Python gateway should validate JWT issuer, audience, signature, expiration, and `ai.invoke` scope.
- User identity provider should be Okta, Azure AD, Cognito, Keycloak, or another managed IdP.

### Secrets

- Move gateway tokens and future Claude credentials into AWS Secrets Manager, SSM Parameter Store, Vault, or an equivalent secrets manager.
- Rotate service credentials periodically.
- Restrict secret access with least-privilege IAM.

### Logging And Monitoring

- Ship Java audit logs to CloudWatch and OpenSearch/ELK.
- Ship Python prompt telemetry to CloudWatch and OpenSearch/ELK.
- Alert on authentication failures, unexpected tool combinations, gateway failures, PII redaction failures, and latency spikes.

### Infrastructure

- Keep the Python AI/MCP Gateway private.
- Expose only the Java Insurance Platform through a public or customer-facing load balancer.
- Enforce TLS for all traffic.
- Use private subnets for the gateway.
- Use immutable Docker image deployments through ECR and ECS/EKS.

### Change Management

- Require pull requests.
- Require CI checks before merge.
- Require review for changes touching auth, logging, PII handling, MCP tools, or model invocation.
- Track deployment approvals and release history.

### Vendor And AI Governance

- Complete vendor review for Claude or any AI provider.
- Confirm data retention, training, and deletion terms.
- Avoid raw prompt retention unless approved and encrypted.
- Document model usage, tool usage, and human-review requirements.

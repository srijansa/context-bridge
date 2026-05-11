package main

import (
	"encoding/json"
	"log"
	"net/http"
	"os"
	"strings"
	"time"
)

const defaultInternalServiceToken = "dev-internal-token"

type app struct {
	internalServiceToken string
}

type mcpGatewayRequest struct {
	CorrelationID   string         `json:"correlationId"`
	WorkflowType    string         `json:"workflowType"`
	UserRole        string         `json:"userRole"`
	CaseID          string         `json:"caseId"`
	PolicyNumber    string         `json:"policyNumber"`
	AllowedTools    []string       `json:"allowedTools"`
	RedactionPolicy string         `json:"redactionPolicy"`
	BusinessContext map[string]any `json:"businessContext"`
}

type aiAssistResponse struct {
	CorrelationID  string    `json:"correlationId"`
	Summary        string    `json:"summary"`
	Recommendation string    `json:"recommendation"`
	Warnings       []string  `json:"warnings"`
	ToolCalls      []string  `json:"toolCalls"`
	CompletedAt    time.Time `json:"completedAt"`
}

type errorResponse struct {
	Detail        string `json:"detail"`
	CorrelationID string `json:"correlationId,omitempty"`
}

func main() {
	port := os.Getenv("PORT")
	if port == "" {
		port = "8081"
	}

	server := &http.Server{
		Addr:              ":" + port,
		Handler:           newAppFromEnv().routes(),
		ReadHeaderTimeout: 5 * time.Second,
	}

	log.Printf("event=go_mcp_gateway_starting port=%s", port)
	if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
		log.Fatalf("event=go_mcp_gateway_failed error=%q", err)
	}
}

func newAppFromEnv() *app {
	token := os.Getenv("INTERNAL_SERVICE_TOKEN")
	if token == "" {
		token = defaultInternalServiceToken
	}

	return &app{internalServiceToken: token}
}

func (a *app) routes() http.Handler {
	mux := http.NewServeMux()
	mux.HandleFunc("GET /actuator/health", healthHandler)
	mux.HandleFunc("POST /internal/mcp/execute", a.executeHandler)
	return mux
}

func healthHandler(w http.ResponseWriter, _ *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusOK)

	if err := json.NewEncoder(w).Encode(map[string]string{"status": "UP"}); err != nil {
		log.Printf("event=health_response_failed error=%q", err)
	}
}

func (a *app) executeHandler(w http.ResponseWriter, r *http.Request) {
	startedAt := time.Now()
	correlationID := r.Header.Get("X-Correlation-Id")

	if !a.authorized(r) {
		log.Printf("event=mcp_execute_unauthorized correlationId=%q", correlationID)
		writeJSON(w, http.StatusUnauthorized, errorResponse{
			Detail:        "Request is not authorized",
			CorrelationID: correlationID,
		})
		return
	}

	var request mcpGatewayRequest
	if err := json.NewDecoder(r.Body).Decode(&request); err != nil {
		log.Printf("event=mcp_execute_invalid_json correlationId=%q error=%q", correlationID, err)
		writeJSON(w, http.StatusBadRequest, errorResponse{
			Detail:        "Request body must be valid JSON",
			CorrelationID: correlationID,
		})
		return
	}

	if request.CorrelationID == "" {
		request.CorrelationID = correlationID
	}
	if correlationID == "" {
		correlationID = request.CorrelationID
	}

	log.Printf(
		"event=mcp_execute_requested correlationId=%q workflowType=%q userRole=%q allowedToolCount=%d",
		correlationID,
		request.WorkflowType,
		request.UserRole,
		len(request.AllowedTools),
	)

	response := aiAssistResponse{
		CorrelationID: correlationID,
		Summary:       "Go MCP Gateway received the request. MCP execution is not implemented yet.",
		Recommendation: "Continue using the existing Python gateway until Go MCP execution reaches " +
			"contract parity.",
		Warnings: []string{
			"Go MCP Gateway execute endpoint is a migration stub.",
			"Redis lookup, MCP tools, and Claude calls are not implemented yet.",
		},
		ToolCalls:   []string{},
		CompletedAt: time.Now().UTC(),
	}

	writeJSON(w, http.StatusOK, response)
	log.Printf(
		"event=mcp_execute_completed correlationId=%q status=%d durationMs=%d",
		correlationID,
		http.StatusOK,
		time.Since(startedAt).Milliseconds(),
	)
}

func (a *app) authorized(r *http.Request) bool {
	header := r.Header.Get("Authorization")
	token, ok := strings.CutPrefix(header, "Bearer ")
	return ok && token == a.internalServiceToken
}

func writeJSON(w http.ResponseWriter, status int, body any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)

	if err := json.NewEncoder(w).Encode(body); err != nil {
		log.Printf("event=json_response_failed status=%d error=%q", status, err)
	}
}

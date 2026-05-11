FROM golang:1.23 AS build

WORKDIR /src
COPY go.mod ./
COPY main.go ./
RUN CGO_ENABLED=0 GOOS=linux go build -o /out/go-mcp-gateway-service .

FROM gcr.io/distroless/static-debian12:nonroot

WORKDIR /app
COPY --from=build /out/go-mcp-gateway-service ./go-mcp-gateway-service

EXPOSE 8081
ENTRYPOINT ["/app/go-mcp-gateway-service"]

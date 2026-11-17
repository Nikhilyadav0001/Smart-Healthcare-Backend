# Smart Healthcare — Go Service (`go-service`)

High-performance, stateless Go microservice responsible for:
1. **Clinical Risk PDF Report Generation** via gRPC (`report.v1.ReportService/GenerateReport`) on port **9095**.
2. **Kafka Event Consumer & Email Dispatcher** (consumer group `go-service`) for:
   - `user.registered` (v1): Dispatches welcome & account verification email with URL.
   - `notification.requested` (v1): Dispatches transactional email (password reset, resend verification, generic alerts).
   - `prediction.completed` (v1): Generates clinical risk PDF report and emails it as an attachment.
3. **HTTP Operations & Observability** on port **8085**:
   - `GET /healthz`: Liveness probe.
   - `GET /readyz`: Readiness probe (checks Redis connectivity).
   - `GET /metrics`: Prometheus metrics scraper.

---

## Architecture & Features

- **Stateless gRPC Server:** Implements `reportv1.ReportServiceServer` with protobuf generated via Buf.
- **Idempotency with Redis:** Prevents duplicate email dispatches on replayed/rebalanced events via `SETNX idempotency:event:<eventId> EX 24h`.
- **Worker Pool & Resilient Processing:** Concurrent worker goroutines, retries with exponential backoff.
- **Dead Letter Queue (DLQ):** Unprocessable events after max retries are automatically forwarded to `<topic>.dlq` (`user.registered.dlq`, `notification.requested.dlq`, `prediction.completed.dlq`) with failure headers.
- **Observability:**
  - `log/slog` JSON logger with automatic `trace_id` and `span_id` extraction.
  - OpenTelemetry OTLP trace export to Alloy (`http://localhost:4318/v1/traces`).
  - Prometheus metrics instrumentation (`reports_generated_total`, `kafka_events_consumed_total`, `emails_sent_total`, `dlq_events_produced_total`).
- **SMTP Mailer:** Integrates with Mailpit (SMTP port 1025) and renders responsive HTML email templates with PDF attachment support.

---

## Directory Structure

```
services/go-service/
├── cmd/
│   └── server/
│       └── main.go              # Main service entrypoint
├── gen/                         # Buf generated Go protobuf & gRPC stubs
│   ├── health/v1/
│   ├── profile/v1/
│   └── report/v1/
├── internal/
│   ├── config/config.go         # Environment-driven configuration
│   ├── consumer/                # Kafka consumer, worker pool, idempotency, DLQ
│   │   ├── consumer.go
│   │   └── events.go
│   ├── grpcserver/              # gRPC ReportService implementation
│   │   ├── server.go
│   │   └── server_test.go
│   ├── mailer/                  # SMTP mailer & template renderer
│   │   ├── mailer.go
│   │   └── mailer_test.go
│   ├── observability/           # slog, OpenTelemetry tracer, Prometheus metrics
│   │   ├── logger.go
│   │   ├── metrics.go
│   │   └── tracer.go
│   └── pdf/                     # Pure-Go clinical PDF report generator
│       ├── report_pdf.go
│       └── report_pdf_test.go
├── templates/                   # HTML email templates
│   ├── notification_email.html
│   ├── report_email.html
│   └── welcome_email.html
├── Dockerfile                   # Multi-stage production container build
├── go.mod
└── go.sum
```

---

## Running Locally

### Prerequisites
Make sure the Docker infrastructure is running:
```bash
docker compose -f infra/docker/compose.data.yml up -d
docker compose -f infra/docker/compose.observability.yml up -d
```

### Running the Go Service
```bash
cd services/go-service
go run ./cmd/server
```

Or run the compiled binary:
```bash
./server.exe
```

### Running Tests
```bash
go test -v ./...
```

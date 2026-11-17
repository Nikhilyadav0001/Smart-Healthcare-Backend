# ------------------------------------------------------------------------------
# Stage 1: Build static Go binary
# ------------------------------------------------------------------------------
FROM golang:1.24-alpine AS builder

WORKDIR /src

RUN apk add --no-cache git ca-certificates tzdata

COPY go.mod go.sum ./
RUN go mod download

COPY . .

RUN CGO_ENABLED=0 GOOS=linux GOARCH=amd64 go build \
    -ldflags="-w -s -extldflags '-static'" \
    -o /bin/smarthealth-go-service ./cmd/server

# ------------------------------------------------------------------------------
# Stage 2: Minimal runtime image
# ------------------------------------------------------------------------------
FROM alpine:3.21

WORKDIR /app

RUN apk add --no-cache ca-certificates tzdata curl && \
    addgroup -S smarthealth && adduser -S smarthealth -G smarthealth

COPY --from=builder /bin/smarthealth-go-service /app/smarthealth-go-service
COPY --from=builder /src/templates /app/templates

USER smarthealth:smarthealth

EXPOSE 8085 9095

ENV ENV=production \
    HTTP_PORT=8085 \
    GRPC_PORT=9095 \
    TEMPLATES_DIR=/app/templates

ENTRYPOINT ["/app/smarthealth-go-service"]

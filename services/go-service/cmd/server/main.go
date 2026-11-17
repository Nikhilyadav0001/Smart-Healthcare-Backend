package main

import (
	"context"
	"encoding/json"
	"fmt"
	"log/slog"
	"net"
	"net/http"
	"os"
	"os/signal"
	"path/filepath"
	"syscall"
	"time"

	"github.com/prometheus/client_golang/prometheus/promhttp"
	"github.com/redis/go-redis/v9"
	reportv1 "github.com/smarthealth/go-service/gen/report/v1"
	"github.com/smarthealth/go-service/internal/config"
	"github.com/smarthealth/go-service/internal/consumer"
	"github.com/smarthealth/go-service/internal/grpcserver"
	"github.com/smarthealth/go-service/internal/mailer"
	"github.com/smarthealth/go-service/internal/observability"
	"google.golang.org/grpc"
	"google.golang.org/grpc/reflection"
)

func findTemplatesDir() string {
	candidates := []string{
		os.Getenv("TEMPLATES_DIR"),
		"templates",
		"services/go-service/templates",
		"../templates",
		"../../templates",
	}
	for _, c := range candidates {
		if c == "" {
			continue
		}
		if fi, err := os.Stat(filepath.Join(c, "welcome_email.html")); err == nil && !fi.IsDir() {
			return c
		}
	}
	return "templates"
}

func main() {
	cfg := config.Load()
	observability.InitLogger(cfg.LogLevel)

	slog.Info("Starting Smart Healthcare Go Service",
		"service", cfg.ServiceName,
		"env", cfg.Env,
		"httpPort", cfg.HTTPPort,
		"grpcPort", cfg.GRPCPort,
		"kafkaBrokers", cfg.KafkaBrokers,
		"smtpHost", cfg.SMTPHost,
		"smtpPort", cfg.SMTPPort,
	)

	// Context for graceful shutdown
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	// 1. OpenTelemetry Tracing
	shutdownTracer, err := observability.InitTracer(context.Background(), cfg.ServiceName, cfg.OtelEndpoint)
	if err != nil {
		slog.Warn("OpenTelemetry tracer init failed", "error", err)
	}
	defer func() {
		ctxTimeout, cancel := context.WithTimeout(context.Background(), 2*time.Second)
		defer cancel()
		_ = shutdownTracer(ctxTimeout)
	}()

	// 2. Redis Connection (for consumer idempotency)
	rdb := redis.NewClient(&redis.Options{
		Addr:     cfg.RedisAddr,
		Password: cfg.RedisPassword,
		DB:       cfg.RedisDB,
	})
	pingCtx, pingCancel := context.WithTimeout(context.Background(), 3*time.Second)
	defer pingCancel()
	if err := rdb.Ping(pingCtx).Err(); err != nil {
		slog.Warn("Redis initial ping failed (service will retry on demand)", "addr", cfg.RedisAddr, "error", err)
	} else {
		slog.Info("Connected to Redis successfully", "addr", cfg.RedisAddr)
	}
	defer rdb.Close()

	// 3. Email Mailer
	templatesDir := findTemplatesDir()
	slog.Info("Resolved email templates directory", "dir", templatesDir)
	mailClient, err := mailer.NewMailer(cfg, templatesDir)
	if err != nil {
		slog.Error("Failed to initialize mailer", "error", err)
		os.Exit(1)
	}

	// 4. Kafka Event Consumer
	eventConsumer, err := consumer.NewEventConsumer(cfg, rdb, mailClient)
	if err != nil {
		slog.Warn("Kafka consumer initialization failed (is Kafka running?)", "error", err)
	} else {
		eventConsumer.Start(ctx)
		defer eventConsumer.Stop()
	}

	// 5. gRPC Server (port 9095)
	grpcListener, err := net.Listen("tcp", fmt.Sprintf(":%s", cfg.GRPCPort))
	if err != nil {
		slog.Error("Failed to listen for gRPC", "port", cfg.GRPCPort, "error", err)
		os.Exit(1)
	}

	grpcServer := grpc.NewServer()
	reportv1.RegisterReportServiceServer(grpcServer, grpcserver.NewReportServer())
	reflection.Register(grpcServer)

	go func() {
		slog.Info("gRPC server listening", "port", cfg.GRPCPort)
		if err := grpcServer.Serve(grpcListener); err != nil && err != grpc.ErrServerStopped {
			slog.Error("gRPC server error", "error", err)
		}
	}()

	// 6. HTTP Server (port 8085) for Health & Prometheus Metrics
	httpMux := http.NewServeMux()

	httpMux.HandleFunc("/healthz", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.WriteHeader(http.StatusOK)
		_ = json.NewEncoder(w).Encode(map[string]any{
			"status":  "UP",
			"service": cfg.ServiceName,
			"time":    time.Now().UTC().Format(time.RFC3339),
		})
	})

	httpMux.HandleFunc("/readyz", func(w http.ResponseWriter, r *http.Request) {
		redisStatus := "UP"
		if err := rdb.Ping(r.Context()).Err(); err != nil {
			redisStatus = "DOWN"
		}
		w.Header().Set("Content-Type", "application/json")
		if redisStatus == "DOWN" {
			w.WriteHeader(http.StatusServiceUnavailable)
		} else {
			w.WriteHeader(http.StatusOK)
		}
		_ = json.NewEncoder(w).Encode(map[string]any{
			"status":  "READY",
			"redis":   redisStatus,
			"service": cfg.ServiceName,
		})
	})

	httpMux.Handle("/metrics", promhttp.Handler())

	httpServer := &http.Server{
		Addr:         fmt.Sprintf(":%s", cfg.HTTPPort),
		Handler:      httpMux,
		ReadTimeout:  10 * time.Second,
		WriteTimeout: 10 * time.Second,
	}

	go func() {
		slog.Info("HTTP server listening", "port", cfg.HTTPPort, "endpoints", []string{"/healthz", "/readyz", "/metrics"})
		if err := httpServer.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			slog.Error("HTTP server error", "error", err)
		}
	}()

	// Wait for OS shutdown signal
	<-ctx.Done()
	slog.Info("Shutting down Go Service gracefully...")

	// Graceful stop gRPC
	grpcServer.GracefulStop()
	slog.Info("gRPC server stopped")

	// Graceful stop HTTP
	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer shutdownCancel()
	if err := httpServer.Shutdown(shutdownCtx); err != nil {
		slog.Error("HTTP server shutdown error", "error", err)
	} else {
		slog.Info("HTTP server stopped")
	}

	slog.Info("Smart Healthcare Go Service stopped cleanly")
}

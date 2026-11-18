package observability

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"log/slog"
	"net/http"
	"os"
	"strings"
	"sync"
	"time"

	"go.opentelemetry.io/otel/trace"
)

type ContextHandler struct {
	slog.Handler
}

func (h *ContextHandler) Handle(ctx context.Context, r slog.Record) error {
	span := trace.SpanFromContext(ctx)
	if span != nil && span.SpanContext().IsValid() {
		r.AddAttrs(
			slog.String("trace_id", span.SpanContext().TraceID().String()),
			slog.String("span_id", span.SpanContext().SpanID().String()),
		)
	}
	return h.Handler.Handle(ctx, r)
}

type otlpLogItem struct {
	timeUnixNano string
	severityText string
	body         string
	level        string
	attrs        map[string]string
}

type OtlpLogger struct {
	stdoutHandler slog.Handler
	serviceName   string
	endpoint      string
	logChan       chan otlpLogItem
	httpClient    *http.Client
	closeOnce     sync.Once
	stopChan      chan struct{}
}

func newOtlpLogger(stdoutHandler slog.Handler, serviceName, otelEndpoint string) *OtlpLogger {
	endpoint := strings.TrimRight(otelEndpoint, "/")
	if !strings.HasSuffix(endpoint, "/v1/logs") {
		endpoint = endpoint + "/v1/logs"
	}

	ol := &OtlpLogger{
		stdoutHandler: stdoutHandler,
		serviceName:   serviceName,
		endpoint:      endpoint,
		logChan:       make(chan otlpLogItem, 1000),
		httpClient:    &http.Client{Timeout: 3 * time.Second},
		stopChan:      make(chan struct{}),
	}
	go ol.batchWorker()
	return ol
}

func (o *OtlpLogger) Enabled(ctx context.Context, level slog.Level) bool {
	return o.stdoutHandler.Enabled(ctx, level)
}

func (o *OtlpLogger) Handle(ctx context.Context, r slog.Record) error {
	// First log to stdout
	_ = o.stdoutHandler.Handle(ctx, r)

	// Extract attributes
	attrs := make(map[string]string)
	r.Attrs(func(a slog.Attr) bool {
		attrs[a.Key] = fmt.Sprintf("%v", a.Value.Any())
		return true
	})

	item := otlpLogItem{
		timeUnixNano: fmt.Sprintf("%d", r.Time.UnixNano()),
		severityText: r.Level.String(),
		body:         r.Message,
		level:        r.Level.String(),
		attrs:        attrs,
	}

	select {
	case o.logChan <- item:
	default:
		// Queue full, drop to prevent blocking app
	}
	return nil
}

func (o *OtlpLogger) WithAttrs(attrs []slog.Attr) slog.Handler {
	return &OtlpLogger{
		stdoutHandler: o.stdoutHandler.WithAttrs(attrs),
		serviceName:   o.serviceName,
		endpoint:      o.endpoint,
		logChan:       o.logChan,
		httpClient:    o.httpClient,
		stopChan:      o.stopChan,
	}
}

func (o *OtlpLogger) WithGroup(name string) slog.Handler {
	return &OtlpLogger{
		stdoutHandler: o.stdoutHandler.WithGroup(name),
		serviceName:   o.serviceName,
		endpoint:      o.endpoint,
		logChan:       o.logChan,
		httpClient:    o.httpClient,
		stopChan:      o.stopChan,
	}
}

func (o *OtlpLogger) batchWorker() {
	ticker := time.NewTicker(300 * time.Millisecond)
	defer ticker.Stop()

	var batch []otlpLogItem

	flush := func() {
		if len(batch) == 0 {
			return
		}
		items := batch
		batch = nil
		o.sendBatch(items)
	}

	for {
		select {
		case <-o.stopChan:
			flush()
			return
		case item := <-o.logChan:
			batch = append(batch, item)
			if len(batch) >= 50 {
				flush()
			}
		case <-ticker.C:
			flush()
		}
	}
}

func (o *OtlpLogger) sendBatch(items []otlpLogItem) {
	logRecords := make([]map[string]any, 0, len(items))
	for _, it := range items {
		otlpAttrs := []map[string]any{
			{
				"key": "level",
				"value": map[string]string{
					"stringValue": it.level,
				},
			},
		}
		for k, v := range it.attrs {
			otlpAttrs = append(otlpAttrs, map[string]any{
				"key": k,
				"value": map[string]string{
					"stringValue": v,
				},
			})
		}

		logRecords = append(logRecords, map[string]any{
			"timeUnixNano": it.timeUnixNano,
			"severityText": it.severityText,
			"body": map[string]string{
				"stringValue": it.body,
			},
			"attributes": otlpAttrs,
		})
	}

	payload := map[string]any{
		"resourceLogs": []map[string]any{
			{
				"resource": map[string]any{
					"attributes": []map[string]any{
						{
							"key": "service.name",
							"value": map[string]string{
								"stringValue": o.serviceName,
							},
						},
					},
				},
				"scopeLogs": []map[string]any{
					{
						"logRecords": logRecords,
					},
				},
			},
		},
	}

	data, err := json.Marshal(payload)
	if err != nil {
		return
	}

	ctx, cancel := context.WithTimeout(context.Background(), 2*time.Second)
	defer cancel()

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, o.endpoint, bytes.NewReader(data))
	if err != nil {
		return
	}
	req.Header.Set("Content-Type", "application/json")

	resp, err := o.httpClient.Do(req)
	if err != nil {
		return
	}
	defer resp.Body.Close()
}

func InitLogger(level, serviceName, otelEndpoint string) *slog.Logger {
	var logLevel slog.Level
	switch strings.ToLower(level) {
	case "debug":
		logLevel = slog.LevelDebug
	case "warn":
		logLevel = slog.LevelWarn
	case "error":
		logLevel = slog.LevelError
	default:
		logLevel = slog.LevelInfo
	}

	opts := &slog.HandlerOptions{
		Level: logLevel,
	}

	jsonHandler := slog.NewJSONHandler(os.Stdout, opts)
	ctxHandler := &ContextHandler{Handler: jsonHandler}

	var rootHandler slog.Handler = ctxHandler
	if otelEndpoint != "" {
		rootHandler = newOtlpLogger(ctxHandler, serviceName, otelEndpoint)
	}

	logger := slog.New(rootHandler)
	slog.SetDefault(logger)
	return logger
}

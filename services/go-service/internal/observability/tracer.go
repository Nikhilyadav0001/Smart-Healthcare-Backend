package observability

import (
	"context"
	"log/slog"
	"strings"
	"time"

	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/exporters/otlp/otlptrace/otlptracehttp"
	"go.opentelemetry.io/otel/propagation"
	"go.opentelemetry.io/otel/sdk/resource"
	sdktrace "go.opentelemetry.io/otel/sdk/trace"
	semconv "go.opentelemetry.io/otel/semconv/v1.26.0"
)

func InitTracer(ctx context.Context, serviceName, endpoint string) (func(context.Context) error, error) {
	cleanEndpoint := endpoint
	insecure := true

	if strings.HasPrefix(cleanEndpoint, "http://") {
		cleanEndpoint = strings.TrimPrefix(cleanEndpoint, "http://")
	} else if strings.HasPrefix(cleanEndpoint, "https://") {
		cleanEndpoint = strings.TrimPrefix(cleanEndpoint, "https://")
		insecure = false
	}

	opts := []otlptracehttp.Option{
		otlptracehttp.WithEndpoint(cleanEndpoint),
		otlptracehttp.WithURLPath("/v1/traces"),
		otlptracehttp.WithTimeout(3 * time.Second),
	}
	if insecure {
		opts = append(opts, otlptracehttp.WithInsecure())
	}

	exporter, err := otlptracehttp.New(ctx, opts...)
	if err != nil {
		slog.Warn("Failed to create OTLP trace exporter, tracing disabled", "error", err)
		return func(context.Context) error { return nil }, nil
	}

	res, err := resource.New(ctx,
		resource.WithAttributes(
			semconv.ServiceNameKey.String(serviceName),
		),
	)
	if err != nil {
		res = resource.Default()
	}

	tp := sdktrace.NewTracerProvider(
		sdktrace.WithBatcher(exporter),
		sdktrace.WithResource(res),
		sdktrace.WithSampler(sdktrace.AlwaysSample()),
	)

	otel.SetTracerProvider(tp)
	otel.SetTextMapPropagator(propagation.NewCompositeTextMapPropagator(
		propagation.TraceContext{},
		propagation.Baggage{},
	))

	return tp.Shutdown, nil
}

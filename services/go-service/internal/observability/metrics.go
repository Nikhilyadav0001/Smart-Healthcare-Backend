package observability

import (
	"github.com/prometheus/client_golang/prometheus"
	"github.com/prometheus/client_golang/prometheus/promauto"
)

var (
	HttpServerRequestsTotal = promauto.NewCounterVec(
		prometheus.CounterOpts{
			Name: "http_server_requests_seconds_count",
			Help: "Total number of HTTP server requests",
		},
		[]string{"method", "status", "uri", "exception", "outcome"},
	)

	ReportsGeneratedTotal = promauto.NewCounterVec(
		prometheus.CounterOpts{
			Name: "reports_generated_total",
			Help: "Total number of clinical risk PDF reports generated",
		},
		[]string{"disease", "risk_level", "status"},
	)

	ReportGenerationDuration = promauto.NewHistogramVec(
		prometheus.HistogramOpts{
			Name:    "reports_generation_duration_seconds",
			Help:    "Duration of clinical PDF report generation in seconds",
			Buckets: []float64{0.005, 0.01, 0.025, 0.05, 0.1, 0.25, 0.5, 1.0, 2.5},
		},
		[]string{"disease"},
	)

	KafkaEventsConsumedTotal = promauto.NewCounterVec(
		prometheus.CounterOpts{
			Name: "kafka_events_consumed_total",
			Help: "Total number of Kafka events processed by consumer group",
		},
		[]string{"topic", "status"},
	)

	KafkaProcessingDuration = promauto.NewHistogramVec(
		prometheus.HistogramOpts{
			Name:    "kafka_events_processing_duration_seconds",
			Help:    "Duration of Kafka event processing in seconds",
			Buckets: []float64{0.01, 0.05, 0.1, 0.25, 0.5, 1.0, 2.0, 5.0},
		},
		[]string{"topic"},
	)

	EmailsSentTotal = promauto.NewCounterVec(
		prometheus.CounterOpts{
			Name: "emails_sent_total",
			Help: "Total number of emails dispatched via SMTP mailer",
		},
		[]string{"type", "status"},
	)

	DLQEventsProducedTotal = promauto.NewCounterVec(
		prometheus.CounterOpts{
			Name: "dlq_events_produced_total",
			Help: "Total number of events routed to dead letter queue",
		},
		[]string{"topic"},
	)
)

func init() {
	HttpServerRequestsTotal.WithLabelValues("GET", "200", "/healthz", "None", "SUCCESS").Add(0)
	HttpServerRequestsTotal.WithLabelValues("GET", "200", "/readyz", "None", "SUCCESS").Add(0)
}

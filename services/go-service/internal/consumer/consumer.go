package consumer

import (
	"context"
	"encoding/json"
	"fmt"
	"log/slog"
	"sync"
	"time"

	"github.com/redis/go-redis/v9"
	"github.com/smarthealth/go-service/internal/config"
	"github.com/smarthealth/go-service/internal/mailer"
	"github.com/smarthealth/go-service/internal/observability"
	"github.com/smarthealth/go-service/internal/pdf"
	"github.com/twmb/franz-go/pkg/kgo"
	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/attribute"
	"go.opentelemetry.io/otel/propagation"
	"go.opentelemetry.io/otel/trace"
)

type EventConsumer struct {
	cfg         *config.Config
	kafkaClient *kgo.Client
	redisClient *redis.Client
	mailer      *mailer.Mailer
	tracer      trace.Tracer
	jobsChan    chan *kgo.Record
	wg          sync.WaitGroup
	quit        chan struct{}
}

func NewEventConsumer(cfg *config.Config, redisClient *redis.Client, mailer *mailer.Mailer) (*EventConsumer, error) {
	topics := []string{
		cfg.TopicUserRegistered,
		cfg.TopicNotificationRequested,
		cfg.TopicPredictionCompleted,
	}

	opts := []kgo.Opt{
		kgo.SeedBrokers(cfg.KafkaBrokers...),
		kgo.ConsumerGroup(cfg.KafkaGroupID),
		kgo.ConsumeTopics(topics...),
		kgo.DisableAutoCommit(),
		kgo.BlockRebalanceOnPoll(),
	}

	client, err := kgo.NewClient(opts...)
	if err != nil {
		return nil, fmt.Errorf("failed to initialize kafka client: %w", err)
	}

	return &EventConsumer{
		cfg:         cfg,
		kafkaClient: client,
		redisClient: redisClient,
		mailer:      mailer,
		tracer:      otel.Tracer("go-service/consumer"),
		jobsChan:    make(chan *kgo.Record, cfg.KafkaWorkers*4),
		quit:        make(chan struct{}),
	}, nil
}

func (c *EventConsumer) Start(ctx context.Context) {
	// Start worker pool
	for i := 1; i <= c.cfg.KafkaWorkers; i++ {
		c.wg.Add(1)
		go c.worker(i)
	}

	// Start fetcher loop
	go c.fetchLoop(ctx)
}

func (c *EventConsumer) fetchLoop(ctx context.Context) {
	for {
		select {
		case <-c.quit:
			return
		case <-ctx.Done():
			return
		default:
			fetches := c.kafkaClient.PollFetches(ctx)
			if fetches.IsClientClosed() {
				return
			}
			if errs := fetches.Errors(); len(errs) > 0 {
				for _, err := range errs {
					slog.Warn("Kafka fetch error", "topic", err.Topic, "partition", err.Partition, "error", err.Err)
				}
				time.Sleep(200 * time.Millisecond)
				continue
			}

			iter := fetches.RecordIter()
			for !iter.Done() {
				record := iter.Next()
				select {
				case c.jobsChan <- record:
				case <-c.quit:
					return
				case <-ctx.Done():
					return
				}
			}
		}
	}
}

func (c *EventConsumer) worker(id int) {
	defer c.wg.Done()
	slog.Debug("Consumer worker started", "workerId", id)

	for record := range c.jobsChan {
		c.processRecord(record)
	}
}

func (c *EventConsumer) processRecord(record *kgo.Record) {
	start := time.Now()
	topic := record.Topic

	// Extract tracing context from Kafka headers
	carrier := make(propagation.HeaderCarrier)
	for _, h := range record.Headers {
		carrier.Set(h.Key, string(h.Value))
	}
	ctx := otel.GetTextMapPropagator().Extract(context.Background(), carrier)
	ctx, span := c.tracer.Start(ctx, fmt.Sprintf("Kafka.Consume %s", topic),
		trace.WithAttributes(
			attribute.String("messaging.system", "kafka"),
			attribute.String("messaging.destination", topic),
			attribute.Int64("messaging.kafka.offset", record.Offset),
			attribute.Int("messaging.kafka.partition", int(record.Partition)),
		),
	)
	defer span.End()

	// Extract Event ID for idempotency check
	var base BaseEvent
	if err := json.Unmarshal(record.Value, &base); err != nil {
		slog.ErrorContext(ctx, "Failed to unmarshal event header/base", "topic", topic, "error", err)
		c.routeToDLQ(ctx, record, fmt.Sprintf("Unmarshal base failed: %v", err))
		_ = c.kafkaClient.CommitRecords(context.Background(), record)
		return
	}

	eventId := base.EventID
	if eventId == "" {
		eventId = fmt.Sprintf("%s-%d-%d", record.Topic, record.Partition, record.Offset)
	}
	span.SetAttributes(attribute.String("event.id", eventId))

	// Redis Idempotency Check (SETNX with TTL)
	idempotencyKey := fmt.Sprintf("idempotency:event:%s", eventId)
	set, err := c.redisClient.SetNX(ctx, idempotencyKey, "PROCESSED", c.cfg.IdempotencyTTL).Result()
	if err != nil {
		slog.WarnContext(ctx, "Redis idempotency check warning, proceeding", "eventId", eventId, "error", err)
	} else if !set {
		slog.InfoContext(ctx, "Event already processed (duplicate detected via Redis SETNX), skipping",
			"eventId", eventId,
			"topic", topic,
		)
		observability.KafkaEventsConsumedTotal.WithLabelValues(topic, "duplicate_skipped").Inc()
		_ = c.kafkaClient.CommitRecords(context.Background(), record)
		return
	}

	// Retry loop with backoff
	var lastErr error
	backoff := c.cfg.RetryBackoff

	for attempt := 1; attempt <= c.cfg.MaxRetries; attempt++ {
		lastErr = c.handleEvent(ctx, record)
		if lastErr == nil {
			break
		}

		slog.WarnContext(ctx, "Event processing failed, retrying",
			"eventId", eventId,
			"topic", topic,
			"attempt", attempt,
			"maxRetries", c.cfg.MaxRetries,
			"error", lastErr,
		)
		time.Sleep(backoff)
		backoff *= 2
	}

	duration := time.Since(start).Seconds()
	observability.KafkaProcessingDuration.WithLabelValues(topic).Observe(duration)

	if lastErr != nil {
		// All retries failed -> send to DLQ
		slog.ErrorContext(ctx, "Event failed after max retries, routing to DLQ",
			"eventId", eventId,
			"topic", topic,
			"error", lastErr,
		)
		c.routeToDLQ(ctx, record, lastErr.Error())
		observability.KafkaEventsConsumedTotal.WithLabelValues(topic, "error").Inc()
	} else {
		observability.KafkaEventsConsumedTotal.WithLabelValues(topic, "success").Inc()
		slog.InfoContext(ctx, "Successfully processed and committed event",
			"eventId", eventId,
			"topic", topic,
			"durationSeconds", duration,
		)
	}

	// Commit Kafka offset after success or DLQ routing
	if err := c.kafkaClient.CommitRecords(context.Background(), record); err != nil {
		slog.ErrorContext(ctx, "Failed to commit record offset", "topic", topic, "error", err)
	}
}

func (c *EventConsumer) handleEvent(ctx context.Context, record *kgo.Record) error {
	switch record.Topic {
	case c.cfg.TopicUserRegistered:
		var event UserRegisteredEvent
		if err := json.Unmarshal(record.Value, &event); err != nil {
			return fmt.Errorf("malformed user.registered event: %w", err)
		}
		return c.mailer.SendWelcomeEmail(ctx, event.Email, event.FullName, event.Role, event.VerificationURL)

	case c.cfg.TopicNotificationRequested:
		var event NotificationRequestedEvent
		if err := json.Unmarshal(record.Value, &event); err != nil {
			return fmt.Errorf("malformed notification.requested event: %w", err)
		}
		return c.mailer.SendNotificationEmail(ctx, event.ToEmail, event.ToName, event.Type, event.Subject, event.Body)

	case c.cfg.TopicPredictionCompleted:
		var event PredictionCompletedEvent
		if err := json.Unmarshal(record.Value, &event); err != nil {
			return fmt.Errorf("malformed prediction.completed event: %w", err)
		}

		// Generate PDF
		pdfData := pdf.ClinicalReportData{
			PatientName:     event.PatientName,
			Disease:         event.Disease,
			Probability:     event.Probability,
			RiskLevel:       event.RiskLevel,
			TopFactors:      event.TopFactors,
			Recommendations: event.Recommendations,
			GeneratedAt:     time.Now(),
		}
		pdfBytes, pdfFilename, err := pdf.GenerateClinicalReport(pdfData)
		if err != nil {
			return fmt.Errorf("failed to generate clinical pdf for prediction: %w", err)
		}

		// Send email with attached PDF
		return c.mailer.SendReportEmail(
			ctx,
			event.PatientEmail,
			event.PatientName,
			event.Disease,
			event.RiskLevel,
			event.Probability,
			event.TopFactors,
			event.Recommendations,
			pdfBytes,
			pdfFilename,
		)

	default:
		return fmt.Errorf("unhandled topic: %s", record.Topic)
	}
}

func (c *EventConsumer) routeToDLQ(ctx context.Context, record *kgo.Record, reason string) {
	dlqTopic := record.Topic + c.cfg.DLQSuffix

	headers := append(record.Headers,
		kgo.RecordHeader{Key: "dlq-error-reason", Value: []byte(reason)},
		kgo.RecordHeader{Key: "dlq-failed-at", Value: []byte(time.Now().UTC().Format(time.RFC3339))},
		kgo.RecordHeader{Key: "dlq-source-topic", Value: []byte(record.Topic)},
	)

	dlqRecord := &kgo.Record{
		Topic:   dlqTopic,
		Key:     record.Key,
		Value:   record.Value,
		Headers: headers,
	}

	c.kafkaClient.Produce(ctx, dlqRecord, func(r *kgo.Record, err error) {
		if err != nil {
			slog.ErrorContext(ctx, "Failed to produce to DLQ", "dlqTopic", dlqTopic, "error", err)
		} else {
			slog.WarnContext(ctx, "Event routed to DLQ", "dlqTopic", dlqTopic, "partition", r.Partition, "offset", r.Offset)
			observability.DLQEventsProducedTotal.WithLabelValues(record.Topic).Inc()
		}
	})
}

func (c *EventConsumer) Stop() {
	close(c.quit)
	close(c.jobsChan)
	c.wg.Wait()
	c.kafkaClient.Close()
	slog.Info("Kafka consumer group gracefully stopped")
}

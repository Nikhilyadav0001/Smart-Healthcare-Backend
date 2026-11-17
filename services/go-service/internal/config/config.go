package config

import (
	"os"
	"strconv"
	"time"
)

type Config struct {
	// Server
	HTTPPort string
	GRPCPort string
	Env      string

	// Kafka
	KafkaBrokers                []string
	KafkaGroupID                string
	TopicUserRegistered         string
	TopicNotificationRequested  string
	TopicPredictionCompleted    string
	DLQSuffix                   string
	KafkaWorkers                int
	MaxRetries                  int
	RetryBackoff                time.Duration

	// Redis
	RedisAddr      string
	RedisPassword  string
	RedisDB        int
	IdempotencyTTL time.Duration

	// SMTP / Mailpit
	SMTPHost     string
	SMTPPort     int
	SMTPUsername string
	SMTPPassword string
	SMTPFrom     string

	// Observability
	OtelEndpoint string
	ServiceName  string
	LogLevel     string
}

func Load() *Config {
	return &Config{
		HTTPPort: getEnv("HTTP_PORT", "8086"),
		GRPCPort: getEnv("GRPC_PORT", "9095"),
		Env:      getEnv("ENV", "development"),

		KafkaBrokers:               []string{getEnv("KAFKA_BROKERS", "localhost:19092")},
		KafkaGroupID:               getEnv("KAFKA_GROUP_ID", "go-service"),
		TopicUserRegistered:        getEnv("TOPIC_USER_REGISTERED", "user.registered"),
		TopicNotificationRequested: getEnv("TOPIC_NOTIFICATION_REQUESTED", "notification.requested"),
		TopicPredictionCompleted:   getEnv("TOPIC_PREDICTION_COMPLETED", "prediction.completed"),
		DLQSuffix:                  getEnv("DLQ_SUFFIX", ".dlq"),
		KafkaWorkers:               getEnvInt("KAFKA_WORKERS", 4),
		MaxRetries:                 getEnvInt("MAX_RETRIES", 3),
		RetryBackoff:               getEnvDuration("RETRY_BACKOFF", 500*time.Millisecond),

		RedisAddr:      getEnv("REDIS_ADDR", "localhost:6379"),
		RedisPassword:  getEnv("REDIS_PASSWORD", "nikhil"),
		RedisDB:        getEnvInt("REDIS_DB", 0),
		IdempotencyTTL: getEnvDuration("IDEMPOTENCY_TTL", 24*time.Hour),

		SMTPHost:     getEnv("SMTP_HOST", "localhost"),
		SMTPPort:     getEnvInt("SMTP_PORT", 1025),
		SMTPUsername: getEnv("SMTP_USERNAME", ""),
		SMTPPassword: getEnv("SMTP_PASSWORD", ""),
		SMTPFrom:     getEnv("SMTP_FROM", "no-reply@smarthealth.local"),

		OtelEndpoint: getEnv("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4318"),
		ServiceName:  getEnv("SERVICE_NAME", "go-service"),
		LogLevel:     getEnv("LOG_LEVEL", "info"),
	}
}

func getEnv(key, defaultVal string) string {
	if val, ok := os.LookupEnv(key); ok && val != "" {
		return val
	}
	return defaultVal
}

func getEnvInt(key string, defaultVal int) int {
	if val, ok := os.LookupEnv(key); ok && val != "" {
		if i, err := strconv.Atoi(val); err == nil {
			return i
		}
	}
	return defaultVal
}

func getEnvDuration(key string, defaultVal time.Duration) time.Duration {
	if val, ok := os.LookupEnv(key); ok && val != "" {
		if d, err := time.ParseDuration(val); err == nil {
			return d
		}
	}
	return defaultVal
}

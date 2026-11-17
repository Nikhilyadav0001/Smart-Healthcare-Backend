package consumer

type BaseEvent struct {
	EventID       string `json:"eventId"`
	SchemaVersion int    `json:"schemaVersion"`
	OccurredAt    string `json:"occurredAt"`
}

type UserRegisteredEvent struct {
	EventID         string `json:"eventId"`
	SchemaVersion   int    `json:"schemaVersion"`
	OccurredAt      string `json:"occurredAt"`
	UserID          string `json:"userId"`
	Email           string `json:"email"`
	FullName        string `json:"fullName"`
	Role            string `json:"role"`
	VerificationURL string `json:"verificationUrl"`
}

type NotificationRequestedEvent struct {
	EventID       string `json:"eventId"`
	SchemaVersion int    `json:"schemaVersion"`
	OccurredAt    string `json:"occurredAt"`
	Type          string `json:"type"`
	ToEmail       string `json:"toEmail"`
	ToName        string `json:"toName"`
	Subject       string `json:"subject"`
	Body          string `json:"body"`
}

type PredictionCompletedEvent struct {
	EventID         string    `json:"eventId"`
	SchemaVersion   int       `json:"schemaVersion"`
	OccurredAt      string    `json:"occurredAt"`
	PatientID       string    `json:"patientId"`
	PatientName     string    `json:"patientName"`
	PatientEmail    string    `json:"patientEmail"`
	PredictionID    string    `json:"predictionId"`
	Disease         string    `json:"disease"`
	Probability     float64   `json:"probability"`
	RiskLevel       string    `json:"riskLevel"`
	TopFactors      []string  `json:"topFactors"`
	Recommendations []string  `json:"recommendations"`
}

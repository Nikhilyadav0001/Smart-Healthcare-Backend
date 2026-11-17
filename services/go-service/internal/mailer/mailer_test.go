package mailer

import (
	"context"
	"testing"

	"github.com/smarthealth/go-service/internal/config"
)

func TestMailer_TemplateRendering(t *testing.T) {
	cfg := &config.Config{
		SMTPHost: "localhost",
		SMTPPort: 1025,
		SMTPFrom: "no-reply@smarthealth.local",
	}

	m, err := NewMailer(cfg, "../../templates")
	if err != nil {
		t.Fatalf("failed to initialize mailer with templates: %v", err)
	}

	// Test sending welcome email to local Mailpit (if running)
	ctx := context.Background()
	err = m.SendWelcomeEmail(ctx, "test-user@smarthealth.local", "Test User", "PATIENT", "http://localhost:8080/auth/verify?token=123")
	if err != nil {
		t.Logf("Mailpit send failed (may not be listening or firewall): %v", err)
	} else {
		t.Log("Successfully sent welcome email to Mailpit")
	}

	// Test sending report email with PDF
	err = m.SendReportEmail(
		ctx,
		"patient@smarthealth.local",
		"John Doe",
		"Diabetes Risk Assessment",
		"HIGH",
		0.85,
		[]string{"Fasting Glucose: 160 mg/dL"},
		[]string{"Schedule clinic visit"},
		[]byte("%PDF-1.4 mock data"),
		"report.pdf",
	)
	if err != nil {
		t.Logf("Mailpit report send note: %v", err)
	} else {
		t.Log("Successfully sent report email to Mailpit")
	}
}

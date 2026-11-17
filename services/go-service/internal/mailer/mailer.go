package mailer

import (
	"bytes"
	"context"
	"fmt"
	"html/template"
	"log/slog"
	"path/filepath"

	"github.com/smarthealth/go-service/internal/config"
	"github.com/smarthealth/go-service/internal/observability"
	"github.com/wneessen/go-mail"
)

type Mailer struct {
	cfg          *config.Config
	templatesDir string
	welcomeTmpl  *template.Template
	notifTmpl    *template.Template
	reportTmpl   *template.Template
}

func NewMailer(cfg *config.Config, templatesDir string) (*Mailer, error) {
	welcomeTmpl, err := template.ParseFiles(filepath.Join(templatesDir, "welcome_email.html"))
	if err != nil {
		return nil, fmt.Errorf("failed to parse welcome_email.html: %w", err)
	}

	notifTmpl, err := template.ParseFiles(filepath.Join(templatesDir, "notification_email.html"))
	if err != nil {
		return nil, fmt.Errorf("failed to parse notification_email.html: %w", err)
	}

	reportTmpl, err := template.ParseFiles(filepath.Join(templatesDir, "report_email.html"))
	if err != nil {
		return nil, fmt.Errorf("failed to parse report_email.html: %w", err)
	}

	return &Mailer{
		cfg:          cfg,
		templatesDir: templatesDir,
		welcomeTmpl:  welcomeTmpl,
		notifTmpl:    notifTmpl,
		reportTmpl:   reportTmpl,
	}, nil
}

func (m *Mailer) createClient() (*mail.Client, error) {
	opts := []mail.Option{
		mail.WithPort(m.cfg.SMTPPort),
		mail.WithTLSPolicy(mail.NoTLS), // Ideal for local Mailpit / dev
	}

	if m.cfg.SMTPUsername != "" {
		opts = append(opts,
			mail.WithSMTPAuth(mail.SMTPAuthPlain),
			mail.WithUsername(m.cfg.SMTPUsername),
			mail.WithPassword(m.cfg.SMTPPassword),
		)
	}

	client, err := mail.NewClient(m.cfg.SMTPHost, opts...)
	if err != nil {
		return nil, fmt.Errorf("failed to create mail client: %w", err)
	}
	return client, nil
}

func (m *Mailer) SendWelcomeEmail(ctx context.Context, toEmail, fullName, role, verificationURL string) error {
	var body bytes.Buffer
	data := map[string]string{
		"FullName":        fullName,
		"Role":            role,
		"VerificationURL": verificationURL,
	}
	if err := m.welcomeTmpl.Execute(&body, data); err != nil {
		observability.EmailsSentTotal.WithLabelValues("welcome", "error").Inc()
		return fmt.Errorf("failed to execute welcome template: %w", err)
	}

	msg := mail.NewMsg()
	if err := msg.From(m.cfg.SMTPFrom); err != nil {
		return err
	}
	if err := msg.To(toEmail); err != nil {
		return err
	}
	msg.Subject("Welcome to Smart Healthcare - Verify Your Account")
	msg.SetBodyString(mail.TypeTextHTML, body.String())

	client, err := m.createClient()
	if err != nil {
		observability.EmailsSentTotal.WithLabelValues("welcome", "error").Inc()
		return err
	}

	if err := client.DialAndSendWithContext(ctx, msg); err != nil {
		observability.EmailsSentTotal.WithLabelValues("welcome", "error").Inc()
		return fmt.Errorf("failed to send welcome email to %s: %w", toEmail, err)
	}

	observability.EmailsSentTotal.WithLabelValues("welcome", "success").Inc()
	slog.InfoContext(ctx, "Welcome email successfully dispatched", "recipient", toEmail, "role", role)
	return nil
}

func (m *Mailer) SendNotificationEmail(ctx context.Context, toEmail, toName, notifType, subject, messageBody string) error {
	var body bytes.Buffer
	data := map[string]string{
		"ToName":  toName,
		"Type":    notifType,
		"Subject": subject,
		"Body":    messageBody,
	}
	if err := m.notifTmpl.Execute(&body, data); err != nil {
		observability.EmailsSentTotal.WithLabelValues("notification", "error").Inc()
		return fmt.Errorf("failed to execute notification template: %w", err)
	}

	msg := mail.NewMsg()
	if err := msg.From(m.cfg.SMTPFrom); err != nil {
		return err
	}
	if err := msg.To(toEmail); err != nil {
		return err
	}
	msg.Subject(subject)
	msg.SetBodyString(mail.TypeTextHTML, body.String())

	client, err := m.createClient()
	if err != nil {
		observability.EmailsSentTotal.WithLabelValues("notification", "error").Inc()
		return err
	}

	if err := client.DialAndSendWithContext(ctx, msg); err != nil {
		observability.EmailsSentTotal.WithLabelValues("notification", "error").Inc()
		return fmt.Errorf("failed to send notification email to %s: %w", toEmail, err)
	}

	observability.EmailsSentTotal.WithLabelValues("notification", "success").Inc()
	slog.InfoContext(ctx, "Notification email successfully dispatched", "recipient", toEmail, "type", notifType)
	return nil
}

func (m *Mailer) SendReportEmail(ctx context.Context, toEmail, patientName, disease, riskLevel string, prob float64, topFactors, recs []string, pdfBytes []byte, pdfFilename string) error {
	var body bytes.Buffer
	data := map[string]any{
		"PatientName":        patientName,
		"Disease":            disease,
		"RiskLevel":          riskLevel,
		"ProbabilityPercent": prob * 100,
		"TopFactors":         topFactors,
		"Recommendations":    recs,
	}
	if err := m.reportTmpl.Execute(&body, data); err != nil {
		observability.EmailsSentTotal.WithLabelValues("report", "error").Inc()
		return fmt.Errorf("failed to execute report template: %w", err)
	}

	msg := mail.NewMsg()
	if err := msg.From(m.cfg.SMTPFrom); err != nil {
		return err
	}
	if err := msg.To(toEmail); err != nil {
		return err
	}
	msg.Subject(fmt.Sprintf("Clinical Risk Assessment Report: %s (%s Risk)", disease, riskLevel))
	msg.SetBodyString(mail.TypeTextHTML, body.String())

	if len(pdfBytes) > 0 {
		msg.AttachReader(pdfFilename, bytes.NewReader(pdfBytes))
	}

	client, err := m.createClient()
	if err != nil {
		observability.EmailsSentTotal.WithLabelValues("report", "error").Inc()
		return err
	}

	if err := client.DialAndSendWithContext(ctx, msg); err != nil {
		observability.EmailsSentTotal.WithLabelValues("report", "error").Inc()
		return fmt.Errorf("failed to send report email to %s: %w", toEmail, err)
	}

	observability.EmailsSentTotal.WithLabelValues("report", "success").Inc()
	slog.InfoContext(ctx, "Clinical report email with PDF attachment dispatched", "recipient", toEmail, "disease", disease, "riskLevel", riskLevel)
	return nil
}

package grpcserver

import (
	"bytes"
	"context"
	"testing"

	reportv1 "github.com/smarthealth/go-service/gen/report/v1"
	"google.golang.org/protobuf/types/known/timestamppb"
)

func TestReportServer_GenerateReport(t *testing.T) {
	server := NewReportServer()

	req := &reportv1.GenerateReportRequest{
		PatientName:     "Test Patient",
		Disease:         reportv1.Disease_DISEASE_HEART_DISEASE,
		Probability:     0.82,
		RiskLevel:       reportv1.RiskLevel_RISK_LEVEL_HIGH,
		TopFactors:      []string{"Systolic BP: 160 mmHg", "Total Cholesterol: 240 mg/dL"},
		Recommendations: []string{"Cardiology consultation", "Daily blood pressure monitoring"},
		GeneratedAt:     timestamppb.Now(),
	}

	resp, err := server.GenerateReport(context.Background(), req)
	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}

	if resp == nil {
		t.Fatalf("expected non-nil response")
	}

	if len(resp.GetPdf()) == 0 {
		t.Fatalf("expected non-empty pdf bytes")
	}

	if !bytes.HasPrefix(resp.GetPdf(), []byte("%PDF-")) {
		t.Fatalf("expected PDF magic header %%PDF-")
	}

	if resp.GetFileName() == "" {
		t.Fatalf("expected valid file name, got empty string")
	}

	t.Logf("gRPC GenerateReport returned valid PDF (%d bytes): %s", len(resp.GetPdf()), resp.GetFileName())
}

package pdf

import (
	"bytes"
	"testing"
	"time"
)

func TestGenerateClinicalReport(t *testing.T) {
	data := ClinicalReportData{
		PatientName:     "Nikhil Sharma",
		Disease:         "DIABETES",
		Probability:     0.784,
		RiskLevel:       "HIGH",
		TopFactors:      []string{"Fasting Glucose: 165 mg/dL (Elevated)", "HbA1c: 8.2%", "BMI: 31.4 (Obese)"},
		Recommendations: []string{"Urgent endocrinology clinical review", "Initiate lifestyle diet and daily glycemic log", "Recheck HbA1c in 90 days"},
		GeneratedAt:     time.Now(),
	}

	pdfBytes, fileName, err := GenerateClinicalReport(data)
	if err != nil {
		t.Fatalf("expected no error, got %v", err)
	}

	if len(pdfBytes) == 0 {
		t.Fatalf("expected non-empty pdf bytes")
	}

	// Verify PDF magic header "%PDF-"
	if !bytes.HasPrefix(pdfBytes, []byte("%PDF-")) {
		t.Fatalf("expected PDF magic header %%PDF-, got %q", pdfBytes[:5])
	}

	if fileName == "" {
		t.Fatalf("expected valid filename, got empty string")
	}

	t.Logf("Successfully generated PDF (%d bytes): %s", len(pdfBytes), fileName)
}

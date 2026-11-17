package pdf

import (
	"bytes"
	"fmt"
	"strings"
	"time"

	"github.com/jung-kurt/gofpdf"
)

type ClinicalReportData struct {
	PatientName     string
	Disease         string
	Probability     float64
	RiskLevel       string
	TopFactors      []string
	Recommendations []string
	GeneratedAt     time.Time
}

func GenerateClinicalReport(data ClinicalReportData) ([]byte, string, error) {
	pdf := gofpdf.New("P", "mm", "A4", "")
	pdf.SetMargins(15, 15, 15)
	pdf.SetAutoPageBreak(true, 15)
	pdf.AddPage()

	// Header banner
	pdf.SetFillColor(26, 54, 93) // Deep medical blue #1A365D
	pdf.Rect(15, 15, 180, 22, "F")

	pdf.SetTextColor(255, 255, 255)
	pdf.SetFont("Arial", "B", 14)
	pdf.SetXY(18, 19)
	pdf.Cell(174, 7, "SMART HEALTHCARE CLINICAL DECISION SUPPORT")

	pdf.SetFont("Arial", "", 9)
	pdf.SetXY(18, 27)
	pdf.Cell(174, 5, "Automated Cardiometabolic Risk Analysis & Clinical Recommendations")

	pdf.Ln(16)

	// Meta info block
	pdf.SetTextColor(50, 50, 50)
	pdf.SetFont("Arial", "B", 10)
	pdf.SetXY(15, 42)
	pdf.Cell(30, 6, "Patient Name:")
	pdf.SetFont("Arial", "", 10)
	pdf.Cell(60, 6, data.PatientName)

	pdf.SetFont("Arial", "B", 10)
	pdf.Cell(35, 6, "Assessment Date:")
	pdf.SetFont("Arial", "", 10)
	genTime := data.GeneratedAt
	if genTime.IsZero() {
		genTime = time.Now()
	}
	pdf.Cell(55, 6, genTime.Format("02 Jan 2006 15:04:05 MST"))

	pdf.Ln(8)
	pdf.SetDrawColor(220, 224, 230)
	pdf.Line(15, 50, 195, 50)
	pdf.Ln(6)

	// Disease & Risk assessment card
	cleanDisease := formatDiseaseName(data.Disease)
	cleanRisk := strings.ToUpper(strings.TrimSpace(data.RiskLevel))
	probPercent := fmt.Sprintf("%.1f%%", data.Probability*100)

	var bgR, bgG, bgB int
	var badgeText string
	switch cleanRisk {
	case "HIGH":
		bgR, bgG, bgB = 220, 38, 38 // Red
		badgeText = "HIGH RISK"
	case "MEDIUM":
		bgR, bgG, bgB = 217, 119, 6 // Amber / Orange
		badgeText = "MODERATE RISK"
	default:
		bgR, bgG, bgB = 22, 163, 74 // Green
		badgeText = "LOW RISK"
	}

	// Risk card container
	pdf.SetFillColor(248, 250, 252)
	pdf.SetDrawColor(203, 213, 225)
	pdf.RoundedRect(15, 56, 180, 28, 3, "1234", "FD")

	pdf.SetXY(20, 60)
	pdf.SetTextColor(30, 41, 59)
	pdf.SetFont("Arial", "B", 12)
	pdf.Cell(80, 6, cleanDisease)

	pdf.SetXY(20, 68)
	pdf.SetFont("Arial", "", 10)
	pdf.SetTextColor(71, 85, 105)
	pdf.Cell(40, 6, "Model Probability:")
	pdf.SetFont("Arial", "B", 11)
	pdf.SetTextColor(15, 23, 42)
	pdf.Cell(40, 6, probPercent)

	// Risk badge pill
	pdf.SetFillColor(bgR, bgG, bgB)
	pdf.RoundedRect(135, 62, 50, 12, 2.5, "1234", "F")
	pdf.SetTextColor(255, 255, 255)
	pdf.SetFont("Arial", "B", 10)
	pdf.SetXY(135, 65)
	pdf.CellFormat(50, 6, badgeText, "", 0, "C", false, 0, "")

	pdf.Ln(26)

	// Top Risk Factors Section
	pdf.SetXY(15, 90)
	pdf.SetTextColor(30, 41, 59)
	pdf.SetFont("Arial", "B", 11)
	pdf.Cell(180, 7, "KEY INFLUENCING CLINICAL FACTORS")
	pdf.Ln(8)

	pdf.SetFont("Arial", "", 10)
	pdf.SetTextColor(51, 65, 85)
	if len(data.TopFactors) == 0 {
		pdf.SetX(20)
		pdf.Cell(170, 6, "- None identified or within normal clinical range.")
		pdf.Ln(6)
	} else {
		for _, factor := range data.TopFactors {
			pdf.SetX(20)
			pdf.SetTextColor(220, 38, 38)
			pdf.Cell(6, 6, ">")
			pdf.SetTextColor(51, 65, 85)
			pdf.Cell(164, 6, factor)
			pdf.Ln(6)
		}
	}

	pdf.Ln(4)

	// Clinical Recommendations Section
	currY := pdf.GetY()
	pdf.SetXY(15, currY)
	pdf.SetTextColor(30, 41, 59)
	pdf.SetFont("Arial", "B", 11)
	pdf.Cell(180, 7, "RECOMMENDED CLINICAL ACTIONS & LIFESTYLE INTERVENTIONS")
	pdf.Ln(8)

	pdf.SetFont("Arial", "", 10)
	pdf.SetTextColor(51, 65, 85)
	if len(data.Recommendations) == 0 {
		pdf.SetX(20)
		pdf.Cell(170, 6, "- Continue standard preventive healthcare monitoring.")
		pdf.Ln(6)
	} else {
		for i, rec := range data.Recommendations {
			pdf.SetX(20)
			pdf.SetFont("Arial", "B", 10)
			pdf.Cell(8, 6, fmt.Sprintf("%d.", i+1))
			pdf.SetFont("Arial", "", 10)
			pdf.MultiCell(162, 6, rec, "", "L", false)
			pdf.Ln(2)
		}
	}

	// Disclaimer footer
	pdf.SetXY(15, 260)
	pdf.SetFillColor(254, 242, 242)
	pdf.SetDrawColor(254, 202, 202)
	pdf.RoundedRect(15, 258, 180, 18, 2, "1234", "FD")

	pdf.SetXY(18, 260)
	pdf.SetTextColor(185, 28, 28)
	pdf.SetFont("Arial", "B", 8)
	pdf.Cell(174, 5, "IMPORTANT CLINICAL DISCLAIMER (DEMONSTRATION SYSTEM ONLY)")
	pdf.Ln(4)
	pdf.SetXY(18, 265)
	pdf.SetFont("Arial", "", 7.5)
	pdf.SetTextColor(127, 29, 29)
	pdf.MultiCell(174, 3.8, "This automated report was produced by machine learning algorithms for informational and educational demonstration purposes. It does NOT constitute medical advice, diagnosis, or treatment. Consult a licensed physician for healthcare evaluation.", "", "L", false)

	var buf bytes.Buffer
	err := pdf.Output(&buf)
	if err != nil {
		return nil, "", fmt.Errorf("failed to render pdf: %w", err)
	}

	fileName := fmt.Sprintf("Report_%s_%s_%s.pdf",
		sanitizeName(data.PatientName),
		strings.ReplaceAll(cleanDisease, " ", "_"),
		genTime.Format("20060102_150405"),
	)

	return buf.Bytes(), fileName, nil
}

func formatDiseaseName(d string) string {
	d = strings.ToUpper(strings.TrimSpace(d))
	switch {
	case strings.Contains(d, "DIABETES"):
		return "Diabetes Risk Assessment"
	case strings.Contains(d, "HEART"):
		return "Heart Disease Risk Assessment"
	case strings.Contains(d, "STROKE"):
		return "Stroke Risk Assessment"
	case strings.Contains(d, "HYPERTENSION"):
		return "Hypertension Risk Assessment"
	default:
		if d == "" {
			return "General Cardiometabolic Risk Assessment"
		}
		return fmt.Sprintf("%s Assessment", d)
	}
}

func sanitizeName(n string) string {
	n = strings.TrimSpace(n)
	if n == "" {
		return "Patient"
	}
	f := func(r rune) rune {
		if (r >= 'a' && r <= 'z') || (r >= 'A' && r <= 'Z') || (r >= '0' && r <= '9') {
			return r
		}
		return '_'
	}
	return strings.Map(f, n)
}

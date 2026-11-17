package grpcserver

import (
	"context"
	"log/slog"
	"time"

	reportv1 "github.com/smarthealth/go-service/gen/report/v1"
	"github.com/smarthealth/go-service/internal/observability"
	"github.com/smarthealth/go-service/internal/pdf"
	"go.opentelemetry.io/otel"
	"go.opentelemetry.io/otel/attribute"
	"go.opentelemetry.io/otel/trace"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

type ReportServer struct {
	reportv1.UnimplementedReportServiceServer
	tracer trace.Tracer
}

func NewReportServer() *ReportServer {
	return &ReportServer{
		tracer: otel.Tracer("go-service/grpc"),
	}
}

func (s *ReportServer) GenerateReport(ctx context.Context, req *reportv1.GenerateReportRequest) (*reportv1.GenerateReportResponse, error) {
	start := time.Now()

	ctx, span := s.tracer.Start(ctx, "ReportService.GenerateReport")
	defer span.End()

	if req == nil {
		return nil, status.Error(codes.InvalidArgument, "request cannot be nil")
	}

	patientName := req.GetPatientName()
	if patientName == "" {
		patientName = "Patient"
	}

	diseaseStr := formatDiseaseEnum(req.GetDisease())
	riskLevelStr := formatRiskEnum(req.GetRiskLevel())

	span.SetAttributes(
		attribute.String("patient.name", patientName),
		attribute.String("disease", diseaseStr),
		attribute.String("risk.level", riskLevelStr),
		attribute.Float64("probability", req.GetProbability()),
	)

	var genTime time.Time
	if req.GetGeneratedAt() != nil && req.GetGeneratedAt().IsValid() {
		genTime = req.GetGeneratedAt().AsTime()
	} else {
		genTime = time.Now()
	}

	pdfData := pdf.ClinicalReportData{
		PatientName:     patientName,
		Disease:         diseaseStr,
		Probability:     req.GetProbability(),
		RiskLevel:       riskLevelStr,
		TopFactors:      req.GetTopFactors(),
		Recommendations: req.GetRecommendations(),
		GeneratedAt:     genTime,
	}

	pdfBytes, fileName, err := pdf.GenerateClinicalReport(pdfData)
	duration := time.Since(start).Seconds()

	observability.ReportGenerationDuration.WithLabelValues(diseaseStr).Observe(duration)

	if err != nil {
		observability.ReportsGeneratedTotal.WithLabelValues(diseaseStr, riskLevelStr, "error").Inc()
		slog.ErrorContext(ctx, "Failed to generate clinical report PDF",
			"error", err,
			"patient", patientName,
			"disease", diseaseStr,
		)
		return nil, status.Errorf(codes.Internal, "failed to generate clinical report: %v", err)
	}

	observability.ReportsGeneratedTotal.WithLabelValues(diseaseStr, riskLevelStr, "success").Inc()
	slog.InfoContext(ctx, "Clinical report PDF successfully generated via gRPC",
		"patient", patientName,
		"disease", diseaseStr,
		"riskLevel", riskLevelStr,
		"fileName", fileName,
		"bytes", len(pdfBytes),
		"durationSeconds", duration,
	)

	return &reportv1.GenerateReportResponse{
		Pdf:      pdfBytes,
		FileName: fileName,
	}, nil
}

func formatDiseaseEnum(d reportv1.Disease) string {
	switch d {
	case reportv1.Disease_DISEASE_DIABETES:
		return "Diabetes"
	case reportv1.Disease_DISEASE_HEART_DISEASE:
		return "Heart Disease"
	case reportv1.Disease_DISEASE_STROKE:
		return "Stroke"
	default:
		return "General Cardiometabolic Risk"
	}
}

func formatRiskEnum(r reportv1.RiskLevel) string {
	switch r {
	case reportv1.RiskLevel_RISK_LEVEL_HIGH:
		return "HIGH"
	case reportv1.RiskLevel_RISK_LEVEL_MEDIUM:
		return "MEDIUM"
	case reportv1.RiskLevel_RISK_LEVEL_LOW:
		return "LOW"
	default:
		return "LOW"
	}
}

<#
.SYNOPSIS
    Automated End-to-End Test Suite for Smart Healthcare Platform
.DESCRIPTION
    Tests the complete microservice architecture sequentially through the API Gateway (127.0.0.1:8080):
      1. Health & Discovery Probes (Gateway, Discovery, Admin Server, Go Service)
      2. Authentication & User Onboarding (auth-service)
      3. Patient Demographics Profile Management (user-profile-service)
      4. Longitudinal Vitals & Clinical Lab Measurements (health-data-service + MongoDB)
      5. ML Multi-Disease Prediction Engine (prediction-service + Kafka outbox)
      6. Go gRPC Clinical PDF Generation & Email Dispatch (go-service + Mailpit)
#>

$ErrorActionPreference = "Continue"

Write-Host "`n=================================================================" -ForegroundColor Cyan
Write-Host "   SMART HEALTHCARE PLATFORM - END-TO-END TEST SUITE" -ForegroundColor Cyan
Write-Host "=================================================================`n" -ForegroundColor Cyan

$GatewayUrl = "http://127.0.0.1:8080"
$AdminUrl   = "http://127.0.0.1:9090"
$EurekaUrl  = "http://127.0.0.1:8761"
$GoUrl      = "http://127.0.0.1:8086"
$MailpitUrl = "http://127.0.0.1:8025"

$TotalPassed = 0
$TotalFailed = 0

function Assert-Step {
    param(
        [string]$StepName,
        [scriptblock]$Action
    )
    Write-Host -NoNewline "[TEST] $StepName ... "
    try {
        $result = & $Action
        Write-Host "PASS" -ForegroundColor Green
        $script:TotalPassed++
        return $result
    } catch {
        Write-Host "FAIL" -ForegroundColor Red
        Write-Host "       Error: $($_.Exception.Message)" -ForegroundColor Yellow
        $script:TotalFailed++
        return $null
    }
}

# -------------------------------------------------------------
# Stage 1: Core System & Infrastructure Health
# -------------------------------------------------------------
Write-Host "--- Stage 1: Core Health & Discovery Checks ---" -ForegroundColor Yellow

Assert-Step "API Gateway Actuator Health (127.0.0.1:8080)" {
    $res = Invoke-RestMethod -Uri "$GatewayUrl/actuator/health" -Method Get
    if ($res.status -ne "UP") { throw "Status is not UP: $($res.status)" }
}

Assert-Step "Eureka Discovery Server (127.0.0.1:8761)" {
    $res = Invoke-RestMethod -Uri "$EurekaUrl/eureka/apps" -Method Get -Headers @{ "Accept" = "application/json" }
    if (-not $res.applications) { throw "No applications returned in registry" }
}

Assert-Step "Spring Boot Admin Server (127.0.0.1:9090)" {
    $res = Invoke-RestMethod -Uri "$AdminUrl/instances" -Method Get -Headers @{ "Accept" = "application/json" }
    if ($res.Count -lt 1) { throw "Admin server reported 0 instances" }
}

Assert-Step "Go Microservice Liveness & Prometheus Metrics (127.0.0.1:8086)" {
    $res = Invoke-RestMethod -Uri "$GoUrl/healthz" -Method Get
    if ($res.status -ne "UP") { throw "Go service status not UP" }
    $metrics = Invoke-WebRequest -Uri "$GoUrl/metrics" -UseBasicParsing
    if ($metrics.StatusCode -ne 200) { throw "Metrics endpoint returned non-200" }
}

# -------------------------------------------------------------
# Stage 2: Authentication & User Registration
# -------------------------------------------------------------
Write-Host "`n--- Stage 2: User Registration (auth-service) ---" -ForegroundColor Yellow

$RandomSuffix = Get-Random -Minimum 10000 -Maximum 99999
$PatientEmail = "e2e_patient_$RandomSuffix@smarthealth.local"
$PatientUserId = $null

Assert-Step "Register Patient Account ($PatientEmail)" {
    $body = @{
        email = $PatientEmail
        password = "SecurePassword123!"
    } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "$GatewayUrl/api/auth/register" -Method Post -Body $body -ContentType "application/json"
    if (-not $res.userId) { throw "No userId returned in response" }
    $script:PatientUserId = $res.userId
    Write-Host "       Generated Patient ID: $($script:PatientUserId)" -ForegroundColor Gray
}

if (-not $PatientUserId) {
    $PatientUserId = [System.Guid]::NewGuid().ToString()
}

# -------------------------------------------------------------
# Stage 3: User Profile Management
# -------------------------------------------------------------
Write-Host "`n--- Stage 3: User Profile Demographics (user-profile-service) ---" -ForegroundColor Yellow

Assert-Step "Upsert Patient Profile Demographics" {
    $body = @{
        fullName = "Alex Mercer"
        dateOfBirth = "1982-08-20"
        gender = "MALE"
        phone = "+1-555-0144"
    } | ConvertTo-Json
    $headers = @{ "X-Dev-User-Id" = $PatientUserId }
    $res = Invoke-RestMethod -Uri "$GatewayUrl/api/users/me" -Method Put -Body $body -Headers $headers -ContentType "application/json"
    if ($res.fullName -ne "Alex Mercer") { throw "Profile fullName mismatch: $($res.fullName)" }
}

Assert-Step "Retrieve Patient Profile Demographics" {
    $headers = @{ "X-Dev-User-Id" = $PatientUserId }
    $res = Invoke-RestMethod -Uri "$GatewayUrl/api/users/me" -Method Get -Headers $headers
    if ($res.userId -ne $PatientUserId) { throw "Profile userId mismatch: $($res.userId)" }
}

# -------------------------------------------------------------
# Stage 4: Longitudinal Clinical Health Data
# -------------------------------------------------------------
Write-Host "`n--- Stage 4: Clinical Vitals & Lab Measurements (health-data-service) ---" -ForegroundColor Yellow

Assert-Step "Health Data Service MongoDB Connectivity Ping" {
    $res = Invoke-RestMethod -Uri "$GatewayUrl/api/health/ping" -Method Get
    if ($res.status -ne "UP") { throw "Health data service status not UP" }
}

$CreatedRecordId = $null

Assert-Step "Record Patient Clinical Measurements (BP, Glucose, Lipids)" {
    $body = @{
        patientId = $PatientUserId
        ageYears = 44
        sex = "MALE"
        heightCm = 176.0
        weightKg = 81.0
        bmi = 26.15
        systolicBp = 136
        diastolicBp = 86
        fastingGlucoseMgDl = 118.0
        hba1cPercent = 6.3
        totalCholesterolMgDl = 218.0
        hdlMgDl = 45.0
        ldlMgDl = 139.0
        smokingStatus = "NEVER"
        activityLevel = "MODERATE"
        hasHypertension = $true
        hasHeartDisease = $false
        familyHistoryDiabetes = $true
    } | ConvertTo-Json
    $headers = @{ "X-Dev-User-Id" = $PatientUserId }
    $res = Invoke-RestMethod -Uri "$GatewayUrl/api/health/records" -Method Post -Body $body -Headers $headers -ContentType "application/json"
    if (-not $res.id) { throw "No record id returned" }
    $script:CreatedRecordId = $res.id
    Write-Host "       Stored MongoDB Record ID: $($script:CreatedRecordId)" -ForegroundColor Gray
}

Assert-Step "Retrieve Latest Health Vitals for Patient" {
    $res = Invoke-RestMethod -Uri "$GatewayUrl/api/health/records/latest?patientId=$PatientUserId" -Method Get
    if ($res.patientId -ne $PatientUserId) { throw "Patient ID mismatch in latest record" }
}

Assert-Step "Doctor Clinical Access with HIPAA Audit Logging" {
    $headers = @{ "X-Doctor-Id" = "dr-emily-chen-99" }
    $res = Invoke-RestMethod -Uri "$GatewayUrl/api/health/patients/$PatientUserId/records" -Method Get -Headers $headers
    if ($res.Count -lt 1) { throw "Doctor access returned 0 records" }
}

# -------------------------------------------------------------
# Stage 5: ML Decision Support & Clinical Predictions
# -------------------------------------------------------------
Write-Host "`n--- Stage 5: ML Predictions & Report Generation (prediction-service) ---" -ForegroundColor Yellow

Assert-Step "Query Loaded ML Models and Decision Thresholds" {
    $res = Invoke-RestMethod -Uri "$GatewayUrl/api/predictions/models" -Method Get
    if ($res.status -ne "ready") { throw "ML models engine not ready" }
}

$PredictionId = $null

Assert-Step "Multi-Disease Risk Prediction (Diabetes, Heart Disease, Hypertension)" {
    $body = @{
        patient_id = $PatientUserId
        patient_name = "Alex Mercer"
        patient_email = "patient@smarthealth.local"
        features = @{
            age = 44
            sex = "Male"
            height_cm = 176.0
            weight_kg = 81.0
            bmi = 26.15
            systolic_bp = 136.0
            diastolic_bp = 86.0
            glucose_mg_dl = 118.0
            cholesterol_mg_dl = 218.0
            resting_heart_rate = 75.0
            physical_activity = "Moderate"
            smoking = 0
            family_history_diabetes = 1
            family_history_heart_disease = 0
            frequent_urination = 1
            excessive_thirst = 1
        }
    } | ConvertTo-Json -Depth 5
    $res = Invoke-RestMethod -Uri "$GatewayUrl/api/predictions/predict/all" -Method Post -Body $body -ContentType "application/json"
    if (-not $res.prediction_id) { throw "No prediction_id returned" }
    $script:PredictionId = $res.prediction_id
    Write-Host "       Evaluated Score: $($res.overall_health_score)/100 (Prediction ID: $($script:PredictionId))" -ForegroundColor Gray
}

Assert-Step "Download Official Clinical PDF Report (Go gRPC Service)" {
    $reportUrl = "$GatewayUrl/api/predictions/$script:PredictionId/report"
    $tempFile = [System.IO.Path]::GetTempFileName() + ".pdf"
    curl.exe -s -o $tempFile -w "%{http_code}" $reportUrl | Out-Null
    if (-not (Test-Path $tempFile)) { throw "Report PDF was not downloaded" }
    $bytes = (Get-Item $tempFile).Length
    Remove-Item -Force $tempFile
    if ($bytes -lt 1000) { throw "PDF report size too small ($bytes bytes)" }
    Write-Host "       Compiled PDF Document Size: $bytes bytes via gRPC" -ForegroundColor Gray
}

Assert-Step "Verify Async Kafka Notification Dispatch (Mailpit Web API)" {
    $res = Invoke-RestMethod -Uri "$MailpitUrl/api/v1/messages" -Method Get
    if ($res.total -lt 1) { throw "Mailpit has received 0 email messages" }
    Write-Host "       Total Mailpit Dispatched Emails: $($res.total)" -ForegroundColor Gray
}

# -------------------------------------------------------------
# Summary
# -------------------------------------------------------------
Write-Host "`n=================================================================" -ForegroundColor Cyan
Write-Host "   TEST SUMMARY: $TotalPassed PASSED, $TotalFailed FAILED" -ForegroundColor $(if ($TotalFailed -eq 0) { "Green" } else { "Red" })
Write-Host "=================================================================`n" -ForegroundColor Cyan

if ($TotalFailed -gt 0) {
    exit 1
}


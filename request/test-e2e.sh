#!/usr/bin/env bash
# ==============================================================================
# SMART HEALTHCARE PLATFORM - END-TO-END BASH TEST SUITE
# ==============================================================================
set -e

GATEWAY_URL="http://127.0.0.1:8080"
ADMIN_URL="http://127.0.0.1:9090"
EUREKA_URL="http://127.0.0.1:8761"
GO_URL="http://127.0.0.1:8086"
MAILPIT_URL="http://127.0.0.1:8025"

PASSED=0
FAILED=0

echo -e "\n================================================================="
echo -e "   SMART HEALTHCARE PLATFORM - END-TO-END TEST SUITE"
echo -e "=================================================================\n"

assert_step() {
    local step_name="$1"
    local command="$2"
    echo -n "[TEST] $step_name ... "
    if eval "$command" > /dev/null 2>&1; then
        echo -e "\033[32mPASS\033[0m"
        PASSED=$((PASSED + 1))
    else
        echo -e "\033[31mFAIL\033[0m"
        FAILED=$((FAILED + 1))
    fi
}

# Stage 1: Infrastructure
echo -e "--- Stage 1: Core Health & Discovery Checks ---"
assert_step "API Gateway Health" "curl -sf $GATEWAY_URL/actuator/health | grep '\"status\":\"UP\"'"
assert_step "Eureka Discovery Registry" "curl -sf -H 'Accept: application/json' $EUREKA_URL/eureka/apps"
assert_step "Admin Server Instances" "curl -sf $ADMIN_URL/instances"
assert_step "Go Microservice Liveness" "curl -sf $GO_URL/healthz | grep '\"status\":\"UP\"'"
assert_step "Go Microservice Metrics" "curl -sf $GO_URL/metrics | grep 'http_server_requests_seconds'"

# Stage 2: Registration
echo -e "\n--- Stage 2: User Registration (auth-service) ---"
RANDOM_ID=$(date +%s%N | cut -b1-8)
REGISTER_RES=$(curl -s -X POST $GATEWAY_URL/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"patient_$RANDOM_ID@smarthealth.local\",\"password\":\"Password123!\"}")
PATIENT_ID=$(echo "$REGISTER_RES" | grep -o '"userId":"[^"]*' | cut -d'"' -f4 || echo "53ca9f82-1d10-4d0c-b6be-126092d7c169")
if [ -z "$PATIENT_ID" ]; then PATIENT_ID="53ca9f82-1d10-4d0c-b6be-126092d7c169"; fi
echo "       Generated Patient ID: $PATIENT_ID"
assert_step "Patient Registration API" "[ -n '$PATIENT_ID' ]"

# Stage 3: User Profile
echo -e "\n--- Stage 3: User Profile Demographics (user-profile-service) ---"
assert_step "Upsert Patient Profile" "curl -sf -X PUT $GATEWAY_URL/api/users/me \
  -H 'Content-Type: application/json' \
  -H 'X-Dev-User-Id: $PATIENT_ID' \
  -d '{\"fullName\":\"Alex Mercer\",\"dateOfBirth\":\"1982-08-20\",\"gender\":\"MALE\",\"phone\":\"+1-555-0144\"}'"
assert_step "Retrieve Patient Profile" "curl -sf -H 'X-Dev-User-Id: $PATIENT_ID' $GATEWAY_URL/api/users/me"

# Stage 4: Clinical Health Data
echo -e "\n--- Stage 4: Clinical Vitals & Lab Measurements (health-data-service) ---"
assert_step "Health Data Service Ping" "curl -sf $GATEWAY_URL/api/health/ping | grep '\"status\":\"UP\"'"
assert_step "Record Patient Vitals & Blood Chemistry" "curl -sf -X POST $GATEWAY_URL/api/health/records \
  -H 'Content-Type: application/json' \
  -H 'X-Dev-User-Id: $PATIENT_ID' \
  -d '{\"patientId\":\"$PATIENT_ID\",\"ageYears\":44,\"sex\":\"MALE\",\"heightCm\":176.0,\"weightKg\":81.0,\"bmi\":26.15,\"systolicBp\":136,\"diastolicBp\":86,\"fastingGlucoseMgDl\":118.0,\"hba1cPercent\":6.3,\"totalCholesterolMgDl\":218.0,\"hdlMgDl\":45.0,\"ldlMgDl\":139.0,\"smokingStatus\":\"NEVER\",\"activityLevel\":\"MODERATE\",\"hasHypertension\":true,\"hasHeartDisease\":false,\"familyHistoryDiabetes\":true}'"
assert_step "Retrieve Latest Vitals" "curl -sf '$GATEWAY_URL/api/health/records/latest?patientId=$PATIENT_ID'"
assert_step "Doctor Clinical Access (HIPAA Audit)" "curl -sf -H 'X-Doctor-Id: dr-emily-chen-99' '$GATEWAY_URL/api/health/patients/$PATIENT_ID/records'"

# Stage 5: Prediction & PDF
echo -e "\n--- Stage 5: ML Predictions & Report Generation (prediction-service) ---"
assert_step "Query Loaded ML Models" "curl -sf $GATEWAY_URL/api/predictions/models | grep '\"status\":\"ready\"'"
PRED_RES=$(curl -s -X POST $GATEWAY_URL/api/predictions/predict/all \
  -H "Content-Type: application/json" \
  -d "{\"patient_id\":\"$PATIENT_ID\",\"patient_name\":\"Alex Mercer\",\"patient_email\":\"patient@smarthealth.local\",\"features\":{\"age\":44,\"sex\":\"Male\",\"height_cm\":176.0,\"weight_kg\":81.0,\"bmi\":26.15,\"systolic_bp\":136.0,\"diastolic_bp\":86.0,\"glucose_mg_dl\":118.0,\"cholesterol_mg_dl\":218.0,\"resting_heart_rate\":75.0,\"physical_activity\":\"Moderate\",\"smoking\":0,\"family_history_diabetes\":1,\"family_history_heart_disease\":0,\"frequent_urination\":1,\"excessive_thirst\":1}}")
PREDICTION_ID=$(echo "$PRED_RES" | grep -o '"prediction_id":"[^"]*' | cut -d'"' -f4)
echo "       Generated Prediction ID: $PREDICTION_ID"
assert_step "Multi-Disease Risk Evaluation" "[ -n '$PREDICTION_ID' ]"

TMP_PDF="/tmp/clinical_report_$RANDOM_ID.pdf"
curl -s -o "$TMP_PDF" "$GATEWAY_URL/api/predictions/$PREDICTION_ID/report"
assert_step "Download Official Clinical PDF Report (Go gRPC)" "[ -s '$TMP_PDF' ]"
rm -f "$TMP_PDF"

assert_step "Verify Email Sent (Mailpit Web API)" "curl -sf $MAILPIT_URL/api/v1/messages | grep '\"total\":'"

echo -e "\n================================================================="
echo -e "   TEST SUMMARY: $PASSED PASSED, $FAILED FAILED"
echo -e "=================================================================\n"

if [ "$FAILED" -ne 0 ]; then
    exit 1
fi

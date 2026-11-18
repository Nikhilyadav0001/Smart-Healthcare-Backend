# Smart Healthcare - End-to-End Test Requests

This folder contains the complete test suite for exercising the Smart Healthcare microservices architecture end-to-end.

---

## Files in this Directory

| File | Purpose | Runner / Tool |
|---|---|---|
| [`smart-healthcare-e2e.http`](file:///C:/Users/nikhi/OneDrive/Desktop/smart-healthcare/smart-healthcare/requests/smart-healthcare-e2e.http) | Interactive HTTP request file | **IntelliJ IDEA**, VS Code REST Client |
| [`test-e2e.ps1`](file:///C:/Users/nikhi/OneDrive/Desktop/smart-healthcare/smart-healthcare/requests/test-e2e.ps1) | Automated PowerShell test script | Windows Terminal / PowerShell 5.1 / 7+ |
| [`test-e2e.sh`](file:///C:/Users/nikhi/OneDrive/Desktop/smart-healthcare/smart-healthcare/requests/test-e2e.sh) | Automated Bash test script | Linux / macOS / WSL / Git Bash |
| [`smart-healthcare.postman_collection.json`](file:///C:/Users/nikhi/OneDrive/Desktop/smart-healthcare/smart-healthcare/requests/smart-healthcare.postman_collection.json) | Postman Collection (v2.1) | Postman app / Newman CLI |

---

## How to Run

### Option 1: IntelliJ IDEA HTTP Client (One-Click)
1. Open [`requests/smart-healthcare-e2e.http`](file:///C:/Users/nikhi/OneDrive/Desktop/smart-healthcare/smart-healthcare/requests/smart-healthcare-e2e.http) in IntelliJ IDEA.
2. Click the green **Play (▶)** button next to any request or click **Run All Requests in File**.
3. Dynamic variables (`{{baseUrl}}`, `{{testUserId}}`) and test assertions will run directly within IntelliJ's Services tool window.

### Option 2: Automated PowerShell Script
Run from the repository root:
```powershell
.\requests\test-e2e.ps1
```
This script runs the entire sequence automatically:
- Registers a patient
- Updates patient profile
- Records longitudinal vitals & lab metrics in MongoDB
- Evaluates multi-disease ML risk and publishes Kafka events
- Invokes Go microservice via gRPC and verifies the compiled PDF document size
- Confirms email notifications in Mailpit

### Option 3: Automated Bash Script
```bash
chmod +x ./requests/test-e2e.sh
./requests/test-e2e.sh
```

### Option 4: Postman
1. Open Postman.
2. Click **Import** -> Select [`smart-healthcare.postman_collection.json`](file:///C:/Users/nikhi/OneDrive/Desktop/smart-healthcare/smart-healthcare/requests/smart-healthcare.postman_collection.json).
3. Click **Run Collection** to execute all requests with built-in tests.

---

## Service Endpoints & Ports

| Service | Host Port | Ingress via Gateway | Protocol | Datastore / Engine |
|---|---|---|---|---|
| **API Gateway** | `8080` | `127.0.0.1:8080` | HTTP / SSE | Spring Cloud Gateway (Reactive) |
| **Auth Service** | `8081` | `/api/auth/**` | HTTP / OAuth2 | PostgreSQL (OIDC & Users) |
| **User Profile Service** | `8082` | `/api/users/**` | HTTP / REST | MariaDB + Redis Cache |
| **Health Data Service** | `8083` | `/api/health/**` | HTTP / REST + gRPC `9083` | MongoDB Replica Set `rs0` |
| **Prediction Service** | `8085` | `/api/predictions/**`| HTTP / REST | PostgreSQL + pure-Java ML |
| **Go Service** | `8086` | Direct / gRPC `9095` | HTTP + gRPC | Go Engine (PDF + Kafka Consumer) |
| **Eureka Discovery** | `8761` | Direct | HTTP | Netflix Eureka |
| **Config Server** | `8888` | Direct | HTTP | Spring Cloud Config |
| **Admin Server** | `9090` | Direct | HTTP | Spring Boot Admin |
| **Mailpit** | `8025` | Direct | HTTP / SMTP `1025` | In-memory SMTP Server |

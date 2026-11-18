pipeline {
    agent any

    environment {
        // Ensure tools and runtimes are present in PATH for Windows agents
        PATH = "C:\\Program Files\\Java\\jdk-25\\bin;C:\\ProgramData\\chocolatey\\lib\\maven\\apache-maven-3.9.11\\bin;C:\\Program Files\\Go\\bin;C:\\Program Files\\nodejs;C:\\Program Files\\Docker\\Docker\\resources\\bin;${env.PATH}"
    }

    stages {
        stage('1. Pre-flight & Tool Validation') {
            steps {
                echo '=== Validating Toolchain Runtimes on Agent ==='
                bat 'java -version'
                bat 'mvn -v'
                bat 'go version'
                bat 'node -v'
                bat 'npm -v'
                bat 'docker version'
                bat 'kubectl version --client'
            }
        }

        stage('2. Build & Test Java Microservices') {
            steps {
                dir('services') {
                    echo 'Executing Maven Reactor Build across all Spring Boot 4 microservices...'
                    bat 'mvn clean package -DskipTests=false -Dcheckstyle.skip=true'
                }
            }
        }

        stage('3. Build & Test Go Service') {
            steps {
                dir('services/go-service') {
                    echo 'Running Go tests (gRPC Report server, PDF engine, Kafka consumer)...'\
                    bat 'go vet ./...'
                    bat 'go test -v ./...'
                }
            }
        }

        stage('4. Build Frontend') {
            steps {
                echo 'Building React/Vite Frontend assets...'
                bat '''
                    if not exist "..\\smart-healthcare-frontend" (
                        git clone https://github.com/Nikhilyadav0001/Smart-Healthcare-Frontend.git ..\\smart-healthcare-frontend
                    ) else (
                        cd ..\\smart-healthcare-frontend
                        git fetch origin
                        git reset --hard origin/main
                        cd ..\\smart-healthcare-pipeline
                    )
                    cd ..\\smart-healthcare-frontend
                    call npm ci
                    call npm run build
                '''
            }
        }

        stage('5. Package Docker Images') {
            steps {
                echo 'Building Docker container images...'
                dir('services/config-server') { bat 'docker build -t smarthealth/config-server:latest .' }
                dir('services/discovery-server') { bat 'docker build -t smarthealth/discovery-server:latest .' }
                dir('services/admin-server') { bat 'docker build -t smarthealth/admin-server:latest .' }
                dir('services/api-gateway') { bat 'docker build -t smarthealth/api-gateway:latest .' }
                dir('services/auth-service') { bat 'docker build -t smarthealth/auth-service:latest .' }
                dir('services/user-profile-service') { bat 'docker build -t smarthealth/user-profile-service:latest .' }
                dir('services/health-data-service') { bat 'docker build -t smarthealth/health-data-service:latest .' }
                dir('services/prediction-service') { bat 'docker build -t smarthealth/prediction-service:latest .' }
                dir('services/go-service') { bat 'docker build -t smarthealth/go-service:latest .' }
                bat 'docker build -t smarthealth/frontend:latest ..\\smart-healthcare-frontend'
            }
        }

        stage('6. Deploy to Kubernetes (CD)') {
            steps {
                echo 'Deploying application manifests to Kubernetes (smart-healthcare namespace)...'
                bat 'kubectl --kubeconfig=C:\\ProgramData\\Jenkins\\.jenkins\\.kube\\config apply -k infra/k8s/base'
                echo 'Verifying rollout status of critical microservices...'
                bat 'kubectl --kubeconfig=C:\\ProgramData\\Jenkins\\.jenkins\\.kube\\config rollout status deployment/postgres -n smart-healthcare --timeout=120s'
                bat 'kubectl --kubeconfig=C:\\ProgramData\\Jenkins\\.jenkins\\.kube\\config rollout status deployment/kafka -n smart-healthcare --timeout=120s'
                bat 'kubectl --kubeconfig=C:\\ProgramData\\Jenkins\\.jenkins\\.kube\\config rollout status deployment/api-gateway -n smart-healthcare --timeout=120s'
                bat 'kubectl --kubeconfig=C:\\ProgramData\\Jenkins\\.jenkins\\.kube\\config rollout status deployment/frontend -n smart-healthcare --timeout=120s'
            }
        }
    }

    post {
        success {
            echo '====================================================='
            echo '  Smart Healthcare CI/CD Pipeline Succeeded!          '
            echo '  All Microservices Tested, Built & Deployed to K8s. '
            echo '====================================================='
        }
        failure {
            echo '====================================================='
            echo '  Pipeline Failed! Inspect stage logs above.          '
            echo '  ====================================================='
        }
    }
}

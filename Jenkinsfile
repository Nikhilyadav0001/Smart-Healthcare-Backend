pipeline {
    agent any

    environment {
        CONFIG_GIT_TOKEN = credentials('CONFIG_GIT_TOKEN')
        JAVA_HOME = 'C:\\Program Files\\Java\\jdk-25.0.2'
        PATH = "${JAVA_HOME}\\bin;C:\\Program Files\\Go\\bin;C:\\Program Files\\nodejs;C:\\Program Files\\Git\\cmd;${env.PATH}"
    }

    stages {
        stage('1. Checkout Monorepo') {
            steps {
                echo 'Checking out source repository...'
                checkout scm
            }
        }

        stage('2. Build & Test Java Backend') {
            steps {
                dir('services') {
                    echo 'Cleaning stale test reports and running Maven packaging on reactor...'
                    bat 'for /d %%D in (*) do if exist "%%D\\target\\surefire-reports" rmdir /s /q "%%D\\target\\surefire-reports"'
                    bat '.\\mvnw.cmd package'
                }
            }
            post {
                always {
                    junit testResults: 'services/**/target/surefire-reports/*.xml', allowEmptyResults: true
                }
            }
        }

        stage('3. Build & Test Go Microservice') {
            steps {
                dir('services/go-service') {
                    echo 'Running Go tests (gRPC Report server, PDF engine, Kafka consumer)...'
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
            echo '====================================================='
        }
    }
}

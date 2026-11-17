pipeline {
    agent any

    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('1. Checkout Monorepo') {
            steps {
                echo 'Checking out latest commit from GitHub...'
                checkout scm
            }
        }

        stage('2. Build & Test Java Backend') {
            steps {
                dir('services') {
                    echo 'Running Maven reactor compilation and tests on JDK 25...'
                    bat '.\\mvnw.cmd test'
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
                dir('services/go-service') {
                    bat 'docker build -t smarthealth/go-service:latest .'
                }
                dir('services/prediction-service') {
                    bat 'docker build -t smarthealth/prediction-service:latest .'
                }
            }
        }
    }

    post {
        success {
            echo '====================================================='
            echo '  Smart Healthcare CI/CD Pipeline Succeeded!          '
            echo '====================================================='
        }
        failure {
            echo '====================================================='
            echo '  Pipeline Failed! Inspect stage logs above.          '
            echo '====================================================='
        }
    }
}

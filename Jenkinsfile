
pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    stages {

        // 1. CHECKOUT
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        // 2. MAVEN TESTS
        stage('Test User Service') {
            steps {
                dir('user-service') {
                    bat 'call mvnw.cmd -B -ntp test'
                }
            }
        }

        stage('Test Product Service') {
            steps {
                dir('product-service') {
                    bat 'call mvnw.cmd -B -ntp test'
                }
            }
        }

        stage('Test Order Service') {
            steps {
                dir('order-service') {
                    bat 'call mvnw.cmd -B -ntp test'
                }
            }
        }

        // 3. MAVEN BUILD
        stage('Build Services') {
            steps {
                dir('user-service') {
                    bat 'call mvnw.cmd -B -ntp package -DskipTests'
                }

                dir('product-service') {
                    bat 'call mvnw.cmd -B -ntp package -DskipTests'
                }

                dir('order-service') {
                    bat 'call mvnw.cmd -B -ntp package -DskipTests'
                }

                dir('api-gateway') {
                    bat 'call mvnw.cmd -B -ntp package -DskipTests'
                }

                dir('eureka-server') {
                    bat 'call mvnw.cmd -B -ntp package -DskipTests'
                }
            }
        }

        // 4. DOCKER BUILD & PUSH
        stage('Docker Build and Push') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKER_USERNAME',
                        passwordVariable: 'DOCKER_PASSWORD'
                    )
                ]) {

                    // Docker Hub login
                    bat '''
                        @echo off
                        echo Logging into Docker Hub...
                        powershell -NoProfile -Command "$env:DOCKER_PASSWORD | docker login -u $env:DOCKER_USERNAME --password-stdin"
                        if errorlevel 1 exit /b 1
                    '''

                    // Build all five Docker images
                    bat '''
                        @echo off

                        echo Building User Service...
                        docker build -t %DOCKER_USERNAME%/user-service:latest ./user-service
                        if errorlevel 1 exit /b 1

                        echo Building Product Service...
                        docker build -t %DOCKER_USERNAME%/product-service:latest ./product-service
                        if errorlevel 1 exit /b 1

                        echo Building Order Service...
                        docker build -t %DOCKER_USERNAME%/order-service:latest ./order-service
                        if errorlevel 1 exit /b 1

                        echo Building API Gateway...
                        docker build -t %DOCKER_USERNAME%/api-gateway:latest ./api-gateway
                        if errorlevel 1 exit /b 1

                        echo Building Eureka Server...
                        docker build -t %DOCKER_USERNAME%/eureka-server:latest ./eureka-server
                        if errorlevel 1 exit /b 1
                    '''

                    // Retry each Docker push up to 3 times
                    script {
                        def images = [
                            'user-service',
                            'product-service',
                            'order-service',
                            'api-gateway',
                            'eureka-server'
                        ]

                        for (def image in images) {
                            echo "Pushing ${image}..."

                            retry(3) {
                                bat """
                                    @echo off
                                    docker push %DOCKER_USERNAME%/${image}:latest
                                    if errorlevel 1 exit /b 1
                                """
                            }

                            echo "${image} pushed successfully."
                        }
                    }

                    bat 'docker logout'
                    echo 'All Docker images pushed successfully.'
                }
            }
        }

        // 5. DEPLOY TO AWS EC2
        stage('Deploy to AWS EC2') {
            steps {
                sshagent(['ec2-deploy-key']) {
                    bat '''
                        @echo off
                        ssh -o BatchMode=yes -o StrictHostKeyChecking=yes ec2-user@16.178.17.40 "cd ~/microservices && docker-compose config --quiet && docker-compose pull && docker-compose up -d --wait --wait-timeout 600 && docker-compose ps"
                    '''
                }
            }
        }
    }

    // 6. PIPELINE RESULT
    post {
        success {
            echo 'Jenkins pipeline completed successfully.'
            echo 'Docker images pushed and EC2 deployment finished.'
        }

        failure {
            echo 'Jenkins pipeline failed. Check the failed stage logs.'
        }

        always {
            echo 'Pipeline execution finished.'
        }
    }
}

pipeline {
    agent any

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        // =========================
        // TESTS
        // =========================

        stage('Test User Service') {
            steps {
                dir('user-service') {
                    bat 'mvnw.cmd test'
                }
            }
        }

        stage('Test Product Service') {
            steps {
                dir('product-service') {
                    bat 'mvnw.cmd test'
                }
            }
        }

        stage('Test Order Service') {
            steps {
                dir('order-service') {
                    bat 'mvnw.cmd test'
                }
            }
        }

        // =========================
        // MAVEN BUILD
        // =========================

        stage('Build Services') {
            steps {

                dir('user-service') {
                    bat 'mvnw.cmd package -DskipTests'
                }

                dir('product-service') {
                    bat 'mvnw.cmd package -DskipTests'
                }

                dir('order-service') {
                    bat 'mvnw.cmd package -DskipTests'
                }

                dir('api-gateway') {
                    bat 'mvnw.cmd package -DskipTests'
                }

                dir('eureka-server') {
                    bat 'mvnw.cmd package -DskipTests'
                }
            }
        }



        // =========================
        // DOCKER - USER SERVICE
        // =========================

        stage('Docker - User Service') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USERNAME',
                    passwordVariable: 'DOCKER_PASSWORD'
                )]) {

                    bat 'docker login -u %DOCKER_USERNAME% -p %DOCKER_PASSWORD%'
                    bat 'docker build -t %DOCKER_USERNAME%/user-service:latest ./user-service'
                    bat 'docker push %DOCKER_USERNAME%/user-service:latest'
                }
            }
        }

        // =========================
        // DOCKER - PRODUCT SERVICE
        // =========================

        stage('Docker - Product Service') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USERNAME',
                    passwordVariable: 'DOCKER_PASSWORD'
                )]) {

                    bat 'docker login -u %DOCKER_USERNAME% -p %DOCKER_PASSWORD%'
                    bat 'docker build -t %DOCKER_USERNAME%/product-service:latest ./product-service'
                    bat 'docker push %DOCKER_USERNAME%/product-service:latest'
                }
            }
        }

        // =========================
        // DOCKER - ORDER SERVICE
        // =========================

        stage('Docker - Order Service') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USERNAME',
                    passwordVariable: 'DOCKER_PASSWORD'
                )]) {

                    bat 'docker login -u %DOCKER_USERNAME% -p %DOCKER_PASSWORD%'
                    bat 'docker build -t %DOCKER_USERNAME%/order-service:latest ./order-service'
                    bat 'docker push %DOCKER_USERNAME%/order-service:latest'
                }
            }
        }

        // =========================
        // DOCKER - API GATEWAY
        // =========================

        stage('Docker - API Gateway') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USERNAME',
                    passwordVariable: 'DOCKER_PASSWORD'
                )]) {

                    bat 'docker login -u %DOCKER_USERNAME% -p %DOCKER_PASSWORD%'
                    bat 'docker build -t %DOCKER_USERNAME%/api-gateway:latest ./api-gateway'
                    bat 'docker push %DOCKER_USERNAME%/api-gateway:latest'
                }
            }
        }

        // =========================
        // DOCKER - EUREKA SERVER
        // =========================

        stage('Docker - Eureka Server') {
            steps {
                withCredentials([usernamePassword(
                    credentialsId: 'dockerhub-credentials',
                    usernameVariable: 'DOCKER_USERNAME',
                    passwordVariable: 'DOCKER_PASSWORD'
                )]) {

                    bat 'docker login -u %DOCKER_USERNAME% -p %DOCKER_PASSWORD%'
                    bat 'docker build -t %DOCKER_USERNAME%/eureka-server:latest ./eureka-server'
                    bat 'docker push %DOCKER_USERNAME%/eureka-server:latest'
                }
            }
        }
        stage('Deploy to AWS EC2') {
            steps {
                sshagent(['ec2-deploy-key']) {
                    bat '''
                        ssh -o StrictHostKeyChecking=no ec2-user@16.178.17.40 "cd ~/microservices && docker-compose pull && docker-compose up -d"
                    '''
                }
            }
        }
    }
}

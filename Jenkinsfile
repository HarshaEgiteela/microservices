pipeline {
    agent any

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

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
            }
        }
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
    }
}

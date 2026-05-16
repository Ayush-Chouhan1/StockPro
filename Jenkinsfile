pipeline {
    agent any

    environment {
        SONARQUBE_ENV = 'sonarqube'
        PROJECT_DIR = 'microservicearchitecture'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build and Test') {
            steps {
                dir("${PROJECT_DIR}") {
                    sh 'mvn clean test'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                dir("${PROJECT_DIR}") {
                    withSonarQubeEnv("${SONARQUBE_ENV}") {
                        sh 'mvn sonar:sonar -DskipTests'
                    }
                }
            }
        }

        stage('Build Docker Stack') {
            steps {
                dir("${PROJECT_DIR}") {
                    sh 'docker compose up -d --build'
                }
            }
        }
    }
}
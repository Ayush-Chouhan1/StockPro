pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        PROJECT_DIR = 'microservicearchitecture'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Copy Env File') {
            steps {
                sh '''
                cp /home/ubuntu/StockPro/microservicearchitecture/.env \
                $WORKSPACE/microservicearchitecture/.env
                '''
            }
        }

        stage('Verify Files') {
            steps {
                dir("${PROJECT_DIR}") {
                    sh '''
                        ls -la
                        test -f docker-compose.yml
                        test -f .env
                    '''
                }
            }
        }

        stage('Deploy Containers') {
            steps {
                dir("${PROJECT_DIR}") {
                    sh 'docker compose up -d --build'
                }
            }
        }

        stage('Verify Running Containers') {
            steps {
                dir("${PROJECT_DIR}") {
                    sh 'docker compose ps'
                }
            }
        }
    }

    post {
        success {
            echo 'Deployment successful'
        }

        failure {
            echo 'Deployment failed'
        }
    }
}
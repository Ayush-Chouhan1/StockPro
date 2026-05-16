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

        stage('Verify Compose File') {
            steps {
                dir("${PROJECT_DIR}") {
                    sh '''
                        ls
                        test -f docker-compose.yml
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
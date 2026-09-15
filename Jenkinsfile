pipeline {
    agent any

    environment {
        IMAGE_NAME = 'jenkins-springboot-ci'
        IMAGE_TAG = "${BUILD_NUMBER}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Unit Tests') {
            steps {
                sh 'mvn -B clean test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: false
                }
            }
        }

        stage('Package') {
            steps {
                sh 'mvn -B package -DskipTests'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                    sh '''
                        mvn -B sonar:sonar \
                          -Dsonar.projectKey=jenkins-springboot-ci \
                          -Dsonar.host.url=http://sonarqube:9000 \
                          -Dsonar.token="$SONAR_TOKEN"
                    '''
                }
            }
        }

        stage('Podman Build') {
            steps {
                sh 'podman build -t $IMAGE_NAME:$IMAGE_TAG -t $IMAGE_NAME:latest .'
            }
        }

        stage('Trivy Image Scan') {
            steps {
                sh '''
                    podman run --rm \
                      -v ${PODMAN_SOCKET_PATH:-/run/podman/podman.sock}:/var/run/docker.sock \
                      -e DOCKER_HOST=unix:///var/run/docker.sock \
                      aquasec/trivy:0.56.2 image \
                      --scanners vuln \
                      --severity HIGH,CRITICAL \
                      --exit-code 1 \
                      $IMAGE_NAME:$IMAGE_TAG
                '''
            }
        }
    }

    post {
        success {
            echo "Pipeline completed. Built image: ${IMAGE_NAME}:${IMAGE_TAG}"
        }
        failure {
            echo 'Pipeline failed. Open the failed stage and Jenkins console output for details.'
        }
        always {
            cleanWs()
        }
    }
}

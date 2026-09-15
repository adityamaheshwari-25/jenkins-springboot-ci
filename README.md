# Jenkins Spring Boot CI Project

This small project teaches a realistic Jenkins CI pipeline using Podman:

```text
Git repository -> Jenkins -> Maven tests -> JUnit report -> Maven package
             -> SonarQube analysis -> Podman image -> Trivy vulnerability scan
```

Everything can run on your own computer. No Azure, AWS, or paid service is required.

## Included

- Java 17 Spring Boot REST API
- Unit and web-layer tests
- Dockerfile
- Jenkinsfile
- Compose setup for Jenkins and SonarQube
- Trivy scan executed as a Podman container

## Prerequisites

Install Podman, a Compose provider (`podman-compose` or a compatible `podman compose` provider), and Git. Install Java 17 and Maven only if you want to run the application outside Jenkins.

On Windows/macOS, start your Podman machine before running commands. Allocate at least 4 GB RAM to it because Jenkins and SonarQube run together.

## 1. Run the application locally

From the project root:

```bash
mvn clean test
mvn spring-boot:run
```

In another terminal:

```bash
curl "http://localhost:8080/api/greetings?name=Aditya"
curl http://localhost:8080/actuator/health
```

Expected greeting:

```json
{"id":1,"message":"Hello, Aditya!"}
```

Stop the application with Ctrl+C.

## 2. Start Jenkins and SonarQube locally with Podman

From the project root:

```bash
podman machine start              # Windows/macOS only
podman compose up -d --build
podman compose ps
```

On Linux with a rootless Podman socket:

```bash
systemctl --user enable --now podman.socket
export PODMAN_SOCKET_PATH="$XDG_RUNTIME_DIR/podman/podman.sock"
podman compose up -d --build
```

For a rootful socket, commonly `/run/podman/podman.sock`:

```bash
sudo systemctl enable --now podman.socket
export PODMAN_SOCKET_PATH=/run/podman/podman.sock
podman compose up -d --build
```

The Compose file defaults to `/run/podman/podman.sock`, matching your current setup. Override `PODMAN_SOCKET_PATH` when using a rootless socket.

Open Jenkins at http://localhost:8080 and SonarQube at http://localhost:9000.

Get the Jenkins initial password:

```bash
podman exec ci-jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Open Jenkins, paste the password, choose **Install suggested plugins**, and create your admin user. The supplied image already installs Pipeline, Git, credentials binding, JUnit, and workspace cleanup plugins.

SonarQube may take one or two minutes to start. First-login credentials are admin / admin; SonarQube will ask you to change the password.

## 3. Create a SonarQube token

In SonarQube, open **My Account** -> **Security**, create a token named `jenkins-local`, click **Generate**, and copy it immediately. Never commit this token to Git.

## 4. Add the token to Jenkins

In Jenkins:

1. Go to **Manage Jenkins** -> **Credentials**.
2. Select the global credentials domain and click **Add Credentials**.
3. Choose **Secret text**.
4. Paste the SonarQube token.
5. Set **ID** to exactly `sonar-token`.
6. Save.

The ID matters because the Jenkinsfile refers to `credentialsId: 'sonar-token'`.

## 5. Put the project in Git

Jenkins needs a Git repository for `checkout scm`. For a local repository:

```bash
git init
git add .
git commit -m "Initial Jenkins CI project"
```

For the easiest setup, create an empty GitHub repository and run:

```bash
git branch -M main
git remote add origin https://github.com/<your-user>/<your-repository>.git
git push -u origin main
```

If it is private, configure a Jenkins GitHub username/token credential and select it in the job configuration.

## 6. Create the Jenkins pipeline job

1. In Jenkins, click **New Item**.
2. Enter `springboot-ci-pipeline`.
3. Choose **Pipeline** and click **OK**.
4. Under **Pipeline**, select **Pipeline script from SCM**.
5. Select **Git**, enter the repository URL, and set the branch to `*/main`.
6. Set the script path to `Jenkinsfile`.
7. Save and click **Build Now**.

The pipeline runs on the Jenkins container. Podman commands use the mounted Podman API socket. SonarQube is reachable from Jenkins using the service name `sonarqube`.

## 7. Understand each stage

1. **Checkout** downloads the selected Git commit.
2. **Unit Tests** runs Maven and publishes XML JUnit reports.
3. **Package** creates the executable Spring Boot JAR and archives it in Jenkins.
4. **SonarQube Analysis** sends source and test information to SonarQube.
5. **Podman Build** creates `jenkins-springboot-ci:<build-number>` and `:latest`.
6. **Trivy Image Scan** checks the image for HIGH and CRITICAL vulnerabilities. A finding intentionally fails the build.

## 8. Run the image created by Jenkins

After a successful build:

```bash
podman run --rm -p 8081:8080 jenkins-springboot-ci:latest
```

Test it at http://localhost:8081/api/greetings?name=Docker.

## 9. Useful commands

```bash
podman compose logs -f jenkins
podman compose logs -f sonarqube
podman image ls jenkins-springboot-ci
podman compose down
podman compose down -v
```

Use `podman compose down -v` only when you intentionally want to reset and delete local Jenkins and SonarQube data volumes.

## Common problems

- **mvn not found:** the supplied Jenkins image installs Maven; install Maven locally if running outside Jenkins.
- **Podman socket error:** ensure the Podman machine is running on Windows/macOS. On Linux, ensure the Podman API socket is active and `PODMAN_SOCKET_PATH` points to it.
- **SonarQube connection refused:** wait for SonarQube to finish starting; inside Jenkins the hostname is `sonarqube`, not `localhost`.
- **Sonar token not found:** verify the credential is Secret text with the exact ID `sonar-token`.
- **Trivy fails:** inspect the reported vulnerabilities and update the base image or dependency. Do not simply remove the scan.

## Next exercise

Create a branch, change the greeting, push it, and inspect the test report, archived JAR, SonarQube result, Podman image, and Trivy output. Later, add Kubernetes manifests and let ArgoCD handle deployment.

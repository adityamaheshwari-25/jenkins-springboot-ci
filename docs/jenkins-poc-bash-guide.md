# Run the Jenkins POC from Git Bash on Windows

These steps assume Git Bash on Windows, with the existing Windows Podman installation. WSL Bash uses different paths and may need its own Podman connection setup. This walkthrough runs the existing CI pipeline and manually starts the application afterward; Kubernetes, Argo CD, and AWS are not needed.

## 1. Open the project

```bash
cd /c/Endava/EndevLocal/jenkins-springboot-ci
export MSYS_NO_PATHCONV=1
```

The environment variable prevents Git Bash from rewriting Linux container paths passed to Windows executables such as `podman.exe`. Keep it set for this terminal session. Set it again in any new Git Bash terminal used for Podman commands involving container paths.

## 2. Prepare ignore files

Remove only the `target/` line from the root `.dockerignore`. The application Dockerfile needs the JAR inside that directory.

The project currently has no `.gitignore`. Create one containing:

```gitignore
target/
.idea/
.vscode/
*.iml
.env
```

If a `.gitignore` has been added since these notes were written, merge these entries instead of replacing it. `.dockerignore` controls image build inputs; `.gitignore` controls untracked files Git ignores.

## 3. Create and push the Git repository

Create an empty GitHub repository named `jenkins-springboot-ci` in an account where you are allowed to store this code. Choose the appropriate visibility. Do not initialise a README, licence, or `.gitignore` on GitHub.

```bash
git init
git add .
git status
```

Verify that `target/` and credentials are not staged. Then run:

```bash
git commit -m "Initial Jenkins Spring Boot CI demo"
git branch -M main
git remote add origin https://github.com/YOUR-USERNAME/jenkins-springboot-ci.git
git push -u origin main
```

Replace `YOUR-USERNAME` and complete authentication when prompted. If Git requests your author identity, configure your actual details and retry the commit:

```bash
git config user.name "Your Name"
git config user.email "your-email@example.com"
```

Checkpoint: GitHub displays `Jenkinsfile`, `pom.xml`, `src/`, the root Dockerfile, and the `jenkins/` directory.

## 4. Start Podman and check Compose

```bash
podman machine start
podman info
podman compose version
podman system connection list
```

If the machine is already running, continue. If no machine exists, run `podman machine init` once, then start it. If no Compose provider is installed, install one through Podman Desktop's Compose setup and repeat the check.

The connection inspected when preparing this walkthrough defaults to the rootful socket `/run/podman/podman.sock`, matching Compose. A different/rootless setup needs its actual socket path and permissions configured. Do not switch connections casually: rootful and rootless Podman use separate container/image storage.

Checkpoint: Podman connects and Compose finds its provider.

## 5. Start Jenkins and SonarQube

Ensure host ports 8080 and 9000 are free, then run from the project directory:

```bash
podman compose up -d --build
podman compose ps
podman compose logs --tail=100 jenkins
podman compose logs --tail=100 sonarqube
```

The first run downloads images and installs build tools and Jenkins plugins. Allow enough memory in the Podman machine for both services; startup logs are the first place to check if either exits.

Verify Jenkins's tools and access to the Podman service:

```bash
podman exec ci-jenkins mvn -version
podman exec ci-jenkins podman info
```

Checkpoint: both containers remain running and both commands succeed. If the second command reports socket permission denied, resolve access for the container's `jenkins` user before proceeding. Mounting the socket does not itself grant permission; do not make it world-writable as a shortcut.

## 6. Complete Jenkins setup

```bash
podman exec ci-jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

Open http://localhost:8080, paste the password, choose Install suggested plugins, create an administrator account, and complete setup. If a previous Jenkins volume already contains a configured installation, use the existing login instead.

## 7. Configure SonarQube and its credential

Open http://localhost:9000. A fresh installation starts with username `admin` and password `admin`; change the password when prompted.

1. Create a local/manual project with key `jenkins-springboot-ci`.
2. In My Account -> Security, generate a project analysis token for that project.
3. Copy the token; do not commit it to Git.
4. In Jenkins, open Manage Jenkins -> Credentials -> global store/domain -> Add Credentials.
5. Choose Secret text, paste the token, and set ID to exactly `sonar-token`.
6. Save.

## 8. Create the pipeline job

1. In Jenkins, select New Item.
2. Name it `springboot-ci-pipeline`, select Pipeline, and click OK.
3. Under Pipeline, choose Pipeline script from SCM.
4. Choose Git and enter your GitHub repository URL.
5. For a private repository, select a Jenkins Username with password credential containing your GitHub username and a personal access token as the password, with repository read access.
6. Set branch specifier to `*/main`.
7. Set Script Path to `Jenkinsfile`.
8. Save.

This repository configuration supplies the `scm` used by `checkout scm`.

## 9. Run and inspect the pipeline

Click Build Now, open the build number, and select Console Output.

```text
Checkout -> Unit Tests -> Package -> SonarQube Analysis
         -> Podman Build -> Trivy Image Scan -> Post actions
```

Inspect Jenkins test results and archived JAR, the SonarQube project analysis, and Trivy's output. List images from Git Bash:

```bash
podman images
```

HIGH or CRITICAL vulnerability findings intentionally fail the scan. Review and update affected dependencies/base images, then rerun. A registry download, scanner database, or socket error is an execution problem instead. The current SonarQube stage submits analysis but does not explicitly enforce its quality gate.

## 10. Run the application from a successful build

Replace `1` with the exact successful Jenkins build number:

```bash
podman run --rm --name ci-demo-app -p 8081:8080 jenkins-springboot-ci:1
```

Keep that terminal open for application logs. In a second Git Bash terminal:

```bash
curl -fsS "http://localhost:8081/api/greetings?name=Aditya"
curl -fsS "http://localhost:8081/actuator/health"
```

Expect `Hello, Aditya!` in the greeting and `UP` in the health response. The image runs Java and Spring Boot on container port 8080; Podman publishes it on host port 8081. Jenkins remains on host port 8080.

Use the successful build's number: `latest` may point to an image whose later Trivy scan failed. This application run is manual, outside the CI pipeline.

## 11. Try another build

Change the Java greeting and any tests that assert its text, then run from the project directory:

```bash
git add src
git commit -m "Update demo greeting"
git push
```

Click Build Now again. This setup does not yet configure a webhook or SCM polling, so pushing alone does not trigger a build. Stop the previous application container before running the new successful image on port 8081.

## 12. Stop and resume

Press Ctrl+C in the terminal attached to the application. If it remains running, use another terminal:

```bash
podman stop ci-demo-app
```

The application's `--rm` option removes its container after it stops. From the project directory, stop the CI services:

```bash
podman compose down
```

Named volumes preserve Jenkins and SonarQube data. Do not add `-v` unless you intend to delete those volumes. To resume later, start the Podman machine if necessary and run `podman compose up -d` from this directory.

## References

- [Jenkins Pipeline setup](https://www.jenkins.io/doc/book/pipeline/getting-started/)
- [Podman Compose](https://docs.podman.io/en/latest/markdown/podman-compose.1.html)
- [Trivy vulnerability scanning](https://trivy.dev/docs/v0.56/scanner/vulnerability/)

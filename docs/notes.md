# Jenkins CI project: short notes

## 1. Why persistent storage?

The `jenkins_home` volume preserves Jenkins jobs, credentials, settings, build history, and archived artifacts when its container is replaced. SonarQube volumes preserve its data, extensions, and logs. Stopping a container does not erase its filesystem; volumes let data survive container removal and recreation. Volumes are not backups.

## 2. Why create a Jenkins image when one already exists?

`jenkins/Dockerfile` starts from the official `jenkins/jenkins:lts-jdk17` image and adds Maven, Podman, and the plugins listed in `jenkins/plugins.txt`. This makes the tools needed by this pipeline available in a repeatable setup. We extend the existing image, rather than building Jenkins from scratch.

The root `Dockerfile` has a different job: it packages the Spring Boot JAR with a Java 17 runtime. `docker-compose.yml` starts Jenkins and SonarQube; `Jenkinsfile` defines the build steps. Podman can build Dockerfiles, and `podman compose` uses an installed Compose provider to run the services.

## 3. Where does the Jenkins controller run in production?

The controller can run as a service or container on a dedicated server/VM, or inside Kubernetes with persistent storage. It manages the UI, configuration, and build scheduling. Separate agents normally execute builds and contain tools such as Maven and Podman. This local project combines the controller and build execution for learning.

## 4. Why clean the build workspace?

`cleanWs()` removes checked-out source and temporary build files after each pipeline run. It saves space and prevents leftover files from affecting later builds. It does not delete Jenkins configuration, archived artifacts, build history, or images stored by Podman.

## 5. Why use a SonarQube token?

The token authenticates the scanner and allows it to submit analysis to SonarQube. Jenkins stores the secret under the credential ID `sonar-token` and temporarily exposes it as `SONAR_TOKEN`. The secret is kept out of Git. This pipeline submits analysis but does not explicitly wait for and enforce the SonarQube quality gate.

## 6. What does Trivy check?

Trivy scans the application image for known vulnerabilities in detected OS packages and supported application dependencies, including Java libraries. Here, `--scanners vuln` enables vulnerability scanning, `--severity HIGH,CRITICAL` selects severities, and `--exit-code 1` fails the build when matching findings exist. It does not test the running API, and this command does not enable secret or misconfiguration scanning.

## 7. Is this only CI?

Yes. The current pipeline checks out code, runs tests, packages the JAR, submits SonarQube analysis, builds an image, and scans it. It does not push the image to a registry or deploy the application. CD would add a delivery/deployment workflow beyond these steps.

```text
Checkout -> Tests -> Package -> SonarQube -> Image build -> Trivy
```

## 8. Why and how do we run the application?

An image is a package; a running container makes the API available for use. The README's manual command starts it:

```bash
podman run --rm -p 8081:8080 jenkins-springboot-ci:latest
```

Podman starts a container, its entrypoint executes `java -jar app.jar`, and Spring Boot starts its web server on container port 8080. Open `http://localhost:8081/api/greetings?name=Aditya` on your computer. Jenkins remains on host port 8080. This application run is separate from the pipeline.

## 9. Can we add local Kubernetes deployment with Argo CD?

Yes. Set up a local Kubernetes cluster, install Argo CD in it, and commit application manifests such as a Deployment and Service to Git. Configure an Argo CD Application to track that Git path and deploy into the local cluster.

The intended extended flow is:

```text
Jenkins CI -> Push scanned image to registry -> Update image tag in Git
           -> Argo CD syncs manifests -> Kubernetes runs application
```

Use a unique image tag or digest for each release. The registry must be reachable by the cluster; an image in the host's Podman storage is not automatically available to Kubernetes nodes. For a local exercise, explicitly loading the image into the cluster nodes is another option.

Argo CD deploys the desired configuration from Git; it does not build images. Sync can be manual or automatic. Pushing an image alone does not update the Git manifest. This CD extension is a proposed next step and is not configured in the current project.

## Known build issue

The root `.dockerignore` excludes `target/`, but the application Dockerfile copies its JAR from that directory. Adjust that exclusion before expecting the image build to succeed.

## References

- [Podman Dockerfile support](https://docs.podman.io/en/stable/markdown/podman-build.1.html)
- [Podman Compose provider](https://docs.podman.io/en/latest/markdown/podman-compose.1.html)
- [Jenkins controller isolation](https://www.jenkins.io/doc/book/security/controller-isolation/)
- [SonarQube tokens](https://docs.sonarsource.com/sonarqube-server/user-guide/managing-tokens)
- [Trivy vulnerability scanning](https://trivy.dev/docs/v0.56/scanner/vulnerability/)
- [Argo CD CI integration](https://argo-cd.readthedocs.io/en/stable/user-guide/ci_automation/)

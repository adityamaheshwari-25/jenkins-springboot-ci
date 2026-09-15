# Why Podman is installed inside Jenkins

The Jenkinsfile executes `podman build` to build the application image and `podman run` to start the Trivy scan container. Since these steps execute inside the Jenkins container, that container needs the Podman command-line tool (CLI).

## How it works

```text
Jenkins executes a pipeline step
    |
    v
Podman CLI inside the Jenkins container
    |
    | Communicates through the mounted Podman socket
    v
Podman service in the Windows host's Linux Podman machine
    |
    v
Builds the application image or starts the Trivy container
```

The CLI inside Jenkins acts as a client of the existing Podman service. A second Podman machine inside Jenkins is not needed. The Trivy container is started by the host's Podman service, not nested inside the Jenkins container.

## The socket connection

Compose mounts the Podman socket into Jenkins at `/run/podman/podman.sock`. The `CONTAINER_HOST` environment variable points the CLI to `unix:///run/podman/podman.sock`.

The Jenkins user needs permission to access that socket. In this local setup, adding supplementary group `10` matches the socket's group ownership. The numeric group ID matters; its displayed name inside Jenkins may be `uucp`.

Verify the connection with:

```bash
podman exec ci-jenkins podman info
```

## Production with separate build agents

The controller schedules builds, while agents execute pipeline commands. Install Maven and the required container CLI on the agents that perform those steps, and configure their access to the appropriate container service. The controller does not need these build tools when it only coordinates the builds.

# Local and AWS deployment notes

## Can we deploy locally first and later move to AWS?

Yes. The Spring Boot code, application Dockerfile, and most Jenkins CI stages can be reused. AWS deployment needs additional permissions, infrastructure, and environment-specific configuration such as networking and secrets.

With Kubernetes and Argo CD, the proposed AWS flow is:

```text
Jenkins: test -> build image -> scan -> push image to Amazon ECR
        -> Update image version in Git manifests
        -> Argo CD syncs manifests to Amazon EKS
        -> Kubernetes runs the application
```

- **Amazon ECR:** stores container images.
- **Amazon EKS:** runs managed Kubernetes on AWS.
- **Jenkins:** builds, tests, scans, and publishes the image.
- **Argo CD:** deploys the configuration recorded in Git.

Jenkins can remain local initially if it has the required connectivity and permissions. Moving the application to AWS does not require moving Jenkins at the same time. Jenkins could also deploy directly; in this Argo CD approach, Argo CD handles deployment.

This is a future extension. The current project performs CI and does not configure AWS or Argo CD deployment.

## Do we need a Kubernetes manifests Git repository locally?

| Deployment method | Requirement |
|---|---|
| `podman run` | No Kubernetes manifests or deployment Git repository needed. |
| Kubernetes with `kubectl` | Kubernetes manifests are needed for the manifest-based approach; Git is useful but not required to apply local files. |
| Argo CD using GitOps | Commit and push manifests to a Git repository Argo CD can access. |

## Must the manifests be in a separate repository?

No. For learning, keep them in the existing application repository:

```text
jenkins-springboot-ci/
|-- src/
|-- Jenkinsfile
|-- Dockerfile
`-- k8s/
    |-- deployment.yaml
    `-- service.yaml
```

This is a proposed structure, not files already added to the project. The Deployment describes the application pods and image; the Service provides a stable network endpoint for them.

Configure an Argo CD Application with the repository URL, revision, `k8s/` path, and destination cluster/namespace. Files existing only on your laptop are not enough for this GitOps workflow: commit and push them somewhere Argo CD can reach.

A separate configuration repository can be introduced later for independent permissions and release management. Argo CD recommends separating application source and deployment configuration, but a separate repository is not mandatory.

## What changes between local Kubernetes and AWS?

Reuse the common application manifests and keep environment-specific settings separate. Image registry addresses, replicas, resource limits, networking, storage, and secrets may differ. Kubernetes nodes must be able to retrieve the image; images in your local Podman storage are not automatically available to the cluster.

## References

- [Using Amazon ECR images with EKS](https://docs.aws.amazon.com/AmazonECR/latest/userguide/ECR_on_EKS.html)
- [Argo CD CI integration](https://argo-cd.readthedocs.io/en/stable/user-guide/ci_automation/)
- [Argo CD repository guidance](https://argo-cd.readthedocs.io/en/release-1.8/user-guide/best_practices/)

FROM docker.io/library/eclipse-temurin:17-jre-noble

WORKDIR /app
COPY target/jenkins-springboot-ci-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]

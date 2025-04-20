FROM maven:3.9.9-amazoncorretto-21 AS build
# Set the working directory in the container
ENV HOME=/app
WORKDIR $HOME
COPY . .
RUN mvn -B -e org.apache.maven.plugins:maven-dependency-plugin:3.1.2:go-offline -DexcludeArtifactIds=domain --no-transfer-progress
RUN mvn -B -e install --no-transfer-progress

FROM amazoncorretto:21
WORKDIR /app
COPY --from=build /app/BFPService/target/BFPService-0.0.1-SNAPSHOT.jar Spring-docker.jar

HEALTHCHECK --interval=1m --timeout=5s --start-period=15s --retries=3 \
  CMD curl --fail --silent localhost:8080/actuator/health | jq --exit-status -n 'inputs | if has(\"status\") then .status==\"UP\" else false end' > /dev/null || exit 1
ENTRYPOINT ["java","-jar","Spring-docker.jar"]
FROM hseeberger/scala-sbt:11.0.12_1.5.5_2.13.6 AS builder

WORKDIR /app

# Copy build files
COPY build.sbt ./
COPY project ./project/

# Cache dependencies
RUN sbt update

# Build the application
COPY src ./src/
RUN sbt clean compile assembly

# Runtime stage
FROM openjdk:11-jre-slim

WORKDIR /app

# Create non-root user and directories
RUN apt-get update && apt-get install -y curl && \
    useradd -m -u 1001 appuser && \
    mkdir -p /app/logs && \
    chown -R appuser:appuser /app

COPY --from=builder --chown=appuser:appuser /app/target/scala-2.13/*assembly*.jar ./app.jar
COPY --chown=appuser:appuser src/main/resources/application.conf ./application.conf
COPY --chown=appuser:appuser src/main/resources/logback.xml ./logback.xml

USER appuser

EXPOSE 8081

HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8081/health || exit 1

CMD ["java", \
     "-XX:+UseContainerSupport", \
     "-XX:MaxRAMPercentage=75.0", \
     "-Dconfig.file=application.conf", \
     "-Dlogback.configurationFile=logback.xml", \
     "-jar", "app.jar"]
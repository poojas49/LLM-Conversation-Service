# Build stage
FROM hseeberger/scala-sbt:11.0.12_1.5.5_2.13.6 AS builder

WORKDIR /app

# Copy only the build files first to cache dependencies
COPY build.sbt ./
COPY project ./project/

# Cache the dependencies
RUN sbt update

# Copy the source code and build
COPY src ./src/
RUN sbt clean compile assembly

# Runtime stage
FROM openjdk:11-jre-slim

WORKDIR /app

# Create non-root user and install curl for healthcheck
RUN apt-get update && apt-get install -y curl && \
    useradd -m -u 1001 appuser && \
    mkdir -p /app/logs && \
    chown -R appuser:appuser /app

# Copy the built jar and configs
COPY --from=builder --chown=appuser:appuser /app/target/scala-2.13/*assembly*.jar ./app.jar
COPY --chown=appuser:appuser src/main/resources/application.conf ./application.conf
COPY --chown=appuser:appuser src/main/resources/logback.xml ./logback.xml

# Set user
USER appuser

# Expose the port the app runs on
EXPOSE 8080

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8080/api/v1/health || exit 1

# Command to run the application with explicit config and memory settings
CMD ["java", \
     "-XX:+UseContainerSupport", \
     "-XX:MaxRAMPercentage=75.0", \
     "-Dconfig.file=application.conf", \
     "-Dlogback.configurationFile=logback.xml", \
     "-jar", "app.jar"]
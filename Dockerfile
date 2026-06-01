FROM dhi.io/eclipse-temurin:25-alpine3.23@sha256:40de9c71890390b202e59d566f954c8832482c2ee82744f2f73d1e5b48ed3eae

ARG project_version_arg
ENV PROJECT_VERSION=$project_version_arg

EXPOSE 8080

USER nonroot
COPY --chown=nonroot:nonroot opentelemetry-javaagent-2.26.1.jar /agent.jar
COPY --chown=nonroot:nonroot build/libs/*.jar /app.jar

# OpenTelemetry instrumentering
ENV OTEL_METRICS_EXPORTER=none
ENV OTEL_LOGS_EXPORTER=none
ENV OTEL_TRACES_EXPORTER=otlp
ENV OTEL_PROPAGATORS=none
ENV OTEL_EXPORTER_OTLP_ENDPOINT=http://otlp.grafana-alloy:4318
ENV OTEL_EXPORTER_OTLP_INSECURE=true
ENV OTEL_SERVICE_NAME=nibas-backend

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70.0", "-Duser.timezone=Europe/Oslo", "-javaagent:/agent.jar", "-jar", "/app.jar"]

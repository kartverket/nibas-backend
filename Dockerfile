FROM dhi.io/eclipse-temurin:26-alpine3.23@sha256:135b10e246dc1939df08c1706a4def28a793c2bfa99311f747d00ab40b68426e

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

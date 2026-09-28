FROM dhi.io/opentelemetry-autoinstrumentation-java:2.31@sha256:06e6c438a48191daf885edb1aa3c0fcb1760a201633b0a06f6e53651e59f2f86 AS otel

FROM dhi.io/eclipse-temurin:26-alpine3.23@sha256:5d6b41e324f47216ab8fd6074e567ccb6ff7711998a3c99cc0d22bc3b0ff62a2

ARG project_version_arg
ENV PROJECT_VERSION=$project_version_arg

EXPOSE 8080

USER nonroot
COPY --chown=nonroot:nonroot --from=otel /javaagent.jar /agent.jar
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

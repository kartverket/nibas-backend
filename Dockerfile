FROM gcr.io/distroless/java25-debian13:nonroot@sha256:66a23001a1a4a5d02098c59fa27d265a8bb8e7d77d64464798e200441e53d040
ARG project_version_arg

ENV PROJECT_VERSION=$project_version_arg

EXPOSE 8080

COPY opentelemetry-javaagent-2.17.0.jar /agent.jar
COPY build/libs/*.jar /app.jar

# OpenTelemetry instrumentering
ENV OTEL_METRICS_EXPORTER=none
ENV OTEL_LOGS_EXPORTER=none
ENV OTEL_TRACES_EXPORTER=otlp
ENV OTEL_PROPAGATORS=none
ENV OTEL_EXPORTER_OTLP_ENDPOINT=http://otlp.grafana-alloy:4318
ENV OTEL_EXPORTER_OTLP_INSECURE=true
ENV OTEL_SERVICE_NAME=nibas-backend

USER nonroot

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70.0", "-Duser.timezone=Europe/Oslo", "-javaagent:/agent.jar", "-jar", "/app.jar"]

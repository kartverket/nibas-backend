FROM dhi.io/opentelemetry-autoinstrumentation-java:2.32@sha256:94b60c6178bbe2189b4c43f5d428fbb3ba7fb528fdceed1888952bc99a27a172 AS otel

FROM dhi.io/eclipse-temurin:26-alpine3.23@sha256:d4bb66dd634d393064fdc5f6c4a290f0107c457106cae8b57f99b1aa7c8228f5

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

FROM eclipse-temurin:24-jdk-alpine@sha256:8fdbcb6bc6b846640cea7058e6eeb56c311fae4efaa506a213789134065c6b90
ARG project_version_arg

ENV PROJECT_VERSION=$project_version_arg
ENV GROUP_NAME=nibas
ENV GROUP_ID=199
ENV USER_NAME=nibas-backend
ENV USER_ID=199

RUN addgroup -g ${GROUP_ID} ${GROUP_NAME} && adduser --uid ${USER_ID} --disabled-password --gecos '' ${USER_NAME} --ingroup ${GROUP_NAME}

# Set timezone to Oslo
RUN apk add --no-cache tzdata
ENV TZ=Europe/Oslo

EXPOSE 8080
RUN mkdir /nibas-backend
COPY build/libs/*.jar /nibas-backend/app.jar
COPY opentelemetry-javaagent-2.17.0.jar /agent.jar
VOLUME /tmp
USER ${USER_ID}

# OpenTelemetry instrumentering
ENV OTEL_METRICS_EXPORTER=none
ENV OTEL_LOGS_EXPORTER=none
ENV OTEL_TRACES_EXPORTER=otlp

ENV OTEL_PROPAGATORS=none

ENV OTEL_EXPORTER_OTLP_ENDPOINT=http://otlp.grafana-alloy:4318
ENV OTEL_EXPORTER_OTLP_INSECURE=true
ENV OTEL_SERVICE_NAME=nibas-backend


ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70.0", "-Djava.security.egd=file:/dev/./urandom", "-javaagent:agent.jar", "-jar", "/nibas-backend/app.jar"]

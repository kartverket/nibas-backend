FROM eclipse-temurin:17-alpine
ARG project_version_arg
ENV PROJECT_VERSION=$project_version_arg
EXPOSE 8080
RUN adduser -u 1242 -D -s /usr/sbin nibas-backend
RUN mkdir /nibas-backend
COPY build/libs/*.jar /nibas-backend/app.jar
USER 1242
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70.0", "-Djava.security.egd=file:/dev/./urandom","-jar", "/nibas-backend/app.jar"]

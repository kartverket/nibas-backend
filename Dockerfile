FROM harbor-staging.statkart.no/proxy_cache/adoptopenjdk/openjdk11:alpine-jre
ARG project_version_arg
ENV PROJECT_VERSION=$project_version_arg
EXPOSE 8080
RUN adduser -u 1242 -D -s /usr/sbin nibas-server-api
RUN mkdir /nibas-server-api
COPY build/libs/*.jar /nibas-server-api/app.jar
USER 1242
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom","-jar", "/nibas-server-api/app.jar"]

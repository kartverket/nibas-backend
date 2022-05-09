FROM eclipse-temurin:17-alpine
ARG project_version_arg

ENV PROJECT_VERSION=$project_version_arg
ENV GROUP_NAME=nibas
ENV GROUP_ID=199
ENV USER_NAME=nibas-backend
ENV USER_ID=199

RUN addgroup -g ${GROUP_ID} ${GROUP_NAME} && adduser --uid ${USER_ID} --disabled-password --gecos '' ${USER_NAME} --ingroup ${GROUP_NAME}

EXPOSE 8080
RUN mkdir /nibas-backend
COPY build/libs/*.jar /nibas-backend/app.jar
RUN mkdir /tmp
VOLUME /tmp
USER ${USER_ID}
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70.0", "-Djava.security.egd=file:/dev/./urandom","-jar", "/nibas-backend/app.jar"]

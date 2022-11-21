#
# A Dockerfile for the LibCal-Proxy service (Cf. https://github.com/UCLALibrary/libcal-proxy)
#

# Our tagged version of Alpine is managed in the project's POM file as 'docker.alpine.version'
ARG ALPINE_TAG

# Pull our source image to use as the base for this image
FROM alpine:${ALPINE_TAG}

# Our JDK version is managed in the project's POM file as 'jdk.version'
ARG JDK_VERSION

# Store some project metadata in the Docker image
LABEL ContainerName=${project.name} ContainerSourceCode=${project.url}

# Copy files from our build environment into the container
WORKDIR /opt/${project.artifactId}
COPY maven/${project.artifactId}-${project.version}.jar /opt/${project.artifactId}/
COPY maven/logback.xml /etc/${project.artifactId}/

# Install packages and setup the application's user
RUN apk add "openjdk${JDK_VERSION}" --quiet --no-cache \
  && addgroup -S ${project.artifactId} \
  && adduser -S ${project.artifactId} -G ${project.artifactId} \
  && chown ${project.artifactId}:${project.artifactId} /opt/${project.artifactId}

# The user that runs the application
USER ${project.artifactId}

# The service's exposed port
EXPOSE ${HTTP_PORT}

# Configure and run the service
ENV JAR_PATH="/opt/${project.artifactId}/${project.artifactId}-${project.version}.jar"
ENV LOGGING_CONFIG="-Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory"
ENV LOGBACK_CONFIG="-Dlogback.configurationFile=/etc/${project.artifactId}/logback.xml"

CMD ["sh", "-c", "exec java ${JAVA_OPTS} ${LOGGING_CONFIG} ${LOGBACK_CONFIG} -jar ${JAR_PATH}"]

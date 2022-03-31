
ARG ALPINE_TAG
ARG BUILDER_TAG

# We use a builder image as a path to a final image
FROM ksclarke/maven-graalvm-musl:${BUILDER_TAG} AS builder

ARG APP_NAME
ARG APP_VERSION

# Run the Maven build
WORKDIR "/opt/${APP_NAME}"
COPY /maven/ .

ENTRYPOINT ["tail", "-f", "/dev/null"]

#RUN cp "target/${APP_NAME}-${APP_VERSION}.jar" "/opt/${APP_NAME}/target/${APP_NAME}-${APP_VERSION}.jar" \

# Set up the image to which to copy our build artifacts
#FROM alpine:${ALPINE_TAG}

# Really only needed for a native compile, but it doesn't hurt
#ENV javax.net.ssl.trustStore /etc/default/cacerts
#ENV javax.net.ssl.trustAnchors /etc/default/cacerts

#ARG LOGGER="-Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory"

#ARG NATIVE_COMPILE
#ARG APP_VERSION

# This is either a placeholder or the uber-jar
#COPY --from=builder /opt/hauth/target/hauth-${APP_VERSION}.jar /opt/hauth/hauth.jar
# This is either the native binary or a startup script for the uber-jar
#COPY --from=builder /opt/hauth/target/hauth /usr/bin/hauth
# Copy in the SSL certs from the builder image
#COPY --from=builder /etc/default/cacerts /etc/default/cacerts

#RUN if [ "${NATIVE_COMPILE}" = true ] ; then \
#      rm /opt/hauth/hauth.jar ; \
#    else \
#      apk add openjdk11 --quiet --no-cache ; \
#    fi \
#    && addgroup -S hauth \
#    && adduser -S hauth -G hauth \
#    && chown hauth:hauth /usr/bin/hauth \
#    && chmod 750 /usr/bin/hauth

# The user that runs the hauth application
#USER hauth

# The port we expose the Hauth service on
#EXPOSE 8888

#CMD [ "sh", "-c", "exec /usr/bin/startup" ]

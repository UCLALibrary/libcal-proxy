
ARG ALPINE_TAG

FROM alpine:${ALPINE_TAG}

# Run the Maven build
WORKDIR "/opt/${APP_NAME}"
COPY /maven/ .

RUN apk add openjdk11 --quiet --no-cache ;
#  && addgroup -S libcal \
#  && adduser -S libcal -G libcal 
#  && chown libcal:libcal /usr/bin/libcal-proxy \
#  && chmod 750 /usr/bin/libcal-proxy

# The user that runs the libcal-proxy application
#USER libcal

# The port we expose the libcal-proxy service on
EXPOSE 8888

ENTRYPOINT ["tail", "-f", "/dev/null"]

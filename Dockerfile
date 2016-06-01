FROM alpine:3.3
MAINTAINER Philip Lombardi <plombardi@datawire.io>

LABEL DESCRIPTION="Datawire Discovery"
LABEL VENDOR="Datawire"

ENV JAVA_HOME=/usr/lib/jvm/default-jvm

RUN apk add --update bash && \
    apk add --no-cache openjdk8 && \
    ln -sf "${JAVA_HOME}/bin/"* "/usr/bin/" && \
    rm -rf /var/cache/apk/*

COPY discovery-web/build/libs/discovery-web-*-fat.jar /opt/discovery/
RUN  ln -s /opt/discovery/discovery-web-*-fat.jar /opt/discovery/discovery-web.jar

ENTRYPOINT ["java", "-Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory", "-jar", "/opt/discovery/discovery-web.jar", "-conf", "/opt/discovery/config/dev.json"]
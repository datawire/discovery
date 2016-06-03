FROM alpine:3.4
MAINTAINER Philip Lombardi <plombardi@datawire.io>
EXPOSE 52689
LABEL DESCRIPTION="Datawire Discovery"
LABEL LICENSE="Apache 2.0"
LABEL VENDOR="Datawire"

ENV JAVA_HOME=/usr/lib/jvm/default-jvm

RUN apk add --update bash && \
    apk add --no-cache openjdk8 && \
    ln -sf "${JAVA_HOME}/bin/"* "/usr/bin/" && \
    rm -rf /var/cache/apk/*

COPY discovery-web/build/libs/discovery-web-*-fat.jar /opt/discovery/
RUN  ln -s /opt/discovery/discovery-web-*-fat.jar /opt/discovery/discovery-web.jar

ENTRYPOINT ["java", \
            "-Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory", \
            "-Dhazelcast.logging.type=slf4j", \
            "-jar", "/opt/discovery/discovery-web.jar", \
            "-conf", \
            "/opt/discovery/config/discovery.json"]
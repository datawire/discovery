FROM alpine:3.4
MAINTAINER Philip Lombardi <plombardi@datawire.io>
LABEL PROJECT_REPO_URL = "git@github.com:datawire/discovery.git" \
      PROJECT_REPO_BROWSER_URL = "https://github.com/datawire/discovery" \
      DESCRIPTION = "Datawire Discovery" \
      VENDOR = "Datawire" \
      VENDOR_URL = "https://datawire.io/"

ENV JAVA_HOME=/usr/lib/jvm/default-jvm

RUN apk --no-cache add \
        bash \
        openjdk8 \
    && ln -sf "${JAVA_HOME}/bin/"* "/usr/bin/" \
    && ln -snf /bin/bash /bin/sh

COPY discovery-web/build/libs/discovery-web-*-fat.jar /opt/discovery/
COPY discovery-web/dist/entrypoint.sh                 /opt/discovery/

RUN  ln -s /opt/discovery/discovery-web-*-fat.jar /opt/discovery/discovery-web.jar

# Exposed Ports
# =======================
# +-------+-------------+
# | Port  | Description |
# + ------+-------------+
# | 52689 | Discovery   |
# | 5701  | Hazelcast   |
# +-------+-------------+
EXPOSE 52689 5701
ENTRYPOINT ["/opt/discovery/entrypoint.sh"]

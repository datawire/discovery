FROM java:openjdk-8-jre-alpine
MAINTAINER Datawire Inc, <dev@datawire.io>

# Exposed Ports
# ----------------------------------------------------------
# 5000  : discovery
# 5701  : Hazelcast Clustering
EXPOSE 5000 5701

LABEL PROJECT_REPO_URL = "git@github.com:datawire/discovery.git" \
      PROJECT_REPO_BROWSER_URL = "https://github.com/datawire/discovery" \
      PROJECT_LICENSE = "https://github.com/datawire/discovery/LICENSE" \
      DESCRIPTION = "Datawire discovery" \
      VENDOR = "Datawire" \
      VENDOR_URL = "https://datawire.io/"

RUN apk --no-cache add \
    bash \
  && ln -snf /bin/bash /bin/sh

RUN mkdir /var/log/datawire

WORKDIR /opt/discovery/

COPY discovery/build/libs/flobber-web-*-fat.jar ./discovery.jar

COPY discovery/src/docker/entrypoint.sh ./entrypoint.sh
RUN  chmod +x entrypoint.sh

ENTRYPOINT ["./entrypoint.sh"]

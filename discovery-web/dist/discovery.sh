#!/usr/bin/env bash
set -euo pipefail

rm -f /var/log/datawire/*.log

java -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory \
     -Dvertx.hazelcast.config=/etc/discovery/cluster.xml \
     -jar /opt/discovery/discovery-web.jar \
     -conf /etc/discovery/discovery.json \
     -cluster

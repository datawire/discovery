#!/usr/bin/env bash
set -e

# Increase the maximum file descriptors.
MAX_FD_LIMIT=`ulimit -H -n`
if [ $? -eq 0 ] ; then
    if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "max" ] ; then
        MAX_FD="$MAX_FD_LIMIT"
    fi

    ulimit -n $MAX_FD
    if [ $? -ne 0 ] ; then
        warn "Could not set maximum file descriptor limit: $MAX_FD"
    fi
else
    warn "Could not query maximum file descriptor limit: $MAX_FD_LIMIT"
fi

# INSTANCE_ENVIRONMENT should be stored in /etc/datawire/environment which is populated by Enventurer.
APP_ENV=${INSTANCE_ENVIRONMENT:?"Environment variable INSTANCE_ENVIRONMENT is not set."}
MIXPANEL_TOKEN="${INSTANCE_MIXPANEL_TOKEN:?"Environment variable INSTANCE_MIXPANEL_TOKEN is not set."}"

rm -f /var/log/datawire/*.log
java -Dapp.platform=aws \
     -Dapp.env=${APP_ENV} \
     -Dapp.mixpanel-token=${MIXPANEL_TOKEN} \
     -Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory \
     -Dvertx.hazelcast.config=config/cluster.xml \
     -Dlogback.configurationFile=config/logback.xml \
     -jar discovery-web.jar \
     -conf config/discovery-prod.json \
     -cluster

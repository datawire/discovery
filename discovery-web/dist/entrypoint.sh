#!/usr/bin/env bash

# Copyright 2016 Datawire. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
set -e

function warn() {
    echo "$*"
}

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

DISCOVERY_HOME=/opt/discovery
DISCOVERY_JAR="${DISCOVERY_HOME}/discovery-web.jar"
DISCOVERY_ENV=${APP_ENV:?Environment variable APP_ENV is not set.}

DEFAULT_JAVA_OPTS="-Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory \
-Dvertx.hazelcast.config=config/cluster.xml"

VERTX_OPTS="run io.datawire.discovery.Discovery -conf ${DISCOVERY_HOME}/config/discovery-${DISCOVERY_ENV}.json -cluster"

function splitJvmOpts() {
    JVM_OPTS=("$@")
}
eval splitJvmOpts $DEFAULT_JAVA_OPTS $JAVA_OPTS

cd ${DISCOVERY_HOME}
java -Dapp.environment=${DISCOVERY_ENV} \
     "${JVM_OPTS[@]}" \
     -jar ${DISCOVERY_JAR} \
     ${VERTX_OPTS}
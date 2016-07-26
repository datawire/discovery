#!/usr/bin/env bash
set -e

APP_NAME="discovery"
APP_ENV="${ENV:?"Environment variable ENV is not set."}"
APP_JAR="discovery-web.jar"

DEFAULT_JAVA_OPTS="\
-Dapp.env=${APP_ENV} \
-Dlogback.configurationFile=config/logback.xml \
-Dvertx.logger-delegate-factory-class-name=io.vertx.core.logging.SLF4JLogDelegateFactory"

DEFAULT_VERTX_OPTS="-conf config/${APP_NAME}-${APP_ENV}.json"

function warn() {
  echo "$*"
}

function splitJvmOpts() {
  JVM_OPTS=("$@")
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

eval splitJvmOpts $DEFAULT_JAVA_OPTS $JAVA_OPTS

java "${JVM_OPTS[@]}" -jar ${APP_JAR} $DEFAULT_VERTX_OPTS $VERTX_OPTS
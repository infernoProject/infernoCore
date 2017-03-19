#!/bin/bash
JAVA_BIN=$(which java)
JAVA_OPTS=${JAVA_OPTS:-""}

DEBUG=${DEBUG:-"false"}
DEBUGGER_PORT=${DEBUGGER_PORT:-"5135"}

if [[ "x${DEBUG}" == "xtrue" ]]; then
    JAVA_OPTS="${JAVA_OPTS} -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=${DEBUGGER_PORT}"
fi

${JAVA_BIN} ${JAVA_OPTS} -DconfigFile=/opt/inferno/config/RealmServer.conf -jar /opt/inferno/realmd.jar $@

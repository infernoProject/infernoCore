#!/bin/bash
JAVA_BIN=$(which java)
JAVA_OPTS=${JAVA_OPTS:-""}

DEBUG=${DEBUG:-"false"}
DEBUGGER_PORT=${DEBUGGER_PORT:-"5135"}

COVERAGE=${COVERAGE:-"false"}

if [[ "x${DEBUG}" == "xtrue" ]]; then
    JAVA_OPTS="${JAVA_OPTS} -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=${DEBUGGER_PORT}"
fi

if [[ "x${COVERAGE}" == "xtrue" ]]; then
    mkdir -p /opt/coverage/reports
    JAVA_OPTS="${JAVA_OPTS} -javaagent:/opt/coverage/jacocoagent.jar=output=file,destfile=/opt/coverage/reports/inferno.coverage,append=true,includes=ru.infernoproject.*,excludes=ru.infernoproject.tests.*,dumponexit=true"
fi

${JAVA_BIN} ${JAVA_OPTS} -DconfigFile=/opt/inferno/config/WorldServer.conf -jar /opt/inferno/worldd.jar $@

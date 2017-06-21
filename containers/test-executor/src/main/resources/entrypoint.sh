#!/bin/bash
JAVA_BIN=$(which java)
JVM_ARGS=${JVM_ARGS:-""}

DB_MANAGER=${DB_MANAGER:-"false"}

if [[ "x${DB_MANAGER}" == "xtrue" ]]; then
    ${JAVA_BIN} ${JVM_ARGS} -DconfigFile=/opt/inferno/config/ITests.conf \
       -cp /opt/inferno/itests/itests.jar:/opt/inferno/itests/itests-tests.jar \
       ru.infernoproject.common.db.DataSourceUtils \
       $@
else
    ${JAVA_BIN} ${JVM_ARGS} -DconfigFile=/opt/inferno/config/ITests.conf \
       -cp /opt/inferno/itests/itests.jar:/opt/inferno/itests/itests-tests.jar \
       org.testng.TestNG -testjar /opt/inferno/itests/itests-tests.jar \
       -d /opt/inferno/itests/results \
       $@
fi
#!/bin/bash
JAVA_BIN=$(which java)
JVM_ARGS=${JVM_ARGS:-""}

${JAVA_BIN} ${JVM_ARGS} -Dconfig.file=/opt/inferno/config/ITests.conf \
   -cp /opt/inferno/itests/itests.jar:/opt/inferno/itests/itests-tests.jar \
   org.testng.TestNG -testjar /opt/inferno/itests/itests-tests.jar \
   -d /opt/inferno/itests/results \
   $@

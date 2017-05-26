#!/bin/bash
JAVA_BIN=$(which java)

${JAVA_BIN} -Dconfig.file=/opt/inferno/config/ITests.conf \
   -cp /opt/inferno/itests/itests.jar:/opt/inferno/itests/itests-tests.jar \
   org.testng.TestNG -testjar /opt/inferno/itests/itests-tests.jar \
   -d /opt/inferno/itests/results \
   $@

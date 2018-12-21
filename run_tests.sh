#!/bin/bash -e

BUILD_NUMBER="nonci"
BUILD_TIMESTAMP=$(date +%Y%m%d.%H%M%S)

DOCKER_REPOSITORY="inferno"

POM_VERSION=$(xpath -q -e '/project/version/text()' pom.xml)
RELEASE_VERSION=$(echo "${POM_VERSION}" | sed 's/-SNAPSHOT//g')
BUILD_VERSION="${RELEASE_VERSION}-${BUILD_TIMESTAMP}-${BUILD_NUMBER}"

# Cleanup
function cleanup {
    mvn versions:set -DnewVersion="${POM_VERSION}"

    # Post Build :: Logs
    echo "Test Realm Server logs"
    docker logs testRealm-${BUILD_TIMESTAMP}

    echo "Test World Server logs"
    docker logs testWorld-${BUILD_TIMESTAMP}

    docker rm -f testRealm-${BUILD_TIMESTAMP}
    docker rm -f testWorld-${BUILD_TIMESTAMP}

    docker rmi ${DOCKER_REPOSITORY}/test-executor:${BUILD_VERSION}
    docker rmi ${DOCKER_REPOSITORY}/realm:${BUILD_VERSION}
    docker rmi ${DOCKER_REPOSITORY}/world:${BUILD_VERSION}
}
trap cleanup EXIT

# Build :: Set Version
mvn versions:set -DnewVersion=${BUILD_VERSION}

# Build :: Build Artifacts
mvn clean install -C -B -Pimage -DskipTests -DdockerRepository=${DOCKER_REPOSITORY}

# Test :: Prepare Environment
docker run -it --rm --net private --name testExecutor-${BUILD_TIMESTAMP} -e DB_MANAGER=true --entrypoint=/bin/bash ${DOCKER_REPOSITORY}/test-executor:${BUILD_VERSION} -c 'for db in realmd world objects characters; do /entrypoint.sh ${db} clean; done'
docker run -it --rm --net private --name testExecutor-${BUILD_TIMESTAMP} -e DB_MANAGER=true --entrypoint=/bin/bash ${DOCKER_REPOSITORY}/test-executor:${BUILD_VERSION} -c 'for db in realmd world objects characters; do /entrypoint.sh ${db} migrate; done'

docker run -d --net private --name testRealm-${BUILD_TIMESTAMP} ${DOCKER_REPOSITORY}/realm:${BUILD_VERSION}
docker run -d --net private --name testWorld-${BUILD_TIMESTAMP} -v $(pwd)/maps:/opt/inferno/maps ${DOCKER_REPOSITORY}/world:${BUILD_VERSION}

REALM_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' testRealm-${BUILD_TIMESTAMP})
WORLD_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' testWorld-${BUILD_TIMESTAMP})
JVM_ARGS="-Drealm.server.host=${REALM_IP} -Dworld.server.host=${WORLD_IP}"

sleep 30

# Test :: Run ITests
mkdir -p $(pwd)/test-results

docker run -it --rm --net private --name testExecutor-${BUILD_TIMESTAMP} -v $(pwd)/test-results:/opt/inferno/itests/results -e JVM_ARGS="${JVM_ARGS}" ${DOCKER_REPOSITORY}/test-executor:${BUILD_VERSION} -verbose 2


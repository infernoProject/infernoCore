#!/bin/bash -e

BUILD_NUMBER="nonci"
BUILD_TIMESTAMP=$(date +%Y%m%d.%H%M%S)

DOCKER_REPOSITORY="inferno"

POM_VERSION=$(xpath -q -e '/project/version/text()' pom.xml)
RELEASE_VERSION="${POM_VERSION//-SNAPSHOT/}"
BUILD_VERSION="${RELEASE_VERSION}-${BUILD_TIMESTAMP}-${BUILD_NUMBER}"

MYSQL_ROOT_PASSWORD="P@ssw0rd"

# Cleanup
function cleanup {
    mvn versions:set -DnewVersion="${POM_VERSION}"

    docker rm -f "testDatabase-${BUILD_TIMESTAMP}"

    # Post Build :: Logs
    echo "Test Realm Server logs"
    docker logs "testRealm-${BUILD_TIMESTAMP}"

    echo "Test World Server logs"
    docker logs "testWorld-${BUILD_TIMESTAMP}"

    docker rm -f "testRealm-${BUILD_TIMESTAMP}"
    docker rm -f "testWorld-${BUILD_TIMESTAMP}"

    docker rmi "${DOCKER_REPOSITORY}/test-executor:${BUILD_VERSION}"
    docker rmi "${DOCKER_REPOSITORY}/realm:${BUILD_VERSION}"
    docker rmi "${DOCKER_REPOSITORY}/world:${BUILD_VERSION}"
}
trap cleanup EXIT

# Build :: Set Version
mvn versions:set -DnewVersion="${BUILD_VERSION}"

# Build :: Build Artifacts
mvn clean install -C -B -Pimage -DskipTests -DdockerRepository=${DOCKER_REPOSITORY}

# Test :: Prepare Environment
JVM_ARGS=""

docker network create --subnet 10.20.30.0/24 private || echo "Docker network already exists"

docker run -d --net private --name "testDatabase-${BUILD_TIMESTAMP}" -e "MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}" mysql
DB_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "testDatabase-${BUILD_TIMESTAMP}")

# Wait for MySQL up
while ! docker exec -it "testDatabase-${BUILD_TIMESTAMP}" mysqladmin ping -h"${DB_IP}" --silent; do
    sleep 1
done

for db in realmd characters objects world; do
docker exec -it "testDatabase-${BUILD_TIMESTAMP}" mysql -u root -p"${MYSQL_ROOT_PASSWORD}" -e "CREATE DATABASE ${db}"
JVM_ARGS="${JVM_ARGS} -Djdbc.${db}.url=jdbc:mysql://${DB_IP}/${db}?useSSL=false&timeout=60&allowPublicKeyRetrieval=true -Djdbc.${db}.username=root -Djdbc.${db}.password=${MYSQL_ROOT_PASSWORD}"

docker run -it --rm --net private --name "testExecutor-${BUILD_TIMESTAMP}" -e JVM_ARGS="${JVM_ARGS}" -e DB_MANAGER=true "${DOCKER_REPOSITORY}/test-executor:${BUILD_VERSION}" ${db} clean
docker run -it --rm --net private --name "testExecutor-${BUILD_TIMESTAMP}" -e JVM_ARGS="${JVM_ARGS}" -e DB_MANAGER=true "${DOCKER_REPOSITORY}/test-executor:${BUILD_VERSION}" ${db} migrate
done

docker run -d --net private --name "testRealm-${BUILD_TIMESTAMP}" -e JVM_ARGS="${JVM_ARGS}" "${DOCKER_REPOSITORY}/realm:${BUILD_VERSION}"
docker run -d --net private --name "testWorld-${BUILD_TIMESTAMP}" -e JVM_ARGS="${JVM_ARGS}" -v "$(pwd)/maps:/opt/inferno/maps" "${DOCKER_REPOSITORY}/world:${BUILD_VERSION}"

REALM_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "testRealm-${BUILD_TIMESTAMP}")
WORLD_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' "testWorld-${BUILD_TIMESTAMP}")
JVM_ARGS="${JVM_ARGS} -Drealm.server.host=${REALM_IP} -Dworld.server.host=${WORLD_IP}"

# Wait for Realm server up
while ! nc -z -vvv -w 3 "${REALM_IP}" 3274; do
    sleep 1
done

# Wait for World server up
while ! nc -z -vvv -w 3 "${WORLD_IP}" 8085; do
    sleep 1
done

# Test :: Run ITests
mkdir -p "$(pwd)/test-results"

docker run -it --rm --net private --name "testExecutor-${BUILD_TIMESTAMP}" -v "$(pwd)/test-results:/opt/inferno/itests/results" -e JVM_ARGS="${JVM_ARGS}" "${DOCKER_REPOSITORY}/test-executor:${BUILD_VERSION}" -verbose 2

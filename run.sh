#!/bin/bash -e

DOCKER_REPOSITORY="inferno"

MYSQL_ROOT_PASSWORD="P@ssw0rd"

POM_VERSION=$(xpath -q -e '/project/version/text()' pom.xml)

# Pre-Build
docker rm -f testDatabase testRealm testWorld || echo "No containers running"
docker rmi ${DOCKER_REPOSITORY}/test-executor:${POM_VERSION} ${DOCKER_REPOSITORY}/realm:${POM_VERSION} ${DOCKER_REPOSITORY}/world:${POM_VERSION} || echo "No images found"

# Build :: Build Artifacts
mvn clean install -C -B -Pimage -DskipTests -DdockerRepository=${DOCKER_REPOSITORY}

# Test :: Prepare Environment
JVM_ARGS="-Dworld.name=TestWorld1"

docker run -d --net private --name testDatabase -e "MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD}" mysql
DB_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' testDatabase)

# Wait for MySQL up
while ! docker exec -it testDatabase mysqladmin ping -h"${DB_IP}" --silent; do
    sleep 1
done

for db in realmd characters objects world; do
docker exec -it testDatabase mysql -u root -p"${MYSQL_ROOT_PASSWORD}" -e "CREATE DATABASE ${db}"
JVM_ARGS="${JVM_ARGS} -Djdbc.${db}.url=jdbc:mysql://${DB_IP}/${db}?useSSL=false&timeout=60&allowPublicKeyRetrieval=true -Djdbc.${db}.username=root -Djdbc.${db}.password=${MYSQL_ROOT_PASSWORD}"

docker run -it --rm --net private --name testExecutor -e JVM_ARGS="${JVM_ARGS}" -e DB_MANAGER=true ${DOCKER_REPOSITORY}/test-executor:${POM_VERSION} ${db} clean
docker run -it --rm --net private --name testExecutor -e JVM_ARGS="${JVM_ARGS}" -e DB_MANAGER=true ${DOCKER_REPOSITORY}/test-executor:${POM_VERSION} ${db} migrate
done

docker run -d --net private --name testRealm -e JVM_ARGS="${JVM_ARGS}" ${DOCKER_REPOSITORY}/realm:${POM_VERSION}
docker run -d --net private --name testWorld -e JVM_ARGS="${JVM_ARGS}" -v $(pwd)/maps:/opt/inferno/maps ${DOCKER_REPOSITORY}/world:${POM_VERSION}

docker exec -it testDatabase mysql -u root -p"${MYSQL_ROOT_PASSWORD}" -e "INSERT INTO \`realmd\`.\`realm_list\` (online, last_seen, name, type, server_host, server_port) VALUES
  (2, NOW(), 'TestWorld1', 1, 'localhost', 8085);"

REALM_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' testRealm)
WORLD_IP=$(docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' testWorld)

docker exec -it testDatabase mysql -u root -p"${MYSQL_ROOT_PASSWORD}" -e "UPDATE \`realmd\`.\`realm_list\` SET server_host = '${WORLD_IP}' WHERE id = 2;"

# Wait for Realm server up
while ! nc -z -vvv -w 3 ${REALM_IP} 3274; do
    sleep 1
done

# Wait for World server up
while ! nc -z -vvv -w 3 ${WORLD_IP} 8085; do
    sleep 1
done

echo "Realm Server: ${REALM_IP}:3274"
echo "World Server: ${WORLD_IP}:8085"
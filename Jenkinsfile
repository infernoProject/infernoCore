pipeline {
    agent {
        docker {
            image 'maven:3.3.9'
            label 'docker'
            args '-v /usr/bin/docker:/usr/bin/docker -v /var/run/docker.sock:/var/run/docker.sock -v ${HOME}/.m2:${WORKSPACE}/.m2 -v ${HOME}/.docker:${WORKSPACE}/.docker'
        }
    }

    environment {
        DOCKER_REPOSITORY = 'registry.v0rt3x.ru/inferno'
        DOCKER_CONFIG = "${env.WORKSPACE}/.docker"
        MAVEN_CONFIG = "${env.WORKSPACE}/.m2"
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '15', daysToKeepStr: '30'))
    }

    stages {
        stage ('Build :: Set Version') {
            steps {
                script {
                    env.POM_VERSION = readMavenPom(file: "${WORKSPACE}/pom.xml").version
                    env.RELEASE_VERSION = env.POM_VERSION.replace("-SNAPSHOT", "")
                    env.BUILD_VERSION = "${env.RELEASE_VERSION}-${env.BUILD_TIMESTAMP}-${env.BUILD_NUMBER}"

                    currentBuild.displayName = "${env.BRANCH_NAME} - ${env.BUILD_VERSION}"
                }

                sh 'mvn versions:set -gs ${WORKSPACE}/.m2/settings.xml -DnewVersion=${BUILD_VERSION}'
            }
        }


        stage ('Build :: Build Artifacts') {
            steps {
                sh 'mvn clean install -gs ${WORKSPACE}/.m2/settings.xml -C -B -Pimage -DskipTests -DdockerRepository=${DOCKER_REPOSITORY}'
            }
        }

        stage ('Test :: Prepare Environment') {
            steps {
                withDockerContainer(image: "${env.DOCKER_REPOSITORY}/test-executor:${env.BUILD_VERSION}", args: "--name testExecutor-${env.BUILD_TIMESTAMP} --net private -e DB_MANAGER=true") {
                    sh 'for db in realmd world objects characters; do /entrypoint.sh ${db} clean; done'
                    sh 'for db in realmd world objects characters; do /entrypoint.sh ${db} migrate; done'
                }

                sh 'docker run -d --name testRealm-${BUILD_TIMESTAMP} --net private ${DOCKER_REPOSITORY}/realm:${BUILD_VERSION}'
                sh 'docker run -d --name testWorld-${BUILD_TIMESTAMP} --net private -v /srv/inferno/maps:/opt/inferno/maps ${DOCKER_REPOSITORY}/world:${BUILD_VERSION}'

                script {
                    env.REALM_IP = sh(returnStdout: true, script: "docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' testRealm-${BUILD_TIMESTAMP}").trim()
                    env.WORLD_IP = sh(returnStdout: true, script: "docker inspect -f '{{range .NetworkSettings.Networks}}{{.IPAddress}}{{end}}' testWorld-${BUILD_TIMESTAMP}").trim()
                    env.JVM_ARGS = "-Drealm.server.host=${env.REALM_IP} -Dworld.server.host=${env.WORLD_IP}"
                }
            }
        }

        stage ('Test :: Run ITests') {
            steps {
                sh 'mkdir -p ${WORKSPACE}/test-results'

                withDockerContainer(image: "${env.DOCKER_REPOSITORY}/test-executor:${env.BUILD_VERSION}", args: "--name testExecutor-${env.BUILD_TIMESTAMP} --net private -v ${env.WORKSPACE}/test-results:/opt/inferno/itests/results") {
                    sh "/entrypoint.sh -verbose 2"
                }
            }

            post {
                always {
                    sh 'docker rm -f testRealm-${BUILD_TIMESTAMP}'
                    sh 'docker rm -f testWorld-${BUILD_TIMESTAMP}'

                    junit 'test-results/junitreports/*.xml'
                }
            }
        }

        stage ('Promote') {
            when {
                branch 'master'
            }

            steps {
                sh 'mvn deploy -gs ${WORKSPACE}/.m2/settings.xml -C -B -Pimage -DskipTests -DskipBuild=true -DdockerRepository=${DOCKER_REPOSITORY}'
            }
        }
    }

    post {
        always {
            sh 'docker rmi $(docker images | grep ${BUILD_VERSION} | awk \'{ print $3 }\')'

            deleteDir()
        }
    }
}

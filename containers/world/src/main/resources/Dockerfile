FROM ${baseImage}

RUN mkdir -p /opt/inferno/config
RUN mkdir -p /opt/inferno/maps

VOLUME /opt/inferno/config

COPY org.jacoco.agent-runtime.jar /opt/coverage/jacocoagent.jar

COPY worldd.jar /opt/inferno/worldd.jar
COPY WorldServer.conf /opt/inferno/config/WorldServer.conf

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 8085 5135

WORKDIR /opt/inferno

ENTRYPOINT ["/entrypoint.sh"]

LABEL project.groupid="${project.groupId}" \
      project.artifactid="${project.artifactId}" \
      project.version="${project.version}"
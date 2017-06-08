FROM ${baseImage}

RUN mkdir -p /opt/inferno/config

VOLUME /opt/inferno/config

COPY org.jacoco.agent-runtime.jar /opt/coverage/jacocoagent.jar

COPY realmd.jar /opt/inferno/realmd.jar
COPY RealmServer.conf /opt/inferno/config/RealmServer.conf

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 3274 5135

WORKDIR /opt/inferno

ENTRYPOINT ["/entrypoint.sh"]
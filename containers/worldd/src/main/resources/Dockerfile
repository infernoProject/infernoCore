FROM ${baseImage}

RUN mkdir -p /opt/inferno/config

VOLUME /opt/inferno/config

COPY world.jar /opt/inferno/world.jar

COPY WorldServer.conf /opt/inferno/config/WorldServer.conf

EXPOSE 8085

WORKDIR /opt/inferno

ENTRYPOINT ["java", "-DconfigFile=/opt/inferno/config/WorldServer.conf", "-jar", "/opt/inferno/world.jar"]
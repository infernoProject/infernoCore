FROM ${baseImage}

RUN mkdir -p /opt/inferno/config

VOLUME /opt/inferno/config

COPY worldd.jar /opt/inferno/worldd.jar
COPY WorldServer.conf /opt/inferno/config/WorldServer.conf

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 8085 5135

WORKDIR /opt/inferno

ENTRYPOINT ["/entrypoint.sh"]

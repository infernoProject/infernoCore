FROM ${baseImage}

RUN mkdir -p /opt/inferno/config

VOLUME /opt/inferno/config

COPY realm.jar /opt/inferno/realm.jar

COPY RealmServer.conf /opt/inferno/config/RealmServer.conf

EXPOSE 3274

WORKDIR /opt/inferno

ENTRYPOINT ["java", "-DconfigFile=/opt/inferno/config/RealmServer.conf", "-jar", "/opt/inferno/realm.jar"]
FROM ${baseImage}

RUN mkdir -p /opt/inferno/itests

COPY itests.jar /opt/inferno/itests/itests.jar
COPY itests-tests.jar /opt/inferno/itests/itests-tests.jar
COPY ITests.conf /opt/inferno/config/ITests.conf

COPY entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

WORKDIR /opt/inferno/itests

ENTRYPOINT ["/entrypoint.sh"]

LABEL project.groupid="${project.groupId}" \
      project.artifactid="${project.artifactId}" \
      project.version="${project.version}"
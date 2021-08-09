FROM alpine:3.14.0

ENV BSC_HOME=/bsc
ENV PATH $PATH:/${BSC_HOME}/bin

# copy files for provisioning
COPY build-apline-3.13/bin ${BSC_HOME}/bin/
COPY keystore ${BSC_HOME}/node1/keystore
COPY genesis.json ${BSC_HOME}/
COPY *.sh ${BSC_HOME}/

WORKDIR ${BSC_HOME}

# TODO: automate keystore generation during provisioning
ARG KEYSTORE_PASS
RUN echo ${KEYSTORE_PASS} > .secret
RUN ./provision.sh

COPY ./entrypoint ${BSC_HOME}

ENTRYPOINT ${BSC_HOME}/entrypoint

EXPOSE 8545
EXPOSE 8656
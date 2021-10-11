FROM alpine:3.14.0

ENV BSC_HOME=/bsc
ENV PATH $PATH:/${BSC_HOME}/bin
ENV BSC_DATADIR=${BSC_HOME}/node1

# copy files for provisioning
COPY build-apline-3.13/bin ${BSC_HOME}/bin/
COPY keystore ${BSC_HOME}/keystore
COPY genesis.json ${BSC_HOME}/

WORKDIR ${BSC_HOME}

# TODO: automate keystore generation during provisioning
ARG KEYSTORE_PASS
RUN echo ${KEYSTORE_PASS} > .secret

COPY ./entrypoint ${BSC_HOME}
COPY ./start.sh ${BSC_HOME}

EXPOSE 8545
EXPOSE 8656

ENTRYPOINT ${BSC_HOME}/entrypoint
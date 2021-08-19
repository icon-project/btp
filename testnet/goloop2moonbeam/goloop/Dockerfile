FROM goloop-icon:0.9.9

RUN apk add --no-cache jq

ENV GOLOOP_PROVISION=/goloop/provisioning
ENV GOLOOP_PROVISION_CONFIG=${GOLOOP_PROVISION}/config
ENV GOLOOP_PROVISION_DATA=${GOLOOP_PROVISION}/data
ENV GOLOOP_DATA_ROOT=/goloop/data
ENV GOLOOP_NODE_DIR=/goloop/data
ENV GOLOOP_CONFIG=/goloop/config/goloop.server.json
ENV GOLOOP_KEY_STORE=/goloop/config/goloop.keystore.json
ENV GOLOOP_KEY_SECRET=/goloop/config/goloop.keysecret
ENV GOLOOP_LOG_WRITER_FILENAME=/goloop/data/log/goloop.log
ENV GOLOOP_P2P_LISTEN=":8080"
ENV GOLOOP_RPC_ADDR=":9080"
ENV GOLOOP_CHAINSCORE=cx0000000000000000000000000000000000000000
ENV GOLOOP_ENGINES="python,java"
ENV PYEE_VERIFY_PACKAGE="true"

RUN mkdir -p ${GOLOOP_PROVISION_CONFIG}

COPY ./*.sh /goloop/bin/
COPY ./entrypoint /
COPY config ${GOLOOP_PROVISION_CONFIG}/

RUN provision.sh
WORKDIR /goloop/config
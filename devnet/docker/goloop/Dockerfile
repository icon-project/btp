FROM iconloop/goloop-icon:v0.9.7

RUN apk add --no-cache jq zip

ENV GOLOOP_PROVISION=/goloop/provisioning
ENV GOLOOP_PROVISION_CONFIG=${GOLOOP_PROVISION}/config
ENV GOLOOP_PROVISION_DATA=${GOLOOP_PROVISION}/data
RUN mkdir -p ${GOLOOP_PROVISION_CONFIG}


ENV GOLOOP_CONFIG=/goloop/config/goloop.server.json
ENV GOLOOP_KEY_SECRET=/goloop/config/goloop.keysecret
ENV GOLOOP_KEY_STORE=/goloop/config/goloop.keystore.json

COPY ./*.sh /goloop/bin/
COPY ./entrypoint /

RUN provision.sh
WORKDIR /goloop/config
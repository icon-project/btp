ARG BTPSIMPLE_VERSION=latest
ARG GOLOOP_IMAGE=goloop:latest

FROM btpsimple:${BTPSIMPLE_VERSION} AS btpsimple
FROM ${GOLOOP_IMAGE}

RUN apk add --no-cache jq

ENV GOLOOP_PROVISION=/goloop/provisioning
ENV GOLOOP_PROVISION_CONFIG=${GOLOOP_PROVISION}/config
ENV GOLOOP_PROVISION_CONTRACTS=${GOLOOP_PROVISION}/contracts
ENV GOLOOP_PROVISION_DATA=${GOLOOP_PROVISION}/data
# copy files for provisioning
COPY --from=btpsimple /btpsimple/contracts ${GOLOOP_PROVISION_CONTRACTS}/
COPY ./*.sh /goloop/bin/
COPY ./entrypoint /

RUN provision.sh
WORKDIR /goloop/config

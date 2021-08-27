ARG BTPSIMPLE_VERSION=latest
ARG GOLOOP_IMAGE=iconloop/goloop-icon:latest

FROM btpsimple:${BTPSIMPLE_VERSION} AS btpsimple
FROM ${GOLOOP_IMAGE}

RUN apk add --no-cache jq curl

ENV GOLOOP_PROVISION=/goloop/provisioning
ENV GOLOOP_PROVISION_CONFIG=${GOLOOP_PROVISION}/config
ENV GOLOOP_PROVISION_DATA=${GOLOOP_PROVISION}/data
# copy files for provisioning
COPY --from=btpsimple /btpsimple/contracts/pyscore ${GOLOOP_PROVISION_CONFIG}/pyscore/
COPY ./*.sh /goloop/bin/
COPY ./entrypoint /

RUN provision.sh
WORKDIR /goloop/config
ARG BMR_VERSION=latest
FROM iconloop/goloop:0.9.5

RUN apk add --no-cache jq zip

ENV GOLOOP_PROVISION=/goloop/provisioning
ENV GOLOOP_PROVISION_CONFIG=${GOLOOP_PROVISION}/config
ENV GOLOOP_PROVISION_DATA=${GOLOOP_PROVISION}/data

# copy files for provisioning
COPY ./pyscore ${GOLOOP_PROVISION_CONFIG}/pyscore/

COPY ./*.sh /goloop/bin/
COPY ./entrypoint /

RUN provision.sh
WORKDIR /goloop/config
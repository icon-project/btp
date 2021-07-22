FROM goloop-icon:0.9.7

RUN apk add --no-cache jq zip

ENV GOLOOP_PROVISION=/goloop/provisioning
ENV GOLOOP_PROVISION_CONFIG=${GOLOOP_PROVISION}/config
ENV GOLOOP_PROVISION_DATA=${GOLOOP_PROVISION}/data


COPY ./*.sh /goloop/bin/
COPY ./entrypoint /

RUN provision.sh
WORKDIR /goloop/config
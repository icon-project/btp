ARG BTPSIMPLE_VERSION=latest
ARG GOLOOP_IMAGE=iconloop/goloop-icon:v0.9.7

FROM ${GOLOOP_IMAGE}
FROM btpsimple:${BTPSIMPLE_VERSION} AS btpsimple
FROM node:16.1-alpine as node

#RUN apk add --update nodejs-current npm
RUN apk add --no-cache jq curl inotify-tools
RUN npm install --global truffle@5.3.0
RUN truffle version

# install btpsimple cli
COPY --from=btpsimple . .
ENV PATH $PATH:/btpsimple/bin

# install goloop cli
COPY --from=goloop /goloop/bin /goloop/bin
ENV PATH $PATH:/goloop/bin

ENV BTPSIMPLE_BASE_DIR=/btpsimple
ENV BTPSIMPLE_CONFIG_DIR=${BTPSIMPLE_BASE_DIR}/config
ENV BTPSIMPLE_CONTRACTS_DIR=${BTPSIMPLE_BASE_DIR}/contracts
ENV BTPSIMPLE_SCRIPTS_DIR=${BTPSIMPLE_BASE_DIR}/js

COPY ./*.sh /btpsimple/bin/
COPY ./entrypoint.sh /
COPY ./provision.sh /
COPY bsc.ks.json /bsc.ks.json

#RUN mkdir {BTPSIMPLE_SCRIPTS_DIR}
COPY ./*.js /btpsimple/js/

RUN chmod +x /provision.sh
RUN chmod +x /entrypoint.sh

WORKDIR /btpsimple/config
ENTRYPOINT ["entrypoint.sh"]
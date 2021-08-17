ARG BASE_IMAGE
FROM ${BASE_IMAGE}
LABEL MAINTAINER="t_arch@iconloop.com"

# install dependencies
#RUN apk add --no-cache jq

ARG VERSION
LABEL BTPSIMPLE_VERSION="$VERSION"

# install
COPY dist/bin/btpsimple /btpsimple/bin/btpsimple
ENV PATH $PATH:/btpsimple/bin

# copy extras
COPY dist/contracts/pyscore/*.zip /btpsimple/contracts/pyscore/
COPY dist/contracts/solidity /btpsimple/contracts/solidity
COPY dist/contracts/javascore /btpsimple/contracts/javascore
WORKDIR /btpsimple

# container configuration
VOLUME ["/btpsimple/data"]

# goloop entrypoint
ENV BTPSIMPLE_BASE_DIR=/btpsimple/data
ENV BTPSIMPLE_CONFIG=/btpsimple/config/config.json
ENV BTPSIMPLE_KEY_STORE=/btpsimple/config/keystore.json
ENV BTPSIMPLE_KEY_SECRET=/btpsimple/config/keysecret
ENV BTPSIMPLE_LOG_WRITER_FILENAME=/btpsimple/data/btpsimple.log

COPY ./entrypoint /btpsimple

RUN chmod +x entrypoint
ENTRYPOINT ["/btpsimple/entrypoint"]

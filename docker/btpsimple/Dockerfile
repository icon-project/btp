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
COPY dist/contracts/javascore /btpsimple/contracts/javascore
COPY dist/contracts/solidity /btpsimple/contracts/solidity

ENV JAVASCORE_DIST_DIR=/btpsimple/contracts/javascore
ENV SOLIDITY_DIST_DIR=/btpsimple/contracts/solidity
ENV PYSCORE_DIST_DIR=/btpsimple/contracts/pyscore

WORKDIR /btpsimple

# container configuration
VOLUME ["/btpsimple/data"]

# goloop entrypoint
ENV BTPSIMPLE_BASE_DIR=/btpsimple/data
ENV BTPSIMPLE_CONFIG=/btpsimple/config/config.json
ENV BTPSIMPLE_KEY_STORE=/btpsimple/config/keystore.json
ENV BTPSIMPLE_KEY_SECRET=/btpsimple/config/keysecret
ENV BTPSIMPLE_LOG_WRITER_FILENAME=/btpsimple/data/btpsimple.log

# entrypoint
RUN { \
    echo '#!/bin/sh'; \
    echo 'set -e'; \
    echo 'if [ "$BTPSIMPLE_CONFIG" != "" ] && [ ! -f "$BTPSIMPLE_CONFIG" ]; then'; \
    echo '  UNSET="BTPSIMPLE_CONFIG"' ; \
    echo '  CMD="btpsimple save $BTPSIMPLE_CONFIG"'; \
    echo '  if [ "$BTPSIMPLE_KEY_SECRET" != "" ] && [ ! -f "$BTPSIMPLE_KEY_SECRET" ]; then'; \
    echo '    mkdir -p $(dirname $BTPSIMPLE_KEY_SECRET)'; \
    echo '    echo -n $(date|md5sum|head -c16) > $BTPSIMPLE_KEY_SECRET' ; \
    echo '  fi'; \
    echo '  if [ "$BTPSIMPLE_KEY_STORE" != "" ] && [ ! -f "$BTPSIMPLE_KEY_STORE" ]; then'; \
    echo '    UNSET="$UNSET BTPSIMPLE_KEY_STORE"' ; \
    echo '    CMD="$CMD --save_key_store=$BTPSIMPLE_KEY_STORE"' ; \
    echo '  fi'; \
    echo '  if [ "$BTPSIMPLE_SRC_ADDRESS" != "" ] && [ -f "$BTPSIMPLE_SRC_ADDRESS" ]; then'; \
    echo '    export BTPSIMPLE_SRC_ADDRESS=$(cat ${BTPSIMPLE_SRC_ADDRESS})' ; \
    echo '  fi'; \
    echo '  if [ "$BTPSIMPLE_SRC_ENDPOINT" != "" ] && [ -f "$BTPSIMPLE_SRC_ENDPOINT" ]; then'; \
    echo '    export BTPSIMPLE_SRC_ENDPOINT=$(cat ${BTPSIMPLE_SRC_ENDPOINT})' ; \
    echo '  fi'; \
    echo '  if [ "$BTPSIMPLE_DST_ADDRESS" != "" ] && [ -f "$BTPSIMPLE_DST_ADDRESS" ]; then'; \
    echo '    export BTPSIMPLE_DST_ADDRESS=$(cat ${BTPSIMPLE_DST_ADDRESS})' ; \
    echo '  fi'; \
    echo '  if [ "$BTPSIMPLE_DST_ENDPOINT" != "" ] && [ -f "$BTPSIMPLE_DST_ENDPOINT" ]; then'; \
    echo '    export BTPSIMPLE_DST_ENDPOINT=$(cat ${BTPSIMPLE_DST_ENDPOINT})' ; \
    echo '  fi'; \
    echo '  if [ "$BTPSIMPLE_OFFSET" != "" ] && [ -f "$BTPSIMPLE_OFFSET" ]; then'; \
    echo '    export BTPSIMPLE_OFFSET=$(cat ${BTPSIMPLE_OFFSET})' ; \
    echo '  fi'; \
    echo '  sh -c "unset $UNSET ; $CMD"' ; \
    echo 'fi'; \
    echo 'exec "$@"'; \
    } > /entrypoint \
    && chmod +x /entrypoint
ENTRYPOINT ["/entrypoint"]

CMD btpsimple start

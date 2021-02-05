ARG BASE_IMAGE
FROM ${BASE_IMAGE}
LABEL MAINTAINER="t_arch@iconloop.com"

# install dependencies
#RUN apk add --no-cache git bash jq

ARG VERSION
LABEL BTPSIMPLE_VERSION="$VERSION"

# install
COPY dist/bin/btpsimple /btpsimple/bin/btpsimple
ENV PATH $PATH:/btpsimple/bin

# copy extras
COPY dist/pyscore/*.zip /btpsimple/contracts/pyscore/
WORKDIR /btpsimple

# container configuration
#EXPOSE 9080/tcp
VOLUME ["/btpsimple/data"]

# goloop entrypoint
ENV BTPSIMPLE_BASE_DIR=/btpsimple/data
ENV BTPSIMPLE_CONFIG=/btpsimple/config/config.json
ENV BTPSIMPLE_KEY_STORE=/btpsimple/config/keystore.json
ENV BTPSIMPLE_KEY_SECRET=/btpsimple/config/keysecret
#ENV BTPSIMPLE_BMRP_ADDR=":9080"
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
        echo '  sh -c "unset $UNSET ; $CMD"' ; \
        echo 'fi'; \
        echo 'exec "$@"'; \
    } > /entrypoint \
    && chmod +x /entrypoint
ENTRYPOINT ["/entrypoint"]

CMD btpsimple start

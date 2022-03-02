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
ADD entrypoint.sh /entrypoint.sh
RUN chmod u+x /entrypoint.sh
ENTRYPOINT ["/entrypoint.sh"]

CMD btpsimple start

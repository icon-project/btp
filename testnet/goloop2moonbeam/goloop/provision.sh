#!/bin/sh
set -e

GOLOOP_PROVISION=${GOLOOP_PROVISION:-/goloop/provisioning}
GOLOOP_PROVISION_CONFIG=${GOLOOP_PROVISION_CONFIG:-${GOLOOP_PROVISION}/config}
GOLOOP_PROVISION_DATA=${GOLOOP_PROVISION_DATA:-${GOLOOP_PROVISION}/data}
mkdir -p ${GOLOOP_PROVISION_CONFIG} && mkdir -p ${GOLOOP_PROVISION_DATA} && cd ${GOLOOP_PROVISION_CONFIG}

##########################
# Chain setup
source server.sh

GOLOOP_NODE_DIR=${GOLOOP_PROVISION_DATA}
GOLOOP_LOG_WRITER_FILENAME=${GOLOOP_PROVISION}/goloop.log
GOLOOP_KEY_SECRET=${GOLOOP_PROVISION_CONFIG}/keysecret
GOLOOP_KEY_STORE=${GOLOOP_PROVISION_CONFIG}/keystore.json
GOLOOP_CONFIG=${GOLOOP_PROVISION_CONFIG}/server.json
ensure_server_start

goloop gn gen --out icon.genesis.json $GOLOOP_KEY_STORE
goloop chain join --genesis_template icon.genesis.json --channel icon --auto_start
goloop chain start icon

##########################
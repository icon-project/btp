#!/bin/sh
set -e

GOLOOP_PROVISION=${GOLOOP_PROVISION:-/goloop/provisioning}
GOLOOP_PROVISION_CONFIG=${GOLOOP_PROVISION_CONFIG:-${GOLOOP_PROVISION}/config}
GOLOOP_PROVISION_DATA=${GOLOOP_PROVISION_DATA:-${GOLOOP_PROVISION}/data}

GOLOOP_NODE_DIR=${GOLOOP_PROVISION_DATA}
GOLOOP_LOG_WRITER_FILENAME=${GOLOOP_PROVISION}/goloop.log
GOLOOP_KEY_SECRET=${GOLOOP_PROVISION_CONFIG}/goloop.keysecret
GOLOOP_KEY_STORE=${GOLOOP_PROVISION_CONFIG}/goloop.keystore.json
GOLOOP_CONFIG=${GOLOOP_PROVISION_CONFIG}/goloop.server.json

cd ${GOLOOP_PROVISION_CONFIG}
##########################
# Chain setup
source server.sh
ensure_server_start

goloop chain join --genesis_template icon.genesis.json --channel icon --auto_start
goloop chain inspect icon --format {{.NID}} > nid.icon

goloop chain start icon
goloop chain stop icon

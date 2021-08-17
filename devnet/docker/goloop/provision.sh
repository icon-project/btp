#!/bin/sh
set -e

GOLOOP_PROVISION=${GOLOOP_PROVISION:-/goloop/provisioning}
GOLOOP_PROVISION_CONFIG=${GOLOOP_PROVISION_CONFIG:-${GOLOOP_PROVISION}/config}
GOLOOP_PROVISION_DATA=${GOLOOP_PROVISION_DATA:-${GOLOOP_PROVISION}/data}
cd ${GOLOOP_PROVISION_CONFIG}

##########################
# Chain setup
source server.sh

GOLOOP_NODE_DIR=${GOLOOP_PROVISION_DATA}
GOLOOP_LOG_WRITER_FILENAME=${GOLOOP_PROVISION}/goloop.log
GOLOOP_KEY_SECRET=${GOLOOP_PROVISION_CONFIG}/goloop.keysecret
GOLOOP_KEY_STORE=${GOLOOP_PROVISION_CONFIG}/goloop.keystore.json
GOLOOP_CONFIG=${GOLOOP_PROVISION_CONFIG}/goloop.server.json
ensure_server_start

goloop gn gen --out icon.genesis.json $GOLOOP_KEY_STORE
echo $(cat icon.genesis.json | jq -r '.*{"chain":{"fee":{"stepLimit":{"invoke":"0x10000000","query":"0x1000000"}}}}') > icon.genesis.json
goloop chain join --genesis_template icon.genesis.json --channel icon --auto_start
goloop chain inspect icon --format {{.NID}} > nid.icon

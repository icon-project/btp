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
GOLOOP_KEY_SECRET=${GOLOOP_PROVISION_CONFIG}/keysecret
GOLOOP_KEY_STORE=${GOLOOP_PROVISION_CONFIG}/keystore.json
GOLOOP_CONFIG=${GOLOOP_PROVISION_CONFIG}/server.json
ensure_server_start

goloop gn gen --out src.genesis.json $GOLOOP_KEY_STORE
echo $(cat src.genesis.json | jq -r '.*{"chain":{"fee":{"stepLimit":{"invoke":"0x10000000","query":"0x1000000"}}}}') > src.genesis.json
goloop gn gen --out dst.genesis.json $GOLOOP_KEY_STORE
echo $(cat dst.genesis.json | jq -r '.*{"chain":{"fee":{"stepLimit":{"invoke":"0x10000000","query":"0x1000000"}}}}') > dst.genesis.json

goloop chain join --genesis_template src.genesis.json --channel src --auto_start
goloop chain join --genesis_template dst.genesis.json --channel dst --auto_start

goloop chain start src
goloop chain start dst

##########################
# Deploy BMC, BMV
source btp.sh

deploy_bmc src
deploy_bmc dst

deploy_bmv src dst
deploy_bmv dst src

##########################
# Configuration for relay
bmc_link src dst
bmc_link dst src

bmc_setLink src dst
bmc_setLink dst src

ensure_key_store src.ks.json src.secret
rpcks src.ks.json src.secret
bmc_addRelayer dst
bmc_addRelay dst src

ensure_key_store dst.ks.json dst.secret
rpcks dst.ks.json dst.secret
bmc_addRelayer src
bmc_addRelay src dst

##########################
# Deploy Token-BSH, IRC2-Token
source token.sh
deploy_bsh src
deploy_bsh dst

deploy_irc2 src
deploy_irc2 dst proxy

##########################
# Configuration for token service
bsh_register src
bsh_register dst

goloop chain stop src
goloop chain stop dst

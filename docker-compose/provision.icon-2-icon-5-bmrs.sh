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
goloop gn gen --out dst.genesis.json $GOLOOP_KEY_STORE

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

ensure_key_store src_1.ks.json src_1.secret
rpcks src_1.ks.json src_1.secret
bmc_addRelayer dst
bmc_addRelay dst src

ensure_key_store src_2.ks.json src_2.secret
rpcks src_2.ks.json src_2.secret
bmc_addRelayer dst
bmc_addRelay dst src

ensure_key_store src_3.ks.json src_3.secret
rpcks src_3.ks.json src_3.secret
bmc_addRelayer dst
bmc_addRelay dst src

ensure_key_store src_4.ks.json src_4.secret
rpcks src_4.ks.json src_4.secret
bmc_addRelayer dst
bmc_addRelay dst src

ensure_key_store src_5.ks.json src_5.secret
rpcks src_5.ks.json src_5.secret
bmc_addRelayer dst
bmc_addRelay dst src

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
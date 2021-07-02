#!/bin/sh
set -e

GOLOOP_PROVISION=${GOLOOP_PROVISION:-/goloop/provisioning}
GOLOOP_PROVISION_CONFIG=${GOLOOP_PROVISION_CONFIG:-${GOLOOP_PROVISION}/config}
GOLOOP_PROVISION_CONTRACTS=${GOLOOP_PROVISION_CONTRACTS:-${GOLOOP_PROVISION}/contracts}
GOLOOP_PROVISION_DATA=${GOLOOP_PROVISION_DATA:-${GOLOOP_PROVISION}/data}
mkdir -p ${GOLOOP_PROVISION_CONFIG}
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

#CONTRACT_BMC=${GOLOOP_PROVISION_CONTRACTS}/pyscore/bmc.zip
CONTRACT_BMC=${GOLOOP_PROVISION_CONTRACTS}/javascore/bmc.jar
echo "Deploy BMC ${CONTRACT_BMC}"
deploy_bmc src ${CONTRACT_BMC}
deploy_bmc dst ${CONTRACT_BMC}

#CONTRACT_BMV=${GOLOOP_PROVISION_CONTRACTS}/pyscore/bmv.zip
CONTRACT_BMV=${GOLOOP_PROVISION_CONTRACTS}/javascore/bmv-icon.jar
echo "Deploy BMV ${CONTRACT_BMV}"
deploy_bmv src dst ${CONTRACT_BMV}
deploy_bmv dst src ${CONTRACT_BMV}

##########################
# Configuration for relay
echo "Register BMV to BMC"
bmc_addVerifier src dst
bmc_addVerifier dst src

echo "Configuration for relay"
bmc_link src dst
bmc_link dst src

echo "setLink"
bmc_setLink src dst
bmc_setLink dst src

##########################
# Register BMC-Owner
echo "Register BMC-Owner"

ensure_key_store src.ks.json src.secret
ensure_key_store dst.ks.json dst.secret

bmc_addOwner src src.ks.json
bmc_addOwner dst dst.ks.json

##########################
# Register BMR
echo "Register BMR by BMC-Owner"
rpcks src.ks.json src.secret
bmc_addRelay src dst dst.ks.json

rpcks dst.ks.json dst.secret
bmc_addRelay dst src src.ks.json

rpcks $GOLOOP_KEY_STORE $GOLOOP_KEY_SECRET

##########################
# Configuration for relayer
echo "Configuration for relayer"
ensure_key_store relayer.ks.json relayer.secret
rpcch src
goloop rpc sendtx transfer --to $(rpceoa relayer.ks.json) --value 0x10
rpcks relayer.ks.json relayer.secret
bmc_registerRelayer src
rpcks $GOLOOP_KEY_STORE $GOLOOP_KEY_SECRET

##########################
# Deploy IRC31, NativeCoin-BSH
source nativecoin.sh

CONTRACT_IRC31=${GOLOOP_PROVISION_CONTRACTS}/javascore/irc31.jar
echo "Deploy IRC31 ${CONTRACT_IRC31}"
deploy_irc31 src ${CONTRACT_IRC31}
deploy_irc31 dst ${CONTRACT_IRC31}

CONTRACT_NATIVECOIN=${GOLOOP_PROVISION_CONTRACTS}/javascore/nativecoin.jar
echo "Deploy NativeCoin-BSH ${CONTRACT_NATIVECOIN}"
deploy_nc src ${CONTRACT_NATIVECOIN}
deploy_nc dst ${CONTRACT_NATIVECOIN}

##########################
# Configuration for native coin service
echo "Register NativeCoin-BSH to BMC"
bmc_addService src nativecoin $(cat nativecoin.src)
bmc_addService dst nativecoin $(cat nativecoin.dst)

echo "Configuration for native coin service"
nc_register src dst
nc_register dst src

echo "Register NativeCoin-BSH to IRC31 as owner"
irc31_addOwner src $(cat nativecoin.src)
irc31_addOwner dst $(cat nativecoin.dst)

##########################
# Deploy Token-BSH, IRC2-Token
source token.sh

CONTRACT_TOKEN=${GOLOOP_PROVISION_CONTRACTS}/pyscore/token.zip
echo "Deploy Token-BSH ${CONTRACT_TOKEN}"
deploy_token src ${CONTRACT_TOKEN}
deploy_token dst ${CONTRACT_TOKEN}

CONTRACT_IRC2=${GOLOOP_PROVISION_CONTRACTS}/pyscore/irc2.zip
echo "Deploy IRC2-Token ${CONTRACT_IRC2}"
deploy_irc2 src ${CONTRACT_IRC2}
deploy_irc2 dst ${CONTRACT_IRC2} proxy

##########################
# Configuration for token service
echo "Register Token-BSH to BMC"
bmc_addService src token $(cat token.src)
bmc_addService dst token $(cat token.dst)

echo "Configuration for token service"
token_register src
token_register dst

goloop chain stop src
goloop chain stop dst

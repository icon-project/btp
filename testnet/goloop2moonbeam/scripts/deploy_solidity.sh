#!/bin/sh
SOLIDITY_DIST_DIR=${SOLIDITY_DIST_DIR:-/btpsimple/contracts/solidity}
MOONBEAM_CHAIN_ID=1281 # https://github.com/PureStake/moonbeam#chain-ids
export MOONBEAM_RPC_URL=${MOONBEAM_RPC_URL:-'http://moonbeam:9933'}

source deploy_util.sh

deploy_solidity_bmc() {
    echo "deploying icon bmc"

    cd $CONFIG_DIR
    echo "$(printf "0x%01x" $MOONBEAM_CHAIN_ID).pra" > net.btp.moonbeam
    export BMC_PRA_NET=$(cat net.btp.moonbeam)

    cd $SOLIDITY_DIST_DIR/bmc
    sed -i 's/"http:\/\/localhost:9933"/process.env.MOONBEAM_RPC_URL/' truffle-config.js
    truffle migrate --network moonbeamlocal
}
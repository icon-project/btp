#!/bin/sh
CONFIG_DIR=${CONFIG_DIR:-/btpsimple/config}
JAVASCORE_DIST_DIR=${JAVASCORE_DIST_DIR:-/btpsimple/contracts/javascore}

source goloop_rpc.sh
rpcch


latest_blocknumber_goloop() {
    goloop rpc lastblock | jq -r .height
}

deploy_javascore_bmc() {
    cd $CONFIG_DIR
    echo "$GOLOOP_RPC_NID.icon" > net.btp.icon

    goloop rpc sendtx deploy $JAVASCORE_DIST_DIR/bmc-0.1.0-debug.jar \
    --content_type application/java --step_limit 13610920001 \
    --param _net=$(cat net.btp.icon) | jq -r . > tx.bmc.icon

    extract_scoreAddress tx.bmc.icon bmc.icon
    echo "btp://$(cat net.btp.icon)/$(cat bmc.icon)" > btp.icon
}
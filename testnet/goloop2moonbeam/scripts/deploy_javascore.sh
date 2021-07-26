#!/bin/sh
HELPER_DIR=${HELPER_DIR:-/btpsimple/helper}
JAVASCORE_DIST_DIR=${JAVASCORE_DIST_DIR:-/btpsimple/contracts/javascore}

source deploy_util.sh

deploy_javascore_bmc() {
    echo "deploying icon bmc"

    cd $CONFIG_DIR
    echo "$GOLOOP_RPC_NID.icon" > net.btp.icon

    goloop rpc sendtx deploy $JAVASCORE_DIST_DIR/bmc-0.1.0-debug.jar \
    --content_type application/java \
    --param _net=$(cat net.btp.icon) | jq -r . > tx.bmc.icon

    extract_scoreAddress tx.bmc.icon bmc.icon
    echo "btp://$(cat net.btp.icon)/$(cat bmc.icon)" > btp.icon
}

deploy_kusamaDecoder(){
    echo "deploying kusamaDecoder"
    
    cd $CONFIG_DIR
    goloop rpc sendtx deploy $JAVASCORE_DIST_DIR/KusamaEventDecoder-optimized.jar \
    --content_type application/java | jq -r . > tx.kusamaDecoder.icon

    extract_scoreAddress tx.kusamaDecoder.icon kusamaDecoder.icon
}

deploy_moonriverDecoder(){
    echo "deploying moonriverDecoder"

    cd $CONFIG_DIR
    goloop rpc sendtx deploy $JAVASCORE_DIST_DIR/MoonriverEventDecoder-optimized.jar \
    --content_type application/java | jq -r . > tx.moonriverDecoder.icon

    extract_scoreAddress tx.moonriverDecoder.icon moonriverDecoder.icon
}

prepare_javascore_bmv() {
    deploy_kusamaDecoder
    deploy_moonriverDecoder
    
    echo "getting BMVInitializeParams"
    cd $HELPER_DIR
    yarn

    latest_blocknumber_moonbase > para.offset
    export PARA_OFFSET=$(cat para.offset)
    export RELAY_ENDPOINT=${RELAY_ENDPOINT:-'wss://kusama-rpc.polkadot.io'}
    export RELAY_OFFSET=${RELAY_OFFSET:-8511058}
    export PARA_ENDPOINT=${PARA_ENDPOINT:-'ws://moonbeam:9944'}

    yarn getBMVInitializeParams
    cp -f para.offset $CONFIG_DIR/
    cp -f BMVInitializeData.json $CONFIG_DIR/
    rm -rf ./node_modules
}

deploy_javascore_bmv() {
    echo "deploying icon bmv"

    prepare_javascore_bmv

    cd $CONFIG_DIR
    tmp=$(cat BMVInitializeData.json)
    relayMtaOffset=$(echo "$tmp" | jq -r .relayMtaOffset)
    paraMtaOffset=$(echo "$tmp" | jq -r .paraMtaOffset)
    relayLastBlockHash=$(echo "$tmp" | jq -r .relayLastBlockHash)
    paraLastBlockHash=$(echo "$tmp" | jq -r .paraLastBlockHash)
    encodedValidators=$(echo "$tmp" | jq -r .encodedValidators)
    relayCurrentSetId=$(echo "$tmp" | jq -r .relayCurrentSetId)
    # paraChainId=$(echo "$tmp" | jq -r .paraChainId)
    

    goloop rpc sendtx deploy $JAVASCORE_DIST_DIR/parachain-BMV-optimized.jar \
        --content_type application/java \
        --param relayMtaOffset=$relayMtaOffset \
        --param paraMtaOffset=$paraMtaOffset \
        --param bmc=$(cat bmc.icon) \
        --param net=$(cat net.btp.icon) \
        --param mtaRootSize=0x8 \
        --param mtaCacheSize=0x8 \
        --param mtaIsAllowNewerWitness=0x1 \
        --param relayLastBlockHash=$relayLastBlockHash \
        --param paraLastBlockHash=$paraLastBlockHash \
        --param encodedValidators=$encodedValidators \
        --param relayEventDecoderAddress=$(cat kusamaDecoder.icon) \
        --param paraEventDecoderAddress=$(cat moonriverDecoder.icon) \
        --param relayCurrentSetId=$relayCurrentSetId \
        --param paraChainId=0x0 \
        | jq -r . > tx.bmv.icon

    extract_scoreAddress tx.bmv.icon bmv.icon
}

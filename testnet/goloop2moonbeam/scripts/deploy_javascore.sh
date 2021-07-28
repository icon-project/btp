#!/bin/sh
ICON_RECEIVER_FEE_ADDRESS=hxb6b5791be0b5ef67063b3c10b840fb81514db2fd
source deploy_util.sh

deploy_javascore_bmc() {
    echo "deploying javascrore bmc"

    cd $CONFIG_DIR
    echo "$GOLOOP_RPC_NID.icon" > net.btp.icon

    goloop rpc sendtx deploy $JAVASCORE_DIST_DIR/bmc-0.1.0-debug.jar \
    --content_type application/java \
    --param _net=$(cat net.btp.icon) | jq -r . > tx.bmc.icon

    extract_scoreAddress tx.bmc.icon bmc.icon
    echo "btp://$(cat net.btp.icon)/$(cat bmc.icon)" > btp.icon
}

_deploy_kusamaDecoder(){
    echo "deploying javascrore kusamaDecoder"
    
    cd $CONFIG_DIR
    goloop rpc sendtx deploy $JAVASCORE_DIST_DIR/KusamaEventDecoder-optimized.jar \
    --content_type application/java | jq -r . > tx.kusamaDecoder.icon

    extract_scoreAddress tx.kusamaDecoder.icon kusamaDecoder.icon
}

_deploy_moonriverDecoder(){
    echo "deploying javascrore moonriverDecoder"

    cd $CONFIG_DIR
    goloop rpc sendtx deploy $JAVASCORE_DIST_DIR/MoonriverEventDecoder-optimized.jar \
    --content_type application/java | jq -r . > tx.moonriverDecoder.icon

    extract_scoreAddress tx.moonriverDecoder.icon moonriverDecoder.icon
}

_prepare_javascore_bmv() {
    _deploy_kusamaDecoder
    _deploy_moonriverDecoder
    
    export PARA_OFFSET=$(moonbeam_blocknumber)
    export RELAY_OFFSET=${RELAY_OFFSET:-8511058}
    export RELAY_ENDPOINT=${RELAY_ENDPOINT:-'wss://kusama-rpc.polkadot.io'}
    export PARA_ENDPOINT=${PARA_ENDPOINT:-'ws://moonbeam:9944'}

    echo "getting BMVInitializeParams at PARA_OFFSET:$PARA_OFFSET RELAY_OFFSET:$RELAY_OFFSET"
    cd $JAVASCORE_HELPER_DIR
    yarn
    yarn getBMVInitializeParams
    wait_file_created $JAVASCORE_HELPER_DIR BMVInitializeData.json
    echo $PARA_OFFSET > $CONFIG_DIR/moonbeam.offset
    cp -f BMVInitializeData.json $CONFIG_DIR/
    rm -rf ./node_modules
}

deploy_javascore_bmv() {
    echo "deploying javascrore bmv"
    _prepare_javascore_bmv

    cd $CONFIG_DIR
    tmp=$(cat BMVInitializeData.json)
    relayMtaOffset=$(echo "$tmp" | jq -r .relayMtaOffset)
    paraMtaOffset=$(echo "$tmp" | jq -r .paraMtaOffset)
    relayLastBlockHash=$(echo "$tmp" | jq -r .relayLastBlockHash)
    paraLastBlockHash=$(echo "$tmp" | jq -r .paraLastBlockHash)
    encodedValidators=$(echo "$tmp" | jq -r .encodedValidators)
    relayCurrentSetId=$(echo "$tmp" | jq -r .relayCurrentSetId)
    # paraChainId=$(echo "$tmp" | jq -r .paraChainId)

    echo "parachain height:$paraMtaOffset block_hash:$paraLastBlockHash"

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

deploy_javascore_IRC31Token() {
    echo "deploy_javascore_IRC31Token"
    cd $CONFIG_DIR

    goloop rpc sendtx deploy $JAVASCORE_DIST_DIR/irc31-0.1.0-debug.jar \
    --content_type application/java | jq -r . > tx.irc31token.icon

    extract_scoreAddress tx.irc31token.icon irc31token.icon
}

deploy_javascore_NativeCoinBSH() {
    echo "deploy_javascore_NativeCoinBSH"

    cd $CONFIG_DIR

    goloop rpc sendtx deploy $JAVASCORE_DIST_DIR/nativecoin-0.1.0-debug.jar \
        --content_type application/java \
        --param _bmc=$(cat bmc.icon) \
        --param _irc31=$(cat irc31token.icon) \
        --param _name=ICX | jq -r . > tx.nativeCoinBsh.icon

    extract_scoreAddress tx.nativeCoinBsh.icon nativeCoinBsh.icon
}

deploy_javascore_FeeAggregation() {
    echo "deploy_javascore_FeeAggregation"

    cd $CONFIG_DIR

    goloop rpc sendtx deploy $JAVASCORE_DIST_DIR/fee-aggregation-system-1.0-optimized.jar \
        --param _cps_address=$ICON_RECEIVER_FEE_ADDRESS \
        --content_type application/java | jq -r . > tx.feeAggregation.icon

    extract_scoreAddress tx.feeAggregation.icon feeAggregation.icon
}

goloop_bmc_addVerifier() {
    echo "goloop_bmc_addVerifier"
    cd $CONFIG_DIR
    goloop rpc sendtx call --to $(cat bmc.icon) \
        --method addVerifier \
        --param _net=$(cat net.btp.moonbeam) \
        --param _addr=$(cat bmv.icon) | jq -r . > tx.verifier.icon

    ensure_txresult tx.verifier.icon
}

goloop_bmc_addLink() {
    echo "goloop_bmc_addLink"
    cd $CONFIG_DIR

    goloop rpc sendtx call --to $(cat bmc.icon) \
        --method addLink \
        --param _link=$(cat btp.moonbeam) | jq -r . > tx.link.icon
    ensure_txresult tx.link.icon

    echo "goloop_bmc_setLinkRotateTerm"
    goloop rpc sendtx call --to $(cat bmc.icon) \
        --method setLinkRotateTerm \
        --param _link=$(cat btp.moonbeam) \
        --param _block_interval=0x1770 \
        --param _max_agg=0x08 \
        | jq -r . > tx.setLinkRotateTerm.icon
    ensure_txresult tx.setLinkRotateTerm.icon

    echo "goloop_bmc_setLinkDelayLimit"
    goloop rpc sendtx call --to $(cat bmc.icon) \
    --method setLinkDelayLimit \
    --param _link=$(cat btp.moonbeam) \
    --param _value=4 \
    | jq -r . > tx.setLinkDelayLimit.icon
    ensure_txresult tx.setLinkDelayLimit.icon

    echo "finished goloop_bmc_addLink"
}

goloop_bmc_addService() {
    echo "goloop_bmc_addService"
    cd $CONFIG_DIR

    goloop rpc sendtx call --to $(cat bmc.icon) \
        --method addService \
        --param _addr=$(cat nativeCoinBsh.icon) \
        --param _svc=CoinTransfer \
        | jq -r . > tx.addService.icon
    ensure_txresult tx.addService.icon
}

goloop_bmc_addRelay() {
    echo "goloop_bmc_addRelay"
    cd $CONFIG_DIR

    goloop rpc sendtx call --to $(cat bmc.icon) \
        --method addRelay \
        --param _link=$(cat btp.moonbeam) \
        --param _addr=$(jq -r .address gochain.keystore.json) \
        | jq -r . > tx.registerRelay.icon
    ensure_txresult tx.registerRelay.icon
}

goloop_bmc_setFeeAggregator() {
    echo "goloop_bmc_setFeeAggregator"
    cd $CONFIG_DIR

    goloop rpc sendtx call --to $(cat bmc.icon) \
        --method setFeeAggregator \
        --param _addr=$(cat feeAggregation.icon) \
        | jq -r . > tx.addFeeAggregation.icon
    ensure_txresult tx.addFeeAggregation.icon
}

goloop_bsh_config_native_coin() {
    echo "goloop_bsh_config_native_coin"
    cd $CONFIG_DIR

    goloop rpc sendtx call --to $(cat nativeCoinBsh.icon) \
        --method register \
        --param _name=DEV \
        | jq -r . > tx.registerCoin.icon
    ensure_txresult tx.registerCoin.icon

    echo "goloop_bsh_setFeeRatio"
    goloop rpc sendtx call --to $(cat nativeCoinBsh.icon) \
        --method setFeeRatio \
        --param _feeNumerator=100 \
        | jq -r . > tx.setFeeRatio.icon
    ensure_txresult tx.setFeeRatio.icon

    echo "goloop_bsh_addOwner"
    goloop rpc sendtx call --to $(cat irc31token.icon) \
        --method addOwner \
        --param _addr=$(cat nativeCoinBsh.icon) \
        | jq -r . > tx.addOwnerIrc31.icon
    ensure_txresult tx.addOwnerIrc31.icon
}
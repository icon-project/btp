#!/bin/sh
ICON_RECEIVER_FEE_ADDRESS=hxb6b5791be0b5ef67063b3c10b840fb81514db2fd
ICON_BAND_PROTOCOL_ADDRESS=hxb6b5791be0b5ef67063b3c10b840fb81514db2fd

deploy_javascore_bmc() {
    echo "deploying javascore bmc"

    cd $CONFIG_DIR
    echo "$GOLOOP_RPC_NID.icon" > net.btp.icon

    goloop rpc sendtx deploy $JAVASCORE_DIST_DIR/bmc-0.1.0-optimized.jar \
    --content_type application/java \
    --param _net=$(cat net.btp.icon) | jq -r . > tx.icon.deploy_bmc

    extract_scoreAddress tx.icon.deploy_bmc bmc.icon
    ensure_file_exist $CONFIG_DIR bmc.icon
    echo "btp://$(cat net.btp.icon)/$(cat bmc.icon)" > btp.icon
}

_deploy_kusamaDecoder(){
    echo "deploying javascore kusamaDecoder"
    
    cd $CONFIG_DIR
    goloop rpc sendtx deploy $JAVASCORE_DIST_DIR/KusamaEventDecoder-optimized.jar \
    --content_type application/java | jq -r . > tx.icon.deploy_kusamaDecoder

    extract_scoreAddress tx.icon.deploy_kusamaDecoder kusamaDecoder.icon
    ensure_file_exist $CONFIG_DIR kusamaDecoder.icon
}

_deploy_moonriverDecoder(){
    echo "deploying javascore moonriverDecoder"

    cd $CONFIG_DIR
    goloop rpc sendtx deploy $JAVASCORE_DIST_DIR/MoonriverEventDecoder-optimized.jar \
    --content_type application/java | jq -r . > tx.icon.deploy_moonriverDecoder

    extract_scoreAddress tx.icon.deploy_moonriverDecoder moonriverDecoder.icon
    ensure_file_exist $CONFIG_DIR moonriverDecoder.icon
}

_prepare_javascore_bmv() {
    _deploy_kusamaDecoder
    _deploy_moonriverDecoder
    
    export PARA_CHAIN_OFFSET=$(moonbeam_blocknumber)
    export RELAY_CHAIN_OFFSET=${RELAY_CHAIN_OFFSET:-8511058}
    export RELAY_ENDPOINT=${RELAY_ENDPOINT:-'wss://kusama-rpc.polkadot.io'}
    export PARA_ENDPOINT=${PARA_ENDPOINT:-'ws://moonbeam:9944'}

    echo "getting BMVInitializeParams at PARA_CHAIN_OFFSET:$PARA_CHAIN_OFFSET RELAY_CHAIN_OFFSET:$RELAY_CHAIN_OFFSET"
    cd $JAVASCORE_HELPER_DIR
    yarn && yarn getBMVInitializeParams
    wait_file_created $JAVASCORE_HELPER_DIR BMVInitializeData.json

    echo $PARA_CHAIN_OFFSET > $CONFIG_DIR/offset.moonbeam_parachain
    echo $RELAY_CHAIN_OFFSET > $CONFIG_DIR/offset.moonbeam_relaychain
    
    cp -f BMVInitializeData.json $CONFIG_DIR/
    rm -rf ./node_modules
}

deploy_javascore_bmv() {
    echo "deploying javascore bmv"
    _prepare_javascore_bmv

    cd $CONFIG_DIR
    tmp=$(cat BMVInitializeData.json)
    relayMtaOffset=$(echo "$tmp" | jq -r .relayMtaOffset)
    paraMtaOffset=$(echo "$tmp" | jq -r .paraMtaOffset)
    relayLastBlockHash=$(echo "$tmp" | jq -r .relayLastBlockHash)
    paraLastBlockHash=$(echo "$tmp" | jq -r .paraLastBlockHash)
    encodedValidators=$(echo "$tmp" | jq -r .encodedValidators)
    relayCurrentSetId=$(echo "$tmp" | jq -r .relayCurrentSetId)
    evmEventIndex=$(echo "$tmp" | jq -r .evmEventIndex) \
    newAuthoritiesEventIndex=$(echo "$tmp" | jq -r .newAuthoritiesEventIndex) \
    candidateIncludedEventIndex=$(echo "$tmp" | jq -r .candidateIncludedEventIndex)
    # paraChainId=$(echo "$tmp" | jq -r .paraChainId) # # Currently deployment of moonbeam is dev the default is 0x0, not relates to Kusama

    echo "parachain height:$paraMtaOffset block_hash:$paraLastBlockHash"

    goloop rpc sendtx deploy $JAVASCORE_DIST_DIR/parachain-BMV-optimized.jar \
        --content_type application/java \
        --param relayMtaOffset=$relayMtaOffset \
        --param paraMtaOffset=$paraMtaOffset \
        --param bmc=$(cat bmc.icon) \
        --param net=$(cat net.btp.moonbeam) \
        --param mtaRootSize=0x10 \
        --param mtaCacheSize=0x10 \
        --param mtaIsAllowNewerWitness=0x1 \
        --param relayLastBlockHash=$relayLastBlockHash \
        --param paraLastBlockHash=$paraLastBlockHash \
        --param relayEventDecoderAddress=$(cat kusamaDecoder.icon) \
        --param paraEventDecoderAddress=$(cat moonriverDecoder.icon) \
        --param relayCurrentSetId=$relayCurrentSetId \
        --param paraChainId=0x0 \
        --param encodedValidators=$encodedValidators \
        --param evmEventIndex=$evmEventIndex \
        --param newAuthoritiesEventIndex=$newAuthoritiesEventIndex \
        --param candidateIncludedEventIndex=$candidateIncludedEventIndex \
        | jq -r . > tx.icon.deploy_bmv

    extract_scoreAddress tx.icon.deploy_bmv bmv.icon
    ensure_file_exist $CONFIG_DIR bmv.icon
}

deploy_javascore_IRC2Token() {
    echo "deploy_javascore_IRC2Token"
    cd $CONFIG_DIR

    goloop rpc sendtx deploy $JAVASCORE_DIST_DIR/irc2-0.1.0-optimized.jar \
    --content_type application/java \
    --param _name=${TOKEN_NAME} \
    --param _symbol=${TOKEN_SYM} \
    --param _initialSupply=${TOKEN_SUPPLY} \
    --param _decimals=${TOKEN_DECIMALS} | jq -r . > tx.icon.deploy_irc2token

    extract_scoreAddress tx.icon.deploy_irc2token irc2token.icon
    ensure_file_exist $CONFIG_DIR irc2token.icon
}

deploy_javascore_NativeCoinIRC2BSH() {
    echo "deploy_javascore_NativeCoinIRC2BSH"

    cd $CONFIG_DIR

    goloop rpc sendtx deploy $JAVASCORE_DIST_DIR/nativecoinIRC2-0.1.0-optimized.jar \
        --content_type application/java \
        --param _bmc=$(cat bmc.icon) \
        --param _irc2=$(cat irc2token.icon) \
        --param _name=ICX \
        --param _tokenName=${TOKEN_NAME} | jq -r . > tx.icon.deploy_nativeCoinIRC2Bsh

    extract_scoreAddress tx.icon.deploy_nativeCoinIRC2Bsh nativeCoinIRC2Bsh.icon
    ensure_file_exist $CONFIG_DIR nativeCoinIRC2Bsh.icon
}

deploy_javascore_IRC2Token() {
    echo "deploy_javascore_IRC2Token"
    cd $CONFIG_DIR

    goloop rpc sendtx deploy $JAVASCORE_DIST_DIR/irc2-0.1.0-optimized.jar \
    --content_type application/java \
    --param _name=${TOKEN_NAME} \
    --param _symbol=${TOKEN_SYM} \
    --param _initialSupply=${TOKEN_SUPPLY} \
    --param _decimals=${TOKEN_DECIMALS} | jq -r . > tx.icon.deploy_irc2token

    extract_scoreAddress tx.icon.deploy_irc2token irc2token.icon
    ensure_file_exist $CONFIG_DIR irc2token.icon
}

deploy_javascore_NativeCoinIRC2BSH() {
    echo "deploy_javascore_NativeCoinIRC2BSH"

    cd $CONFIG_DIR

    goloop rpc sendtx deploy $JAVASCORE_DIST_DIR/nativecoinIRC2-0.1.0-optimized.jar \
        --content_type application/java \
        --param _bmc=$(cat bmc.icon) \
        --param _irc2=$(cat irc2token.icon) \
        --param _name=ICX \
        --param _tokenName=${TOKEN_NAME} | jq -r . > tx.icon.deploy_nativeCoinIRC2Bsh

    extract_scoreAddress tx.icon.deploy_nativeCoinIRC2Bsh nativeCoinIRC2Bsh.icon
    ensure_file_exist $CONFIG_DIR nativeCoinIRC2Bsh.icon
}

deploy_javascore_NativeCoinBSH() {
    echo "deploy_javascore_NativeCoinBSH"

    cd $CONFIG_DIR

    IRC2_SERIALIZED_SCORE=$(xxd -p $JAVASCORE_DIST_DIR/irc2Tradeable-0.1.0-optimized.jar | tr -d '\n')
    goloop rpc sendtx deploy $JAVASCORE_DIST_DIR/nativecoin-0.1.0-optimized.jar \
        --content_type application/java \
        --param _bmc=$(cat bmc.icon) \
        --param _name=ICX \
        --param _serializedIrc2=$IRC2_SERIALIZED_SCORE | jq -r . > tx.icon.deploy_nativeCoinBsh

    extract_scoreAddress tx.icon.deploy_nativeCoinBsh nativeCoinBsh.icon
    ensure_file_exist $CONFIG_DIR nativeCoinBsh.icon
}

deploy_javascore_FeeAggregation() {
    echo "deploy_javascore_FeeAggregation"

    cd $CONFIG_DIR

    goloop rpc sendtx deploy $JAVASCORE_DIST_DIR/fee-aggregation-system-1.0-optimized.jar \
        --param _cps_address=$ICON_RECEIVER_FEE_ADDRESS \
        --param _band_protocol_address=$ICON_BAND_PROTOCOL_ADDRESS \
        --content_type application/java | jq -r . > tx.icon.deploy_feeAggregation

    extract_scoreAddress tx.icon.deploy_feeAggregation feeAggregation.icon
    ensure_file_exist $CONFIG_DIR feeAggregation.icon
}

goloop_bmc_addVerifier() {
    echo "goloop_bmc_addVerifier"
    cd $CONFIG_DIR
    goloop rpc sendtx call --to $(cat bmc.icon) \
        --method addVerifier \
        --param _net=$(cat net.btp.moonbeam) \
        --param _addr=$(cat bmv.icon) | jq -r . > tx.icon.addVerifier

    ensure_txresult tx.icon.addVerifier
}

goloop_bmc_addLink() {
    echo "goloop_bmc_addLink"
    cd $CONFIG_DIR

    goloop rpc sendtx call --to $(cat bmc.icon) \
        --method addLink \
        --param _link=$(cat btp.moonbeam) | jq -r . > tx.icon.addLink
    ensure_txresult tx.icon.addLink

    echo "goloop_bmc_setLinkRotateTerm"
    goloop rpc sendtx call --to $(cat bmc.icon) \
        --method setLinkRotateTerm \
        --param _link=$(cat btp.moonbeam) \
        --param _block_interval=0x1770 \
        --param _max_agg=0x08 \
        | jq -r . > tx.icon.setLinkRotateTerm
    ensure_txresult tx.icon.setLinkRotateTerm

    echo "goloop_bmc_setLinkDelayLimit"
    goloop rpc sendtx call --to $(cat bmc.icon) \
    --method setLinkDelayLimit \
    --param _link=$(cat btp.moonbeam) \
    --param _value=4 \
    | jq -r . > tx.icon.setLinkDelayLimit
    ensure_txresult tx.icon.setLinkDelayLimit

    echo "finished goloop_bmc_addLink"
}

goloop_bmc_addService() {
    echo "goloop_bmc_addService"
    cd $CONFIG_DIR

    goloop rpc sendtx call --to $(cat bmc.icon) \
        --method addService \
        --param _addr=$(cat nativeCoinBsh.icon) \
        --param _svc=nativecoin \
        | jq -r . > tx.icon.addService
    ensure_txresult tx.icon.addService
}

goloop_bmc_addServiceIRC2() {
    echo "goloop_bmc_addServiceIRC2"
    cd $CONFIG_DIR

    goloop rpc sendtx call --to $(cat bmc.icon) \
        --method addService \
        --param _addr=$(cat nativeCoinIRC2Bsh.icon) \
        --param _svc=$SVC_NAME \
        | jq -r . > tx.icon.addService
    ensure_txresult tx.icon.addService
}

goloop_bmc_addRelay() {
    echo "goloop_bmc_addRelay"
    cd $CONFIG_DIR

    goloop rpc sendtx call --to $(cat bmc.icon) \
        --method addRelay \
        --param _link=$(cat btp.moonbeam) \
        --param _addr=$(jq -r .address $GOLOOP_KEY_STORE) \
        | jq -r . > tx.icon.addRelay
    ensure_txresult tx.icon.addRelay
}

goloop_bmc_setFeeAggregator() {
    echo "goloop_bmc_setFeeAggregator"
    cd $CONFIG_DIR

    goloop rpc sendtx call --to $(cat bmc.icon) \
        --method setFeeAggregator \
        --param _addr=$(cat feeAggregation.icon) \
        | jq -r . > tx.icon.setFeeAggregator
    ensure_txresult tx.icon.setFeeAggregator
}

goloop_bsh_config_native_coin() {
    echo "goloop_bsh_config_native_coin"
    cd $CONFIG_DIR

    goloop rpc sendtx call --to $(cat nativeCoinBsh.icon) \
        --method register \
        --param _name=DEV \
        --param _symbol=DEV \
        --param _decimals=18 | jq -r . > tx.icon.registerCoin
    ensure_txresult tx.icon.registerCoin

    goloop rpc call --to $(cat nativeCoinBsh.icon) \
        --method coinAddress --param _coinName=DEV | sed -e 's/^"//' -e 's/"$//' > irc2TradeableToken.icon
    ensure_file_exist $CONFIG_DIR irc2TradeableToken.icon

    echo "goloop_bsh_setFeeRatio"
    goloop rpc sendtx call --to $(cat nativeCoinBsh.icon) \
        --method setFeeRatio \
        --param _feeNumerator=100 \
        | jq -r . > tx.icon.setFeeRatio
    ensure_txresult tx.icon.setFeeRatio
}

goloop_bsh_config_native_coin_IRC2() {
    echo "goloop_bsh_config_native_coin_IRC2"
    cd $CONFIG_DIR

    echo "goloop_bsh_setFeeRatio"
    goloop rpc sendtx call --to $(cat nativeCoinIRC2Bsh.icon) \
        --method setFeeRatio \
        --param _feeNumerator=100 \
        | jq -r . > tx.icon.setFeeRatio
    ensure_txresult tx.icon.setFeeRatio
}
#!/bin/sh
source deploy_util.sh

deploy_solidity_bmc() {
    echo "deploying solidity bmc"
    echo "$(printf "0x%01x" $MOONBEAM_CHAIN_ID).pra" > $CONFIG_DIR/net.btp.moonbeam

    cd $SOLIDITY_DIST_DIR/bmc
    sed -i 's/"http:\/\/localhost:9933"/process.env.MOONBEAM_RPC_URL/' truffle-config.js
    rm -rf .openzeppelin build node_modules
    yarn
    truffle compile --all

    BMC_PRA_NET=$(cat $CONFIG_DIR/net.btp.moonbeam) \
    truffle migrate --network moonbeamlocal

    wait_file_created $CONFIG_DIR bmc.moonbeam
    echo "btp://$(cat $CONFIG_DIR/net.btp.moonbeam)/$(cat $CONFIG_DIR/bmc.moonbeam)" > $CONFIG_DIR/btp.moonbeam
}

deploy_solidity_bsh() {
    echo "deploying solidity bsh"

    cd $SOLIDITY_DIST_DIR/bsh
    sed -i 's/"http:\/\/localhost:9933"/process.env.MOONBEAM_RPC_URL/' truffle-config.js
    rm -rf .openzeppelin build node_modules
    yarn
    truffle compile --all


    BSH_COIN_URL=https://moonbeam.network \
    BSH_COIN_NAME=DEV \
    BSH_COIN_FEE=100 \
    BMC_PERIPHERY_ADDRESS=$(cat $CONFIG_DIR/bmc.moonbeam) \
    BSH_SERVICE=CoinTransfer \
    truffle migrate --network moonbeamlocal

    wait_file_created $CONFIG_DIR bsh.moonbeam
}

deploy_solidity_bmv() {
    echo "deploying solidity bmv"

    cd $SOLIDITY_DIST_DIR/bmv
    sed -i 's/"http:\/\/localhost:9933"/process.env.MOONBEAM_RPC_URL/' truffle-config.js
    rm -rf .openzeppelin build node_modules
    yarn
    truffle compile --all

    LAST_BOCK=$(latest_block_goloop)
    LAST_HEIGHT=$(echo $LAST_BOCK | jq -r .height)
    LAST_HASH=0x$(echo $LAST_BOCK | jq -r .block_hash)
    echo "goloop height:$LAST_HEIGHT hash:$LAST_HASH"
    

    BMC_CONTRACT_ADDRESS=$(cat $CONFIG_DIR/bmc.moonbeam) \
    BMV_ICON_NET=$(cat $CONFIG_DIR/net.btp.icon) \
    BMV_ICON_ENCODED_VALIDATORS=0xd69500b6b5791be0b5ef67063b3c10b840fb81514db2fd \
    BMV_ICON_INIT_OFFSET=$LAST_HEIGHT \
    BMV_ICON_INIT_ROOTSSIZE=8 \
    BMV_ICON_INIT_CACHESIZE=8 \
    BMV_ICON_LASTBLOCK_HASH=$LAST_HASH \
    truffle migrate --network moonbeamlocal

    echo $LAST_HEIGHT > $CONFIG_DIR/icon.offset
    wait_file_created $CONFIG_DIR bmv.moonbeam
}

moonbeam_bmc_addVerifier() {
    echo "moonbeam_bmc_addVerifier"


    cd $SOLIDITY_DIST_DIR/bmc
    cp $SCRIPT_DIR/mb_bmc_add_verifier.js .
    
    ICON_NET=$(cat $CONFIG_DIR/net.btp.icon) \
    BMV_MOONBEAM=$(cat $CONFIG_DIR/bmv.moonbeam) \
    truffle exec mb_bmc_add_verifier.js --network moonbeamlocal
}

moonbeam_bmc_addLink() {
    echo "moonbeam_bmc_addLink"

    cd $SOLIDITY_DIST_DIR/bmc
    cp $SCRIPT_DIR/mb_bmc_add_link.js .

    ICON_BTP_ADDRESS=$(cat $CONFIG_DIR/btp.icon) \
    truffle exec mb_bmc_add_link.js --network moonbeamlocal
}

moonbeam_bmc_addService() {
    echo "moonbeam_bmc_addService"

    cd $SOLIDITY_DIST_DIR/bmc
    cp $SCRIPT_DIR/mb_bmc_add_service.js .

    BSH_MOONBEAM=$(cat $CONFIG_DIR/bsh.moonbeam) \
    ICON_BTP_ADDRESS=$(cat $CONFIG_DIR/btp.icon) \
    RELAY_ADDRESS=0x$(cat $CONFIG_DIR/moonbeam.keystore.json | jq -r .address) \
    truffle exec mb_bmc_add_service.js --network moonbeamlocal
}

moonbeam_bsh_registerCoin() {
    echo "moonbeam_bsh_registerCoin"
    cd $SOLIDITY_DIST_DIR/bsh
    cp $SCRIPT_DIR/mb_bsh_register_coin.js .
    truffle exec mb_bsh_register_coin.js --network moonbeamlocal
}
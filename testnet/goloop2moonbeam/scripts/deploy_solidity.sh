#!/bin/sh
source deploy_util.sh

deploy_solidity_bmc() {
    echo "deploying solidity bmc"

    cd $CONFIG_DIR
    echo "$(printf "0x%01x" $MOONBEAM_CHAIN_ID).pra" > net.btp.moonbeam
    export BMC_PRA_NET=$(cat net.btp.moonbeam)

    cd $SOLIDITY_DIST_DIR/bmc
    sed -i 's/"http:\/\/localhost:9933"/process.env.MOONBEAM_RPC_URL/' truffle-config.js
    truffle migrate --network moonbeamlocal
    wait_file_created $CONFIG_DIR bmc.moonbeam
}

deploy_solidity_bsh() {
    echo "deploying solidity bsh"

    cd $SOLIDITY_DIST_DIR/bsh
    sed -i 's/"http:\/\/localhost:9933"/process.env.MOONBEAM_RPC_URL/' truffle-config.js

    export BSH_COIN_URL=https://moonbeam.network
    export BSH_COIN_NAME=DEV
    export BSH_COIN_FEE=100
    export BMC_PERIPHERY_ADDRESS=$(cat $CONFIG_DIR/bmc.moonbeam)
    export BSH_SERVICE=CoinTransfer

    truffle migrate --network moonbeamlocal
    wait_file_created $CONFIG_DIR bsh.moonbeam
}

deploy_solidity_bmv() {
    echo "deploying solidity bmv"

    cd $SOLIDITY_DIST_DIR/bmv
    sed -i 's/"http:\/\/localhost:9933"/process.env.MOONBEAM_RPC_URL/' truffle-config.js

    LAST_BOCK=$(latest_block_goloop)
    LAST_HEIGHT=$(echo $LAST_BOCK | jq -r .height)
    LAST_HASH=0x$(echo $LAST_BOCK | jq -r .block_hash)
    echo "goloop height:$LAST_HEIGHT hash:$LAST_HASH"

    export BMC_CONTRACT_ADDRESS=$(cat $CONFIG_DIR/bmc.moonbeam)
    export BMV_ICON_NET=$(cat $CONFIG_DIR/net.btp.icon)
    export BMV_ICON_ENCODED_VALIDATORS=0xd69500b6b5791be0b5ef67063b3c10b840fb81514db2fd
    export BMV_ICON_INIT_OFFSET=$LAST_HEIGHT
    export BMV_ICON_INIT_ROOTSSIZE=8
    export BMV_ICON_INIT_CACHESIZE=8
    export BMV_ICON_LASTBLOCK_HASH=$LAST_HASH

    truffle migrate --network moonbeamlocal
    echo $LAST_HEIGHT > $CONFIG_DIR/icon.offset
    wait_file_created $CONFIG_DIR bmv.moonbeam
}

moonbeam_bmc_addVerifier() {
    export ICON_NET=$(cat $CONFIG_DIR/net.btp.icon)
    export BMV_MOONBEAM=$(cat $CONFIG_DIR/bmv.moonbeam)

    cp $SCRIPT_DIR/mb_bmc_add_relayer.js $SOLIDITY_DIST_DIR/bmc/
    cd $SOLIDITY_DIST_DIR/bmc
    truffle exec mb_bmc_add_relayer.js --network moonbeamlocal
}

moonbeam_bmc_addLink() {
    echo "moonbeam_bmc_addLink"
}

moonbeam_bmc_addService() {
    echo "moonbeam_bmc_addService"
}

moonbeam_bsh_registerCoin() {
    echo "moonbeam_bsh_registerCoin"
}
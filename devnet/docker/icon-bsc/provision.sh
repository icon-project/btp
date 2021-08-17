#!/bin/sh
set -e

# Parts of this code is adapted from https://github.com/icon-project/btp/blob/goloop2moonbeam/testnet/goloop2moonbeam/scripts

export CONFIG_DIR=${CONFIG_DIR:-${BTPSIMPLE_CONFIG_DIR}}
export CONTRACTS_DIR=${CONTRACTS_DIR:-${BTPSIMPLE_CONTRACTS_DIR}}
export SCRIPTS_DIR=${SCRIPTS_DIR:-${BTPSIMPLE_SCRIPTS_DIR}}

export BSC_NID="0x97"
export BSC_BMC_NET="0x97.bsc"
export BSC_RPC_URI=http://goloop:9080/api/v3/icon

# configure env in the container
export GOLOOP_RPC_URI=http://goloop:9080/api/v3/icon
export GOLOOP_CONFIG=$CONFIG_DIR/goloop.server.json
export GOLOOP_KEY_STORE=$CONFIG_DIR/goloop.keystore.json
export GOLOOP_KEY_SECRET=$CONFIG_DIR/goloop.keysecret
export GOLOOP_RPC_KEY_STORE=$CONFIG_DIR/goloop.keystore.json
export GOLOOP_RPC_KEY_SECRET=$CONFIG_DIR/goloop.keysecret
export GOLOOP_RPC_STEP_LIMIT=${GOLOOP_RPC_STEP_LIMIT:-5000000000}
export GOLOOP_CHAINSCORE=cx000000000000000000000000000000000000000

export GOLOOP_RPC_NID=${GOLOOP_RPC_NID:-$(cat $CONFIG_DIR/nid.icon)}

BSC_KEY_SECRET="Perlia0"

source rpc.sh

deploy_solidity_bmc() {
  echo "deploying solidity bmc"
  cd $CONTRACTS_DIR/solidity/bmc
  rm -rf contracts/test build .openzeppelin

  BMC_PRA_NET=$BSC_BMC_NET \
  truffle migrate --network bscDocker --compile-all

  BMC_ADDRESS=$(jq -r '.networks[] | .address' build/contracts/BMCPeriphery.json)
  echo btp://0x97.bsc/"${BMC_ADDRESS}" > /btpsimple/config/btp.bsc

  jq -r '.networks[] | .address' build/contracts/BMCManagement.json > $CONFIG_DIR/bmc.bsc

  wait_for_file $CONFIG_DIR/bmc.bsc
}

deploy_solidity_bmv() {
   echo "deploying solidity bmv"
   cd $CONTRACTS_DIR/solidity/bmv
   rm -rf contracts/test build .openzeppelin

   LAST_BOCK=$(goloop_lastblock)
   LAST_HEIGHT=$(echo $LAST_BOCK | jq -r .height)
   LAST_HASH=0x$(echo $LAST_BOCK | jq -r .block_hash)
   echo "goloop height:$LAST_HEIGHT hash:$LAST_HASH"
   echo $LAST_HEIGHT > $CONFIG_DIR/offset.icon

   BMC_CONTRACT_ADDRESS=$(cat $CONFIG_DIR/bmc.bsc) \
   BMV_ICON_NET=$(cat $CONFIG_DIR/net.btp.icon) \
   BMV_ICON_ENCODED_VALIDATORS=0xd69500b6b5791be0b5ef67063b3c10b840fb81514db2fd \
   BMV_ICON_INIT_OFFSET=$LAST_HEIGHT \
   BMV_ICON_INIT_ROOTSSIZE=8 \
   BMV_ICON_INIT_CACHESIZE=8 \
   BMV_ICON_LASTBLOCK_HASH=$LAST_HASH \
   truffle migrate --compile-all --network bscDocker

   jq -r '.networks[] | .address' build/contracts/BMV.json > $CONFIG_DIR/bmv.bsc

   wait_for_file $CONFIG_DIR/bmv.bsc
}

deploy_javascore_bmc() {
  echo "deploying javascore bmc"
  cd $CONFIG_DIR
  goloop rpc sendtx deploy $CONTRACTS_DIR/javascore/bmc-optimized.jar \
    --content_type application/java \
    --param _net=$(cat net.btp.icon) | jq -r . > tx.bmc.icon
  extract_scoreAddress tx.bmc.icon bmc.icon
  echo "btp://$(cat net.btp.icon)/$(cat bmc.icon)" > btp.icon
}

add_icon_verifier() {
  echo "adding icon verifier $(cat $CONFIG_DIR/bmv.bsc)"
  cd $CONTRACTS_DIR/solidity/bmc
  truffle exec --network bscDocker "$SCRIPTS_DIR"/bmc.js \
  --method addVerifier --net $(cat $CONFIG_DIR/net.btp.icon) --addr $(cat $CONFIG_DIR/bmv.bsc)
}

add_icon_link() {
  echo "adding icon link $(cat $CONFIG_DIR/btp.icon)"
  cd $CONTRACTS_DIR/solidity/bmc
  truffle exec --network bscDocker "$SCRIPTS_DIR"/bmc.js \
  --method addLink --link $(cat $CONFIG_DIR/btp.icon) --blockInterval 2000 --maxAggregation 1 --delayLimit 3
}

add_icon_relay() {
  echo "adding icon link $(cat $CONFIG_DIR/bmv.bsc)"
  cd $CONTRACTS_DIR/solidity/bmc
  truffle exec --network bscDocker "$SCRIPTS_DIR"/bmc.js \
  --method addRelay --link $(cat $CONFIG_DIR/btp.icon) --addr 0xAaFc8EeaEE8d9C8bD3262CCE3D73E56DeE3FB776
}

eth_blocknumber() {
  curl -s -X POST 'http://binancesmartchain:8545' --header 'Content-Type: application/json' \
    --data-raw '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[], "id": 1}' | jq -r .result | xargs printf "%d\n"
}

goloop_lastblock() {
  goloop rpc lastblock
}

provision() {
 if [ ! -f /btpsimple/provision ]; then
   echo "start provisioning..."

   cp /bsc.ks.json "$BTPSIMPLE_CONFIG_DIR"/bsc.ks.json
   # shellcheck disable=SC2059
   printf $BSC_KEY_SECRET > "$BTPSIMPLE_CONFIG_DIR"/bsc.secret
   echo "$GOLOOP_RPC_NID.icon" > net.btp.icon

   deploy_solidity_bmc
   eth_blocknumber > /btpsimple/config/offset.bsc
   deploy_solidity_bmv
   deploy_javascore_bmc
   add_icon_verifier
   add_icon_link
   add_icon_relay

   touch /btpsimple/provision
   echo "provision is now complete"
 fi
}

wait_for_file() {
    FILE_NAME=$1
    timeout=10
    while [ ! -f "$FILE_NAME" ];
    do
        if [ "$timeout" == 0 ]; then
            echo "ERROR: Timeout while waiting for the file $FILE_NAME."
            exit 1
        fi
        sleep 1
        timeout=$(expr $timeout - 1)

        echo "waiting for the output file: $FILE_NAME"
    done
}
# run provisioning
provision
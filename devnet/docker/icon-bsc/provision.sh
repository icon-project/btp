#!/bin/sh
set -e

# Parts of this code is adapted from https://github.com/icon-project/btp/blob/goloop2moonbeam/testnet/goloop2moonbeam/scripts

export CONFIG_DIR=${CONFIG_DIR:-${BTPSIMPLE_CONFIG_DIR}}
export CONTRACTS_DIR=${CONTRACTS_DIR:-${BTPSIMPLE_CONTRACTS_DIR}}
export SCRIPTS_DIR=${SCRIPTS_DIR:-${BTPSIMPLE_SCRIPTS_DIR}}

export BSC_NID="0x97"
export BSC_BMC_NET="0x97.bsc"

# configure env in the container
export GOLOOP_RPC_URI=http://goloop:9080/api/v3/icon
export GOLOOP_RPC_ADMIN_URI=http://goloop:9080/admin/system
export GOLOOP_CONFIG=$CONFIG_DIR/goloop.server.json
export GOLOOP_KEY_STORE=$CONFIG_DIR/goloop.keystore.json
export GOLOOP_KEY_SECRET=$CONFIG_DIR/goloop.keysecret
export GOLOOP_RPC_KEY_STORE=$CONFIG_DIR/goloop.keystore.json
export GOLOOP_RPC_KEY_SECRET=$CONFIG_DIR/goloop.keysecret
export GOLOOP_RPC_STEP_LIMIT=${GOLOOP_RPC_STEP_LIMIT:-5000000000}
export GOLOOP_CHAINSCORE=cx000000000000000000000000000000000000000

export GOLOOP_RPC_NID=${GOLOOP_RPC_NID:-$(cat $CONFIG_DIR/nid.icon)}

export TOKEN_NAME=ETH
export TOKEN_SYM=ETH
export TOKEN_SUPPLY=0x3e8
export TOKEN_DECIMALS=0x12
export SVC_NAME=TokenBSH

BSC_KEY_SECRET="Perlia0"

source rpc.sh

deploy_solidity_bmc() {
  echo "deploying solidity bmc"
  cd $CONTRACTS_DIR/solidity/bmc
  rm -rf contracts/test build .openzeppelin
  npm install --legacy-peer-deps
  BMC_PRA_NET=$BSC_BMC_NET \
  truffle migrate --network bscDocker --compile-all

  BMC_ADDRESS=$(jq -r '.networks[] | .address' build/contracts/BMCPeriphery.json)
  echo btp://0x97.bsc/"${BMC_ADDRESS}" > /btpsimple/config/btp.bsc
  echo "${BMC_ADDRESS}" > $CONFIG_DIR/bmc.periphery.bsc
  wait_for_file $CONFIG_DIR/bmc.periphery.bsc

  jq -r '.networks[] | .address' build/contracts/BMCManagement.json > $CONFIG_DIR/bmc.bsc
  wait_for_file $CONFIG_DIR/bmc.bsc
}

deploy_solidity_bmv() {
  echo "deploying solidity bmv"
  cd $CONTRACTS_DIR/solidity/bmv
  rm -rf contracts/test build .openzeppelin
  npm install --legacy-peer-deps

  BMC_CONTRACT_ADDRESS=$(cat $CONFIG_DIR/bmc.periphery.bsc) \
  BMV_ICON_NET=$(cat $CONFIG_DIR/net.btp.icon) \
  BMV_ICON_ENCODED_VALIDATORS=0xd69500275c118617610e65ba572ac0a621ddd13255242b \
  BMV_ICON_INIT_OFFSET=$(cat $CONFIG_DIR/offset.icon) \
  BMV_ICON_INIT_ROOTSSIZE=8 \
  BMV_ICON_INIT_CACHESIZE=8 \
  BMV_ICON_LASTBLOCK_HASH=$(cat $CONFIG_DIR/last.hash.icon) \
  truffle migrate --compile-all --network bscDocker

  jq -r '.networks[] | .address' build/contracts/BMV.json > $CONFIG_DIR/bmv.bsc

  wait_for_file $CONFIG_DIR/bmv.bsc
}

deploy_solidity_tokenBSH_BEP20() {
  echo "deploying solidity Token BSH"
  cd $CONTRACTS_DIR/solidity/TokenBSH
  rm -rf contracts/test build .openzeppelin
  npm install --legacy-peer-deps
  SVC_NAME=TokenBSH

  BSH_TOKEN_FEE=1 \
  BMC_PERIPHERY_ADDRESS=$BMC_ADDRESS \
  BSH_SERVICE=$SVC_NAME \
  truffle migrate --compile-all --network bscDocker

  BSH_IMPL_ADDRESS=$(jq -r '.networks[] | .address' build/contracts/BSHImpl.json)
  jq -r '.networks[] | .address' build/contracts/BSHImpl.json > $CONFIG_DIR/token_bsh.impl.bsc
  jq -r '.networks[] | .address' build/contracts/BSHProxy.json > $CONFIG_DIR/token_bsh.proxy.bsc

  wait_for_file $CONFIG_DIR/token_bsh.impl.bsc
  wait_for_file $CONFIG_DIR/token_bsh.proxy.bsc

  jq -r '.networks[] | .address' build/contracts/BEP20TKN.json > $CONFIG_DIR/bep20_token.bsc
  wait_for_file $CONFIG_DIR/bep20_token.bsc
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
  --method addLink --link $(cat $CONFIG_DIR/btp.icon) --blockInterval 3000 --maxAggregation 2 --delayLimit 3
}

add_icon_relay() {
  echo "adding icon link $(cat $CONFIG_DIR/bmv.bsc)"
  BSC_RELAY_USER=$(cat  $CONFIG_DIR/bsc.ks.json | jq -r .address)
  cd $CONTRACTS_DIR/solidity/bmc
  truffle exec --network bscDocker "$SCRIPTS_DIR"/bmc.js \
  --method addRelay --link $(cat $CONFIG_DIR/btp.icon) --addr "0x${BSC_RELAY_USER}"
}

bsc_addService() {
  echo "adding ${SVC_NAME} service into BMC"
  cd $CONTRACTS_DIR/solidity/bmc
  BSH_IMPL_ADDRESS=$(cat $CONFIG_DIR/token_bsh.impl.bsc)
  truffle exec --network bscDocker "$SCRIPTS_DIR"/bmc.js \
  --method addService --name $SVC_NAME --addr "$BSH_IMPL_ADDRESS"
}

bsc_registerToken() {
  echo "Registering ${TOKEN_NAME} into tokenBSH"
  cd $CONTRACTS_DIR/solidity/TokenBSH
  BEP20_TKN_ADDRESS=$(cat $CONFIG_DIR/bep20_token.bsc)
  truffle exec --network bscDocker "$SCRIPTS_DIR"/bsh.js \
  --method registerToken --name $TOKEN_NAME --symbol $TOKEN_SYM --addr "$BEP20_TKN_ADDRESS"
}

bsc_updateRxSeq() {
  cd $CONTRACTS_DIR/solidity/bmc
  truffle exec --network bscDocker "$SCRIPTS_DIR"/bmc.js \
  --method updateRxSeq --link $(cat $CONFIG_DIR/btp.icon) --value 1
}

token_fundBSH() {
  echo "Funding BSH"
  cd $CONTRACTS_DIR/solidity/TokenBSH
  truffle exec --network bscDocker "$SCRIPTS_DIR"/bsh.js \
  --method fundBSH --addr $(cat $CONFIG_DIR/token_bsh.proxy.bsc) --amount 100
}

getBalance() {
  cd $CONTRACTS_DIR/solidity/TokenBSH
  BSC_USER=0x$(cat $CONFIG_DIR/bsc.ks.json | jq -r .address)
  truffle exec --network bscDocker "$SCRIPTS_DIR"/bsh.js \
  --method getBalance --addr $BSC_USER
}

setMTASolidity(){
  echo "Setting latest block and last hash"
  cd $CONTRACTS_DIR/solidity/bmv 

  LAST_BOCK=$(goloop_lastblock)
  LAST_HEIGHT=$(echo $LAST_BOCK | jq -r .height)
  LAST_HASH=0x$(echo $LAST_BOCK | jq -r .block_hash)
  echo "goloop height:$LAST_HEIGHT hash:$LAST_HASH"
  echo $LAST_HEIGHT > $CONFIG_DIR/offset.icon
  echo $LAST_HASH > $CONFIG_DIR/last.hash.icon
  
  truffle exec --network bscDocker "$SCRIPTS_DIR"/bmv.js \
  --method setMTA --offset "$(cat $CONFIG_DIR/offset.icon)" --lasthash "$(cat $CONFIG_DIR/last.hash.icon)"
}

######################################## javascore service methods - start ######################################

deploy_javascore_bmc() {
  echo "deploying javascore BMC"
  cd $CONFIG_DIR
  goloop rpc sendtx deploy $CONTRACTS_DIR/javascore/bmc-optimized.jar \
  --content_type application/java \
  --param _net=$(cat net.btp.icon) | jq -r . > tx.bmc.icon
  extract_scoreAddress tx.bmc.icon bmc.icon
  echo "btp://$(cat net.btp.icon)/$(cat bmc.icon)" > btp.icon
}

deploy_javascore_bmv() {
  echo "deploying javascore BMV"
  eth_blocknumber >/btpsimple/config/offset.bsc
  cd $CONFIG_DIR
  goloop rpc sendtx deploy $CONTRACTS_DIR/javascore/bmv-optimized.jar \
  --content_type application/java \
  --param network=$(cat net.btp.icon) \
  --param bmc=$(cat bmc.icon) \
  --param offset=$(cat offset.bsc) \
  --param rootSize=0x3 \
  --param cacheSize=0xA \
  --param isAllowNewerWitness=0x1 | \
  jq -r . > tx.bmv.icon
  extract_scoreAddress tx.bmv.icon bmv.icon
  echo "BMV deployment success"
}

deploy_javascore_bsh() {
  echo "deploying javascore Token BSH"
  cd $CONFIG_DIR
  goloop rpc sendtx deploy $CONTRACTS_DIR/javascore/bsh-optimized.jar \
  --content_type application/java \
  --param _bmc=$(cat bmc.icon) | jq -r . > tx.token_bsh.icon
  extract_scoreAddress tx.token_bsh.icon token_bsh.icon
}

deploy_javascore_irc2() {
  echo "deploying javascore IRC2Token"
  cd $CONFIG_DIR
  goloop rpc sendtx deploy $CONTRACTS_DIR/javascore/irc2-token-optimized.jar \
  --content_type application/java \
  --param _name=${TOKEN_NAME} \
  --param _symbol=${TOKEN_SYM} \
  --param _initialSupply=${TOKEN_SUPPLY} \
  --param _decimals=${TOKEN_DECIMALS} | jq -r . > tx.irc2_token.icon
  extract_scoreAddress tx.irc2_token.icon irc2_token.icon
}

bmc_javascore_addVerifier() {
  echo "adding verifier"
  cd $CONFIG_DIR
  goloop rpc sendtx call --to $(cat bmc.icon) \
  --method addVerifier \
  --param _net=$BSC_BMC_NET \
  --param _addr=$(cat bmv.icon) | jq -r . > tx.addverifier.icon
  ensure_txresult tx.addverifier.icon
  echo "Added verifier for $(cat bmv.icon)"
}

bmc_javascore_addLink() {
  echo "Adding bsc link"
  cd $CONFIG_DIR
  LAST_BOCK=$(goloop_lastblock)
  LAST_HEIGHT=$(echo $LAST_BOCK | jq -r .height)
  LAST_HASH=0x$(echo $LAST_BOCK | jq -r .block_hash)
  echo "goloop height:$LAST_HEIGHT hash:$LAST_HASH"
  echo $LAST_HEIGHT > $CONFIG_DIR/offset.icon
  echo $LAST_HASH > $CONFIG_DIR/last.hash.icon
  
  goloop rpc sendtx call --to $(cat bmc.icon) \
  --method addLink \
  --param _link=$(cat btp.bsc) | jq -r . > tx.addLink.icon
  ensure_txresult tx.addLink.icon
  echo "Added Link $(cat btp.bsc)"
}

bmc_javascore_addRelay() {
  echo "Adding bsc Relay"
  ICON_RELAY_USER=$(cat  $CONFIG_DIR/goloop.keystore.json | jq -r .address)
  cd $CONFIG_DIR
  goloop rpc sendtx call --to $(cat bmc.icon) \
  --method addRelay \
  --param _link=$(cat btp.bsc) \
  --param _addr=${ICON_RELAY_USER} | jq -r . > tx.addLink.icon
  ensure_txresult tx.addLink.icon
  echo "Added Link $(cat btp.bsc)"
}

bsh_javascore_register() {
  cd $CONFIG_DIR
  FEE_NUMERATOR=0x1
  goloop rpc sendtx call --to $(cat token_bsh.icon) \
  --method register \
  --param name=${TOKEN_NAME} \
  --param symbol=${TOKEN_SYM} \
  --param feeNumerator=${FEE_NUMERATOR} \
  --param decimals=${TOKEN_DECIMALS} \
  --param address=$(cat irc2_token.icon) | jq -r . > tx.register.icon
  ensure_txresult tx.register.icon
}

bmc_javascore_addService() {
  cd $CONFIG_DIR
  goloop rpc sendtx call --to $(cat bmc.icon) \
  --method addService \
  --param _svc=${SVC_NAME} \
  --param _addr=$(cat token_bsh.icon) | jq -r . > tx.addService.icon
  ensure_txresult tx.addService.icon
}

bmc_javascore_getServices() {
  cd $CONFIG_DIR
  goloop rpc call --to $(cat bmc.icon) \
  --method getServices
}

bsh_javascore_balance() {
  cd $CONFIG_DIR
  if [ $# -lt 1 ]; then
    echo "Usage: bsh_balance [EOA=$(rpceoa)]"
    return 1
  fi

  local EOA=$(rpceoa $1)
  echo "Balance of user $EOA"
  goloop rpc call --to $(cat token_bsh.icon) \
  --method getBalance \
  --param user=$EOA \
  --param tokenName=$TOKEN_NAME
}

bsh_javascore_transfer() {
  cd $CONFIG_DIR
  if [ $# -lt 2 ]; then
    echo "Usage: bsh_transfer [VAL=0x10] [EOA=$(rpceoa)]"
    return 1
  fi
  local VAL=${1:-0x10}
  local EOA=$2
  local FROM=$(rpceoa $GOLOOP_RPC_KEY_STORE)
  echo "Transfering $VAL to: $EOA from: $FROM "
  TX=$(
    goloop rpc sendtx call --to $(cat token_bsh.icon) \
    --method transfer \
    --param tokenName=${TOKEN_NAME} \
    --param value=$VAL \
    --param to=btp://$BSC_BMC_NET/$EOA | jq -r .
  )
  ensure_txresult $TX
}

irc2_javascore_balance() {
  cd $CONFIG_DIR
  if [ $# -lt 1 ]; then
    echo "Usage: irc2_balance [EOA=$(rpceoa)]"
    return 1
  fi
  local EOA=$(rpceoa $1)
  goloop rpc call --to $(cat irc2_token.icon) \
  --method balanceOf \
  --param _owner=$EOA
}

irc2_javascore_transfer() {
  cd $CONFIG_DIR
  if [ $# -lt 1 ]; then
    echo "Usage: irc2_transfer [VAL=0x10] [EOA=Address of Token-BSH]"
    return 1
  fi
  local VAL=${1:-0x10}
  local EOA=$(rpceoa ${2:-$(cat token_bsh.icon)})
  local FROM=$(rpceoa $GOLOOP_RPC_KEY_STORE)
  echo "Transfering $VAL to: $EOA from: $FROM "
  TX=$(
    goloop rpc sendtx call --to $(cat irc2_token.icon) \
    --method transfer \
    --param _to=$EOA \
    --param _value=$VAL | jq -r .
  )
  ensure_txresult $TX
}

rpceoa() {
  local EOA=${1:-${GOLOOP_RPC_KEY_STORE}}
  if [ "$EOA" != "" ] && [ -f "$EOA" ]; then
    echo $(cat $EOA | jq -r .address)
  else
    echo $EOA
  fi
}

########################################################### javascore service methods - END #####################################################################
eth_blocknumber() {
  curl -s -X POST 'http://binancesmartchain:8545' --header 'Content-Type: application/json' \
  --data-raw '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[], "id": 1}' | jq -r .result | xargs printf "%d\n"
}

goloop_lastblock() {
  goloop rpc lastblock
}

provision() {
  if [ ! -f $BTPSIMPLE_CONFIG_DIR/provision ]; then
    echo "start provisioning..."

    cp /bsc.ks.json "$BTPSIMPLE_CONFIG_DIR"/bsc.ks.json
    # shellcheck disable=SC2059
    printf $BSC_KEY_SECRET >"$BTPSIMPLE_CONFIG_DIR"/bsc.secret
    echo "$GOLOOP_RPC_NID.icon" >net.btp.icon

    eth_blocknumber >/btpsimple/config/offset.bsc

    deploy_javascore_bmc    
    deploy_javascore_bmv
    deploy_javascore_bsh
    deploy_javascore_irc2


    deploy_solidity_bmc

    bmc_javascore_addVerifier
    bmc_javascore_addLink
    bmc_javascore_addRelay
    bmc_javascore_addService
    bsh_javascore_register

    deploy_solidity_bmv    
    deploy_solidity_tokenBSH_BEP20

    add_icon_verifier
    add_icon_link
    add_icon_relay
    bsc_addService
    bsc_registerToken

    token_fundBSH
    
    touch $BTPSIMPLE_CONFIG_DIR/provision
    echo "provision is now complete"
  fi
}

wait_for_file() {
  FILE_NAME=$1
  timeout=10
  while [ ! -f "$FILE_NAME" ]; do
    if [ "$timeout" == 0 ]; then
      echo "ERROR: Timeout while waiting for the file $FILE_NAME."
      exit 1
    fi
    sleep 1
    timeout=$(expr $timeout - 1)

    echo "waiting for the output file: $FILE_NAME"
  done
}
wait-for-it.sh $GOLOOP_RPC_ADMIN_URI
# run provisioning
provision

#!/bin/sh
source utils.sh
# Parts of this code is adapted from https://github.com/icon-project/btp/blob/goloop2moonbeam/testnet/goloop2moonbeam/scripts
deploy_solidity_bmc() {
  echo "deploying solidity bmc"
  cd $CONTRACTS_DIR/solidity/bmc
  rm -rf contracts/test build .openzeppelin
  #npm install --legacy-peer-deps
  yarn --prod

  BMC_PRA_NET=$BSC_BMC_NET \
    truffle migrate --network bscDocker --compile-all

  generate_metadata "BMC"
}

deploy_solidity_bmv() {
  echo "deploying solidity bmv"
  cd $CONTRACTS_DIR/solidity/bmv
  rm -rf contracts/test build .openzeppelin
  #npm install --legacy-peer-deps
  yarn --prod

  BMC_CONTRACT_ADDRESS=$(cat $CONFIG_DIR/bmc.periphery.bsc) \
  BMV_ICON_NET=$(cat $CONFIG_DIR/net.btp.icon) \
  BMV_ICON_ENCODED_VALIDATORS=0xd69500275c118617610e65ba572ac0a621ddd13255242b \
  BMV_ICON_INIT_OFFSET=$(cat $CONFIG_DIR/offset.icon) \
  BMV_ICON_INIT_ROOTSSIZE=8 \
  BMV_ICON_INIT_CACHESIZE=8 \
  BMV_ICON_LASTBLOCK_HASH=$(cat $CONFIG_DIR/last.hash.icon) \
    truffle migrate --compile-all --network bscDocker

  generate_metadata "BMV"
}

deploy_solidity_tokenBSH_BEP20() {
  echo "deploying solidity Token BSH"
  cd $CONTRACTS_DIR/solidity/TokenBSH
  rm -rf contracts/test build .openzeppelin
  #npm install --legacy-peer-deps
  yarn --prod
  SVC_NAME=TokenBSH

  BSH_TOKEN_FEE=1 \
    BMC_PERIPHERY_ADDRESS=$BMC_ADDRESS \
    BSH_SERVICE=$SVC_NAME \
    truffle migrate --compile-all --network bscDocker

  generate_metadata "TOKEN_BSH"
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
  BSC_RELAY_USER=$(cat $CONFIG_DIR/bsc.ks.json | jq -r .address)
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
  truffle exec --network bscDocker "$SCRIPTS_DIR"/bsh.token.js \
    --method registerToken --name $TOKEN_NAME --symbol $TOKEN_SYM --addr "$BEP20_TKN_ADDRESS"
}

bsc_updateRxSeq() {
  cd $CONTRACTS_DIR/solidity/bmc
  truffle exec --network bscDocker "$SCRIPTS_DIR"/bmc.js \
    --method updateRxSeq --link $(cat $CONFIG_DIR/btp.icon) --value 1
}

token_bsc_fundBSH() {
  echo "Funding solidity BSH"
  cd $CONTRACTS_DIR/solidity/TokenBSH
  truffle exec --network bscDocker "$SCRIPTS_DIR"/bsh.token.js \
    --method fundBSH --addr $(cat $CONFIG_DIR/token_bsh.proxy.bsc) --amount 99
}

token_approveTransfer() {
  cd $CONTRACTS_DIR/solidity/TokenBSH
  truffle exec --network bscDocker "$SCRIPTS_DIR"/bsh.token.js \
    --method approve --addr $(cat $CONFIG_DIR/token_bsh.proxy.bsc) --amount $1
}

bsc_init_btp_transfer() {
  ICON_NET=$(cat $CONFIG_DIR/net.btp.icon)
  ALICE_ADDRESS=$(get_alice_address)
  BTP_TO="btp://$ICON_NET/$ALICE_ADDRESS"
  cd $CONTRACTS_DIR/solidity/TokenBSH
  truffle exec --network bscDocker "$SCRIPTS_DIR"/bsh.token.js \
    --method transfer --to $BTP_TO --amount $1
}

calculateTransferFee() {
  cd $CONTRACTS_DIR/solidity/TokenBSH
  truffle exec --network bscDocker "$SCRIPTS_DIR"/bsh.token.js \
    --method calculateTransferFee --amount $1
}

get_Bob_Token_Balance() {
  cd $CONTRACTS_DIR/solidity/TokenBSH
  BSC_USER=0x$(cat $CONFIG_DIR/bsc.ks.json | jq -r .address)
  BOB_BALANCE=$(truffle exec --network bscDocker "$SCRIPTS_DIR"/bsh.token.js \
    --method getBalance --addr $BSC_USER)
}

get_Bob_Token_Balance_with_wait() {
  echo "Checking Bob's Balance after BTP transfer:"
  get_Bob_Token_Balance
  BOB_INITIAL_BAL=$BOB_BALANCE
  COUNTER=30
  while true; do
    printf "."
    if [ $COUNTER -le 0 ]; then
      printf "\nError: timed out while getting Bob's Balance: Balance unchanged\n"
      echo $BOB_CURRENT_BAL
      exit 1
    fi
    sleep 3
    COUNTER=$(expr $COUNTER - 3)
    get_Bob_Token_Balance
    BOB_CURRENT_BAL=$BOB_BALANCE
    if [ "$BOB_CURRENT_BAL" != "$BOB_INITIAL_BAL" ]; then
      printf "\nBTP Transfer Successfull! \n"
      break
    fi
  done
  echo "Bob's Balance after BTP transfer: $BOB_CURRENT_BAL ETH"
}

setMTASolidity() {
  echo "Setting latest block and last hash"
  cd $CONTRACTS_DIR/solidity/bmv

  LAST_BOCK=$(goloop_lastblock)
  LAST_HEIGHT=$(echo $LAST_BOCK | jq -r .height)
  LAST_HASH=0x$(echo $LAST_BOCK | jq -r .block_hash)
  echo "goloop height:$LAST_HEIGHT hash:$LAST_HASH"
  echo $LAST_HEIGHT >$CONFIG_DIR/offset.icon
  echo $LAST_HASH >$CONFIG_DIR/last.hash.icon

  truffle exec --network bscDocker "$SCRIPTS_DIR"/bmv.js \
    --method setMTA --offset "$(cat $CONFIG_DIR/offset.icon)" --lasthash "$(cat $CONFIG_DIR/last.hash.icon)"
}

generate_metadata() {
  option=$1
  case "$option" in

  BMC)
    echo "###################  Generating BMC Solidity metadata ###################"

    BMC_ADDRESS=$(jq -r '.networks[] | .address' build/contracts/BMCPeriphery.json)
    echo btp://0x97.bsc/"${BMC_ADDRESS}" >$CONFIG_DIR/btp.bsc
    echo "${BMC_ADDRESS}" >$CONFIG_DIR/bmc.periphery.bsc
    wait_for_file $CONFIG_DIR/bmc.periphery.bsc

    jq -r '.networks[] | .address' build/contracts/BMCManagement.json >$CONFIG_DIR/bmc.bsc
    wait_for_file $CONFIG_DIR/bmc.bsc

    create_contracts_address_json "solidity" "BMCPeriphery" $BMC_ADDRESS
    create_abi "BMCPeriphery"
    create_contracts_address_json "solidity" "BMCManagement" $(cat $CONFIG_DIR/bmc.bsc)
    create_abi "BMCManagement"
    echo "DONE."
    ;;

  BMV)
    echo "################### Generating BMV  Solidity metadata ###################"

    jq -r '.networks[] | .address' build/contracts/BMV.json >$CONFIG_DIR/bmv.bsc
    wait_for_file $CONFIG_DIR/bmv.bsc

    create_contracts_address_json "solidity" "BMV" $(cat $CONFIG_DIR/bmv.bsc)
    create_abi "BMV"
    create_abi "DataValidator"
    echo "DONE."
    ;;

  TOKEN_BSH)
    echo "################### Generating Token BSH & BEP20  Solidity metadata ###################"

    BSH_IMPL_ADDRESS=$(jq -r '.networks[] | .address' build/contracts/BSHImpl.json)
    jq -r '.networks[] | .address' build/contracts/BSHImpl.json >$CONFIG_DIR/token_bsh.impl.bsc
    jq -r '.networks[] | .address' build/contracts/BSHProxy.json >$CONFIG_DIR/token_bsh.proxy.bsc

    wait_for_file $CONFIG_DIR/token_bsh.impl.bsc
    wait_for_file $CONFIG_DIR/token_bsh.proxy.bsc

    jq -r '.networks[] | .address' build/contracts/BEP20TKN.json >$CONFIG_DIR/bep20_token.bsc
    wait_for_file $CONFIG_DIR/bep20_token.bsc

    create_contracts_address_json "solidity" "BSHImpl" $BSH_IMPL_ADDRESS
    create_abi "BSHProxy"
    create_contracts_address_json "solidity" "BSHProxy" $(cat $CONFIG_DIR/token_bsh.proxy.bsc)
    create_abi "BSHImpl"
    create_contracts_address_json "solidity" "BEP20TKN" $(cat $CONFIG_DIR/bep20_token.bsc)
    create_abi "BEP20TKN"
    echo "DONE."
    ;;

  *)
    echo "Invalid option for generating meta data"
    ;;
  esac
}

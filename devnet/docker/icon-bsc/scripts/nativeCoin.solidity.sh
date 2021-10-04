#!/bin/sh

source rpc.sh
source utils.sh
# Parts of this code is adapted from https://github.com/icon-project/btp/blob/goloop2moonbeam/testnet/goloop2moonbeam/scripts
deploy_solidity_nativeCoin_BSH() {
    echo "deploying solidity Token BSH"
    cd $CONTRACTS_DIR/solidity/bsh
    rm -rf contracts/test build .openzeppelin
    #npm install --legacy-peer-deps
    yarn --prod

    BSH_COIN_URL=https://ethereum.org/en/ \
        BSH_COIN_NAME=DEV \
        BSH_COIN_FEE=100 \
        BSH_FIXED_FEE=50000 \
        BMC_PERIPHERY_ADDRESS=$(cat $CONFIG_DIR/bmc.periphery.bsc) \
        BSH_SERVICE=nativecoin \
        truffle migrate --compile-all --network bscDocker

    BSH_PERIPHERY_ADDRESS=$(jq -r '.networks[] | .address' build/contracts/BSHPeriphery.json)
    jq -r '.networks[] | .address' build/contracts/BSHCore.json >$CONFIG_DIR/bsh.core.bsc
    jq -r '.networks[] | .address' build/contracts/BSHPeriphery.json >$CONFIG_DIR/bsh.periphery.bsc

    wait_for_file $CONFIG_DIR/bsh.core.bsc
    wait_for_file $CONFIG_DIR/bsh.periphery.bsc
}

bmc_solidity_addNativeService() {
    echo "adding ${SVC_NAME} service into BMC"
    cd $CONTRACTS_DIR/solidity/bmc
    truffle exec --network bscDocker "$SCRIPTS_DIR"/bmc.js \
        --method addService --name nativecoin --addr "$BSH_PERIPHERY_ADDRESS"
}

nativeBSH_solidity_register() {
    echo "Register Coin Name with NativeBSH"
    cd $CONTRACTS_DIR/solidity/bsh
    truffle exec --network bscDocker "$SCRIPTS_DIR"/bsh.nativeCoin.js \
        --method register --name "ICX"
}

get_bob_ICX_balance(){
    cd $CONTRACTS_DIR/solidity/bsh
    BOB_BALANCE=$(truffle exec --network bscDocker "$SCRIPTS_DIR"/bsh.nativeCoin.js \
        --method getBalanceOf  --addr $(get_bob_address) --name "ICX")
}

get_Bob_ICX_Balance_with_wait() {
  printf " Waiting for 30s to check Bob's Balance after BTP transfer before timeout \n"
  get_bob_ICX_balance
  BOB_INITIAL_BAL=$BOB_BALANCE
  COUNTER=30
  while true;
  do 
    printf "."
    if [ $COUNTER -le 0 ]; then
      printf "\n Error: timed out while getting Bob's Balance: Balance unchanged \n"
      echo "$BOB_CURRENT_BAL"
      exit 1
    fi
    sleep 3
    COUNTER=$(expr $COUNTER - 3)
    get_bob_ICX_balance
    BOB_CURRENT_BAL=$BOB_BALANCE
    if [ "$BOB_CURRENT_BAL" != "$BOB_INITIAL_BAL" ]; then
      printf "\n BTP Native Transfer Successfull! \n"
      break
    fi
  done
  echo "Bob's Balance after BTP transfer: $BOB_CURRENT_BAL"
}
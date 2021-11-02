#!/bin/sh

# Parts of this code is adapted from https://github.com/icon-project/btp/blob/goloop2moonbeam/testnet/goloop2moonbeam/scripts
source env.variables.sh
source rpc.sh
source /btpsimple/bin/keystore.sh
source utils.sh
ensure_key_store alice.ks.json alice.secret

deploy_javascore_irc31() {
  echo "deploying javascore IRC31"
  cd $CONFIG_DIR
  goloop rpc sendtx deploy $CONTRACTS_DIR/javascore/irc31-0.1.0-optimized.jar \
    --content_type application/java | jq -r . >tx.irc31.icon
  extract_scoreAddress tx.irc31.icon irc31.icon
  create_contracts_address_json "javascore" "IRC31" $(cat irc31.icon)
}

deploy_javascore_nativeCoin_BSH() {
  echo "deploying javascore Native coin BSH"
  cd $CONFIG_DIR
  goloop rpc sendtx deploy $CONTRACTS_DIR/javascore/nativecoin-0.1.0-optimized.jar \
    --content_type application/java \
    --param _bmc=$(cat bmc.icon) \
    --param _irc31=$(cat irc31.icon) \
    --param _name=ICX | jq -r . >tx.nativebsh.icon
  extract_scoreAddress tx.nativebsh.icon nativebsh.icon
  create_contracts_address_json "javascore" "NativeBSH" $(cat nativebsh.icon)
}

bmc_javascore_addNativeService() {
  echo "Adding NativeCoin service into BMC"
  cd $CONFIG_DIR
  goloop rpc sendtx call --to $(cat bmc.icon) \
    --method addService \
    --param _svc=nativecoin \
    --param _addr=$(cat nativebsh.icon) | jq -r . >tx/addService.native.icon
  ensure_txresult tx/addService.native.icon
}

nativeBSH_javascore_register() {
  echo "Register Coin Name with NativeBSH"
  cd $CONFIG_DIR
  goloop rpc sendtx call --to $(cat nativebsh.icon) \
    --method register \
    --param _name=BNB | jq -r . >tx/register.nativeCoin.icon
  ensure_txresult tx/register.nativeCoin.icon
}

nativeBSH_javascore_setFeeRatio() {
  echo "Setting Fee ratio for NativeCoin"
  cd $CONFIG_DIR
  goloop rpc sendtx call --to $(cat nativebsh.icon) \
    --method setFeeRatio \
    --param _feeNumerator=100 | jq -r . >tx/setFeeRatio.nativebsh.icon
  ensure_txresult tx/setFeeRatio.nativebsh.icon
}

irc31_javascore_addOwner() {
  echo "Adding Native BSH as IRC31 contract owner"
  cd $CONFIG_DIR
  goloop rpc sendtx call --to $(cat irc31.icon) \
    --method addOwner \
    --param _addr=$(cat nativebsh.icon) | jq -r . >tx/addOwner.irc31.icon
  ensure_txresult tx/addOwner.irc31.icon
}

deposit_ICX_for_Alice() {
  get_alice_balance
  echo "Depositing $(wei2coin $ICX_DEPOSIT_AMOUNT) ICX to Alice"
  cd ${CONFIG_DIR}
  goloop rpc sendtx transfer \
    --to $(get_alice_address) \
    --value $ICX_DEPOSIT_AMOUNT | jq -r . >tx/deposit.alice
  ensure_txresult tx/deposit.alice
}

transfer_ICX_from_Alice_to_Bob() {
  ICX_TRANSER_AMOUNT=$1
  echo "Transfer $(wei2coin $ICX_TRANSER_AMOUNT) ICX from Alice to Bob"
  cd ${CONFIG_DIR}
  LAST_BOCK=$(goloop_lastblock)
  LAST_HEIGHT=$(echo $LAST_BOCK | jq -r .height)
  LAST_HASH=0x$(echo $LAST_BOCK | jq -r .block_hash)
  echo "goloop height:$LAST_HEIGHT hash:$LAST_HASH"
  echo "$(get_bob_address)"
  echo "$BSC_BMC_NET,$ICX_TRANSER_AMOUNT "
  goloop rpc sendtx call \
    --to $(cat nativebsh.icon) --method transferNativeCoin \
    --param _to=btp://$BSC_BMC_NET/$(get_bob_address) --value $ICX_TRANSER_AMOUNT \
    --key_store alice.ks.json --key_secret alice.secret |
    jq -r . >tx/Alice2Bob.transfer
  ensure_txresult tx/Alice2Bob.transfer
}

get_alice_balance() {
  balance=$(goloop rpc balance $(get_alice_address) | jq -r)
  balance=$(hex2int $balance)
  balance=$(wei2coin $balance)
  echo "Alice's balance: $balance (ICX)"
}

get_alice_native_balance() {
  cd $CONFIG_DIR

  local EOA=$(rpceoa alice.ks.json)
  coinId=$(goloop rpc call --to $(cat nativebsh.icon) \
    --method coinId \
    --param _coinName=$1 | jq -r)
  #echo "Alice coin_id: $coinId"

  balance=$(goloop rpc call --to $(cat irc31.icon) \
    --method balanceOf \
    --param _owner=$EOA --param _id=$coinId | jq -r)

  balance=$(hex2int $balance)
  balance=$(wei2coin $balance)
  echo "$balance ($1)"
}

check_alice_native_balance_with_wait() {
  echo "Checking Alice's balance..."

  cd $CONFIG_DIR
  ALICE_INITIAL_BAL=$(get_alice_native_balance $1)
  COUNTER=60
  while true; do
    printf "."
    if [ $COUNTER -le 0 ]; then
      printf "\nError: timed out while getting Alice's Balance: Balance unchanged\n"
      echo $ALICE_CURR_BAL
      exit 1
    fi
    sleep 3
    COUNTER=$(expr $COUNTER - 3)
    ALICE_CURR_BAL=$(get_alice_native_balance $1)
    if [ "$ALICE_CURR_BAL" != "$ALICE_INITIAL_BAL" ]; then
      printf "\nBTP Transfer Successfull! \n"
      break
    fi
  done
  echo "Alice's Balance after BTP transfer: $ALICE_CURR_BAL"
}

goloop_lastblock() {
  goloop rpc lastblock
}

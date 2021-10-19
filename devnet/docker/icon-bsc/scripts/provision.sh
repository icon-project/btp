#!/bin/sh
set -e

# Parts of this code is adapted from https://github.com/icon-project/btp/blob/goloop2moonbeam/testnet/goloop2moonbeam/scripts
source env.variables.sh
BSC_KEY_SECRET="Perlia0"

source rpc.sh

eth_blocknumber() {
  curl -s -X POST $BSC_RPC_URI --header 'Content-Type: application/json' \
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
    mkdir -p $BTPSIMPLE_CONFIG_DIR/tx
    eth_blocknumber >/btpsimple/config/offset.bsc

    source token.javascore.sh
    source token.solidity.sh

    deploy_javascore_bmc
    deploy_javascore_bmv
    deploy_javascore_bsh
    deploy_javascore_irc2

    deploy_solidity_bmc

    bmc_javascore_addService
    bsh_javascore_register

    source nativeCoin.javascore.sh
    deploy_javascore_irc31
    deploy_javascore_nativeCoin_BSH
    bmc_javascore_addNativeService
    nativeBSH_javascore_register
    nativeBSH_javascore_setFeeRatio
    irc31_javascore_addOwner

    deploy_solidity_tokenBSH_BEP20
    bsc_addService
    bsc_registerToken

    source nativeCoin.solidity.sh
    deploy_solidity_nativeCoin_BSH
    bmc_solidity_addNativeService
    nativeBSH_solidity_register

    token_bsc_fundBSH
    token_icon_fundBSH

    bmc_javascore_addVerifier
    bmc_javascore_addLink
    bmc_javascore_addRelay

    deploy_solidity_bmv
    add_icon_verifier
    add_icon_link
    add_icon_relay

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

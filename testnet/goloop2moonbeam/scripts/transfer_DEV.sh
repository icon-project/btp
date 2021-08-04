#!/bin/sh
set -e

source transfer_util.sh

MOONBEAM_PREFUND_PK=39539ab1876910bbf3a223d84a29e28f1cb4e2e456503e7e91ed39b2e7223d68
MOONBEAM_GAS_LIMIT=6721975
DEV_DEPOSIT_AMOUNT=1000000000000000000
DEV_TRANSER_AMOUNT=74706176

deposit_DEV_for_bob() {
    echo "$1. Deposit $DEV_DEPOSIT_AMOUNT DEV for Bob"

    cd ${CONFIG_DIR}
    eth transaction:send \
                --network $MOONBEAM_RPC_URL \
                --pk $MOONBEAM_PREFUND_PK \
                --gas $MOONBEAM_GAS_LIMIT \
                --to $(get_bob_address) \
                --value $DEV_DEPOSIT_AMOUNT | jq -r > tx.deposit_dev

    eth transaction:get --network $MOONBEAM_RPC_URL $(cat tx.deposit_dev) | jq -r .receipt
    get_bob_balance
}

transfer_DEV_from_bob_to_alice() {
    echo "$1. Transfer $DEV_TRANSER_AMOUNT DEV from Bob to Alice"

    cd ${CONFIG_DIR}
    encoded_data=$(eth method:encode abi.bsh_core.json "transferNativeCoin('$(cat alice.btp.address)')")
    eth transaction:send \
                --network $MOONBEAM_RPC_URL \
                --pk $(get_bob_private_key) \
                --gas $MOONBEAM_GAS_LIMIT \
                --to $(cat bsh_core.moonbeam) \
                --data $encoded_data \
                --value $DEV_TRANSER_AMOUNT | jq -r > tx.transfer_dev
    eth transaction:get --network $MOONBEAM_RPC_URL $(cat tx.transfer_dev) | jq -r .receipt
    get_bob_balance
}

check_alice_balance_in_Goloop() {
    echo "$1. Checking Alice's balance after 10 seconds..."
    sleep 10

    cd $CONFIG_DIR
    coin_id=$(goloop rpc call \
        --to $(cat nativeCoinBsh.icon) \
        --method coinId --param _coinName=DEV | jq -r )
    echo "Alice coin_id: $coin_id"

    balance=$(goloop rpc call \
        --to $(cat irc31token.icon) \
        --method balanceOf \
        --param _owner=$(get_alice_address) \
        --param _id=$coin_id | jq -r )
    
    echo "Alice balance: $balance (DEV)"
}

echo "This script demonstrates how to transfer a NativeCoin from MOONBEAM to ICON."
create_bob_account_in_Moonbeam  "1"
deposit_DEV_for_bob             "2"
create_alice_account_in_Gochain "3"
transfer_DEV_from_bob_to_alice  "4"
check_alice_balance_in_Goloop   "5"
#!/bin/sh
set -e

source transfer_util.sh

MOONBEAM_PREFUND_PK=39539ab1876910bbf3a223d84a29e28f1cb4e2e456503e7e91ed39b2e7223d68
MOONBEAM_GAS_LIMIT=6721975


deposit_DEV_for_bob() {
    echo "$1. Deposit DEV for Bob"
    read -p 'Enter amount of DEV to be deposited: ' DEPOSIT_AMOUNT 
    if ! [[ "$DEPOSIT_AMOUNT" =~ ^[+-]?[0-9]+\.?[0-9]*$ ]]; then 
        echo "DEPOSIT_AMOUNT must be a numbers" 
        exit 0 
    fi


    cd ${CONFIG_DIR}
    eth transaction:send \
                --network $MOONBEAM_RPC_URL \
                --pk $MOONBEAM_PREFUND_PK \
                --gas $MOONBEAM_GAS_LIMIT \
                --to $(get_bob_address) \
                --value $DEPOSIT_AMOUNT | jq -r > tx.deposit_dev

    eth transaction:get --network $MOONBEAM_RPC_URL $(cat tx.deposit_dev) | jq -r .receipt
    get_bob_balance
}

transfer_DEV_from_bob_to_alice() {
    echo "$1. Transfer DEV from Bob to Alice"
    read -p 'Enter amount of DEV to be transfered: ' TRANSFER_AMOUNT 
    if ! [[ "$TRANSFER_AMOUNT" =~ ^[+-]?[0-9]+\.?[0-9]*$ ]]; then 
        echo "DEPOSIT_AMOUNT must be a numbers" 
        exit 0 
    fi

    cd ${CONFIG_DIR}
    encoded_data=$(eth method:encode abi.bsh_core.json "transferNativeCoin('$(cat alice.btp.address)')")
    eth transaction:send \
                --network $MOONBEAM_RPC_URL \
                --pk $MOONBEAM_PREFUND_PK \
                --gas $MOONBEAM_GAS_LIMIT \
                --to $(cat bsh_core.moonbeam) \
                --data $encoded_data \
                --value $TRANSFER_AMOUNT | jq -r > tx.transfer_dev
    eth transaction:get --network $MOONBEAM_RPC_URL $(cat tx.transfer_dev) | jq -r .receipt
}

check_alice_balance_in_Goloop() {
    echo "$1. Checking Alice's balance after 10 seconds..."
    sleep 10

    cd $CONFIG_DIR
    coin_id=$(goloop rpc sendtx call \
        --to $(cat nativeCoinBsh.icon) \
        --method coinId --param _coinName=DEV | jq -r )

    balance=$(goloop rpc sendtx call \
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
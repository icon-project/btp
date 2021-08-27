#!/bin/sh
set -e
source transfer_util.sh

ICX_DEPOSIT_AMOUNT=$(coin2wei 1000) # 1000 ICX
ICX_TRANSER_AMOUNT=$(coin2wei 1) # 1 ICX

deposit_ICX_for_Alice() {
    get_alice_balance
    read -r -p "Do you want to deposit $(wei2coin $ICX_DEPOSIT_AMOUNT) ICX to Alice ? [Y/n] " confirm
    case $confirm in
        [yY][eE][sS]|[yY])
            echo "$1. Depositing $(wei2coin $ICX_DEPOSIT_AMOUNT) ICX to Alice"

            cd ${CONFIG_DIR}
            goloop rpc sendtx transfer \
                --to $(get_alice_address) \
                --value $ICX_DEPOSIT_AMOUNT | jq -r . > tx.deposit.alice
            ensure_txresult tx.deposit.alice
            get_alice_balance
        ;;
        *) return 0
        ;;
    esac
}

transfer_ICX_from_Alice_to_Bob() {
    echo "$1. Transfer $(wei2coin  $ICX_TRANSER_AMOUNT) ICX from Alice to Bob"

    cd ${CONFIG_DIR}
    goloop rpc sendtx call \
        --to $(cat nativeCoinBsh.icon) --method transferNativeCoin \
        --param _to=$(cat bob.btp.address) --value $ICX_TRANSER_AMOUNT \
        --key_store alice.ks.json --key_secret alice.secret \
        | jq -r . > tx.Alice2Bob.transfer
    ensure_txresult tx.Alice2Bob.transfer
}

check_bob_balance_in_Moonbeam() {
    echo "$1. Checking Bob's balance after 10 seconds..."
    sleep 10

    cd $CONFIG_DIR
    eth abi:add bshcore abi.bsh_core.json
    eth contract:call --network $MOONBEAM_RPC_URL bshcore@$(cat bsh_core.moonbeam) "getBalanceOf('$(get_bob_address)', 'ICX')"
}

echo "This script demonstrates how to transfer a NativeCoin from ICON to MOONBEAM."
create_alice_account_in_Gochain "1"
deposit_ICX_for_Alice           "2"
create_bob_account_in_Moonbeam  "3"
transfer_ICX_from_Alice_to_Bob  "4"
check_bob_balance_in_Moonbeam   "5"

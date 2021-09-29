#!/bin/sh
set -e
source transfer_util.sh

ICX_DEPOSIT_AMOUNT=$(coin2wei 1000) # 1000 ICX
ICX_TRANSER_AMOUNT=$(coin2wei 10) # 1 ICX

deposit_ICX_for_Alice() {
    get_alice_balance
    read -r -p "Do you want to deposit $(wei2coin $ICX_DEPOSIT_AMOUNT) ICX to Alice ? [Y/n] " confirm
    case $confirm in
        [yY][eE][sS]|[yY])
            echo "$1. Depositing $(wei2coin $ICX_DEPOSIT_AMOUNT) ICX to Alice"

            cd ${CONFIG_DIR}
            goloop rpc sendtx transfer \
                --to $(get_alice_address) \
                --value $ICX_DEPOSIT_AMOUNT | jq -r . > tx.alice.deposit
            ensure_txresult tx.alice.deposit
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
        | jq -r . > tx.alice.transfer
    ensure_txresult tx.alice.transfer
}

bob_balance_in_moonbeam_before_transfering() {
    cd $CONFIG_DIR
    eth abi:add bshcore abi.bsh_core.json

    RESULT=$(eth contract:call --network $MOONBEAM_RPC_URL bshcore@$(cat bsh_core.moonbeam) "getBalanceOf('$(get_bob_address)', 'ICX')")
    BOB_BALANCE=$(echo "$RESULT" | awk '/_usableBalance/ {print $2}' | sed 's/[^0-9]*//g')
    echo "$1. Bob's balance before transfering: $(wei2coin $BOB_BALANCE) (ICX)"
}

bob_balance_in_moonbeam_after_transfering() {
    echo "$1. Checking Bob's balance after transfering with 60s timeout"
    cd $CONFIG_DIR

    BF_TF=$BOB_BALANCE
    TIMEOUT=60

    printf "["
    while true;
    do
        printf  "▓"
        if [ $TIMEOUT -le 0 ]; then
            printf "] error!\n"
            echo "Timeout while checking Bob's balance."
            exit 1
        fi
        # sleep 3 seconds as the moonbeam block interval
        sleep 3 
        TIMEOUT=$(expr $TIMEOUT - 3)

        RESULT=$(eth contract:call --network $MOONBEAM_RPC_URL bshcore@$(cat bsh_core.moonbeam) "getBalanceOf('$(get_bob_address)', 'ICX')")
        BOB_BALANCE=$(echo "$RESULT" | awk '/_usableBalance/ {print $2}' | sed 's/[^0-9]*//g')
        if [ "$BOB_BALANCE" != "$BF_TF" ]; then
            printf "] done!\n"
            break
        fi
    done
    echo "Bob's balance after transfering: $(wei2coin $BOB_BALANCE) (ICX)"
}


echo "This script demonstrates how to transfer a NativeCoin from ICON to MOONBEAM."
create_alice_account_in_Gochain "1"
deposit_ICX_for_Alice           "2"
create_bob_account_in_Moonbeam  "3"
bob_balance_in_moonbeam_before_transfering "4"
transfer_ICX_from_Alice_to_Bob  "5"
bob_balance_in_moonbeam_after_transfering   "6"

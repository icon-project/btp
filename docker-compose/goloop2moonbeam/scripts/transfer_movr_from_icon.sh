#!/bin/sh
set -e
source transfer_util.sh


MOONBEAM_PREFUND_PK=39539ab1876910bbf3a223d84a29e28f1cb4e2e456503e7e91ed39b2e7223d68
MOONBEAM_GAS_LIMIT=6721975

MOVR_DEPOSIT_AMOUNT=$(coin2wei 1000) # 1000 MOVR
MOVR_TRANSER_AMOUNT=$(coin2wei 10) # 1 MOVR


deposit_ICX_for_Alice() {
    get_alice_balance

    echo "$1. Funding $(wei2coin $MOVR_DEPOSIT_AMOUNT) ICX to Alice for transaction Gas"

    cd ${CONFIG_DIR}
    goloop rpc sendtx transfer \
        --to $(get_alice_address) \
        --value $MOVR_DEPOSIT_AMOUNT | jq -r . > tx.alice.deposit
    ensure_txresult tx.alice.deposit
    get_alice_balance
}


deposit_MOVR_for_Alice() {
    get_alice_balance
    read -r -p "Do you want to Fund $(wei2coin $MOVR_DEPOSIT_AMOUNT) MOVR to Alice ? [Y/n] " confirm
    case $confirm in
        [yY][eE][sS]|[yY])
            echo "$1. Depositing $(wei2coin $MOVR_DEPOSIT_AMOUNT) MOVR to Alice"

            cd ${CONFIG_DIR}            
            goloop rpc sendtx call --to $(cat irc2token.icon) \
            --method transfer \
            --param _to=$(get_alice_address) \
            --param _value=$MOVR_DEPOSIT_AMOUNT | jq -r . > tx.alice.deposit.irc2
            ensure_txresult tx.alice.deposit.irc2
            get_alice_irc2_balance
        ;;
        *) return 0
        ;;
    esac
}

Alice_deposit_MOVR_to_BSH(){
     echo "$1. Deposit $(wei2coin  $MOVR_TRANSER_AMOUNT) MOVR from Alice to BSH"
        cd ${CONFIG_DIR}
      goloop rpc sendtx call --to $(cat irc2token.icon) \
            --key_store alice.ks.json --key_secret alice.secret \
            --method transfer \
            --param _to=$(cat nativeCoinIRC2Bsh.icon) --param _value=$MOVR_TRANSER_AMOUNT | jq -r . > tx.alice.deposit.irc2.bsh

     ensure_txresult tx.alice.deposit.irc2.bsh
}

transfer_MOVR_from_Alice_to_Bob() {
    echo "$1. Transfer $(wei2coin  $MOVR_TRANSER_AMOUNT) MOVR from Alice to Bob"

    cd ${CONFIG_DIR}
    goloop rpc sendtx call \
        --to $(cat nativeCoinIRC2Bsh.icon) --method transfer \
        --param _coinName=MOVR \
        --param _to=$(cat bob.btp.address) \
        --param _value=$MOVR_TRANSER_AMOUNT \
        --key_store alice.ks.json --key_secret alice.secret \
        | jq -r . > tx.alice.transfer.irc2
    ensure_txresult tx.alice.transfer.irc2
}

bob_balance_in_moonbeam_before_transfering() {
    cd $CONFIG_DIR
    eth abi:add bshcore abi.bsh_core.json

    RESULT=$(eth contract:call --network $MOONBEAM_RPC_URL bshcore@$(cat bsh_core.moonbeam) "getBalanceOf('$(get_bob_address)', 'MOVR')")
    BOB_BALANCE=$(echo "$RESULT" | awk '/_usableBalance/ {print $2}' | sed 's/[^0-9]*//g')
    echo "$1. Bob's balance before transfering: $(wei2coin $BOB_BALANCE) (MOVR)"
}


fund_bsh_in_Moonbeam(){
    echo "$1. Funding MoonBeam BSH"

    cd ${CONFIG_DIR}
    encoded_data=$(eth method:encode abi.bsh_core.json "transferNativeCoin('$(cat alice.btp.address)')")
    eth transaction:send \
                --network $MOONBEAM_RPC_URL \
                --pk $MOONBEAM_PREFUND_PK \
                --gas $MOONBEAM_GAS_LIMIT \
                --to $(cat bsh_core_erc20.moonbeam) \
                --data $encoded_data \
                --value $(coin2wei 10000) | jq -r > tx.bob.transfer
    RESULT=$(eth address:balance --network $MOONBEAM_RPC_URL $(cat bsh_core_erc20.moonbeam))
    echo "BSH balance after funding: $RESULT (MOVR)"
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

        RESULT=$(eth contract:call --network $MOONBEAM_RPC_URL bshcore@$(cat bsh_core_erc20.moonbeam) "getBalanceOf('$(get_bob_address)', 'MOVR')")
        BOB_BALANCE=$(echo "$RESULT" | awk '/_usableBalance/ {print $2}' | sed 's/[^0-9]*//g')
        if [ "$BOB_BALANCE" != "$BF_TF" ]; then
            printf "] done!\n"
            break
        fi
    done
    echo "Bob's balance after transfering: $(wei2coin $BOB_BALANCE) (MOVR)"
}


echo "This script demonstrates how to transfer a MOVR from ICON to MOONBEAM."
create_alice_account_in_Gochain "1"
deposit_ICX_for_Alice "2"
deposit_MOVR_for_Alice          "3"
Alice_deposit_MOVR_to_BSH       "4"
create_bob_account_in_Moonbeam  "5"
fund_bsh_in_Moonbeam "6"
bob_balance_in_moonbeam_before_transfering "7"
transfer_MOVR_from_Alice_to_Bob  "8"
bob_balance_in_moonbeam_after_transfering   "9"

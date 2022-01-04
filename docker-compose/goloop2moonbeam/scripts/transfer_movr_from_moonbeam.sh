#!/bin/sh
set -e

source transfer_util.sh

MOONBEAM_PREFUND_PK=39539ab1876910bbf3a223d84a29e28f1cb4e2e456503e7e91ed39b2e7223d68
MOONBEAM_GAS_LIMIT=6721975
MOVR_DEPOSIT_AMOUNT=$(coin2wei 10)
MOVR_TRANSER_AMOUNT=$(coin2wei 1)

deposit_MOVR_for_bob() {
    get_bob_balance_MOVR
    read -r -p "Do you want to deposit $(wei2coin $MOVR_DEPOSIT_AMOUNT) (MOVR) to BOB ? [Y/n] " confirm
    case $confirm in
        [yY][eE][sS]|[yY])
            echo "$1. Depositing $(wei2coin $MOVR_DEPOSIT_AMOUNT) MOVR for Bob"

            cd ${CONFIG_DIR}
            eth transaction:send \
                        --network $MOONBEAM_RPC_URL \
                        --pk $MOONBEAM_PREFUND_PK \
                        --gas $MOONBEAM_GAS_LIMIT \
                        --to $(get_bob_address) \
                        --value $MOVR_DEPOSIT_AMOUNT | jq -r > tx.bob.deposit

            eth transaction:get --network $MOONBEAM_RPC_URL $(cat tx.bob.deposit) | jq -r .receipt
            get_bob_balance
        ;;
        *) 
            echo "$1. Skip depositing MOVR for Bob"
            return 0
        ;;
    esac
}

transfer_MOVR_from_bob_to_alice() {
    echo "$1. Transfering $(wei2coin $MOVR_TRANSER_AMOUNT) (MOVR) from Bob to Alice"

    cd ${CONFIG_DIR}
    encoded_data=$(eth method:encode abi.bsh_core.json "transferNativeCoin('$(cat alice.btp.address)')")
    eth transaction:send \
                --network $MOONBEAM_RPC_URL \
                --pk $(get_bob_private_key) \
                --gas $MOONBEAM_GAS_LIMIT \
                --to $(cat bsh_core_erc20.moonbeam) \
                --data $encoded_data \
                --value $MOVR_TRANSER_AMOUNT | jq -r > tx.bob.transfer
    eth transaction:get --network $MOONBEAM_RPC_URL $(cat tx.bob.transfer) | jq -r .receipt
    get_bob_balance_MOVR
}


fund_bsh_in_Icon() {
    echo "$1. Fund ICON BSH with $(wei2coin $MOVR_DEPOSIT_AMOUNT) MOVR"

    cd ${CONFIG_DIR}
    goloop rpc sendtx call \
        --to $(cat irc2token.icon) --method transfer \
        --param _to=$(cat nativeCoinIRC2Bsh.icon) \
        --param _value=$MOVR_DEPOSIT_AMOUNT \
        | jq -r . > tx.fundBSH.irc2
    ensure_txresult tx.fundBSH.irc2
    get_bsh_irc2_balance
}


get_bsh_irc2_balance() {
  cd $CONFIG_DIR
  balance=$(goloop rpc call --to $(cat irc2token.icon) \
    --method balanceOf \
    --param _owner=$(cat nativeCoinIRC2Bsh.icon) | jq -r .)
  balance=$(hex2int $balance)
  balance=$(wei2coin $balance)
  echo "BSH balance:: $balance (MOVR)"
}


check_alice_balance_in_Goloop_before_transfer() {
    echo "$1. Checking Alice's balance Before Transfer"
    cd $CONFIG_DIR

    balance=$(goloop rpc call \
        --to $(cat irc2token.icon) \
        --method balanceOf \
        --param _owner=$(get_alice_address) | jq -r )
    
    balance=$(hex2int $balance)
    balance=$(wei2coin $balance)
    echo "Alice balance: $balance (MOVR)"
}

check_alice_balance_in_Goloop() {
    echo "$1. Checking Alice's balance after 10 seconds..."
    sleep 10

    cd $CONFIG_DIR

    balance=$(goloop rpc call \
        --to $(cat irc2token.icon) \
        --method balanceOf \
        --param _owner=$(get_alice_address) | jq -r )
    
    balance=$(hex2int $balance)
    balance=$(wei2coin $balance)
    echo "Alice balance: $balance (MOVR)"
}

echo "This script demonstrates how to transfer a MOVR from MOONBEAM to ICON."
create_bob_account_in_Moonbeam  "1"
deposit_MOVR_for_bob            "2"
create_alice_account_in_Gochain "3"
fund_bsh_in_Icon                "4"
check_alice_balance_in_Goloop_before_transfer   "5"
transfer_MOVR_from_bob_to_alice "6"
check_alice_balance_in_Goloop   "7"

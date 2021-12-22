#!/bin/sh
set -e

source transfer_util.sh

MOONBEAM_PREFUND_PK=39539ab1876910bbf3a223d84a29e28f1cb4e2e456503e7e91ed39b2e7223d68
MOONBEAM_GAS_LIMIT=6721975
ICX_DEPOSIT_AMOUNT=$(coin2wei 10)
ICX_TRANSER_AMOUNT=$(coin2wei 1)

deposit_ICX_for_bob() {
    get_bob_balance
    read -r -p "Do you want to deposit $(wei2coin $ICX_DEPOSIT_AMOUNT) ICX to BOB ? [Y/n] " confirm
    case $confirm in
        [yY][eE][sS]|[yY])
            echo "$1. Depositing $(wei2coin $ICX_DEPOSIT_AMOUNT) MOVR for Bob (for transaction fees)"

            cd ${CONFIG_DIR}
            #fund bob with MOVR native coin for transaction gas
            eth transaction:send \
                        --network $MOONBEAM_RPC_URL \
                        --pk $MOONBEAM_PREFUND_PK \
                        --gas $MOONBEAM_GAS_LIMIT \
                        --to $(get_bob_address) \
                        --value $ICX_DEPOSIT_AMOUNT | jq -r > tx.bob.deposit

            eth transaction:get --network $MOONBEAM_RPC_URL $(cat tx.bob.deposit) | jq -r .receipt
            get_bob_balance_MOVR

            echo "$1. Depositing $(wei2coin $ICX_DEPOSIT_AMOUNT) ICX for Bob"
            ##Fund bob with ERC20_ICX to transfer
            encoded_data=$(eth method:encode abi.bsh_core_erc20.json "transfer('$(get_bob_address)', '$ICX_DEPOSIT_AMOUNT')")
            eth transaction:send \
                        --network $MOONBEAM_RPC_URL \
                        --pk 5fb92d6e98884f76de468fa3f6278f8807c48bebc13595d45af5bdc4da702133 \
                        --gas $MOONBEAM_GAS_LIMIT \
                        --to $(cat bsh_core_erc20.moonbeam) \
                        --data $encoded_data | jq -r > tx.bob.transfer
            eth transaction:get --network $MOONBEAM_RPC_URL $(cat tx.bob.transfer) | jq -r .receipt

        ;;
        *) 
            echo "$1. Skip depositing ICX for Bob"
            return 0
        ;;
    esac
}

transfer_ICX_from_bob_to_alice() {
    echo "$1. Transfering $(wei2coin $ICX_TRANSER_AMOUNT) ICX from Bob to Alice"

    cd ${CONFIG_DIR}

    encoded_data1=$(eth method:encode abi.bsh_core_erc20.json "approve('$(cat bsh_core_erc20.moonbeam)','$ICX_TRANSER_AMOUNT')")
    eth transaction:send \
                --network $MOONBEAM_RPC_URL \
                --pk $(get_bob_private_key) \
                --gas $MOONBEAM_GAS_LIMIT \
                --to $(cat bsh_core_erc20.moonbeam) \
                --data $encoded_data1  | jq -r > tx.bob.approve.transfer

    encoded_data=$(eth method:encode abi.bsh_core_erc20.json "transferWrappedCoin('ICX','$ICX_TRANSER_AMOUNT','$(cat alice.btp.address)')")
    eth transaction:send \
                --network $MOONBEAM_RPC_URL \
                --pk $(get_bob_private_key) \
                --gas $MOONBEAM_GAS_LIMIT \
                --to $(cat bsh_core_erc20.moonbeam) \
                --data $encoded_data | jq -r > tx.bob.transfer
    eth transaction:get --network $MOONBEAM_RPC_URL $(cat tx.bob.transfer) | jq -r .receipt
}

fund_bsh_in_Icon() {
    echo "$1. Fund ICON BSH with $(wei2coin  $ICX_DEPOSIT_AMOUNT) ICX"

    cd ${CONFIG_DIR}
    # goloop rpc sendtx transfer \
    #             --to $(cat nativeCoinIRC2Bsh.icon) \
    #             --value 1 | jq -r . > tx.fundBSH.icx.irc2
    # ensure_txresult tx.fundBSH.icx.irc2

    goloop rpc sendtx call \
        --to $(cat nativeCoinIRC2Bsh.icon) --method transferNativeCoin \
        --param _to=$(cat bob.btp.address) --value $ICX_DEPOSIT_AMOUNT \
        | jq -r . > tx.fund.bsh.native.icon
    ensure_txresult tx.fund.bsh.native.icon
}

get_alice_balance_before_transfer(){
    get_alice_balance
}

get_alice_balance_after_transfer(){
    echo "$1. Checking Alice's ICX balance after 10 seconds..."
    sleep 10
    get_alice_balance
}

echo "This script demonstrates how to transfer a ICX from MOONBEAM to ICON."
create_bob_account_in_Moonbeam  "1"
deposit_ICX_for_bob             "2"
create_alice_account_in_Gochain "3"
fund_bsh_in_Icon "4"
get_alice_balance_before_transfer "5"
transfer_ICX_from_bob_to_alice  "6"
get_alice_balance_after_transfer  "7"
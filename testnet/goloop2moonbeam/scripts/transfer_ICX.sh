#!/bin/sh
set -e

DEPOSIT_AMOUNT=2000000000
TRANSFER_AMOUNT=1000000

source goloop_rpc.sh
rpcch

create_alice_account_in_Gochain() {
    echo "1. creating Alice account in ICON"

    if [ ! -f "${CONFIG_DIR}/alice.secret" ];then
        cd ${CONFIG_DIR}
        echo -n $(date|md5sum|head -c16) > alice.secret
        goloop ks gen -o alice.ks.json  -p $(cat alice.secret)
        cat alice.ks.json | jq -r .address > alice.address

        echo "btp://$(cat net.btp.icon)/$(cat alice.address)" > alice.btp.address
    fi;
}

deposit_ICX_for_alice() {
    echo "2. deposit_ICX_for_alice"

    cd ${CONFIG_DIR}
    goloop rpc sendtx transfer \
        --to $(cat $CONFIG_DIR/alice.address) \
        --value $DEPOSIT_AMOUNT | jq -r . > tx.deposit.alice
    ensure_txresult tx.deposit.alice
}

create_bob_account_in_Moonbeam() {
    echo "3. create_bob_account_in_Moonbeam"
    cd ${CONFIG_DIR}

    eth address:random > bob.account
    cat  bob.account | jq -r .address > bob.address
    cat  bob.account | jq -r .privateKey > bob.private
    echo "btp://$(cat net.btp.moonbeam)/$(cat bob.address)" > $CONFIG_DIR/bob.btp.address
}

transfer_ICX_from_alice_to_bob() {
    echo "4. transfer_ICX_from_alice_to_bob"
    cd ${CONFIG_DIR}
    echo "$(goloop rpc balance $(cat alice.address))"


    goloop rpc sendtx call \
        --to $(cat nativeCoinBsh.icon) --method transferNativeCoin \
        --param _to=$(cat bob.btp.address) --value $TRANSFER_AMOUNT \
        --key_store alice.ks.json --key_secret alice.secret \
        | jq -r . > tx.Alice2Bob.transfer
    ensure_txresult tx.Alice2Bob.transfer
}

check_bob_balance() {
    sleep 10

    cd $CONFIG_DIR
    eth abi:add bshcore abi.bsh_core.json
    eth contract:call --network $MOONBEAM_RPC_URL bshcore@$(cat bsh_core.moonbeam) "getBalanceOf('$(cat bob.address)', 'ICX')"
}

echo "This script demonstrates how to transfer a NativeCoin from ICON to MOONBEAM."
create_alice_account_in_Gochain
deposit_ICX_for_alice
create_bob_account_in_Moonbeam
transfer_ICX_from_alice_to_bob
check_bob_balance

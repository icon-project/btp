#!/bin/sh
set -e

if [ ! -f "$PROVISION_STATUS_DONE" ]; then
    echo "provisioning not done yet"
    exit 1
fi

DEPOSIT_AMOUNT=2000000000
TRANSFER_AMOUNT=1000000
MOONBEAM_PREFUND_PK=39539ab1876910bbf3a223d84a29e28f1cb4e2e456503e7e91ed39b2e7223d68

source goloop_rpc.sh
rpcch

create_Alice_account_in_Gochain() {
    echo "1. create Alice account in ICON"

    cd ${CONFIG_DIR}
    echo -n $(date|md5sum|head -c16) > alice.secret
    goloop ks gen -o alice.ks.json  -p $(cat alice.secret)
    echo "btp://$(cat net.btp.icon)/$(cat alice.ks.json | jq -r .address)" > alice.btp.address
}

deposit_ICX_for_Alice() {
    echo "2. deposit $DEPOSIT_AMOUNT ICX to Alice"

    cd ${CONFIG_DIR}
    goloop rpc sendtx transfer \
        --to $(cat alice.ks.json | jq -r .address) \
        --value $DEPOSIT_AMOUNT | jq -r . > tx.deposit.alice
    ensure_txresult tx.deposit.alice

    echo "Alice's balance: $(goloop rpc balance $(cat alice.address) | xargs printf "%d\n") ICX"
}

create_Bob_account_in_Moonbeam() {
    echo "3. Create bob's account in Moonbeam"

    cd ${CONFIG_DIR}
    eth address:random > bob.account
    cat bob.account | jq -r .address > bob.address
    cat bob.account | jq -r .privateKey > bob.private
    echo "btp://$(cat net.btp.moonbeam)/$(cat bob.address)" > $CONFIG_DIR/bob.btp.address
}

transfer_ICX_from_Alice_to_Bob() {
    echo "4. Transfer $TRANSFER_AMOUNT ICX from Alice to Bob"

    cd ${CONFIG_DIR}
    goloop rpc sendtx call \
        --to $(cat nativeCoinBsh.icon) --method transferNativeCoin \
        --param _to=$(cat bob.btp.address) --value $TRANSFER_AMOUNT \
        --key_store alice.ks.json --key_secret alice.secret \
        | jq -r . > tx.Alice2Bob.transfer
    ensure_txresult tx.Alice2Bob.transfer
}

check_Bob_balance() {
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

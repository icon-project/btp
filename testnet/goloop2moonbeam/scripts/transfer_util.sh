#!/bin/sh
set -e

source util.sh
if [ ! -f "$PROVISION_STATUS_DONE" ]; then
    echo "provisioning not done yet"
    exit 1
fi

create_bob_account_in_Moonbeam() {
    echo "$1. create Bob's account in Moonbeam"
    cd ${CONFIG_DIR}

    if [ ! -f bob.account ]; then
        eth address:random > bob.account
        echo "btp://$(cat net.btp.moonbeam)/$(cat $(_get_bob_address))" > $CONFIG_DIR/bob.btp.address
    fi
    echo "Bob's address: $(cat bob.address)"
}

get_bob_address() {
    cat  ${CONFIG_DIR}/bob.account | jq -r .address
}

get_bob_private_key() {
    cat  ${CONFIG_DIR}/bob.account | jq -r .privateKey | sed -e 's/^0x//'
}

get_bob_balance() {
    bob_balance=$(eth address:balance --network $MOONBEAM_RPC_URL $(get_bob_address))
    echo "Bob's balance: $bob_balance DEV" 
}

create_alice_account_in_Gochain() {
    echo "$1. create Alice account in ICON"

    cd ${CONFIG_DIR}
    if [ ! -f alice.secret ]; then
        echo -n $(date|md5sum|head -c16) > alice.secret
        goloop ks gen -o alice.ks.json  -p $(cat alice.secret)
        echo "btp://$(cat net.btp.icon)/$(cat alice.ks.json | jq -r .address)" > alice.btp.address
    fi
    echo "Alice's address: $(cat alice.ks.json | jq -r .address))"
}

get_alice_address() {
    cat alice.ks.json | jq -r .address
}

get_alice_balance() {
    echo "Alice's balance: $(goloop rpc balance $(get_alice_address)) ICX"
}
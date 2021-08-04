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

    if [ ! -f bob.btp.address ]; then
        eth address:random > bob.account
        echo "btp://$(cat net.btp.moonbeam)/$(get_bob_address)" > bob.btp.address
    fi
    echo "Bob's btp address: $(cat bob.btp.address)"
}

get_bob_address() {
    cat  ${CONFIG_DIR}/bob.account | jq -r .address
}

get_bob_private_key() {
    cat  ${CONFIG_DIR}/bob.account | jq -r .privateKey | sed -e 's/^0x//'
}

get_bob_balance() {
    bob_balance=$(eth address:balance --network $MOONBEAM_RPC_URL $(get_bob_address) | eth convert -f eth -t wei)
    echo "Bob's balance: $bob_balance wei (DEV)" 
}

create_alice_account_in_Gochain() {
    echo "$1. create Alice account in ICON"

    cd ${CONFIG_DIR}
    if [ ! -f alice.secret ]; then
        echo -n $(date|md5sum|head -c16) > alice.secret
        goloop ks gen -o alice.ks.json  -p $(cat alice.secret)
        echo "btp://$(cat net.btp.icon)/$(cat alice.ks.json | jq -r .address)" > alice.btp.address
    fi
    echo "Alice's btp address: $(cat alice.btp.address)"
}

get_alice_address() {
    cat alice.ks.json | jq -r .address
}

get_alice_balance() {
    balance=$(goloop rpc balance $(get_alice_address) | jq -r)
    echo "Alice's balance: $balance (ICX)"
}
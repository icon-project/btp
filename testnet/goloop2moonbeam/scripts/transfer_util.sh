#!/bin/sh
set -e

source util.sh
if [ ! -f "$PROVISION_STATUS_DONE" ]; then
    echo "provisioning not done yet"
    exit 1
fi

PRECISION=18
COIN_UNIT=$((10 ** $PRECISION))

coin2wei() {
    amount=$1
    printf '%s * %s\n' $COIN_UNIT $amount | bc
}

wei2coin() {
    amount=$1
    printf 'scale=%s; %s / %s\n' $PRECISION $amount $COIN_UNIT | bc
}

create_bob_account_in_Moonbeam() {
    cd ${CONFIG_DIR}
    if [ ! -f bob.btp.address ]; then
        echo "$1. Creating Bob's account in Moonbeam"

        eth address:random > bob.account
        echo "btp://$(cat net.btp.moonbeam)/$(get_bob_address)" > bob.btp.address
    else
        echo "$1. Skip creating Bob account. Already existed"
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
    bob_balance=$(eth address:balance --network $MOONBEAM_RPC_URL $(get_bob_address))
    echo "Bob's balance: $bob_balance (DEV)" 
}

create_alice_account_in_Gochain() {
    cd ${CONFIG_DIR}
    if [ ! -f alice.secret ]; then
        echo "$1. Creating Alice account in ICON"

        echo -n $(date|md5sum|head -c16) > alice.secret
        goloop ks gen -o alice.ks.json  -p $(cat alice.secret)
        echo "btp://$(cat net.btp.icon)/$(cat alice.ks.json | jq -r .address)" > alice.btp.address
    else
        echo "$1. Skip creating Alice account. Already existed"
    fi
    echo "Alice's btp address: $(cat alice.btp.address)"
}

get_alice_address() {
    cat alice.ks.json | jq -r .address
}

get_alice_balance() {
    balance=$(goloop rpc balance $(get_alice_address) | jq -r)
    balance=$(hex2int $balance)
    balance=$(wei2coin $balance)
    echo "Alice's balance: $balance (ICX)"
}

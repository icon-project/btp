#!/bin/sh
set -e

create_bob_account_in_Moonbeam() {
    echo "1. create_bob_account_in_Moonbeam"
    cd ${CONFIG_DIR}

    eth address:random > bob.account
    cat  bob.account | jq -r .address > bob.address
    cat  bob.account | jq -r .privateKey > bob.private
    echo "btp://$(cat net.btp.moonbeam)/$(cat bob.address)" > $CONFIG_DIR/bob.btp.address
}

deposit_DEV_for_bob() {
    echo "2. deposit_DEV_for_bob"
}

create_alice_account_in_Gochain() {
    echo "3. creating Alice account in ICON"

    cd ${CONFIG_DIR}
    echo -n $(date|md5sum|head -c16) > alice.secret
    goloop ks gen -o alice.ks.json  -p $(cat alice.secret)
    echo "btp://$(cat net.btp.icon)/$(cat alice.ks.json | jq -r .address)" > alice.btp.address
}

transfer_DEV_from_bob_to_alice() {
     echo "4. transfer_DEV_from_bob_to_alice"
}

# WIP
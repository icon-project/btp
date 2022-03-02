cd ${GOLOOP_PROVISION_CONFIG}

source btp.sh
source token.sh
source rpc.sh

generate_keystores() {
    echo ${GOLOOP_PROVISION_CONFIG}
    if [ ! -f "${GOLOOP_PROVISION_CONFIG}/alice.secret" ];then
        echo "Generating keystore for Alice"

        echo -n $(date|md5sum|head -c16) > alice.secret
        goloop ks gen -o alice.ks.json  -p $(cat alice.secret)
    fi;

    if [ ! -f "${GOLOOP_PROVISION_CONFIG}/bob.secret" ];then
        echo "Generating keystore for Bob"

        echo -n $(date|md5sum|head -c16) > bob.secret
        goloop ks gen -o bob.ks.json  -p $(cat bob.secret)
    fi;
}

mint_token_alice() {
    echo "Minting 10 irc2_token for Alice"

    rpcch src
    rpcks $GOLOOP_KEY_STORE $GOLOOP_KEY_SECRET

    TX=$(goloop rpc sendtx call --to $(cat irc2_token.src) \
        --method transfer \
        --param _to=$(jq -r .address alice.ks.json) \
        --param _value=10 | jq -r .)
    echo $TX
    ensure_txresult $TX

    echo "Alice's balance: $(goloop rpc call --to $(cat irc2_token.src) --method balanceOf --param _owner=$(jq -r .address alice.ks.json))"
}

alice_deposit_tokens() {
    echo "Alice desposits 10 irc2_token to BSH"

    rpcch src
    rpcks alice.ks.json alice.secret

    tx=$(goloop rpc sendtx call --to $(cat irc2_token.src) \
    --method transfer \
    --param _to=$(cat token_bsh.src) \
    --param _value=10 | jq -r .)
    ensure_txresult $tx

    echo "Alice's balance: $(goloop rpc call --to $(cat token_bsh.src) --method balanceOf --param _owner=$(jq -r .address alice.ks.json))"
}

alice_transfer_tokens() {
    echo "Alice transfer 10 irc2_token to Bob via BSH"

    rpcch src
    rpcks alice.ks.json alice.secret

    tx=$(goloop rpc sendtx call --to $(cat token_bsh.src) \
    --method transfer \
    --param _tokenName=IRC2Token \
    --param _to=btp://$(cat net.btp.dst)/$(jq -r .address bob.ks.json) \
    --param _value=10 | jq -r .)
    ensure_txresult $tx

    echo "Alice's balance: $(goloop rpc call --to $(cat token_bsh.src) --method balanceOf --param _owner=$(jq -r .address alice.ks.json))"
}

bob_withdraw_tokens() {
    echo "Bob withdraw 10 irc2_token from BSH"

    rpcch dst
    rpcks bob.ks.json bob.secret

    tx=$(goloop rpc sendtx call --to $(cat token_bsh.dst) \
    --method reclaim \
    --param _tokenName=IRC2Token \
    --param _value=10 | jq -r .)
    ensure_txresult $tx

    echo "Bob's balance: $(goloop rpc call --to $(cat irc2_token.dst) --method balanceOf --param _owner=$(jq -r .address bob.ks.json))"
}


generate_keystores
mint_token_alice
alice_deposit_tokens
alice_transfer_tokens
bob_withdraw_tokens
``#!/bin/sh

setup() {
    apk add jq
    goloop chain join --genesis_template src.genesis.json --channel src
    sleep 1
    goloop chain join --genesis_template dst.genesis.json --channel dst
    sleep 1

    goloop chain start src
    sleep 2
    goloop chain start dst
}

deploy_bmc() {
    echo "deploy BMC"

    rpcch src > /dev/null
    echo "deploy bmc on src chain"
    goloop rpc sendtx deploy javascore/bmc.jar --content_type application/java \
      --param _net=$(cat net.btp.$(rpcch)) | jq -r . > tx.bmc.$(rpcch)
    sleep 4
    goloop rpc txresult $(cat tx.bmc.$(rpcch)) | jq -r .scoreAddress > bmc.$(rpcch)
    echo "btp://$(cat net.btp.$(rpcch))/$(cat bmc.$(rpcch))" > btp.$(rpcch)

    rpcch dst > /dev/null
    echo "$GOLOOP_RPC_NID.icon" > net.btp.$(rpcch)
    echo "deploy bmc on dst chain"
    goloop rpc sendtx deploy javascore/bmc.jar --content_type application/java \
      --param _net=$(cat net.btp.$(rpcch)) | jq -r . > tx.bmc.$(rpcch)
    sleep 3
    goloop rpc txresult $(cat tx.bmc.$(rpcch)) | jq -r .scoreAddress > bmc.$(rpcch)
    echo "btp://$(cat net.btp.$(rpcch))/$(cat bmc.$(rpcch))" > btp.$(rpcch)
}

deploy_bmv() {
    echo "deploy BMV"

    rpcch dst > /dev/null
    goloop rpc call --to $GOLOOP_CHAINSCORE --method getValidators| jq -r 'map(.)|join(",")' > validators.$(rpcch)
    echo "0x$(printf %x $(goloop chain inspect $(rpcch) --format {{.Height}}))" > offset.$(rpcch)

    rpcch src > /dev/null
    echo "deploy bmv on src chain"
    goloop rpc sendtx deploy javascore/bmv-icon.jar --content_type application/java \
      --param _bmc=$(cat bmc.$(rpcch)) \
      --param _net=$(cat net.btp.dst) \
      --param _validators=$(cat validators.dst) \
      --param _offset=$(cat offset.dst) \
       | jq -r . > tx.bmv.$(rpcch)
    sleep 3
    goloop rpc txresult $(cat tx.bmv.$(rpcch)) | jq -r .scoreAddress > bmv.$(rpcch)

    rpcch src > /dev/null
    goloop rpc call --to $GOLOOP_CHAINSCORE --method getValidators| jq -r 'map(.)|join(",")' > validators.$(rpcch)
    echo "0x$(printf %x $(goloop chain inspect $(rpcch) --format {{.Height}}))" > offset.$(rpcch)
    rpcch dst > /dev/null
    echo "deploy bmv on dst chain"
    goloop rpc sendtx deploy javascore/bmv-icon.jar --content_type application/java \
      --param _bmc=$(cat bmc.$(rpcch)) \
      --param _net=$(cat net.btp.src) \
      --param _validators=$(cat validators.src) \
      --param _offset=$(cat offset.src) \
       | jq -r . > tx.bmv.$(rpcch)
    sleep 3
    goloop rpc txresult $(cat tx.bmv.$(rpcch)) | jq -r .scoreAddress > bmv.$(rpcch)

    rpcch src > /dev/null
}

deploy_bsh() {
    echo "deploy BSH"
    rpcch src > /dev/null
    goloop rpc sendtx deploy javascore/bsh.jar --content_type application/java \
      --param _bmc=$(cat bmc.$(rpcch)) | jq -r . > tx.token.$(rpcch)
    sleep 3
    goloop rpc txresult $(cat tx.token.$(rpcch)) | jq -r .scoreAddress > token.$(rpcch)

    rpcch dst > /dev/null
    goloop rpc sendtx deploy javascore/bsh.jar --content_type application/java \
      --param _bmc=$(cat bmc.$(rpcch)) | jq -r . > tx.token.$(rpcch)
    sleep 3
    goloop rpc txresult $(cat tx.token.$(rpcch)) | jq -r .scoreAddress > token.$(rpcch)
}

deploy_irc2() {
    echo "deploy IRC2"
    rpcch src > /dev/null
    goloop rpc sendtx deploy javascore/irc2.jar --content_type application/java \
      --param _name=IRC2Token \
      --param _symbol=I2T \
      --param _initialSupply=1000 \
      --param _decimals=18 \
      | jq -r . > tx.irc2.$(rpcch)
    sleep 3
    goloop rpc txresult $(cat tx.irc2.$(rpcch)) | jq -r .scoreAddress > irc2.$(rpcch)

    rpcch dst > /dev/null
    goloop rpc sendtx deploy javascore/irc2.jar --content_type application/java \
      --param _name=IRC2Token \
      --param _symbol=I2T \
      --param _initialSupply=0x3E8 \
      --param _decimals=0x12 \
      | jq -r . > tx.irc2.$(rpcch)
    sleep 3
    goloop rpc txresult $(cat tx.irc2.$(rpcch)) | jq -r .scoreAddress > irc2.$(rpcch)
}

deploy_feeaggregation() {
    echo "deploy fee aggregation"
    rpcch src > /dev/null
    KEYSTORE_ADDRESS=$(cat keystore.json | jq -r .address)
    goloop rpc sendtx deploy javascore/fee-aggregation.jar --content_type application/java \
      --param _cps_address=$KEYSTORE_ADDRESS \
      --param _band_protocol_address=$KEYSTORE_ADDRESS \
      | jq -r . > tx.feeaggr.$(rpcch)

    sleep 3
    goloop rpc txresult $(cat tx.feeaggr.$(rpcch)) | jq -r .scoreAddress > feeaggr.$(rpcch)
}

deploy_result() {
    for channel in src dst
    do
        rpcch ${channel} > /dev/null
        printf "{
            \"net_btp\" : \"$(cat net.btp.$(rpcch))\",
            \"tx_bmc\" : \"$(cat tx.bmc.$(rpcch))\",
            \"bmc\" : \"$(cat bmc.$(rpcch))\",
            \"btp\" : \"$(cat btp.$(rpcch))\",
            \"tx.bmv\" : \"$(cat tx.bmv.$(rpcch))\",
            \"bmv\" : \"$(cat bmv.$(rpcch))\",
            \"tx.bsh\" : \"$(cat tx.token.$(rpcch))\",
            \"bsh\" : \"$(cat token.$(rpcch))\",
            \"tx.irc2\" : \"$(cat tx.irc2.$(rpcch))\",
            \"irc2\" : \"$(cat irc2.$(rpcch))\"
        }" | jq . > ${channel}_result.json

        if [[ "$channel" == "src" ]]; then
            feeinfo="{
                \"tx.feeaggregation\" : \"$(cat tx.feeaggr.${channel})\",
                \"feeaggregation\" : \"$(cat feeaggr.${channel})\"
            }"
            echo "$(jq ". + $feeinfo" ${channel}_result.json)" > ${channel}_result.json
        fi
    done
}

register_bmv() {
    echo "register BMV"
    rpcch src > /dev/null
    goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
      --method addVerifier \
      --param _net=$(cat net.btp.dst) \
      --param _addr=$(cat bmv.$(rpcch)) \
      | jq -r . > tx.verifier.$(rpcch)

    rpcch dst > /dev/null
    goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
      --method addVerifier \
      --param _net=$(cat net.btp.src) \
      --param _addr=$(cat bmv.$(rpcch)) \
      | jq -r . > tx.verifier.$(rpcch)
}

register_link() {
    echo "register link"
    rpcch src > /dev/null
    goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
      --method addLink \
      --param _link=$(cat btp.dst) \
      | jq -r . > tx.link.$(rpcch)

    sleep 2
    echo "check $(rpcch) link"
    goloop rpc call --to $(cat bmc.$(rpcch)) --method getLinks

    rpcch dst > /dev/null
    goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
      --method addLink \
      --param _link=$(cat btp.src) \
      | jq -r . > tx.link.$(rpcch)

    sleep 2
    echo "check $(rpcch) link"
    goloop rpc call --to $(cat bmc.$(rpcch)) --method getLinks
}

configure_link() {
    echo "configure link"
    rpcch src > /dev/null
    goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
      --method setLinkRotateTerm \
      --param _link=$(cat btp.dst) \
      --param _block_interval=0x3e8 \
      --param _max_agg=0x10 \
      | jq -r . > tx.setlink.$(rpcch)

    sleep 2
    echo "check $(rpcch) status"
    goloop rpc call --to $(cat bmc.src) --method getStatus --param _link=$(cat btp.dst)

    rpcch dst > /dev/null
    goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
      --method setLinkRotateTerm \
      --param _link=$(cat btp.src) \
      --param _block_interval=0x3e8 \
      --param _max_agg=0x10 \
      | jq -r . > tx.setlink.$(rpcch)

    sleep 2
    echo "check $(rpcch) status"
    goloop rpc call --to $(cat bmc.dst) --method getStatus --param _link=$(cat btp.src)
}

register_tokenBSH() {
    echo "register token BSH"
    rpcch src > /dev/null
    goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
      --method addService \
      --param _svc=TokenBSH \
      --param _addr=$(cat token.$(rpcch)) | jq -r . > tx.service.token.$(rpcch)

    rpcch dst > /dev/null
    goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
      --method addService \
      --param _svc=TokenBSH \
      --param _addr=$(cat token.$(rpcch)) | jq -r . > tx.service.token.$(rpcch)
}

register_irc2() {
    echo "register IRC2 token"
    rpcch src > /dev/null
    goloop rpc sendtx call --to $(cat token.$(rpcch)) \
      --method register \
      --param name=IRC2Token \
      --param symbol=I2T \
      --param feeNumerator=0x64 \
      --param decimals=0x12 \
      --param address=$(cat irc2.$(rpcch))

    sleep 2
    echo "check $(rpcch) token name"
    goloop rpc call --to $(cat token.$(rpcch)) --method tokenNames

    rpcch dst > /dev/null
    goloop rpc sendtx call --to $(cat token.$(rpcch)) \
      --method register \
      --param name=IRC2Token \
      --param symbol=I2T \
      --param feeNumerator=0x64 \
      --param decimals=0x12 \
      --param address=$(cat irc2.$(rpcch))

    sleep 2
    echo "check $(rpcch) token name"
    goloop rpc call --to $(cat token.$(rpcch)) --method tokenNames
}

register_bmc_owner() {
    echo "register BMC owner"
    echo -n $(date|md5sum|head -c16) > src.secret
    goloop ks gen -o src.ks.json  -p $(cat src.secret)
    echo -n $(date|md5sum|head -c16) > dst.secret
    goloop ks gen -o dst.ks.json  -p $(cat dst.secret)

    rpcch src > /dev/null
    echo "add $(rpcch) BMC owner"
    goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
      --method addOwner \
      --param _addr=$(jq -r .address $(rpcch).ks.json)

    echo "check $(rpcch) BMC owner"
    goloop rpc call --to $(cat bmc.$(rpcch)) --method getOwners


    rpcks $GOLOOP_KEY_STORE $GOLOOP_KEY_SECRET
    rpcch dst > /dev/null
    echo "add $(rpcch) BMC owner"
    goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
      --method addOwner \
      --param _addr=$(jq -r .address $(rpcch).ks.json)

    echo "check $(rpcch) BMC owner"
    goloop rpc call --to $(cat bmc.$(rpcch)) --method getOwners


}

register_bmr() {
    echo "register BMR"
    rpcch src > /dev/null
    rpcks src.ks.json src.secret
    echo "add $(rpcch) BMC relay"
    goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
      --method addRelay \
      --param _link=$(cat btp.dst) \
      --param _addr=$(jq -r .address dst.ks.json)

    sleep 2
    echo "get $(rpcch) relays"
    goloop rpc call --to $(cat bmc.src) --method getRelays --param _link=$(cat btp.dst)

    rpcch dst > /dev/null
    rpcks dst.ks.json dst.secret
    echo "add $(rpcch) BMC relay"
    goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
      --method addRelay \
      --param _link=$(cat btp.src) \
      --param _addr=$(jq -r .address src.ks.json)

    sleep 2
    echo "get $(rpcch) relays"
    goloop rpc call --to $(cat bmc.dst) --method getRelays --param _link=$(cat btp.src)
}

register_fee_aggregation() {
    echo "register FeeAggregator"

    goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
      --method setFeeAggregator \
      --param _addr=$(cat feeaggr.$(rpcch)) \
      | jq -r . > tx.setFeeAggr.$(rpcch)
    sleep 2
    echo "$(rpcch) getFeeAggregator"
    goloop rpc call --to $(cat bmc.$(rpcch)) \
      --method getFeeAggregator
}

register_all() {
    echo "register all"
    register_bmv
    register_link
    configure_link
    register_tokenBSH
    register_irc2
    register_bmc_owner
    register_bmr
    register_fee_aggregation

    rpcch src
    rpcks $GOLOOP_KEY_STORE $GOLOOP_KEY_SECRET
}

deploy_all() {
    echo "deploy all"
    deploy_bmc
    deploy_bmv
    deploy_bsh
    deploy_irc2
    deploy_feeaggregation
    deploy_result

    rpcch src
}

source rpc.sh
rpcks $GOLOOP_KEY_STORE $GOLOOP_KEY_SECRET

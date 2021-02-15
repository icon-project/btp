#!/bin/sh

deploy_bmc() {
  local CHAIN=$1
  rpcch ${CHAIN}
  echo "$GOLOOP_RPC_NID.icon" > net.btp.${CHAIN}
  goloop rpc sendtx deploy pyscore/bmc.zip \
      --param _net=$(cat net.btp.${CHAIN}) | jq -r . > tx.bmc.${CHAIN}
  extract_scoreAddress tx.bmc.${CHAIN} bmc.${CHAIN}
  echo "btp://$(cat net.btp.${CHAIN})/$(cat bmc.${CHAIN})" > btp.${CHAIN}
}

deploy_bmv() {
  local CHAIN=$1
  local LINK=$2
  rpcch ${LINK}
  goloop rpc call --to $GOLOOP_CHAINSCORE --method getValidators| jq -r 'map(.)|join(",")' > validators.${LINK}
  goloop chain inspect ${LINK} --format {{.Height}} > offset.${LINK}

  rpcch ${CHAIN}
  goloop rpc sendtx deploy pyscore/bmv.zip \
      --param _bmc=$(cat bmc.${CHAIN}) \
      --param _net=$(cat net.btp.${LINK}) \
      --param _validators=$(cat validators.${LINK}) \
      --param _offset=$(cat offset.${LINK}) \
       | jq -r . > tx.bmv.${CHAIN}
  extract_scoreAddress tx.bmv.${CHAIN} bmv.${CHAIN}
}

bmc_link() {
  local CHAIN=$1
  local LINK=$2
  rpcch ${CHAIN}
  goloop rpc sendtx call --to $(cat bmc.${CHAIN}) \
    --method addLink \
    --param _link=$(cat btp.${LINK}) | jq -r . > tx.link.${CHAIN}
  ensure_txresult tx.link.${CHAIN}
}

bmc_setLink() {
  local CHAIN=$1
  local LINK=$2
  rpcch ${CHAIN}
  goloop rpc sendtx call --to $(cat bmc.${CHAIN}) \
    --method setLink \
    --param _link=$(cat btp.${LINK}) \
    --param _block_interval=0x3e8 \
    --param _max_agg=0x10 \
    --param _delay_limit=3 | jq -r . > tx.setlink.${CHAIN}
  ensure_txresult tx.setlink.${CHAIN}
}

bmc_addRelayer() {
  local CHAIN=$1
  rpcch ${CHAIN}
  goloop rpc sendtx call --to $(cat bmc.${CHAIN}) \
      --method addRelayer \
      --param _addr=$(jq -r .address $(rpcks)) \
      --param _desc="${CHAIN} relayer" | jq -r . > tx.relayer.${CHAIN}
  ensure_txresult tx.relayer.${CHAIN}
}

bmc_addRelay() {
  local CHAIN=$1
  local LINK=$2
  rpcch ${CHAIN}
  goloop rpc sendtx call --to $(cat bmc.${CHAIN}) \
      --method addRelay \
      --param _link=$(cat btp.${LINK}) \
      --param _addr=$(jq -r .address $(rpcks)) | jq -r . > tx.relay.${CHAIN}
  ensure_txresult tx.relay.${CHAIN}
}

source rpc.sh
rpcks $GOLOOP_KEY_STORE $GOLOOP_KEY_SECRET
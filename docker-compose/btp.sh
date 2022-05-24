#!/bin/sh

deploy_bmc() {
  if [ $# -lt 1 ] ; then
    echo "Usage: deploy_bmc CHAIN [SCORE_FILE=bmc.jar]"
    return 1
  fi
  local CHAIN=$1
  local SCORE_FILE=${2:-bmc.jar}
  local SCORE_TYPE=${SCORE_FILE##*.}
  if [ "$SCORE_TYPE" == "jar" ]; then
    local CONTENT_TYPE="--content_type application/java"
  fi

  rpcch ${CHAIN}
  echo "$GOLOOP_RPC_NID.icon" > net.btp.${CHAIN}
  goloop rpc sendtx deploy ${SCORE_FILE} ${CONTENT_TYPE} \
      --param _net=$(cat net.btp.${CHAIN}) | jq -r . > tx.bmc.${CHAIN}
  sleep 2
  extract_scoreAddress tx.bmc.${CHAIN} bmc.${CHAIN}
  echo "btp://$(cat net.btp.${CHAIN})/$(cat bmc.${CHAIN})" > btp.${CHAIN}
}

deploy_bmv() {
  if [ $# -lt 2 ] ; then
    echo "Usage: deploy_bmv CHAIN LINK [SCORE_FILE=bmv-icon.jar]"
    return 1
  fi
  local CHAIN=$1
  local LINK=$2
  local SCORE_FILE=${3:-bmv-icon.jar}
  local SCORE_TYPE=${SCORE_FILE##*.}
  if [ "$SCORE_TYPE" == "jar" ]; then
    local CONTENT_TYPE="--content_type application/java"
  fi
  rpcch ${LINK}
  goloop rpc call --to $GOLOOP_CHAINSCORE --method getValidators| jq -r 'map(.)|join(",")' > validators.${LINK}
  echo "0x$(printf %x $(goloop chain inspect ${LINK} --format {{.Height}}))" > offset.${LINK}

  rpcch ${CHAIN}
  goloop rpc sendtx deploy ${SCORE_FILE} ${CONTENT_TYPE} \
      --param _bmc=$(cat bmc.${CHAIN}) \
      --param _net=$(cat net.btp.${LINK}) \
      --param _validators=$(cat validators.${LINK}) \
      --param _offset=$(cat offset.${LINK}) \
       | jq -r . > tx.bmv.${CHAIN}
  extract_scoreAddress tx.bmv.${CHAIN} bmv.${CHAIN}
}

bmc_addVerifier() {
  if [ $# -lt 2 ] ; then
    echo "Usage: bmc_addVerifier CHAIN LINK"
    return 1
  fi
  local CHAIN=$1
  local LINK=$2
  rpcch ${CHAIN}
  goloop rpc sendtx call --to $(cat bmc.${CHAIN}) \
      --method addVerifier \
      --param _net=$(cat net.btp.${LINK}) \
      --param _addr=$(cat bmv.${CHAIN}) | jq -r . > tx.verifier.${CHAIN}
  ensure_txresult tx.verifier.${CHAIN}
}

bmc_addService() {
  if [ $# -lt 3 ] ; then
    echo "Usage: bmc_addService CHAIN SVC ADDR"
    return 1
  fi
  local CHAIN=$1
  local SVC=$2
  local ADDR=$3
  rpcch ${CHAIN}
  goloop rpc sendtx call --to $(cat bmc.${CHAIN}) \
      --method addService \
      --param _svc=${SVC} \
      --param _addr=${ADDR} | jq -r . > tx.service.${SVC}.${CHAIN}
  ensure_txresult tx.service.${SVC}.${CHAIN}
}

bmc_link() {
  if [ $# -lt 2 ] ; then
    echo "Usage: bmc_link CHAIN LINK"
    return 1
  fi
  local CHAIN=$1
  local LINK=$2
  rpcch ${CHAIN}
  goloop rpc sendtx call --to $(cat bmc.${CHAIN}) \
    --method addLink \
    --param _link=$(cat btp.${LINK}) | jq -r . > tx.link.${CHAIN}
  ensure_txresult tx.link.${CHAIN}
}

bmc_setLinkRotateTerm() {
  if [ $# -lt 2 ] ; then
    echo "Usage: bmc_setLinkRotateTerm CHAIN LINK [INTERVAL=0x3e8] [MAX_AGG=0x10]"
    return 1
  fi
  local CHAIN=$1
  local LINK=$2
  local INTERVAL=${3:-0x3e8}
  local MAX_AGG=${4:-0x10}
  rpcch ${CHAIN}
  goloop rpc sendtx call --to $(cat bmc.${CHAIN}) \
    --method setLinkRotateTerm \
    --param _link=$(cat btp.${LINK}) \
    --param _block_interval=${INTERVAL} \
    --param _max_agg=${MAX_AGG} | jq -r . > tx.setlink.${CHAIN}
  ensure_txresult tx.setlink.${CHAIN}
}

bmc_addOwner() {
  if [ $# -lt 1 ] ; then
    echo "Usage: bmc_addOwner CHAIN [EOA]"
    return 1
  fi
  local CHAIN=$1
  local OWNER=$(rpceoa $2)
  rpcch ${CHAIN}
  goloop rpc sendtx call --to $(cat bmc.${CHAIN}) \
      --method addOwner \
      --param _addr=${OWNER} | jq -r . > tx.owner.${CHAIN}
  ensure_txresult tx.owner.${CHAIN}
}

bmc_addRelay() {
  if [ $# -lt 3 ] ; then
    echo "Usage: bmc_addRelay CHAIN LINK EOA"
    return 1
  fi
  local CHAIN=$1
  local LINK=$2
  local RELAY=$(rpceoa $3)
  rpcch ${CHAIN}
  goloop rpc sendtx call --to $(cat bmc.${CHAIN}) \
      --method addRelay \
      --param _link=$(cat btp.${LINK}) \
      --param _addr=${RELAY} | jq -r . > tx.relay.${CHAIN}
  ensure_txresult tx.relay.${CHAIN}
}

bmc_registerRelayer() {
  if [ $# -lt 1 ] ; then
    echo "Usage: bmc_registerRelayer CHAIN [BOND=0x10]"
    return 1
  fi
  local CHAIN=$1
  local BOND=${2:-0x10}
  rpcch ${CHAIN}
  goloop rpc sendtx call --to $(cat bmc.${CHAIN}) \
      --method registerRelayer \
      --param _desc="${CHAIN} relayer" \
      --value ${BOND} | jq -r . > tx.relayer.${CHAIN}
  ensure_txresult tx.relayer.${CHAIN}
}

bmc_addServiceCandidate() {
  if [ $# -lt 3 ] ; then
    echo "Usage: bmc_addServiceCandidate CHAIN SVC ADDR"
    return 1
  fi
  local CHAIN=$1
  local SVC=$2
  local ADDR=$3
  rpcch ${CHAIN}
  goloop rpc sendtx call --to $(cat bmc.${CHAIN}) \
      --method addServiceCandidate \
      --param _svc=${SVC} \
      --param _addr=${ADDR} | jq -r . > tx.service.candidate.${SVC}.${CHAIN}
  ensure_txresult tx.service.candidate.${SVC}.${CHAIN}
}

bmc_call() {
  local METHOD=$1
  local CHAIN=$2
  local LINK=$3
  case "$METHOD" in
    getVerifiers|getServices|getLinks|getRoutes|getRelayers|getOwners|getServiceCandidates)
    ;;
    getStatus|getRelays)
      local PARAM="--param _link=$(cat btp.$LINK)"
      if [ -z "$LINK" ];then
        return 1
      fi
    ;;
    *)
    echo "Usage bmc_call METHOD CHAIN"
    echo "  METHOD: [getVerifiers|getServices|getLinks|getRoutes|getRelayers|getOwners|getServiceCandidates]"
    echo "Usage bmc_call METHOD CHAIN LINK"
    echo "  METHOD: [getStatus|getRelays]"
    return 1
    ;;
  esac
  rpcch ${CHAIN}
  goloop rpc call --to $(cat bmc.${CHAIN}) \
    --method $METHOD $PARAM
}

source rpc.sh
rpcks $GOLOOP_KEY_STORE $GOLOOP_KEY_SECRET

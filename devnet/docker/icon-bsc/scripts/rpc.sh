#!/bin/sh

rpchelp() {
  echo "rpcch CHANNEL"
  echo "rpcks KEYSTORE_PATH"
}

rpcch() {
  if [ ! "$1" == "" ]; then
    export GOLOOP_RPC_CHANNEL=$1
    URI_PREFIX=http://$(goloop system info -f '{{.Setting.RPCAddr}}')/api
    export GOLOOP_RPC_URI=$URI_PREFIX/v3/$GOLOOP_RPC_CHANNEL
    export GOLOOP_RPC_NID=$(goloop chain inspect $GOLOOP_RPC_CHANNEL --format {{.NID}})
    export GOLOOP_DEBUG_URI=$URI_PREFIX/v3d/$GOLOOP_RPC_CHANNEL
    export GOLOOP_RPC_STEP_LIMIT=${GOLOOP_RPC_STEP_LIMIT:-1}
  fi
  echo $GOLOOP_RPC_CHANNEL
}

rpcks() {
  if [ ! "$1" == "" ]; then
    export GOLOOP_RPC_KEY_STORE=$1
    if [ ! "$2" == "" ]; then
      if [ -f "$2" ]; then
        export GOLOOP_RPC_KEY_SECRET=$2
      else
        export GOLOOP_RPC_KEY_PASSWORD=$2
      fi
    fi
  fi
  echo $GOLOOP_RPC_KEY_STORE
}

rpceoa() {
  local EOA=${1:-${GOLOOP_RPC_KEY_STORE}}
  if [ "$EOA" != "" ] && [ -f "$EOA" ]; then
    echo $(cat $EOA | jq -r .address)
  else
    echo $EOA
  fi
}

ensure_txresult() {
  OLD_SET_OPTS=$(set +o)
  set +e
  local TX=$1

  if [ -f "${TX}" ]; then
    TX=$(cat ${TX})
  fi

  sleep 2
  RESULT=$(goloop rpc txresult ${TX})
  RET=$?
  echo $RESULT
  while [ "$RET" != "0" ] && [ "$(echo $RESULT | grep -E 'Executing|Pending')" == "$RESULT" ]; do
    sleep 1
    RESULT=$(goloop rpc txresult ${TX})
    RET=$?
    echo $RESULT
  done
  eval "${OLD_SET_OPTS}"

  if [ "$RET" != "0" ]; then
    echo $RESULT
    return $RET
  else
    STATUS=$(echo $RESULT | jq -r .status)
    if [ "$STATUS" == "0x1" ]; then
      return 0
    else
      echo $RESULT
      return 1
    fi
  fi
}

extract_scoreAddress() {
  local TX=$1
  local ADDR=$2

  RESULT=$(ensure_txresult $TX)
  RET=$?

  if [ "$RET" != "0" ]; then
    echo $RESULT
    return $RET
  else
    SCORE=$(echo $RESULT | jq -r .scoreAddress)
    echo $SCORE | tee ${ADDR}
  fi
}

export GOLOOP_CHAINSCORE=cx0000000000000000000000000000000000000000

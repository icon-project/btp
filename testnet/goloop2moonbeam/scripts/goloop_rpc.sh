#!/bin/sh

GOLOOPCHAIN=${GOLOOPCHAIN:-'goloop'}

rpcch() {
  export GOLOOP_CONFIG=$CONFIG_DIR/$GOLOOPCHAIN.server.json
  export GOLOOP_KEY_STORE=$CONFIG_DIR/$GOLOOPCHAIN.keystore.json
  export GOLOOP_KEY_SECRET=$CONFIG_DIR/$GOLOOPCHAIN.keysecret
  export GOLOOP_RPC_URI=http://$GOLOOPCHAIN:9080/api/v3/icon
  export GOLOOP_RPC_KEY_STORE=$CONFIG_DIR/$GOLOOPCHAIN.keystore.json
  export GOLOOP_RPC_KEY_SECRET=$CONFIG_DIR/$GOLOOPCHAIN.keysecret
  export GOLOOP_RPC_NID=${GOLOOP_RPC_NID:-$(cat $CONFIG_DIR/nid.icon)}
  export GOLOOP_RPC_STEP_LIMIT=${GOLOOP_RPC_STEP_LIMIT:-5000000000}
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
    exit $RET
  else
    STATUS=$(echo $RESULT | jq -r .status)
    if [ "$STATUS" == "0x1" ]; then
      return 0
    else
      echo $RESULT
      exit 1
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
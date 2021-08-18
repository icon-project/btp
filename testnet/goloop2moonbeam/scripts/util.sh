export CONFIG_DIR=${CONFIG_DIR:-/btpsimple/config}
export SCRIPT_DIR=${SCRIPT_DIR:-/btpsimple/scripts}
export SOLIDITY_DIST_DIR=${SOLIDITY_DIST_DIR:-/btpsimple/contracts/solidity}
export JAVASCORE_DIST_DIR=${JAVASCORE_DIST_DIR:-/btpsimple/contracts/javascore}
export JAVASCORE_HELPER_DIR=${JAVASCORE_HELPER_DIR:-$JAVASCORE_DIST_DIR/helper}
export MOONBEAM_CHAIN_ID=1281 # https://github.com/PureStake/moonbeam#chain-ids
export MOONBEAM_RPC_URL=${MOONBEAM_RPC_URL:-'http://moonbeam:9933'}
export PROVISION_STATUS_DONE=$CONFIG_DIR/provision.done
export PROVISION_STATUS_PROCESSING=$CONFIG_DIR/provision.processing

# goloop env
export GOLOOP_CONFIG=$CONFIG_DIR/$GOLOOPCHAIN.server.json
export GOLOOP_KEY_STORE=$CONFIG_DIR/$GOLOOPCHAIN.keystore.json
export GOLOOP_KEY_SECRET=$CONFIG_DIR/$GOLOOPCHAIN.keysecret
export GOLOOP_RPC_URI=http://$GOLOOPCHAIN:9080/api/v3/icon
export GOLOOP_RPC_KEY_STORE=$CONFIG_DIR/$GOLOOPCHAIN.keystore.json
export GOLOOP_RPC_KEY_SECRET=$CONFIG_DIR/$GOLOOPCHAIN.keysecret
export GOLOOP_RPC_NID=${GOLOOP_RPC_NID:-$(cat $CONFIG_DIR/nid.icon)}
export GOLOOP_RPC_STEP_LIMIT=${GOLOOP_RPC_STEP_LIMIT:-5000000000}
export GOLOOP_CHAINSCORE=cx0000000000000000000000000000000000000000
GOLOOPCHAIN=${GOLOOPCHAIN:-'goloop'}

# disable line lenght to support big number operators
export BC_LINE_LENGTH=0 


latest_block_goloop() {
    goloop rpc lastblock
}

moonbeam_blocknumber() {
    curl -s -X POST 'http://moonbeam:9933' --header 'Content-Type: application/json' \
    --data-raw '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[], "id": 1}' | jq -r .result | xargs printf "%d\n"
}

wait_file_created() {
    FILE_DIR=$1
    FILE_NAME=$2

    timeout=10
    while [ ! -f $FILE_DIR/$FILE_NAME ];
    do
        if [ "$timeout" == 0 ]; then
            echo "ERROR: Timeout while waiting for the file $FILE_NAME."
            exit 1
        fi
        sleep 1
        timeout=$(expr $timeout - 1)

        echo "waiting for the output file: $FILE_NAME"
    done
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

uppercase() {
  input=$1
  printf '%s\n' "$input" | awk '{ print toupper($0) }'
}

hex2int() {
  input=$1
  input=$(echo $input | sed 's/^0x//g')
  input=$(uppercase $input)
  echo "ibase=16; $input" | bc
}

ensure_file_exist() {
    FILE_DIR=$1
    FILE_NAME=$2
    
    if [ ! -f $FILE_DIR/$FILE_NAME ]; then
      echo "Missing file $FILE_NAME"
    fi
}
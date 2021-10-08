#!/bin/sh

get_alice_address() {
  cat $CONFIG_DIR/alice.ks.json | jq -r .address
}

get_bob_address() {
  #cat $CONFIG_DIR/bsc.ks.json | jq -r .address
  echo 0x$(cat $CONFIG_DIR/bsc.ks.json | jq -r .address)
}

hex2int() {
  input=$1
  input=$(echo $input | sed 's/^0x//g')
  input=$(uppercase $input)
  echo "ibase=16; $input" | bc
}

decimal2Hex() {
  printf '0x%x\n' $1
}

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

uppercase() {
  input=$1
  printf '%s\n' "$input" | awk '{ print toupper($0) }'
}

create_contracts_address_json() {
  TYPE="${1-solidity}"
  NAME="$2"
  VALUE="$3"
  if test -f "$CONFIG_DIR/addresses.json"; then
    echo "appending address.json"
    objJSON="{\"$NAME\":\"$VALUE\"}"
    cat $CONFIG_DIR/addresses.json | jq --arg type "$TYPE" --argjson jsonString "$objJSON" '.[$type] += $jsonString' >$CONFIG_DIR/addresses.json
  else
    echo "creating address.json"
    objJSON="{\"$TYPE\":{\"$NAME\":\"$VALUE\"}}"
    jq -n --argjson jsonString "$objJSON" '$jsonString' >$CONFIG_DIR/addresses.json
    wait_for_file $CONFIG_DIR/addresses.json
  fi
}

create_abi() {
  NAME=$1
  echo "Generating abi file from ./build/contracts/$NAME.json"
  [ ! -d $CONFIG_DIR/abi ] && mkdir -p $CONFIG_DIR/abi
  cat "./build/contracts/$NAME.json" | jq -r .abi >$CONFIG_DIR/abi/$NAME.json
  wait_for_file $CONFIG_DIR/abi/$NAME.json
}

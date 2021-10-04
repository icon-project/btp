#!/bin/sh

get_alice_address() {
  cat $CONFIG_DIR/alice.ks.json | jq -r .address
}

get_bob_address(){
  #cat $CONFIG_DIR/bsc.ks.json | jq -r .address
  echo 0x$(cat $CONFIG_DIR/bsc.ks.json | jq -r .address)
}

hex2int() {
  input=$1
  input=$(echo $input | sed 's/^0x//g')
  input=$(uppercase $input)
  echo "ibase=16; $input" | bc
}

decimal2Hex(){
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
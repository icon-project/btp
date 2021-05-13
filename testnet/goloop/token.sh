#!/bin/sh

deploy_bsh() {
  local CHAIN=$1
  rpcch ${CHAIN}
  goloop rpc sendtx deploy pyscore/token_bsh.zip \
    --param _bmc=$(cat bmc.${CHAIN}) | jq -r . > tx.token_bsh.${CHAIN}
  extract_scoreAddress tx.token_bsh.${CHAIN} token_bsh.${CHAIN}
}

deploy_irc2() {
  local CHAIN=$1
  if [ "$2" == "proxy" ]; then
    PARAM_OWNER="--param _owner=$(cat token_bsh.${CHAIN})"
  else
    PARAM_OWNER=""
  fi
  rpcch ${CHAIN}
  goloop rpc sendtx deploy pyscore/irc2_token.zip \
    --param _name=${TOKEN_NAME} \
    --param _symbol=${TOKEN_SYM} \
    --param _initialSupply=${TOKEN_SUPPLY} \
    --param _decimals=${TOKEN_DECIMALS} \
    ${PARAM_OWNER} | jq -r . > tx.irc2_token.${CHAIN}
  extract_scoreAddress tx.irc2_token.${CHAIN} irc2_token.${CHAIN}
}

bsh_register() {
  local CHAIN=$1
  rpcch ${CHAIN}
  goloop rpc sendtx call --to $(cat token_bsh.${CHAIN}) \
    --method register \
    --param _name=${TOKEN_NAME} \
    --param _addr=$(cat irc2_token.${CHAIN}) | jq -r . > tx.register.${CHAIN}
  ensure_txresult tx.register.${CHAIN}
}

bsh_balance() {
  if [ $# -lt 1 ] ; then
    echo "Usage: bsh_balance CHAIN [EOA=$(rpceoa)]"
    return 1
  fi
  local CHAIN=$1
  local EOA=$(rpceoa $2)
  rpcch ${CHAIN} > /dev/null
  goloop rpc call --to $(cat token_bsh.${CHAIN}) \
    --method balanceOf \
    --param _owner=$EOA
}

bsh_transfer() {
  if [ $# -lt 2 ] ; then
    echo "Usage: bsh_transfer CHAIN LINK [VAL=0x10] [EOA=$(rpceoa)]"
    return 1
  fi
  local CHAIN=$1
  local LINK=$2
  local VAL=${3:-0x10}
  local EOA=$(rpceoa $4)
  rpcch ${CHAIN} > /dev/null
  TX=$(goloop rpc sendtx call --to $(cat token_bsh.${CHAIN}) \
    --method transfer \
    --param _tokenName=${TOKEN_NAME} \
    --param _to=btp://$(cat net.btp.${LINK})/$EOA \
    --param _value=$VAL | jq -r .)
  ensure_txresult $TX
}

bsh_reclaim() {
  if [ $# -lt 2 ] ; then
    echo "Usage: bsh_reclaim CHAIN [VAL=0x10]"
    return 1
  fi
  local CHAIN=$1
  local VAL=${2:-0x10}
  rpcch ${CHAIN} > /dev/null
  TX=$(goloop rpc sendtx call --to $(cat token_bsh.${CHAIN}) \
    --method reclaim \
    --param _tokenName=${TOKEN_NAME} \
    --param _value=$VAL | jq -r .)
  ensure_txresult $TX
}

irc2_balance() {
  if [ $# -lt 1 ] ; then
    echo "Usage: irc2_balance CHAIN [EOA=$(rpceoa)]"
    return 1
  fi
  local CHAIN=$1
  local EOA=$(rpceoa $2)
  rpcch ${CHAIN} > /dev/null
  goloop rpc call --to $(cat irc2_token.${CHAIN}) \
    --method balanceOf \
    --param _owner=$EOA
}

irc2_transfer() {
  if [ $# -lt 1 ] ; then
    echo "Usage: irc2_transfer CHAIN [VAL=0x10] [EOA=Address of Token-BSH]"
    return 1
  fi
  local CHAIN=$1
  local VAL=${2:-0x10}
  local EOA=$(rpceoa ${3:-$(cat token_bsh.${CHAIN})})
  rpcch ${CHAIN} > /dev/null
  TX=$(goloop rpc sendtx call --to $(cat irc2_token.${CHAIN}) \
    --method transfer \
    --param _to=$EOA \
    --param _value=$VAL | jq -r .)
  ensure_txresult $TX
}

TOKEN_NAME=IRC2Token
TOKEN_SYM=I2T
TOKEN_SUPPLY=0xF4240
TOKEN_DECIMALS=0x12

source rpc.sh
rpcks $GOLOOP_KEY_STORE $GOLOOP_KEY_SECRET
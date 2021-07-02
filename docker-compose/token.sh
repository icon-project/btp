#!/bin/sh

deploy_token() {
  if [ $# -lt 1 ] ; then
    echo "Usage: deploy_token CHAIN [SCORE_FILE=token.zip]"
    return 1
  fi
  local CHAIN=$1
  local SCORE_FILE=${2:-token.zip}
  local SCORE_TYPE=${SCORE_FILE##*.}
  if [ "$SCORE_TYPE" == "jar" ]; then
    local CONTENT_TYPE="--content_type application/java"
  fi

  rpcch ${CHAIN}
  goloop rpc sendtx deploy ${SCORE_FILE} ${CONTENT_TYPE} \
    --param _bmc=$(cat bmc.${CHAIN}) | jq -r . > tx.token.${CHAIN}
  extract_scoreAddress tx.token.${CHAIN} token.${CHAIN}
}

deploy_irc2() {
  local CHAIN=$1
  local SCORE_FILE=${2:-irc2.zip}
  local SCORE_TYPE=${SCORE_FILE##*.}
  if [ "$SCORE_TYPE" == "jar" ]; then
    local CONTENT_TYPE="--content_type application/java"
  fi
  if [ "$3" == "proxy" ]; then
    PARAM_OWNER="--param _owner=$(cat token.${CHAIN})"
  else
    PARAM_OWNER=""
  fi
  rpcch ${CHAIN}
  goloop rpc sendtx deploy ${SCORE_FILE} ${CONTENT_TYPE} \
    --param _name=${TOKEN_NAME} \
    --param _symbol=${TOKEN_SYM} \
    --param _initialSupply=${TOKEN_SUPPLY} \
    --param _decimals=${TOKEN_DECIMALS} \
    ${PARAM_OWNER} | jq -r . > tx.irc2.${CHAIN}
  extract_scoreAddress tx.irc2.${CHAIN} irc2.${CHAIN}
}

token_register() {
  local CHAIN=$1
  local ADDR=${2:-$(cat irc2.${CHAIN})}
  rpcch ${CHAIN}
  goloop rpc sendtx call --to $(cat token.${CHAIN}) \
    --method register \
    --param _name=${TOKEN_NAME} \
    --param _addr=${ADDR} | jq -r . > tx.register.${CHAIN}
  ensure_txresult tx.register.${CHAIN}
}

token_balance() {
  if [ $# -lt 1 ] ; then
    echo "Usage: token_balance CHAIN [EOA=$(rpceoa)]"
    return 1
  fi
  local CHAIN=$1
  local EOA=$(rpceoa $2)
  rpcch ${CHAIN} > /dev/null
  goloop rpc call --to $(cat token.${CHAIN}) \
    --method balanceOf \
    --param _owner=$EOA
}

token_transfer() {
  if [ $# -lt 2 ] ; then
    echo "Usage: token_transfer CHAIN LINK [VAL=0x10] [EOA=$(rpceoa)]"
    return 1
  fi
  local CHAIN=$1
  local LINK=$2
  local VAL=${3:-0x10}
  local EOA=$(rpceoa $4)
  rpcch ${CHAIN} > /dev/null
  TX=$(goloop rpc sendtx call --to $(cat token.${CHAIN}) \
    --method transfer \
    --param _tokenName=${TOKEN_NAME} \
    --param _to=btp://$(cat net.btp.${LINK})/$EOA \
    --param _value=$VAL | jq -r .)
  ensure_txresult $TX
}

token_reclaim() {
  if [ $# -lt 1 ] ; then
    echo "Usage: token_reclaim CHAIN [VAL=0x10]"
    return 1
  fi
  local CHAIN=$1
  local VAL=${2:-0x10}
  rpcch ${CHAIN} > /dev/null
  TX=$(goloop rpc sendtx call --to $(cat token.${CHAIN}) \
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
  goloop rpc call --to $(cat irc2.${CHAIN}) \
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
  local EOA=$(rpceoa ${3:-$(cat token.${CHAIN})})
  rpcch ${CHAIN} > /dev/null
  TX=$(goloop rpc sendtx call --to $(cat irc2.${CHAIN}) \
    --method transfer \
    --param _to=$EOA \
    --param _value=$VAL | jq -r .)
  ensure_txresult $TX
}

TOKEN_NAME=IRC2Token
TOKEN_SYM=I2T
TOKEN_SUPPLY=0x3E8
TOKEN_DECIMALS=0x12

source rpc.sh
rpcks $GOLOOP_KEY_STORE $GOLOOP_KEY_SECRET

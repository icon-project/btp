#!/bin/sh

#
# Copyright 2021 ICON Foundation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

deploy_nc() {
  if [ $# -lt 1 ] ; then
    echo "Usage: deploy_nc CHAIN [SCORE_FILE=nativecoin.jar]"
    return 1
  fi
  local CHAIN=$1
  local SCORE_FILE=${2:-nativecoin.jar}
  local SCORE_TYPE=${SCORE_FILE##*.}
  if [ "$SCORE_TYPE" == "jar" ]; then
    local CONTENT_TYPE="--content_type application/java"
  fi

  rpcch ${CHAIN}
  goloop rpc sendtx deploy ${SCORE_FILE} ${CONTENT_TYPE} \
    --param _bmc=$(cat bmc.${CHAIN}) \
    --param _irc31=$(cat irc31.${CHAIN}) \
    --param _name=${CHAIN} | jq -r . > tx.nativecoin.${CHAIN}
  extract_scoreAddress tx.nativecoin.${CHAIN} nativecoin.${CHAIN}
}

nc_fee() {
  if [ $# -lt 2 ] ; then
    echo "Usage: nc_fee CHAIN FEE_RATE"
    return 1
  fi
  local CHAIN=$1
  local FEE_RATE=$2
  rpcch ${CHAIN}
  goloop rpc sendtx call --to $(cat nativecoin.${CHAIN}) \
    --method setFeeRate \
    --param _feeRate=${FEE_RATE} | jq -r . > tx.nativecoin.fee.${CHAIN}
  ensure_txresult tx.nativecoin.fee.${CHAIN}
}

nc_register() {
  if [ $# -lt 2 ] ; then
    echo "Usage: nc_register CHAIN COIN"
    return 1
  fi
  local CHAIN=$1
  local COIN=$2
  rpcch ${CHAIN}
  goloop rpc sendtx call --to $(cat nativecoin.${CHAIN}) \
    --method register \
    --param _name=${COIN} | jq -r . > tx.nativecoin.register.${CHAIN}
  ensure_txresult tx.nativecoin.register.${CHAIN}
}

nc_balance() {
  if [ $# -lt 1 ] ; then
    echo "Usage: nc_balance CHAIN [COIN=CHAIN] [EOA=$(rpceoa)]"
    return 1
  fi
  local CHAIN=$1
  local COIN=${2:-${CHAIN}}
  local EOA=$(rpceoa $3)
  rpcch ${CHAIN} > /dev/null
  goloop rpc call --to $(cat nativecoin.${CHAIN}) \
    --method balanceOf \
    --param _owner=$EOA \
    --param _coinName=${COIN}
}

nc_transfer() {
  if [ $# -lt 3 ] ; then
    echo "Usage: nc_transfer CHAIN LINK EOA [COIN=CHAIN] [VAL=0x10]"
    return 1
  fi
  local CHAIN=$1
  local LINK=$2
  local EOA=$(rpceoa $3)
  local COIN=${4:-${CHAIN}}
  local VAL=${5:-0x10}
  rpcch ${CHAIN} > /dev/null
  local CALL_CMD="goloop rpc sendtx call --to $(cat nativecoin.${CHAIN}) \
      --param _to=btp://$(cat net.btp.${LINK})/${EOA}"
  if [ "${COIN}" == "${CHAIN}" ]; then
    CALL_CMD="${CALL_CMD} --method transferNativeCoin \
      --value ${VAL}"
  else
    CALL_CMD="${CALL_CMD} --method transfer \
      --param _coinName=${COIN} \
      --param _value=${VAL}"
  fi
  TX=$(${CALL_CMD} | jq -r .)
  ensure_txresult $TX
}

nc_reclaim() {
  if [ $# -lt 1 ] ; then
    echo "Usage: nc_reclaim CHAIN [COIN=CHAIN] [VAL=0x10]"
    return 1
  fi
  local CHAIN=$1
  local COIN=${2:-${CHAIN}}
  local VAL=${3:-0x10}
  rpcch ${CHAIN} > /dev/null
  TX=$(goloop rpc sendtx call --to $(cat nativecoin.${CHAIN}) \
    --method reclaim \
    --param _coinName=${COIN} \
    --param _value=${VAL} | jq -r .)
  ensure_txresult $TX
}

nc_coin() {
  if [ $# -lt 1 ] ; then
    echo "Usage: nc_coin CHAIN [COIN=CHAIN]"
    return 1
  fi
  local CHAIN=$1
  local COIN=${2:-${CHAIN}}
  rpcch ${CHAIN} > /dev/null
  goloop rpc call --to $(cat nativecoin.${CHAIN}) \
    --method coinId \
    --param _coinName=${COIN} | jq -r .
}

nc_addOwner() {
  if [ $# -lt 1 ] ; then
    echo "Usage: nc_addOwner CHAIN [EOA]"
    return 1
  fi
  local CHAIN=$1
  local OWNER=$(rpceoa $2)
  rpcch ${CHAIN}
  goloop rpc sendtx call --to $(cat nativecoin.${CHAIN}) \
      --method addOwner \
      --param _addr=${OWNER} | jq -r . > tx.nativecoin.owner.${CHAIN}
  ensure_txresult tx.nativecoin.owner.${CHAIN}
}

deploy_irc31() {
  if [ $# -lt 1 ] ; then
    echo "Usage: deploy_irc31 CHAIN [SCORE_FILE=irc31.jar]"
    return 1
  fi
  local CHAIN=$1
  local SCORE_FILE=${2:-irc31.jar}
  local SCORE_TYPE=${SCORE_FILE##*.}
  if [ "$SCORE_TYPE" == "jar" ]; then
    local CONTENT_TYPE="--content_type application/java"
  fi

  rpcch ${CHAIN}
  goloop rpc sendtx deploy ${SCORE_FILE} ${CONTENT_TYPE} \
     | jq -r . > tx.irc31.${CHAIN}
  extract_scoreAddress tx.irc31.${CHAIN} irc31.${CHAIN}
}

irc31_balance() {
  if [ $# -lt 1 ] ; then
    echo "Usage: irc31_balance CHAIN [COIN=CHAIN] [EOA=$(rpceoa)]"
    return 1
  fi
  local CHAIN=$1
  local COIN=${2:-${CHAIN}}
  local EOA=$(rpceoa $3)
  rpcch ${CHAIN} > /dev/null
  goloop rpc call --to $(cat irc31.${CHAIN}) \
    --method balanceOf \
    --param _owner=${EOA} \
    --param _id=$(nc_coin ${CHAIN} ${COIN})
}

irc31_transfer() {
  if [ $# -lt 3 ] ; then
    echo "Usage: irc31_transfer CHAIN EOA VAL [COIN=CHAIN]"
    return 1
  fi
  local CHAIN=$1
  local EOA=$(rpceoa $2)
  local VAL=$3
  local COIN=${4:-${CHAIN}}
  local FROM=$(rpceoa)
  if [ "${FROM}" == "${EOA}" ]; then
    echo "EOA must not be \$GOLOOP_RPC_KEY_STORE"
    return 1
  fi
  rpcch ${CHAIN} > /dev/null
  TX=$(goloop rpc sendtx call --to $(cat irc31.${CHAIN}) \
    --method transferFrom \
    --param _from=${FROM} \
    --param _to=${EOA} \
    --param _id=$(nc_coin ${CHAIN} ${COIN}) \
    --param _value=$VAL | jq -r .)
  ensure_txresult $TX
}

irc31_approve() {
  if [ $# -lt 1 ] ; then
    echo "Usage: irc31_approve CHAIN [EOA=NC_ADDR] [APPROVED=0x1]"
    return 1
  fi
  local CHAIN=$1
  local EOA=${2:-$(cat nativecoin.${CHAIN})}
  local APPROVED=${3:-"0x1"}
  rpcch ${CHAIN} > /dev/null
  TX=$(goloop rpc sendtx call --to $(cat irc31.${CHAIN}) \
    --method setApprovalForAll \
    --param _operator=${EOA} \
    --param _approved=${APPROVED} | jq -r .)
  ensure_txresult $TX
}

irc31_addOwner() {
  if [ $# -lt 1 ] ; then
    echo "Usage: irc31_addOwner CHAIN [EOA]"
    return 1
  fi
  local CHAIN=$1
  local OWNER=$(rpceoa $2)
  rpcch ${CHAIN}
  goloop rpc sendtx call --to $(cat irc31.${CHAIN}) \
      --method addOwner \
      --param _addr=${OWNER} | jq -r . > tx.irc31.owner.${CHAIN}
  ensure_txresult tx.irc31.owner.${CHAIN}
}

source rpc.sh
rpcks $GOLOOP_KEY_STORE $GOLOOP_KEY_SECRET

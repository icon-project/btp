#!/bin/bash

RELAY_BIN=../bin/relay
DEPLOYMENTS=deployments.json

if [ ! -f ${RELAY_BIN} ]; then
    (cd ..; make relay)
fi

HARDHAT_NETWORK=$(cat ${DEPLOYMENTS} | jq -r .hardhat.network)
HARDHAT_BMC_ADDRESS=$(cat ${DEPLOYMENTS} | jq -r .hardhat.contracts.bmcp)
HARDHAT_ENDPOINT=http://localhost:8545
HARDHAT_KEYSTORE=./docker/hardhat/keystore0.json
HARDHAT_KEYPASS=hardhat

ICON_NETWORK=$(cat ${DEPLOYMENTS} | jq -r .icon.network)
ICON_BMC_ADDRESS=$(cat ${DEPLOYMENTS} | jq -r .icon.contracts.bmc)
ICON_ENDPOINT=http://localhost:9080/api/v3/icon_dex
ICON_KEYSTORE=./docker/icon/config/keystore.json
ICON_KEYPASS=gochain

if [ "x$1" = x ]; then
    echo "Usage: $0 <target_chain>"
    exit 1
else
    TARGET=$1
fi

case ${TARGET} in
  hardhat)
    SRC_ADDRESS=btp://${ICON_NETWORK}/${ICON_BMC_ADDRESS}
    SRC_ENDPOINT=${ICON_ENDPOINT}
    SRC_KEY_STORE=${ICON_KEYSTORE}
    SRC_KEY_PASSWORD=${ICON_KEYPASS}
    DST_ADDRESS=btp://${HARDHAT_NETWORK}/${HARDHAT_BMC_ADDRESS}
    DST_ENDPOINT=${HARDHAT_ENDPOINT}
    DST_KEY_STORE=${HARDHAT_KEYSTORE}
    DST_KEY_PASSWORD=${HARDHAT_KEYPASS}
  ;;
  *)
    echo "Error: unknown target: $TARGET"
    exit 1
esac

if [ "x$BMV_BRIDGE" = xtrue ]; then
  echo "Using Bridge mode"
else
  echo "Using BTPBlock mode"
  BMV_BRIDGE=false
fi

${RELAY_BIN} \
    --direction both \
    --src.address ${SRC_ADDRESS} \
    --src.endpoint ${SRC_ENDPOINT} \
    --src.key_store ${SRC_KEY_STORE} \
    --src.key_password ${SRC_KEY_PASSWORD} \
    --src.bridge_mode=${BMV_BRIDGE} \
    --dst.address ${DST_ADDRESS} \
    --dst.endpoint ${DST_ENDPOINT} \
    --dst.key_store ${DST_KEY_STORE} \
    --dst.key_password ${DST_KEY_PASSWORD} \
    start

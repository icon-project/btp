#!/bin/bash

BRIDGE_BIN=../bin/bridge
DEPLOYMENTS=deployments.json

if [ ! -f ${BRIDGE_BIN} ]; then
    (cd ..; make bridge)
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
  icon)
    SRC_ADDRESS=btp://${HARDHAT_NETWORK}/${HARDHAT_BMC_ADDRESS}
    SRC_ENDPOINT=${HARDHAT_ENDPOINT}
    DST_ADDRESS=btp://${ICON_NETWORK}/${ICON_BMC_ADDRESS}
    DST_ENDPOINT=${ICON_ENDPOINT}
    KEY_STORE=${ICON_KEYSTORE}
    KEY_PASSWORD=${ICON_KEYPASS}
  ;;
  hardhat)
    SRC_ADDRESS=btp://${ICON_NETWORK}/${ICON_BMC_ADDRESS}
    SRC_ENDPOINT=${ICON_ENDPOINT}
    DST_ADDRESS=btp://${HARDHAT_NETWORK}/${HARDHAT_BMC_ADDRESS}
    DST_ENDPOINT=${HARDHAT_ENDPOINT}
    KEY_STORE=${HARDHAT_KEYSTORE}
    KEY_PASSWORD=${HARDHAT_KEYPASS}
  ;;
  *)
    echo "Error: unknown target: $TARGET"
    exit 1
esac

${BRIDGE_BIN} \
    --src.address ${SRC_ADDRESS} \
    --src.endpoint ${SRC_ENDPOINT} \
    --dst.address ${DST_ADDRESS} \
    --dst.endpoint ${DST_ENDPOINT} \
    --key_store ${KEY_STORE} \
    --key_password ${KEY_PASSWORD} \
    start

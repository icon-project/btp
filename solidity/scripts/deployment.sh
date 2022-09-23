#!/bin/bash

source ./common.sh

vars="vars.json"
truffle_net_opt="--network $(props .deployment_network)"

echo "Generate temporary variable file..."
cp $vars $VARS

echo "Deploy bmc contracts..."
pushd $BASE_DIR/bmc
    export BMC_NETWORK_ID=$(props .deployment.network_id)
    rm -rf build .openzeppelin
    npm ci && \
    npx truffle compile --all && \
    npx truffle migrate --reset $truffle_net_opt && \
    npx truffle exec $truffle_net_opt ${CWD}/js/record-address.js bmc
    if [ "$?" -ne 0 ]
    then
        echo "Fail to deploy bmc contracts..."
        exit 1
    fi
popd

echo "Deploy bmv contracts..."
pushd $BASE_DIR/bmv
    export BMC_ADDR=$(props .addresses.bmc_periphery)
    export SRC_NETWORK_ID=$(props .target.network_id)
    export NETWORK_TYPE_ID=$(props .deployment.network_type_id)
    export FIRST_BLOCK_UPDATE=$(props .deployment.first_block_header)
    export SEQUENCE_OFFSET=$(props .deployment.sequence_offset)
    rm -rf build
    npm ci && \
    npx truffle compile --all && \
    npx truffle migrate --reset $truffle_net_opt && \
    npx truffle exec $truffle_net_opt ${CWD}/js/record-address.js bmv
    if [ "$?" -ne 0 ]
    then
        echo "Fail to deploy bmv contracts..."
        exit 1
    fi
popd

echo "Deploy bsh contracts..."
pushd $BASE_DIR/debug-bsh
    export BMC_ADDR=$(props .addresses.bmc_periphery)
    export BSH_SERVICE_NAME=$(props .deployment.service_name)
    rm -rf build
    npm ci && \
    npx truffle compile --all && \
    npx truffle migrate --reset $truffle_net_opt && \
    npx truffle exec $truffle_net_opt ${CWD}/js/record-address.js bsh
    if [ "$?" -ne 0 ]
    then
        echo "Fail to deploy bsh contracts..."
        exit 1
    fi
popd

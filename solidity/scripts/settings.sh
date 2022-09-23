#!/bin/bash

source ./common.sh

cwd=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)
base_dir=${cwd}/../

echo "Set BMC Variables..."
pushd $base_dir/bmc
    npx truffle exec --network $(props .deployment_network) ${cwd}/js/bmc-settings.js
popd

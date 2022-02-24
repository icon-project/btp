#!/bin/sh

set -e

source /server.sh

provision() {
    ensure_server_start
    cd ./config
    goloop gn gen --out genesis.json $GOLOOP_KEY_STORE
    
    echo $(cat genesis.json | jq -r '.*{"chain":{"fee":{"stepLimit":{"invoke":"0x10000000","query":"0x1000000"}}}}') > genesis.json
    
    goloop chain join --genesis_template genesis.json --channel icon --auto_start > chain_id
    goloop chain start icon
    server_stop
}

#!/bin/sh

set -e

source /server.sh

provision() {
    ensure_server_start
    cd ./config
    goloop gn gen --out genesis.json $GOLOOP_KEY_STORE
    
    echo $(cat genesis.json | jq -r '.*{"chain":{"fee":{"stepLimit":{"invoke":"0x12A05F200","query":"0x2faf080"}}}}') > genesis.json
    
    goloop chain join --genesis_template genesis.json --channel icon --auto_start > chain_id
    goloop chain start icon
    server_stop
}

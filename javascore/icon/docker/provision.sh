#!/bin/sh

set -e

source /server.sh

provision() {
    ensure_server_start
    cd ./config
    goloop gn gen --out genesis.json $GOLOOP_KEY_STORE
    goloop chain join --genesis_template genesis.json --channel icon --auto_start > chain_id
    goloop chain start icon
    server_stop
}

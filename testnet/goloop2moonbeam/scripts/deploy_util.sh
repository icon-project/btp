CONFIG_DIR=${CONFIG_DIR:-/btpsimple/config}
JAVASCORE_DIST_DIR=${JAVASCORE_DIST_DIR:-/btpsimple/contracts/javascore}

source goloop_rpc.sh
rpcch

latest_blocknumber_goloop() {
    goloop rpc lastblock | jq -r .height
}

latest_blocknumber_moonbase() {
    curl -s -X POST 'http://moonbase:9933' --header 'Content-Type: application/json' \
    --data-raw '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[], "id": 1}' | jq -r .result | xargs printf "%d\n"
}

latest_blocknumber_kusama() {
    curl -s -X POST 'https://kusama-rpc.polkadot.io' --header 'Content-Type: application/json' \
    --data-raw '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[], "id": 1}' | jq -r .result | xargs printf "%d\n"
}

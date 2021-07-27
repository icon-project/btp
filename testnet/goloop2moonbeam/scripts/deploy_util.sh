export CONFIG_DIR=${CONFIG_DIR:-/btpsimple/config}

source goloop_rpc.sh
rpcch

latest_block_goloop() {
    goloop rpc lastblock
}

latest_blocknumber_moonbase() {
    curl -s -X POST 'http://moonbase:9933' --header 'Content-Type: application/json' \
    --data-raw '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[], "id": 1}' | jq -r .result | xargs printf "%d\n"
}

latest_blocknumber_kusama() {
    # Doesn't work
    curl -s -X POST 'https://kusama-rpc.polkadot.io' --header 'Content-Type: application/json' \
    --data-raw '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[], "id": 1}' | jq -r .result | xargs printf "%d\n"
}

wait_file_created() {
    FILE_DIR=$1
    FILE_NAME=$2

    timeout=10
    while [ ! -f $FILE_DIR/$FILE_NAME ];
    do
        if [ "$timeout" == 0 ]; then
            echo "ERROR: Timeout while waiting for the file $FILE_NAME."
            exit 1
        fi
        sleep 1
        ((timeout--))

        echo "waiting for the output file: $FILE_NAME"
    done
}
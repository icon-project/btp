
latest_blocknumber_moonbase() {
    curl -s -X POST 'http://moonbase:9933' --header 'Content-Type: application/json' \
    --data-raw '{"jsonrpc":"2.0","method":"eth_blockNumber","params":[], "id": 1}' | jq -r .result | xargs printf "%d\n"
}

latest_blocknumber_moonbase
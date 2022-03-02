#!/bin/sh
deploy_near_bmc(){

    echo "Initializing all variables"

    echo "Setting near env to $NODE_ENV"

    echo "Creating $ACCOUNT_ID in local with $MASTER_ACCOUNT"

    near create-account $ACCOUNT_ID --initialBalance 100 --masterAccount $MASTER_ACCOUNT --keyPath $KEY_PATH

    echo "deploying Contract"

    near deploy --accountId $ACCOUNT_ID --wasmFile $WASMFILE \
    --initFunction new --initArgs '{"network": "0x1.near"}' \
    --masterAccount $MASTER_ACCOUNT --keyPath $KEY_PATH \
    --nodeUrl $NODE_URL
}

# deploy_near_bmv(){

#     echo "Initializing all variables"

#     echo "Setting near env to $NODE_ENV"

#     echo "Creating $ACCOUNT_ID in local with $MASTER_ACCOUNT"

#     near create-account $ACCOUNT_ID --initialBalance 100 --masterAccount $MASTER_ACCOUNT --keyPath $KEY_PATH

#     echo "deploying Contract"

#     near deploy --accountId $ACCOUNT_ID --wasmFile $WASMFILE \
#     --initFunction new --initArgs '{"network": "0x1.near"}' \
#     --masterAccount $MASTER_ACCOUNT --keyPath $KEY_PATH \
#     --nodeUrl $NODE_URL
# }

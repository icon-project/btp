# Create and manage keystore guild

## Platform preparation

### ICON

* goloop cli
    ```
    go get github.com/icon-project/goloop/cmd/goloop
    ```

* create new store, more [details](https://github.com/icon-project/goloop/blob/master/doc/goloop_cli.md#goloop-ks)
    ```
    goloop ks gen --out keystore.json --password $YOUR_PASSWORD
    ```
* import existing privite key

### EVM

* install ethkey cli
    ```
    go get github.com/ethereum/go-ethereum/cmd/ethkey
    ```
* install jq v1.6

  **Mac OSX**
    ```
    brew install jq
    ```

* create new store, more [details](https://github.com/ethereum/go-ethereum/tree/v1.10.5/cmd/ethkey)
    ```
    ethkey generate keystore.json
    Password: # typing $YOUR_PASSWORD
    Repeat password: # reapeating typing $YOUR_PASSWORD
    Address: # CLI will generate $YOUR_ACCOUNT_ADDRESS 

    # coinType requires by BTP project
    cat <<< $(jq '. += {"coinType":"evm"}' keystore.json) > keystore.json
    ```

* import existing privite key

### Password raw

After creating your keystore, BTP requires users to enter and raw password to a file and provide in configuration. The format of file is `keystore.secret`

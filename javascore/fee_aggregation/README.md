## FeeAggregation


### Requirements
- Install [ICON Local Node](https://github.com/icon-project/goloop/blob/master/doc/gochain_icon_local_node_guide.md)

### Build
```
$ ./gradlew build
$ ./gradlew optimizedJar
```
### Deploy contract
```
./gradlew deployToLocal -PkeystoreName=<your_wallet_json> -PkeystorePass=<password> 
```

### Tests
```
$ ./gradlew test
```

### Integration Tests
**NOTE!!**
You should update the config file before run integration tests
- With gochain, default:
```
# config/env.props
node.url=http://localhost:9082/api/v3

chain.nid=0x03
chain.godWallet=godWallet.json
chain.godPassword=gochain
```
```shell
# godWallet.json
{
  "address": "hxb6b5791be0b5ef67063b3c10b840fb81514db2fd",
  "id": "87323a66-289a-4ce2-88e4-00278deb5b84",
  "version": 3,
  "coinType": "icx",
  "crypto": {
    "cipher": "aes-128-ctr",
    "cipherparams": {
      "iv": "069e46aaefae8f1c1f840d6b09144999"
    },
    "ciphertext": "f35ff7cf4f5759cb0878088d0887574a896f7f0fc2a73898d88be1fe52977dbd",
    "kdf": "scrypt",
    "kdfparams": {
      "dklen": 32,
      "n": 65536,
      "r": 8,
      "p": 1,
      "salt": "0fc9c3b24cdb8175"
    },
    "mac": "1ef4ff51fdee8d4de9cf59e160da049eb0099eb691510994f5eca492f56c817a"
  }
}
```

- With goloop, config:
```
node.url=http://localhost:9080/api/v3/{channel}

chain.nid={{ nid in channel config }}
chain.godWallet=godWallet.json          # need update follow by channel config
chain.godPassword={{ password in channel config }}
```

**Run integration test**
```
$ ./gradlew integrationTest
```


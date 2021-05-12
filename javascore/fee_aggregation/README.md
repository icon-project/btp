## FeeAggregation


### Build
### Build & deploy contract

Open a terminal window
```
$ ./gradlew build
$ ./gradlew optimizedJar
# Deploy contract
$ make build GOCHAIN_LOCAL_ROOT=$(GOLOOP_ROOT) URI=$(URI of JSON-RPC API) KEY_STORE=$(KeyStore file for wallet) KEY_SECRET=$(Secret(password) file for KeyStore) NID=$(Network ID) STEP_LIMIT=$(StepLimit) CPS_ADDRESS=$(Contribution Proposal System Address) 

# Check result
$ make result GOCHAIN_LOCAL_ROOT=$(GOLOOP_ROOT) URI=$(URI of JSON-RPC API) HASH=$(Transaction hash)
```

### Test


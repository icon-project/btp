# Tutorial

## Introduction
This document describes how to setup a BTP network and interchain token transfer scenario.

## Preparation

### Requirements
* [Docker](https://docs.docker.com)
* goloop v0.9.6 or later

### blockchain
1. Start the server  
```shell
$ export CONFIG_DIR=/path/to/config
$ docker run -d --name goloop -p 9080:9080 \
  -v ${CONFIG_DIR}:/goloop/config \
  iconloop/goloop-icon
```
for follows, execute under `docker exec -ti --workdir /goloop/config goloop sh`.

install jq
```shell
# apk add jq
```

2. Create genesis  

```shell
# goloop gn gen --out src.genesis.json $GOLOOP_KEY_STORE
# goloop gn gen --out dst.genesis.json $GOLOOP_KEY_STORE
```

modify genesis for javaee
```shell
# echo $(cat src.genesis.json | jq -r '.*{"chain":{"fee":{"stepLimit":{"invoke":"0x10000000","query":"0x1000000"}}}}') > src.genesis.json
# echo $(cat dst.genesis.json | jq -r '.*{"chain":{"fee":{"stepLimit":{"invoke":"0x10000000","query":"0x1000000"}}}}') > dst.genesis.json
```

3. Join the chain

```shell
# goloop chain join --genesis_template src.genesis.json --channel src
# goloop chain join --genesis_template dst.genesis.json --channel dst
```

4. Start the chain   

```shell
# goloop chain start src
# goloop chain start dst
```

## Deploy Smart Contracts
### Package
zip each BTP-Smart-Contracts from project source, and copy files to `/path/to/config` which is mounted with `/goloop/config` of goloop container

```shell
$ make dist-py
$ mkdir -p ${CONFIG_DIR}/pyscore
$ cp build/contracts/pyscore/*.zip ${CONFIG_DIR}/pyscore/
```

package for javascore/bmc
```shell
$ make dist-java
$ mkdir -p ${CONFIG_DIR}/javascore
$ cp build/contracts/javascore/*.jar ${CONFIG_DIR}/javascore/
```

### Environment for JSON-RPC
To use `goloop` as json-rpc client, execute shell via `docker exec -ti --workdir /goloop/config goloop sh` on goloop container.  
For parse json-rpc response, install jq via `apk add jq` to goloop container.  
Prepare 'rpc.sh' file as below, and apply by `source rpc.sh`.  
Set keystore for json-rpc by `rpcks $GOLOOP_KEY_STORE $GOLOOP_KEY_SECRET`
```shell
rpchelp() {
  echo "rpcch CHANNEL"
  echo "rpcks KEYSTORE_PATH"
}

rpcch() {
  if [ ! "$1" == "" ]; then
    export GOLOOP_RPC_CHANNEL=$1
    URI_PREFIX=http://$(goloop system info -f '{{.Setting.RPCAddr}}')/api
    export GOLOOP_RPC_URI=$URI_PREFIX/v3/$GOLOOP_RPC_CHANNEL
    export GOLOOP_RPC_NID=$(goloop chain inspect $GOLOOP_RPC_CHANNEL --format {{.NID}})
    export GOLOOP_DEBUG_URI=$URI_PREFIX/v3d/$GOLOOP_RPC_CHANNEL
    export GOLOOP_RPC_STEP_LIMIT=${GOLOOP_RPC_STEP_LIMIT:-0x10000000}
  fi
  echo $GOLOOP_RPC_CHANNEL
}

rpcks() {
  if [ ! "$1" == "" ]; then
    export GOLOOP_RPC_KEY_STORE=$1
    if [ ! "$2" == "" ]; then
      if [ -f "$2" ]; then
        export GOLOOP_RPC_KEY_SECRET=$2
      else
        export GOLOOP_RPC_KEY_PASSWORD=$2
      fi
    fi
  fi
  echo $GOLOOP_RPC_KEY_STORE
}

export GOLOOP_CHAINSCORE=cx0000000000000000000000000000000000000000
```

### BMC
Deploy BMC contract to 'src' chain
```shell
# rpcch src
# echo "$GOLOOP_RPC_NID.icon" > net.btp.$(rpcch)
# goloop rpc sendtx deploy javascore/bmc.jar --content_type application/java \
    --param _net=$(cat net.btp.$(rpcch)) | jq -r . > tx.bmc.$(rpcch)
```

Extract BMC contract address from deploy result
```shell
# goloop rpc txresult $(cat tx.bmc.$(rpcch)) | jq -r .scoreAddress > bmc.$(rpcch)
```

Create BTP-Address
```shell
# echo "btp://$(cat net.btp.$(rpcch))/$(cat bmc.$(rpcch))" > btp.$(rpcch)
```

For 'dst' chain, same flows with replace 'src' to 'dst'.
```shell
# rpcch dst
# echo "$GOLOOP_RPC_NID.icon" > net.btp.$(rpcch)
# goloop rpc sendtx deploy javascore/bmc.jar --content_type application/java \
    --param _net=$(cat net.btp.$(rpcch)) | jq -r . > tx.bmc.$(rpcch)
# sleep 2
# goloop rpc txresult $(cat tx.bmc.$(rpcch)) | jq -r .scoreAddress > bmc.$(rpcch)
# echo "btp://$(cat net.btp.$(rpcch))/$(cat bmc.$(rpcch))" > btp.$(rpcch)
```

### BMV
To create parameters for deploy BMV contract 
```shell
# rpcch dst
# goloop rpc call --to $GOLOOP_CHAINSCORE --method getValidators| jq -r 'map(.)|join(",")' > validators.$(rpcch)
# echo "0x$(printf %x $(goloop chain inspect $(rpcch) --format {{.Height}}))" > offset.$(rpcch)
```

Deploy BMV contract to 'src' chain  
```shell
# rpcch src
# goloop rpc sendtx deploy javascore/bmv-icon.jar --content_type application/java \
    --param _bmc=$(cat bmc.$(rpcch)) \
    --param _net=$(cat net.btp.dst) \
    --param _validators=$(cat validators.dst) \
    --param _offset=$(cat offset.dst) \
     | jq -r . > tx.bmv.$(rpcch)
```

Extract BMV contract address from deploy result
```shell
# goloop rpc txresult $(cat tx.bmv.$(rpcch)) | jq -r .scoreAddress > bmv.$(rpcch)
```

For 'dst' chain, same flows with replace 'src' to 'dst' and 'dst' to 'src'.
```shell
# rpcch src
# goloop rpc call --to $GOLOOP_CHAINSCORE --method getValidators| jq -r 'map(.)|join(",")' > validators.$(rpcch)
# echo "0x$(printf %x $(goloop chain inspect $(rpcch) --format {{.Height}}))" > offset.$(rpcch)
# rpcch dst
# goloop rpc sendtx deploy javascore/bmv-icon.jar --content_type application/java \
    --param _bmc=$(cat bmc.$(rpcch)) \
    --param _net=$(cat net.btp.src) \
    --param _validators=$(cat validators.src) \
    --param _offset=$(cat offset.src) \
     | jq -r . > tx.bmv.$(rpcch)
sleep 2
# goloop rpc txresult $(cat tx.bmv.$(rpcch)) | jq -r .scoreAddress > bmv.$(rpcch)
```

### Token-BSH
Deploy Token-BSH contract to 'src' chain
```shell
# rpcch src
# goloop rpc sendtx deploy pyscore/token.zip \
    --param _bmc=$(cat bmc.$(rpcch)) | jq -r . > tx.token.$(rpcch)
```

Extract Token-BSH contract address from deploy result
```shell
# goloop rpc txresult $(cat tx.token.$(rpcch)) | jq -r .scoreAddress > token.$(rpcch)
```

For 'dst' chain, same flows with replace 'src' to 'dst'.
```shell
# rpcch dst
# goloop rpc sendtx deploy pyscore/token.zip \
    --param _bmc=$(cat bmc.$(rpcch)) | jq -r . > tx.token.$(rpcch)
# goloop rpc txresult $(cat tx.token.$(rpcch)) | jq -r .scoreAddress > token.$(rpcch)
```

### IRC-2.0 Token
Deploy IRC-2.0 Token contract to 'src' chain
```shell
# rpcch src
# goloop rpc sendtx deploy pyscore/irc2.zip \
    --param _name=IRC2Token \
    --param _symbol=I2T \
    --param _initialSupply=1000 \
    --param _decimals=18 \
    | jq -r . > tx.irc2.$(rpcch)
```

Extract IRC-2.0 Token contract address from deploy result
```shell
# goloop rpc txresult $(cat tx.irc2.$(rpcch)) | jq -r .scoreAddress > irc2.$(rpcch)
```

For 'dst' chain, same flows with replace 'src' to 'dst' and add `_owner` parameter. because IRC-2.0 Token contract of 'dst' chain is proxy.
```shell
# rpcch dst
# goloop rpc sendtx deploy pyscore/irc2.zip \
    --param _name=IRC2Token \
    --param _symbol=I2T \
    --param _initialSupply=0x3E8 \
    --param _decimals=0x12 \
    --param _owner=$(cat token.$(rpcch)) \
    | jq -r . > tx.irc2.$(rpcch)
# goloop rpc txresult $(cat tx.irc2.$(rpcch)) | jq -r .scoreAddress > irc2.$(rpcch)
```

## Configuration

### Register BMV
Register verifier of 'dst' chain to 'src' chain
```shell
# rpcch src
# goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
    --method addVerifier \
    --param _net=$(cat net.btp.dst) \
    --param _addr=$(cat bmv.$(rpcch)) \
    | jq -r . > tx.verifier.$(rpcch)
```

Register verifier of 'src' chain to 'dst' chain
```shell
# rpcch dst
# goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
    --method addVerifier \
    --param _net=$(cat net.btp.src) \
    --param _addr=$(cat bmv.$(rpcch)) \
    | jq -r . > tx.verifier.$(rpcch)
```

### Register Link
Register BTP-Address of 'dst' chain to 'src' chain
```shell
# rpcch src
# goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
    --method addLink \
    --param _link=$(cat btp.dst) \
    | jq -r . > tx.link.$(rpcch)
```

Register BTP-Address of 'src' chain to 'dst' chain
```shell
# rpcch dst
# goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
    --method addLink \
    --param _link=$(cat btp.src) \
    | jq -r . > tx.link.$(rpcch)
```

> To retrieve list of registered links, use `getLinks` method of BMC.  
> `# goloop rpc call --to $(cat bmc.$(rpcch)) --method getLinks`

### Configure Link
To use multiple-BMR for relay, should set properties of link via `BMC.setLinkRotateTerm`

Set properties of 'dst' link to 'src' chain
```shell
# rpcch src
# goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
    --method setLinkRotateTerm \
    --param _link=$(cat btp.dst) \
    --param _block_interval=0x3e8 \
    --param _max_agg=0x10 \
    | jq -r . > tx.setlink.$(rpcch)
```

Set properties of 'src' link to 'dst' chain
```shell
# rpcch dst
# goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
    --method setLinkRotateTerm \
    --param _link=$(cat btp.src) \
    --param _block_interval=0x3e8 \
    --param _max_agg=0x10 \
    | jq -r . > tx.setlink.$(rpcch)
```

> To retrieve properties of link, use `getStatus(_link)` method of BMC.
>
> ```shell
> # rpcch src
> # goloop rpc call --to $(cat bmc.src) --method getStatus --param _link=$(cat btp.dst)
>
> # rpcch dst
> # goloop rpc call --to $(cat bmc.dst) --method getStatus --param _link=$(cat btp.src)
> ```

### Register Token-BSH
Register Token service to BMC
```shell
# rpcch src
# goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
  --method addService \
  --param _svc=token \
  --param _addr=$(cat token.$(rpcch)) | jq -r . > tx.service.token.$(rpcch)

# rpcch dst
# goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
  --method addService \
  --param _svc=token \
  --param _addr=$(cat token.$(rpcch)) | jq -r . > tx.service.token.$(rpcch)
```

### Register IRC2Token
Register IRC 2.0 Token contract to Token-BSH
```shell
# rpcch src
# goloop rpc sendtx call --to $(cat token.$(rpcch)) \
  --method register \
  --param _name=IRC2Token \
  --param _addr=$(cat irc2.$(rpcch))

# rpcch dst
# goloop rpc sendtx call --to $(cat token.$(rpcch)) \
  --method register \
  --param _name=IRC2Token \
  --param _addr=$(cat irc2.$(rpcch))
```

> To retrieve list of registered token, use `tokenNames` method of Token-BSH.  
>
> `# goloop rpc call --to $(cat token.$(rpcch)) --method tokenNames`

### Register BMC-Owner and BMR
Create key store for BMC-Owner, BMR of both chain
```shell
# echo -n $(date|md5sum|head -c16) > src.secret
# goloop ks gen -o src.ks.json  -p $(cat src.secret)
# echo -n $(date|md5sum|head -c16) > dst.secret
# goloop ks gen -o dst.ks.json  -p $(cat dst.secret)
```

Register BMC-Owner to 'src' chain
```shell
# rpcch src
# goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
    --method addOwner \
    --param _addr=$(jq -r .address $(rpcch).ks.json)
```

BMC-Owner register BMR to 'src' chain (Address of BMR could be any keystore)
```shell
# rpcks src.ks.json src.secret
# goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
    --method addRelay \
    --param _link=$(cat btp.dst) \
    --param _addr=$(jq -r .address dst.ks.json)
```

For 'dst' chain, same flows with replace 'src' to 'dst'
```shell
# rpcks $GOLOOP_KEY_STORE $GOLOOP_KEY_SECRET
# rpcch dst
# goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
    --method addOwner \
    --param _addr=$(jq -r .address $(rpcch).ks.json)
# rpcks dst.ks.json dst.secret
# goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
    --method addRelay \
    --param _link=$(cat btp.src) \
    --param _addr=$(jq -r .address src.ks.json)
```

> To retrieve list of registered BMC-Owners, use `getOwners` method of BMC.  
> `# goloop rpc call --to $(cat bmc.$(rpcch)) --method getOwners`

> To retrieve list of registered relay of link, use `getRelays(_link)` method of BMC.  
>
> ```shell
> # rpcch src
> # goloop rpc call --to $(cat bmc.src) --method getRelays --param _link=$(cat btp.dst)`
>
> # rpcch dst
> # goloop rpc call --to $(cat bmc.dst) --method getRelays --param _link=$(cat btp.src)`
> ```

### Register relayer candidate : only for testing of ICON main network environment
Create key store for relayer
```shell
# echo -n $(date|md5sum|head -c16) > relayer.secret
# goloop ks gen -o relayer.ks.json  -p $(cat relayer.secret)
```

To register relayer candidate, bonding is required.
The account of 'god' transfer some icx to account of relayer candidate.
Assume 'src' chain as ICON
```shell
# rpcks $GOLOOP_KEY_STORE $GOLOOP_KEY_SECRET
# rpcch src
# goloop rpc sendtx transfer --to $(jq -r .address relayer.ks.json) --value 0x10
```

Register to 'src' chain
```shell
# rpcch src
# rpcks relayer.ks.json relayer.secret
# goloop rpc sendtx call --to $(cat bmc.$(rpcch)) \
  --method registerRelayer \
  --param _desc="first candidate for relayer" \
  --value 0x10
```

> To retrieve list of registered relayers, use `getRelayers` method of BMC.  
>
> `# goloop rpc call --to $(cat bmc.src) --method getRelayers`

## Start relay

Prepare 'btpsimple' docker image via `make btpsimple-image`

Start relay 'src' chain to 'dst' chain
```shell
$ docker run -d --name btpsimple_src --link goloop \
  -v ${CONFIG_DIR}:/btpsimple/config \
  -e BTPSIMPLE_CONFIG=/btpsimple/config/src.config.json \
  -e BTPSIMPLE_SRC_ADDRESS=$(cat ${CONFIG_DIR}/btp.src) \
  -e BTPSIMPLE_SRC_ENDPOINT=http://goloop:9080/api/v3/src \
  -e BTPSIMPLE_DST_ADDRESS=$(cat ${CONFIG_DIR}/btp.dst) \
  -e BTPSIMPLE_DST_ENDPOINT=http://goloop:9080/api/v3/dst \
  -e BTPSIMPLE_OFFSET=$(cat ${CONFIG_DIR}/offset.src) \
  -e BTPSIMPLE_KEY_STORE=/btpsimple/config/src.ks.json \
  -e BTPSIMPLE_KEY_SECRET=/btpsimple/config/src.secret \
  btpsimple
```

For relay of 'dst' chain, same flows with replace 'src' to 'dst' and 'dst' to 'src'.
```shell
$ docker run -d --name btpsimple_dst --link goloop \
  -v ${CONFIG_DIR}:/btpsimple/config \
  -e BTPSIMPLE_CONFIG=/btpsimple/config/dst.config.json \
  -e BTPSIMPLE_SRC_ADDRESS=$(cat ${CONFIG_DIR}/btp.dst) \
  -e BTPSIMPLE_SRC_ENDPOINT=http://goloop:9080/api/v3/dst \
  -e BTPSIMPLE_DST_ADDRESS=$(cat ${CONFIG_DIR}/btp.src) \
  -e BTPSIMPLE_DST_ENDPOINT=http://goloop:9080/api/v3/src \
  -e BTPSIMPLE_OFFSET=$(cat ${CONFIG_DIR}/offset.dst) \
  -e BTPSIMPLE_KEY_STORE=/btpsimple/config/dst.ks.json \
  -e BTPSIMPLE_KEY_SECRET=/btpsimple/config/dst.secret \
  btpsimple
```

> To retrieve status of relay, use `getStatus(_link)` method of BMC.  
> ```shell
> # rpcch src
> # goloop rpc call --to $(cat bmc.src) --method getStatus --param _link=$(cat btp.dst)
>
> # rpcch dst
> # goloop rpc call --to $(cat bmc.dst) --method getStatus --param _link=$(cat btp.src)
> ```

## Interchain Token Transfer
> To use `goloop` as json-rpc client, execute shell via `docker exec -ti --workdir /goloop/config goloop sh` on goloop container.

Create key store for Alice and Bob
```shell
# echo -n $(date|md5sum|head -c16) > alice.secret
# goloop ks gen -o alice.ks.json  -p $(cat alice.secret)
# echo -n $(date|md5sum|head -c16) > bob.secret
# goloop ks gen -o bob.ks.json  -p $(cat bob.secret)
```

Mint token to Alice
```shell
# rpcch src
# rpcks $GOLOOP_KEY_STORE $GOLOOP_KEY_SECRET
# goloop rpc sendtx call --to $(cat irc2.src) \
  --method transfer \
  --param _to=$(jq -r .address alice.ks.json) \
  --param _value=10
```

> To retrieve balance of Alice, use `balanceOf(_owner)` method of IRC-2.0 Token contract.  
>
> `# goloop rpc call --to $(cat irc2.src) --method balanceOf --param _owner=$(jq -r .address alice.ks.json)`

Alice transfer token to Token-BSH
```shell
# rpcch src
# rpcks alice.ks.json alice.secret
# goloop rpc sendtx call --to $(cat irc2.src) \
  --method transfer \
  --param _to=$(cat token.src) \
  --param _value=10
```

> To retrieve balance of Alice which is able to interchain-transfer, use `balanceOf(_owner)` method of Token-BSH.  
>
> `# goloop rpc call --to $(cat token.src) --method balanceOf --param _owner=$(jq -r .address alice.ks.json)`

Alice transfer token to Bob via Token-BSH
```shell
# rpcch src
# rpcks alice.ks.json alice.secret
# goloop rpc sendtx call --to $(cat token.src) \
  --method transfer \
  --param _tokenName=IRC2Token \
  --param _to=btp://$(cat net.btp.dst)/$(jq -r .address bob.ks.json) \
  --param _value=5
```

> To retrieve locked-balance of Alice, use `balanceOf(_owner)` method of Token-BSH.
>
> `# goloop rpc call --to $(cat token.src) --method balanceOf --param _owner=$(jq -r .address alice.ks.json)`
>
> To retrieve transferred balance of Bob which is, use `balanceOf(_owner)` method of Token-BSH.
>
> `# goloop rpc call --to $(cat token.dst) --method balanceOf --param _owner=$(jq -r .address bob.ks.json)`
>
> alice usable balance is 5, bob usable balance is 5

Bob withdraw usable token from Token-BSH
```shell
# rpcch dst
# rpcks bob.ks.json bob.secret
# goloop rpc sendtx call --to $(cat token.dst) \
  --method reclaim \
  --param _tokenName=IRC2Token \
  --param _value=5
```

> To retrieve balance of Bob which is, use `balanceOf(_owner)` method of IRC-2.0 Token contract.
>
> `# goloop rpc call --to $(cat irc2.dst) --method balanceOf --param _owner=$(jq -r .address bob.ks.json)`

## Docker-compose

Tutorial with [Docker-compose](https://docs.docker.com/compose/)  
Using javascore/bmc instead of pyscore/bmc

### Preparation
Prepare 'btpsimple' docker image via `make btpsimple-image` and Copy files from project source to `/path/to/tutorial`
```shell
$ make btpsimple-image
$ mkdir -p /path/to/tutorial
$ cp docker-compose/* /path/to/tutorial/
```

### Run chain and relay
`docker-compose up` will build `tutorial_goloop` docker image that provisioned of belows

* scripts files in `/goloop/bin`
* source chain and destination chain (channel name : `src`, `dst`) 
* transaction related files in `/goloop/config`
  - Transaction hash : `tx.<method>.<chain>`
  - SCORE Address : `<score>.<chain>`
  - BTP Address : `btp.<chain>`, `net.btp.<chain>`

And It creates containers for `goloop`, `btpsimple_src`, `btpsimple_dst` services

### Prepare wallet

> To use `goloop` as json-rpc client, execute shell via `$ docker-compose exec tutorial_goloop sh`

Create key store for Alice and Bob
```shell
# source /goloop/bin/keystore.sh
# ensure_key_store alice.ks.json alice.secret
# ensure_key_store bob.ks.json bob.secret
```

### Approve NativeCoin-BSH
> apply `# source /goloop/bin/nativecoin.sh` for transfer and retrieve balance

Alice for source chain
```shell
# rpcks alice.ks.json alice.secret
# irc31_approve src
```

Bob for destination chain
```shell
# rpcks bob.ks.json bob.secret
# irc31_approve dst
```

### Transfer native-coin
Mint native-coin of source chain to Alice
```shell
# rpcks $GOLOOP_KEY_STORE $GOLOOP_KEY_SECRET
# rpcch src
# goloop rpc sendtx transfer --to $(rpceoa alice.ks.json) --value 0x64
```

> To retrieve balance of Alice, use `# goloop rpc balance $(rpceoa alice.ks.json)`

Alice transfer native-coin of source chain to Bob of destination chain via NativeCoin-BSH
```shell
# rpcks alice.ks.json alice.secret
# nc_transfer src dst bob.ks.json src 0x64
```

> To retrieve locked-balance of Alice, `# nc_balance src src alice.ks.json | jq -r .locked`
> To retrieve transferred balance of Bob, use `# irc31_balance dst src bob.ks.json`

### Transfer native-coin represented as IRC31-Token 
Bob transfer irc31-token of destination chain to Alice of source chain via NativeCoin-BSH
```shell
# rpcks bob.ks.json bob.secret
# nc_transfer dst src alice.ks.json src 0x10
```

> To retrieve locked-balance of Bob,
> ```
> # nc_balance dst src bob.ks.json | jq -r .locked`
> ```
>
> To retrieve transferred balance of Alice, use
> ```shell
> # rpcch src
> # goloop rpc balance $(rpceoa alice.ks.json)
> ```
>
> To retrieve remained balance of Bob, use
> ```shell
> # irc31_balance dst src bob.ks.json
> ```

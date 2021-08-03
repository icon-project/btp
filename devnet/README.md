# BSC Private Network Setup

This directory contains docker image and predefined genesis required to run private Binance Smart Chain network. The
genesis file is based on BSC official testnet PoA genesis. 

It's configured for one sealer account to allow running single node.

## Build docker image

```
docker build --tag bsc-node docker --build-arg KEYSTORE_PASS=<SECRET>
```

Note: The docker folder contains test accounts which are copied during the docker build in the provisioning step.

See [this page](https://geth.ethereum.org/docs/interface/managing-your-accounts) for information on how to create Ethereum accounts.

## Start BSC docker node

```
docker run -d -p 8545:8545 -p 8546:8546 bsc-node
```

## Building from source (optional)

For ICON BTP purpose, we need to build from [PR](https://github.com/binance-chain/bsc/pull/118) branch 
containing SHA3-256 FIPS 202 hash precompile.

For detail build instructions please read [BSC readme](https://github.com/binance-chain/bsc) or
alternatively running the following script will build from the target branch using a docker image.

```./build.sh```
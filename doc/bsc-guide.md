# Binance Smart Chain BTP Guide (WIP)

## Overview

This document provides guide for Binance Smart Chain BTP development environment.

### Local Development

### Requirements

- GoLang 1.13+  
- OpenJDK 11
- Docker
- Goloop 0.9.7 (build from source)
- Node.js version >= 16.1.0 or above
- Binance Smart Chain

On macOS, you also need Xcode and Command Line Tools.

### Build goloop

#### Get the code
checkout version 0.9.7
```
git clone -b v0.9.7 https://github.com/icon-project/goloop 
```

#### Build executables
navigate to goloop directory
```
make
```

build goloop docker image
```
make goloop-image
```

This is sufficient for BTP purpose at this point, however for more details, please read [Goloop build guide] (https://github.com/icon-project/goloop/blob/master/doc/build.md)

### Build Binance Smart Chain docker

We will need a custom BSC build to include SHA3-256 FIPS 202 hash,
please read instructions [here](https://github.com/icon-project/btp/tree/btp_web3labs/devnet)
for information on how to build bsc node docker image.

### Build BTP Relay

### Get the code
currently, lives in Web3 Labs branch
```
git clone -b btp_web3labs https://github.com/icon-project/btp 
```

### Build executables
navigate to btp directory
```
make
```

### Build JavaScore Contracts
from btp directory, run
```
make dist-javascore
```
This will build dist contract jars for bmc, bmv, bsh and example irc2 token 

### Build Solidity Contracts

Build BSC BSH and BEP Token Service

```
make dist-sol
```
This will prepare copy of bmc, bmv and TokenBSH solidity contracts

Note: this step doesn't compile the contracts, this happens at later stage during the btp docker image setup

### Build BTP Simple Docker Image

```
make btpsimple-image
```
This will prepare copy of bmc, bmv and TokenBSH solidity contracts

### Running BTP BSC Docker

From <btp repo>/devnet/docker/icon-bsc project directory

Build with docker-compose using the following script
```
./build.sh
```

Once build is complete, start docker-compose
```
docker-compose up
```
If all successful, this should start docker network containing provisioned
goloop, binance smart chain and BSC ICON BTP relayer.

## Testnet

The current testnet setup is using unofficial BSC testnet hosted by Web3 Labs. 

| NID | BTP Address   | RPC Endpoint                                          |
|:-----|:--------|:-----------------------------------------------------|
| 0x97.bsc  | btp://bsc.0x79/0xAD50f33C3346F8e3403c510ee75FEBA1D904fa3F  | ws://35.214.59.124:8546                           |
| 0x03  | btp://bsc.0x79/0xAD50f33C3346F8e3403c510ee75FEBA1D904fa3F  | https://btp.net.solidwallet.io/api/v3/icon_dex

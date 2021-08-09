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
git clone -b 0.9.7 https://github.com/icon-project/btp 
```

#### Build executables
navigate to goloop directory
```
make
```

This is sufficient for BTP purpose at this point, however for more details, please read [Goloop build guide] (https://github.com/icon-project/goloop/blob/master/doc/build.md)

### Build BTP Relay

### Get the code
currently, lives in Web3 Labs branch
```
git clone -b web3_labs https://github.com/icon-project/btp 
```

### Build executables
navigate to btp directory
```
make
```

Build Binance Smart Chain docker

We will need a custom BSC build to include SHA3-256 FIPS 202 hash, 
please read instructions [here](https://github.com/icon-project/btp/tree/btp_web3labs/devnet)
for information on how to build bsc node docker image.

### Running BTP BSC Docker

From <btp repo>/devnet/docker/icon-bsc project directory

Build with docker-compose using the following script
```
./build
```

Once build is complete, start docker-compose
```
docker-compose up
```
If all successful, this should start docker network containing provisioned
goloop, binance smart chain and BSC ICON BTP relayer.

### Build JavaScore Contracts
 
Build BSC BMV 
   
https://github.com/icon-project/btp/tree/btp_web3labs/javascore

Note: we will be building and deploying BMC from ICONDAO repo

### Build Solidity Contracts

Build BSC BSH and BEP Token Service

https://github.com/icon-project/btp/tree/btp_web3labs/solidity/TokenBSH

Note: we will be building and deploying BMV & BMC from ICONDAO repo

## Testnet

The current testnet setup is using unofficial BSC testnet hosted by Web3 Labs. 

| NID | BTP Address   | RPC Endpoint                                          |
|:-----|:--------|:-----------------------------------------------------|
| 0x97.bsc  | btp://bsc.0x79/0xAD50f33C3346F8e3403c510ee75FEBA1D904fa3F  | ws://35.214.59.124:8546                           |
| 0x03  | btp://bsc.0x79/0xAD50f33C3346F8e3403c510ee75FEBA1D904fa3F  | https://btp.net.solidwallet.io/api/v3/icon_dex

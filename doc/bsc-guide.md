# Binance Smart Chain BTP Guide

## Overview

This document provides guide for Binance Smart Chain BTP development environment.

### Local Development

### Requirements

- GoLang 1.16
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

#### Get the code
currently, lives in Web3 Labs branch
```
git clone -b btp_web3labs https://github.com/icon-project/btp 
```

#### Build executables
navigate to btp directory
```
make
```

Note: 
- In case make fails with "missing go.sum entry" with go-ethereum, it can be fixed by manually adding `go mod download github.com/ethereum/go-ethereum`.

- If running `make` after the above command still encounters several "missing go.sum entry", running `go mod tidy` will fix the errors, then proceed with `make`.

### BUILD & RUN:
Follow Make build(Method 1) or step-by-step build (Method 2) to start the network and run the provision

### Method 1(Using Make)
To build and run using the make commands, navigate to  `<btp repo>/devnet/docker/icon-bsc` project directory
#### 1. Clean & remove artifacts
To remove the build folder & work folder which contains previous artifacts, logs and all the provision images(dist-javascore, dist-sol, btpsimple-image)
    
    make remove

#### 2. build docker images
To build the provision images(dist-javascore, dist-sol, btpsimple-image)
    
    make build

#### 3. Run network
To start the network and run the provision
    
    make run

Note:
 - A successfull build should have four docker conatiners (goloop, binancesmartchaincontainer, btp-icon, btp-bsc) started and running.
 - To try token transfer/ native token transfer examples on the btp network, please follow [token-transfer-guide.md](https://github.com/icon-project/btp/blob/btp_web3labs/doc/token-transfer-guide.md). Please wait for the entire provision to finish.

### (Or) Method 2 (Manual step-by-step build)
#### Build JavaScore Contracts
from btp directory, run
```
make dist-javascore
```
This will build dist contract jars for bmc, bmv, bsh and example irc2 token 

#### Build Solidity Contracts

Build BSC BSH and BEP Token Service

```
make dist-sol
```
This will prepare copy of bmc, bmv and TokenBSH solidity contracts

Note: this step doesn't compile the contracts, this happens at later stage during the btp docker image setup

#### Build BTP Simple Docker Image

```
make btpsimple-image
```
This will prepare copy of bmc, bmv and TokenBSH solidity contracts

#### Running BTP BSC Docker

From <btp repo>/devnet/docker/icon-bsc project directory

Build with docker-compose using the following script
```
./build.sh
```

Once build is complete, start docker-compose
```
docker-compose up -d
```
If all successful, this should start docker network containing provisioned
goloop, binance smart chain and BSC ICON BTP relayer.

## Setup Testnet

There is official BSC testnet, as alternative we will setup a dedicated private BSC testnet on one of the cloud providers

* AWS, GCD or similiar
* OS: Ubuntu 20.x/TLS or Debian
* Minimum configuration: CPU with 2+ cores, 4GB RAM, 30GB (enough for private)
8 MBit/sec download Internet service
  
### Setup private BSC

#### Get and build BSC docker

```
git clone https://github.com/web3labs/bsc-docker.git

cd bsc-docker

# Build all docker images
make build-simple

# Generate genesis.json, validators & bootstrap cluster data
# Once finished, all cluster bootstrap data are generated at ./storage
make bootstrap-simple

# Start cluster
make start-simple
```

#### Build and run BTP testnet docker

This can be done on local dev machine or another cloud VM:

1. Edit `env.variables.sh` and `docker-compose.testnet.yml` and update `BSC_RPC_URI` to point BSC IP address
endpoint.
   
e.g. `http://35.214.59.124:8545`
    
2. Build and run docker

```
cd ~/btp/devnet/docker/icon-bsc

# Build docker-compose
make build-testnet

# Run
make run-testnet
```

The current testnet setup is using unofficial BSC testnet hosted by Web3 Labs. 

| NID | BTP Address   | RPC Endpoint                                          |
|:-----|:--------|:-----------------------------------------------------|
| 0x97.bsc  | btp://bsc.0x79/0xAD50f33C3346F8e3403c510ee75FEBA1D904fa3F  | ws://35.214.59.124:8546                           |
| 0x03  | btp://bsc.0x79/0xAD50f33C3346F8e3403c510ee75FEBA1D904fa3F  | https://btp.net.solidwallet.io/api/v3/icon_dex

# End-to-End Testing Demo

In this demo, you will learn how to perform end-to-end testing between ICON and Hardhat (EVM-compatible) environment.

> **Note**
> The code in this folder is written specifically for the learning experience and is intended only for demonstration purposes.
> This demo code is not intended to be used in production.

## Prerequisites

To run the demo, the following software needs to be installed.

 * Node.js 18 (LTS) \[[download](https://nodejs.org/en/download/)\]
 * Docker compose (V2) \[[download](https://docs.docker.com/compose/install/)\]
 * OpenJDK 11 or above \[[download](https://adoptium.net/)\]
 * jq \[[download](https://github.com/stedolan/jq)\]
 * go \[[download](https://go.dev/doc/install)\]

## Install required packages

This is a one-time setup procedure.
Before moving to the next step, you need to install all required packages via `npm` command.

```
npm install
```

A new directory named `node_modules` is created in the current working directory.

## Setup ICON node

This is also a one-time setup procedure unless you delete the ICON node data directory (`./docker/icon/data`)
that preserves the setup procedure described below.

To enable BTP block feature on the local ICON node, you need to perform several steps like
decentralization, registering the node as a PRep, upgrading revision to 21, and so on.
Because it's difficult to perform these steps manually, we provide a handy script for them.

Before executing the script, you need to start nodes first.

```
make start-nodes
```

Then, run the following command to setup the local ICON node.

```
make setup-node
```

You will be seeing a series of commands executed. But at first, the script will be stopped with the following message.

```
Error: ICON: need to wait until the next term for decentralization
```

As the error message said, you need to wait until the next term
(default is 100, see `termPeriod` in `./docker/icon/config/icon_config.json`)
The node setup would be completed if you can see the following message when you run the `make setup-node` command again.
```
ICON: node setup completed
```

## Build and Deploy contracts

Now it's time to build and deploy BTP and DApp contracts on the nodes.

To build all contracts, run the following command.

```
make build-all
```

It compiles both Java and Solidity contracts and generates artifacts for later deployment.

If no build errors were found, then you can deploy all the contracts using the following command.

```
make deploy-all
```
> **Note**
> By default, above command uses BMV for BTPBlock.
> If you want to use BMV for Bridge, add `BMV_BRIDGE=true` environment variable as follows.
> ```
> BMV_BRIDGE=true make deploy-all
> ```

All contracts (BMC, BMV, xCall and DApp) have now been successfully deployed and linked on both the ICON and Hardhat chains.
The generated file, `deployments.json`, contains information needed to interact with the relays,
such as contract addresses and the network address of each chain.

The next step is to run a demo scenario script to verify the message delivery between two chains.

## Run Demo scenarios

Before running the demo scenario script, you need to spin up relay server to forward BTP messages to each chain.
Multiple terminal windows are required to complete these next steps.

Open a terminal window and run the following command to start the relay.

```
./relay.sh hardhat
```


> **Note**
> If you deployed BMV for Bridge in the previous step, add `BMV_BRIDGE=true` environment variable as follows.
> ```
> BMV_BRIDGE=true ./relay.sh hardhat
> ```

You can now run the demo scenario script via the following command.

```
make run-demo
```

## Appendix

### Directory layout
| Directory                       | Description                                      |
|:--------------------------------|:-------------------------------------------------|
| dapp-sample                     | Java contract for DApp sample                    |
| docker                          | Docker-related files for ICON and Hardhat chains |
| scripts                         | Scripts for setup, deployment and demo scenarios |
| solidity                        | Root directory for compiling Solidity contracts  |
| solidity/contracts/dapp-sample  | Solidity contract for DApp sample                |

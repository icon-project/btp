# BTP (Block Transmission Protocol) Relay System

## Introduction

We need to build a usable [BTP](doc/btp.md) Relay System which can deliver digital tokens between multiple chains.

Target chains
* ICON (goloop)
* Polkadot parachain

Terminologies

| Word            | Description                                                                                            |
|:----------------|:-------------------------------------------------------------------------------------------------------|
| BTP             | Blockchain Transmission Protocol, [ICON BTP Standard](https://github.com/icon-project/IIPs/blob/master/IIPS/iip-25.md) defined by ICON. |
| BTP Message     | A verified message which is delivered by the relay                                                     |
| Service Message | A payload in a BTP message                                                                             |
| Relay Message   | A message including BTPMessages with proofs for that, and other block update messages.                 |
| NetworkAddress  | Network Type and Network ID <br/> *0x1.icon* <br/> *0x1.icon*                                        |
| ContractAddress | Addressing contract in the network <br/> *btp://0x1.icon/cx87ed9048b594b95199f326fc76e76a9d33dd665b* |

> BTP Standard

### Components

* [BTP Message Verifier(BMV)](doc/bmc.md) - smart contract
  - Update blockchain verification information
  - Verify delivered BTP message and decode it

* [BTP Message Center(BMC)](doc/bmv.md) - smart contract
  - Receive BTP messages through transactions.
  - Send BTP messages through events.

* [BTP Service Handler(BSH)](doc/bsh.md) - smart contract
  - Handle service messages related to the service.
  - Send service messages through the BMC

* [BTP Message Relay(BMR)](doc/bmr.md) - external software
  - Monitor BTP events
  - Gather proofs for the events
  - Send BTP Relay Message

### Blockchain specifics

* [ICON](doc/icon.md)
* [Polkadot parachain with frontier](doc/polkadot_parachain_with_frontier.md)

## BTP Project

### Documents

* [Build Guide](doc/build.md)
* [Tutorial](doc/tutorial.md)
* [btpsimple command line](doc/btpsimple_cli.md)

### Layout

| Directory                | Description  |
|:--------------------|:-------|
| /cmd           |   Root of implement of BMR |
| /cmd/btpsimple           |   Reference implement of BMR. only provide unidirectional relay. (golang) |
| /chain/   |   BMR module interface and common codes |
| /chain/`<chain>`    | Implement of BMR module (`Sender`,`Receiver`), `<chain>` is name of blockchain |
| /common | Common codes (golang) |
| /doc | Documents |
| /docker | Docker related resources |
| /docker-compose | Setup of local network for running e2e of BTP |
| /`<env>` | Root of implement of BTP smart contracts, `<env>` is name of smart contract execution environment |
| /`<env>`/bmc | Implement of BMC smart contract |
| /`<env>`/bmv | Root of implement of BMV smart contract |
| /`<env>`/bmv/`<src>` | Implement of BMV smart contract, `<src>` is name of source blockchain |
| /`<env>`/lib | Library for execution environment |
| /`<env>`/`<svc>` | Root of implement of BSH smart contract, `<svc>` is name of BTP service |
| /`<env>`/token_bsh | Reference implement of BSH smart contract for Interchain-Token transfer service |
| /`<env>`/token_bsh/sample/irc2_token | Implement of IRC-2.0 smart contract, example for support legacy smart contract |

### BTP implement for ICON blockchain

#### BMR module
| Directory                | Description  |
|:--------------------|:-------|
| /chain/icon    | BMR module for ICON blockchain |
| /chain/pra    | BMR module for Polkadot Parachain blockchain |
| /chain/pra/receiver_relay*    | BMR module to listen for Polkadot Relay blockchain |
| *_test.go    | BMR files to perform unit test for the *.go file |

#### Python SCORE of ICON
| Directory                | Description  |
|:--------------------|:-------|
| /pyscore | Implement of BTP smart contracts for Python SCORE of ICON blockchain |
| /pyscore/bmv/icon | Implement of BMV smart contract for ICON blockchain |
| /pyscore/lib | BTP interface and common codes for Python SCORE |
| /pyscore/lib/icon | ICON related common codes |

#### Java SCORE of ICON
| Directory                | Description  |
|:--------------------|:-------|
| /javascore | Implement of BTP smart contracts for Java SCORE of ICON blockchain |
| /javascore/bmc | Implement of BMC smart contract |
| /javascore/bmv/icon | Implement of BMV smart contract for ICON blockchain |
| /javascore/lib | BTP interface and common codes for Java SCORE |

#### Java SCORE of Parachain

| Directory                | Description  |
|:--------------------|:-------|
| /javascore/bmv/parachain | Implement of BMV smart contract for moonbeam parachain |
| /javascore/bmv/sovereignChain | Implement of BMV smart contract for edgeware parachain |
| /javascore/bmv/eventDecoder | Implement of event decoder for substrate chain |
| /javascore/bmv/helper | Script to get parachain initialize parameters and deploy contract |
| /javascore/lib | BTP interface and common codes for Java SCORE |

#### Solidity of ICON

| Directory                | Description  |
|:--------------------|:-------|
| /solidity | Implement of BTP solidity smart contracts of ICON blockchain |
| /solidity/bmc | Implement of BMC solidity smart contract |
| /solidity/bmv | Implement of BMV solidity smart contract for ICON blockchain |
| /solidity/bsh | Implement of BMC solidity smart contract |
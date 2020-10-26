# BTP (Block Transmission Protocol) Relay System

## Introduction

We need to build a usable BTP Relay System which can deliver digital tokens between multiple chains.

Target chains
* ICON (loopchain)
* ICONEE (goloop)

Terminologies

| Word            | Description                                                                                            |
|:----------------|:-------------------------------------------------------------------------------------------------------|
| BTP             | Blockchain Transmission Protocol, defined by ICON                                                      |
| BTP Message     | A verified message which is delivered by the relay                                                     |
| Service Message | A payload in a BTP message                                                                             |
| Relay Message   | A message including BTPMessages with proofs for that, and other block update messages.                 |
| NetworkAddress  | Network Type and Network ID <br/> *0x1.icon* <br/> *0x1.iconee*                                        |
| ContractAddress | Addressing contract in the network <br/> *btp://0x1.iconee/cx87ed9048b594b95199f326fc76e76a9d33dd665b* |


## Components

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

## Blockchain specifics
* [ICON](doc/icon.md)
* [ICON Enterprise Edition](doc/iconee.md)

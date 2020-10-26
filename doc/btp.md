# Blockchain Transmission Protocol

## Introduction

Blockchain records lots of verifiable data.
With this verifiable data, we may construct a network communicatingBTP Message
verifiable messages between blockchains without direct connection.

The relay delivers messages between blockchains. Each messages are verified
by smart contract, and also it sends messages for the reply.

Blockchain Transmission Protocol is used to deliver messages between blockchains.
A Relay delivers BTP messages between blockchains and smart contracts will verify and decode the delivered messages according to the services.
Complex and heavy functions of smart contracts can be shared if they are implemented according to standardized interfaces. Smart contracts are also more extensible staying compliant to the standards.


## Motivation

BTP Messages from multiple services are delivered to multiple blockchains.
BTP smart contracts to support multiple services and blockchains.
BTP Message Center(BMC) is the center of smart contracts.

For each blockchain, BTP Message Verifier(BMV) verifies the relay message and decodes it into standardized messages (BTP Messages).

For each service, BTP Message Handler(BSH) handles received messages of the service and sends messages through the BTP Message Center(BMC).


## Terminology
  
* [Network Address](#network-address)

  A string to identify blockchain network
  
* [BTP Address](#btp-address)

  A string of URL for locating an account of the blockchain network
  
* [Relay Message](#relay-message)

  A message that a relay sends to the blockchain.
  
* [BTP Message](#btp-message)

  Standardized messages delivered between different blockchains

* [BTP Message Center(BMC)](bmc.md)

  BMC accepts messages from a relay (Relay Messages).
  A Relay Message contains standardized messages(BTP Messages) and proof of existence for these messages.
  Corresponding BMV will verify and decode the Relay Message, then the BMC will process the BTP Messages.

  If the destination of the message isn't current BMC, then it's sent to the next BMC to reach its destination.
  If current BMC is the destination, then it's dispatched to the corresponding BSH.
  If the message cannot be processed, then it sends an error back to the source.


* [BTP Message Verifier(BMV)](bmv.md)

  BMV verifies a Relay Message and decodes it into BTP Messages.


* [BTP Service Handler(BSH)](bsh.md)

  BSH handles BTP Messages of the service. It also sends messages according to different service scenarios.


* [BTP Message Relay(BMR)](bmr.md)

  The software relays Relay Message between blockchains


### Network Address

A string to identify blockchain network

```
<NID>.<Network System>
```

**Network System**:
Short name of the blockchain network system.

| Name   | Description          |
|:-------|:---------------------|
| icon   | Loopchain for ICON   |
| iconee | Loopchain Enterprise |

**NID**:
ID of the network in the blockchain network system.

> Example

| Network Address | Description                                  |
|:----------------|:---------------------------------------------|
| `0x1.icon`      | ICON Network with nid="0x1" <- ICON Main-Net |
| `0x1.iconee`    | Loopchain Enterprise network with nid="0x1"  |

### BTP Address

A string of URL for locating an account of the blockchain network

> Example
```
btp://<Network Address>/<Account Identifier>
```
**Account Identifier**:
Identifier of the account including smart contract.
It should be composed of URL safe characters except "."(dot).

> Example
```
btp://0x1.icon/hxc0007b426f8880f9afbab72fd8c7817f0d3fd5c0
btp://0x1.iconee/cx429731644462ebcfd22185df38727273f16f9b87
```

It could be expanded to other resources.

### BTP Message

A message delivered across blockchains.

| Name | Type    | Description                                          |
|:-----|:--------|:-----------------------------------------------------|
| src  | String  | BTP Address of source BMC                            |
| dst  | String  | BTP Address of destination BMC                       |
| svc  | String  | name of the service                                  |
| sn   | Integer | serial number of the message                         |
| msg  | Bytes   | serialized bytes of Service Message or Error Message |

if **sn** is negative, **msg** should be Error Message.
It would be serialized in [RLP serialization](#rlp-serialization).


### Error Message

A message for delivering error information.

| Name | Type    | Description   |
|:-----|:--------|:--------------|
| code | Integer | error code    |
| msg  | String  | error message |

It would be serialized in [RLP serialization](#rlp-serialization).


### RLP serialization

For encoding [BTP Message](#btp-message) and [Error Message](#error-message), it uses Recursive Length Prefix (RLP).
RLP supports bytes and list naturally.
Here are some descriptions about other types.

#### String

It uses UTF-8 encoded bytes of the string.
There is no termination bytes.

#### Integer

It uses the shortest form of two's complemented bytes representations.
If it's negative, the highest bit of the first byte should be 1.

> Example

| Value | Encoded bytes |
|:------|:--------------|
| 0     | 0x00          |
| -1    | 0xff          |
| -128  | 0x80          |

### Relay Message

It's used to deliver BTP Messages along with other required contents. Normally, it contains the following.

* BTP Messages along with their proof of existence
* Trust information updates along with their proof of consensus

The relay gathers the information through APIs of a source blockchain system and its internal database. The actual content of the message is decided according to the blockchain system and BMV implementation.


## Components

Following diagram shows an example, delivering a message from left
blockchain to the other blockchain.

![btp_components](./img/btp_components.svg)

In the diagram, there are a few components.

* [BTP Service Handler(BSH)](bsh.md)
* [BTP Message Center(BMC)](bmc.md)
* [BTP Message Verifier(BMV)](bmv.md)

### Message delivery.

1. BSH sends an Service Message through BMC.

   * BSH calls [BMC.sendMessage](bmc.md#sendmessage) with followings.

     | Name    | Type    | Description                                   |
     |:--------|:--------|:----------------------------------------------|
     | _to     | String  | Network Address of the destination blockchain |
     | _svc    | String  | Name of the service.                          |
     | _sn     | Integer | Serial number of the message.                 |
     | _msg    | Bytes   | Service message to be delivered.              |

   * BMC lookup the destination BMC belonging to *_to*.
     If there is no known BMC to the network, then it fails.

   * BMC builds an BTP Message.

     | Name | Type    | Description                                   |
     |:-----|:--------|:----------------------------------------------|
     | src  | String  | BTP Address of current BMC                    |
     | dst  | String  | BTP Address of destination BMC in the network |
     | svc  | String  | Given service name                            |
     | sn   | Integer | Given serial number                           |
     | msg  | Bytes   | Given service message                         |

   * BMC decide the next BMC according to the destination.
     If there is no route to the destination BMC.

   * BMC generates an event with BTP Message.
   
     | Name  | Type    | Description                                |
     |:------|:--------|:-------------------------------------------|
     | _next | String  | BTP Address of the next BMC                |
     | _seq  | Integer | Sequence number of the msg to the next BMC |
     | _msg  | Bytes   | Serialized BTP Message                     |

2. The BTP Message Relay(BMR) detects events.
   * The relay detects [BMC.Message](bmc.md#message) through various ways.
   * The relay can confirm that it occurs and it's finalized.
   
3. BMR gathers proofs
   * Relay gathers proofs of the event(POE)s
     - Proof for the new block
     - Proof for the event in the block
   * Relay builds Relay Message including followings.
     - Proof of the new events
     - New events including the BTP Message.
   * Relay calls [BMC.handleRelayMessage](bmc.md#handlerelaymessage)
     with built Relay Message.
     
   | Name  | Type   | Description                                     |
   |:------|:-------|:------------------------------------------------|
   | _prev | String | BTP Address of the previous BMC                 |
   | _msg  | Bytes  | serialized Relay Message including BTP Messages |
     
4. BMC handles Relay Message

   * It finds BMV for the network address of the previous BMC.
   * It gets the sequence number of the next message from the source network.
   * BMC calls [BMV.handleRelayMessage](bmv.md#handlerelaymessage)
     to decode Relay Message and get a list of BTP Messages.
     
     | Name  | Type    | Description                                               |
     |:------|:--------|:----------------------------------------------------------|
     | _bmc  | String  | BTP Address of the current BMC                            |
     | _prev | String  | BTP Address of given previous BMC                         |
     | _seq  | Integer | Next sequence number of the BTP Message from previous BMC |
     | _msg  | Bytes   | The Relay Message                                         |

5. BMV decodes Relay Message

   * It verifies and decodes Relay Message, then returns a list of
     BTP Messages.
   * If it fails to verify the message, then it fails.
   * The events from the previous BMC to the current BMC will be processed.
   * The events should have proper sequence number, otherwise it fails.

6. BSH handles Service Messages

   * BMC dispatches BTP Messages.
   * If the destination BMC isn't current one, then it locates
     the next BMC and generates the event.
   * If the destination BMC is the current one, then it locates BSH
     for the service of the BTP Message.
   * Calls [BSH.handleBTPMessage](bsh.md#handlebtpmessage) if
     the message has a positive value as *_sn*.

     | Name  | Type    | Description                           |
     |:------|:--------|:--------------------------------------|
     | _from | String  | Network Address of the source network |
     | _svc  | String  | Given service name                    |
     | _sn   | Integer | Given serial number                   |
     | _msg  | Bytes   | Given service message                 |
     
   * Otherwise, it calls [BSH.handleBTPError](bsh.md#handlebtperror).
   
     | Name  | Type    | Description                                    |
     |:------|:--------|:-----------------------------------------------|
     | _src  | String  | BTP Address of the BMC that generated the error|
     | _svc  | String  | Given service name                             |
     | _sn   | Integer | Given serial number                            |
     | _code | Integer | Given error code                               |
     | _msg  | String  | Given error message                            |


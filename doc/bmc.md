# BTP Message Center(BMC)

## Introduction

BTP Message Center is a smart contract which builds BTP Message and
sends it to BTP Message Relay and handles Relay Message from the other.

## Setup

1. Registers [BSH](bsh.md)s for the services.
   (BSH should be deployed before the registration)
2. Registers [BMV](bmv.md)s for the directly connected blockchains.
   (BMV should be deployed before the registration)
3. Add links, BMCs of directly connected blockchains
4. Add routes to other BMCs of in-directly connected blockchains

## Send a message

BSH sends a message through [BMC.sendMessage](bmc.md#sendmessage).
It accepts the requests from the registered BTP Service Handler(BSH).
Of course, if service name of those requests is different from
registration, then they will be rejected.

Then it builds a BTP Message from the request.
1. Decide destination BMC from given Network Address
2. Fill in other information from parameters.
3. Serialize them for sending.

Then it tries to send the BTP Message.
1. Decide next BMC from the destination referring routing information.
2. Get sequence number corresponding to the next.
3. Emit the event, [Message](#message) including the information.

The event will be monitored by the Relay, it will build Relay Message
for next BMC.

## Receive a message

It receives the Relay Message, then it tries to decode it with registered
BMV. It may contain multiple BTP Messages.
It dispatches received BTP Messages one-by-one in the sequence.

If it is the destination, then it tries to find the BSH for the
service, and then calls [BSH.handleBTPMessage](bsh.md#handlebtpmessage).
It calls [BSH.handleBTPError](bsh.md#handlebtperror) if it's an error.

If it's not the destination, then it tries to send the message to
the next route.

If it fails, then it replies an error.
BTP Message for error reply is composed of followings.
* sn : negated serial number of the message.
* dst : BTP Address of the source.
* src : BTP Address of the BMC.
* msg : Error Message including error code and message.

## Interface

### Writable methods

#### handleRelayMessage
* Params
  - _prev: String ( BTP Address of the BMC generates the message )
  - _msg: String ( base64 encoded string of serialized bytes of Relay Message )
* Description:
  - It verify and decode RelayMessage with BMV, and dispatch BTP Messages
    to registered BSHs
  - It's allowed to be called by registered Relay.

#### sendMessage
* Params
  - _to: String ( Network Address of destination network )
  - _svc: String ( name of the service )
  - _sn: Integer ( serial number of the message, it should be positive )
  - _msg: Bytes ( serialized bytes of Service Message )
* Description:
  - It sends the message to specific network.
  - It's allowed to be called by registered BSHs.
  
#### addService
* Params
  - _svc: String (the name of the service)
  - _addr: Address (the address of the smart contract handling the service)
* Description:
  - It registers the smart contract for the service.
  - It's called by the operator to manage the BTP network.
  
#### removeService
* Params
  - _svc: String (the name of the service)
* Description:
  - It de-registers the smart contract for the service.
  - It's called by the operator to manage the BTP network.
  
#### addVerifier
* Params
  - _net: String (Network Address of the blockchain )
  - _addr: Address (the address of BMV)
* Description
  - Registers BMV for the network.
  - It's called by the operator to manage the BTP network.
  
#### removeVerifier
* Params
  - _net: String (Network Address of the blockchain )
* Description
  - De-registers BMV for the network.
  - It may fail if it's referred by the link.
  - It's called by the operator to manage the BTP network.
  
#### addLink
* Params
  - _link: String (BTP Address of connected BMC)
* Description
  - If it generates the event related with the link, the relay shall
    handle the event to deliver BTP Message to the BMC.
  - If the link is already registered, or its network is already
    registered then it fails.
  - If there is no verifier related with the network of the link,
    then it fails.
  - It initializes status information for the link.
  - It's called by the operator to manage the BTP network.
    
#### removeLink
* Params
  - _link: String (BTP Address of connected BMC)
* Description
  - It removes the link and status information.
  - It's called by the operator to manage the BTP network.
  
#### addRoute
* Params
  - _dst: String ( BTP Address of the destination BMC )
  - _link: String ( BTP Address of the next BMC for the destination )
* Description:
  - Add route to the BMC.
  - It may fail if there more than one BMC for the network.
  - It's called by the operator to manage the BTP network.
  
#### removeRoute
* Params
  - _dst: String ( BTP Address of the destination BMC )
* Description:
  - Remove route to the BMC.
  - It's called by the operator to manage the BTP network.
  
### Read-only methods

#### getServices
* Description
  - Get registered services.
* Returns
  - A dictionary with the name of the service as key and address of the BSH
    related with the service as value.
    ```json
    {
      "token": "cx72eaed466599ca5ea377637c6fa2c5c0978537da"
    }
    ```

#### getVerifiers
* Description
  - Get registered verifiers.
* Returns
  - A dictionary with the Network Address as a key and smart contract
    address of the BMV as a value.
    ```json
    {
        "0x1.iconee": "cx72eaed466599ca5ea377637c6fa2c5c0978537da"
    }
    ```

#### getLinks
* Description
  - Get registered links.
* Returns
  -  A list of links ( BTP Addresses of the BMCs )
  ```json
  [ "btp://0x1.iconee/cx9f8a75111fd611710702e76440ba9adaffef8656" ]
  ```
  
#### getRoutes
* Description:
  - Get routing information.
* Return
  - A dictionary with the BTP Address of the destination BMC as key and
    the BTP Address of the next as value.
    ```json
    {
      "btp://0x2.iconee/cx1d6e4decae8160386f4ecbfc7e97a1bc5f74d35b": "btp://0x1.iconee/cx9f8a75111fd611710702e76440ba9adaffef8656"
    }
    ```
    
#### getStatus
* Params
  - _link: String ( BTP Address of the connected BMC )
* Description:
  - Get status of BMC.
  - It's used by the relay to resolve next BTP Message to send.
  - If target is not registered, it will fail.
* Return
  - The object contains followings fields.
  
    | Field    | Type    | Description                                      |
    |:---------|:--------|:-------------------------------------------------|
    | tx_seq   | Integer | next sequence number of the next sending message |
    | rx_seq   | Integer | next sequence number of the message to receive   |
    | verifier | Object  | status information of the BMV                    |
  
  
### Events

#### Message
* Indexed: 1
* Params
  - _next: String ( BTP Address of the BMC to handle the message )
  - _seq: Integer ( sequence number of the message from current BMC to the next )
  - _msg: Bytes ( serialized bytes of BTP Message )
* Description
  - It sends the message to the next BMC.
  - The relay monitors this event.


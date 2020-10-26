# BTP Service Handler(BSH)

## Introduction

It may send messages through BTP Message Center(BMC) from any user
request. Of course, the request may come from other smart contracts.
It also have responsibility to handle the message from other BSHs.

BSH can communicate other BSHs with same service name.
If there is already the service using same name, then it should choose
other name for the service when it registers a new service.
Of course, if it wants to be a part of the service, then it should
use same name. And also it follows the protocol of the service.

Before it's registered to the BMC, it can't send an message, and also
it won't handle the message from others.
To be BSH, followings are required.

1. Implements the interface
2. Registered to the BMC through [BMC.addService](bmc.md#addservice)

After the registration, it may send messages through
[BMC.sendMessage](bmc.md#sendmessage).
If there is an error while it delivers the message, then it will
return error information though [handleBTPError](#handlebtperror).
If it's successfully delivered, then BMC will call
[handleBTPMessage](#handlebtpmessage) of the target BSH.
While it processes the message, it may reply though
[BMC.sendMessage](bmc.md#sendmessage).

## Security

It should not handle messages or errors from other contract
except the BMC.
BMC also accepts only the service messages from registered BSH.
Of course, BSH may have other APIs, but APIs related with BMC are
only called by BMC.

## Interface

### Writable methods

#### handleBTPMessage
* Description
  - Handle BTP Message from other blockchain.
  - Accept the message only from the BMC.
  - If it fails, then BMC will generate BTP Message including
    error information, then it would be delivered to the source.
* Params
  - _from: String ( Network Address of source network )
  - _svc: String ( name of the service )
  - _sn: Integer ( serial number of the message )
  - _msg: Bytes ( serialized bytes of ServiceMessage )

#### handleBTPError
* Description
  - Handle the error on delivering the message.
  - Accept the error only from the BMC.
* Params
  - _src: String ( BTP Address of BMC generates the error )
  - _svc: String ( name of the service )
  - _sn: Integer ( serial number of the original message )
  - _code: Integer ( code of the error )
  - _msg: String ( message of the error )


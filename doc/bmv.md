# BTP Message Verifier(BMV)

## Introduction

BTP Message Verifier verify and decode Relay Message to
[BTP Message](btp.md#btp-message)s.
Relay Message is composed of both BTP Messages and with proof of
existence of BTP Messages.

For easy verification, it may update trust information for the
followed events. Most of implementations may track the hashes of block
headers.
 
If the blockchain system provide proof of absence of the BTP Messages,
then it just sustains latest one. It updates the hash only if it sees
proof of absence of further BTP Messages in the block.

But most blockchain system doesn't provide the proof of absence of data.
So, they need to provide the method to verify any of old hashes.

Merkle Accumulator can be used for verifying old hashes.
BMV sustains roots of Merkle Tree Accumulator, and the relay
will sustain all elements of Merkle Tree Accumulator. The relay
may make the proof of any one of old hashes.
So, even if byzantine relay updated the trust information with the
proof of new block, normal relay can send BTP Messages in
the past block with the proof.

## Interface

### Writable methods

#### handleRelayMessage

* Description
  - Decodes Relay Message and process BTP Messages
  - If there is an error, then it sends a BTP Message containing
    Error Message
  - It ignores BTP Messages with old sequence numbers. But if it
    sees a BTP Message with future sequence number, it should fail.
* Params
  - _bmc: String ( BTP Address of the BMC handles the message )
  - _prev: String ( BTP Address of the BMC generates the message )
  - _seq: Integer ( next sequence number to get a message )
  - _msg: Bytes ( serialized bytes of Relay Message )
* Returns
  - List of serialized bytes of a [BTP Message](btp.md#btp-message)

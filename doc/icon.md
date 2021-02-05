# ICON

## Introduction

***TODO***

## BMV Trust

| Name        | Type            | Description |
|:------------|:----------------|:------------|
| NextHeight  | Integer         |             |
| MerkleRoots | List of Bytes   |             |
| Validators  | List of Address |             |


## BMV Prove

| Name                | Type          | Description                                                                                                                                      |
|:--------------------|:--------------|:-------------------------------------------------------------------------------------------------------------------------------------------------|
| BlockHeader         | Bytes         | fips-sha3-256(BlockHeader) → *BlockID* <br/>BlockHeader.Result.ReceiptRootHash → *ReceiptRootHash* <br/>BlockHeader.Height → *BlockHeight* <br/> |
| Votes               | Bytes         | [Prove with Votes](#prove-with-votes)                                                                                                                  |
| NextVaidators       | Bytes         | Validators = decode(NextValidators).ToAddress()                                                                                                  |
| *BlockHeight*       | Integer       | Check BlockHeight == LastHeight                                                                                                                  |
| *BlockID*           | Bytes         | [Prove with MTA](#prove-with-mta)                                                                                                                      |
| BlockProof          | List of Bytes | [Prove with MTA](#prove-with-mta)                                                                                                                      |
| ReceiptProofs       | List of Bytes | [Prove with MPT](#prove-with-mpt)                                                                                                                      |
| *ReceiptRootHash*   | Bytes         | [Prove with MPT](#prove-with-mpt)                                                                                                                      |

### Prove with Votes
````
for VoteItem in Votes.Items {
    VoteMessage = encode({
        Height: BlockHeader.Height
        Round: Votes.Round
        Type: 1
        BlockPartSetID: Votes.BlockPartSetID
        BlockID: BlockID
        Timestamp: VoteItem.Timestamp
    })
    PublicKey = DeducePublicKey(
        VoteItem.Signature,
        fips-sha3-256(VoteMessage))
    )
    Check ToAddress(PublicKey) in Validators
}
````

### Prove with MTA
````
MTA.Prove(
    roots=MerkleRoots,
    witness=BlockProof
)
````

### Prove with MPT
````
for ReceiptProof in ReceiptProofs {
    MPT.Prove(
        key=encode(ReceiptProof.Index),
        root=ReceiptRootHash
        proofs=ReceiptProof.Proofs
    ) → Receipt
    
    for EventProof in ReceiptProof.EventProofs {
        MPT.Prove(
            key=encode(EventProof.Index),
            root=Receipt.EventLogHash
            proofs=EventProof.Proofs
        ) → EventLog
    }
}
````

## Binary format

ICON uses RLP with Null(RLPn) for binary encoding and decoding.

* RLPn is [RLP](https://github.com/ethereum/wiki/wiki/RLP) with Null ( `[ 0xF8 0x00]` )

| Type      | RLPn     | Description                                                                                                                       |
|:----------|:---------|:----------------------------------------------------------------------------------------------------------------------------------|
| B_LIST    | List     | List of items                                                                                                                     |
| B_BYTES   | Bytes    | Raw bytes                                                                                                                         |
| B_BIGINT  | Bytes    | N bytes of integer representation.<br/>ex)<br/>0x00 → [ 0x00 ]<br/>0x80 → [ 0x00 0x80 ]<br/>-0x01 → [ 0xff ]<br/>-0x80 → [ 0x80 ] |
| B_INT     | B_BIGINT | 64bits signed integer                                                                                                             |
| B_ADDRESS | Bytes    | 1 byte<br/>- 0x00 ← EOA<br/>- 0x01 ← SCORE<br/>20 bytes : Identifier                                                              |
| B_NULL    | Null     | B_BYTES(N), B_ADDRESS(N) or B_LIST(N) can be Null                                                                                 |

Suffixed `(N)` means a nullable value.

## RelayMessage

> B_LIST of followings

| Field         | Type       | Description                                                                                                                            |
|:--------------|:-----------|:---------------------------------------------------------------------------------------------------------------------------------------|
| BlockUpdates  | B_LIST(N)  | List of encoded bytes of the [BlockUpdate](#blockupdate). If there is no updates, it would be empty, then BlockProof must not be Null. |
| BlockProof    | B_BYTES(N) | Encoded bytes of the [BlockProof](#blockproof). If it’s not Null, then BlockUpdates must be empty.                                     |
| ReceiptProofs | B_LIST(N)  | List of encoded bytes of the [ReceiptProof](#receiptproof).                                                                            |


### BlockUpdate

> B_LIST of followings

| Field          | Type      | Description                                                               |
|:---------------|:----------|:--------------------------------------------------------------------------|
| Header         | B_BYTES   | Encoded bytes of the [BlockHeader](#blockheader)                          |
| Votes          | B_BYTES   | Encoded bytes of the [Votes](#votes)                                      |
| NextValidators | B_BYTES(N) | Encoded bytes of the [Validators](#validators). Null if it’s not changed. |


### BlockProof

> B_LIST of followings

| Field        | Type    | Description                                      |
|:-------------|:--------|:-------------------------------------------------|
| Header       | B_BYTES | Encoded bytes of the [BlockHeader](#blockheader) |
| BlockWitness | -       | [BlockWitness](#blockwitness)                    |


### BlockWitness

> B_LIST of followings

| Field   | Type    | Description                                                                                      |
|:--------|:--------|:-------------------------------------------------------------------------------------------------|
| Height  | B_BYTES | Encoded bytes of the [BlockHeader](#blockheader)                                                 |
| Witness | B_LIST  | List of bytes. Only hashes of witness.Directions can be recovered by height of the block header. |


### ReceiptProof

> B_LIST of followings

| Field       | Type   | Description                                                                                                                 |
|:------------|:-------|:----------------------------------------------------------------------------------------------------------------------------|
| Index       | B_INT  | Index to the Receipt.                                                                                                       |
| Proofs      | B_LIST | List of [Merkle Proof](#merkle-proof) of receipt. [MPT Node](#mpt-node) including encoded bytes of the [Receipt](#receipt). |
| EventProofs | B_LIST | List of [EventProof](#eventproof).                                                                                          |


### EventProof

> B_LIST of followings

| Field  | Type   | Description                                                                                                                 |
|:-------|:-------|:----------------------------------------------------------------------------------------------------------------------------|
| Index  | B_INT  | Index to the EventLog.                                                                                                      |
| Proofs | B_LIST | List of [Merkle Proof](#merkle-proof) of event. [MPT Node](#mpt-node) including encoded bytes of the [EventLog](#eventlog). |


### Block Header

> B_LIST of followings

| Field                  | Type         | Description                                      |
|:-----------------------|:-------------|:-------------------------------------------------|
| Version                | B_INT        | 1 ← Version 1 (legacy)<br/>N ← Version N         |
| Height                 | B_INT        | Height of the block, <br/>0 means genesis block. |
| Timestamp              | B_INT        | Micro-seconds after EPOCH                        |
| Proposer               | B_ADDRESS(N) | Proposer of the block                            |
| PrevID                 | B_BYTES(N)   | 32 bytes hash value                              |
| VotesHash              | B_BYTES(N)   | 32 bytes hash value                              |
| NextValidatorsHash     | B_BYTES(N)   | 32 bytes hash value                              |
| PatchTransactionsHash  | B_BYTES(N)   | 32 bytes hash value                              |
| NormalTransactionsHash | B_BYTES(N)   | 32 bytes hash value                              |
| LogsBloom              | B_BYTES      | N(1~256) bytes bloom log value                   |
| Result                 | B_BYTES(N)   | Encoded bytes of the [Result](#result)           |


### Result

> B_LIST of followings

| Field             | Type       | Description                                                 |
|:------------------|:-----------|:------------------------------------------------------------|
| StateHash         | B_BYTES(N) | Hash of world state (account information)                   |
| PatchReceiptHash  | B_BYTES(N) | Root hash of [Merkle List](#merkle-list) of patch receipts  |
| NormalReceiptHash | B_BYTES(N) | Root hash of [Merkle List](#merkle-list) of normal receipts |


### Validators

>  B_LIST of Validators

| Field     | Type    | Description                                             |
|:----------|:--------|:--------------------------------------------------------|
| Validator | B_BYTES | 21 bytes → same as Address<br/>Other bytes → public key |


### Votes

> B_LIST of followings

| Field          | Type                    | Description                                                                |
|:---------------|:------------------------|:---------------------------------------------------------------------------|
| Round          | B_INT                   | Round for votes.<br/>If consensus doesn’t use round, it should be 0(zero). |
| BlockPartSetID | [PartSetID](#partsetid) | PartSetID of the proposed block                                            |
| Items          | B_LIST                  | List of [VoteItem](#voteitem)                                              |


#### VoteItem

> B_LIST of followings

| Field     | Type    | Description                                                         |
|:----------|:--------|:--------------------------------------------------------------------|
| Timestamp | B_INT   | Voted time in micro-seconds                                         |
| Signature | B_BYTES | RSV format signature for [VoteMessage](#votemessage) by a validator |

Public key of the validator can be recovered with `Signature` and
SHA3Sum256([VoteMessage](#votemessage)).


#### VoteMessage
> B_LIST of followings

| Field          | Type                    | Description                             |
|:---------------|:------------------------|:----------------------------------------|
| Height         | B_INT                   | [BlockHeader](#blockheader).Height      |
| Round          | B_INT                   | [Votes](#votes).Round                   |
| Type           | B_INT                   | 0 ← PreVote<br/>1 ← PreCommit           |
| BlockID        | B_BYTES(N)              | SHA3Sum256([BlockHeader](#blockheader)) |
| BlockPartSetID | [PartSetID](#partsetid) | [Votes](#votes).BlockPartSetID.         |
| Timestamp      | B_INT                   | [VoteItem](#voteitem).Timestamp         |

`Type` field should be `1` for votes of a block.


#### PartSetID

> B_LIST of followings

| Field          | Type       | Description                                                                |
|:---------------|:-----------|:---------------------------------------------------------------------------|
| Count          | B_INT      | Number of block parts                                                      |
| Hash           | B_BYTES(N) | Hash of block parts                                                        |


### Receipt

> B_LIST of followings

| Field              | Type                               | Description                                                                                    |
|:-------------------|:-----------------------------------|:-----------------------------------------------------------------------------------------------|
| Status             | B_INT                              | Result status<br/>0 ← SUCCESS<br/>N ← FAILURE ( N is failure code )                            |
| To                 | B_ADDRESS                          | The target address of the transaction                                                          |
| CumulativeStepUsed | B_BIGINT                           | Cumulative step used                                                                           |
| StepUsed           | B_BIGINT                           | Step used                                                                                      |
| StepPrice          | B_BIGINT                           | Step price in LOOP                                                                             |
| LogsBloom          | B_BIGINT                           | 2048 bits without padding zeros<br/>So, if there is no bit, then it would be a byte with zero. |
| EventLogs          | B_LIST(N) of [EventLog](#eventlog) | A list of event logs (empty if there is EventLogHash)                                          |
| SCOREAddress       | B_ADDRESS(N)                       | The address of deployed smart contract                                                         |
| EventLogHash       | B_BYTES(O)                         | (from Revision7) Root hash of [Merkle List](#merkle-list) of eventLogs                         |


#### EventLog

> B_LIST of followings

| Field   | Type                 | Description                    |
|:--------|:---------------------|:-------------------------------|
| Addr    | B_ADDRESS            | SCORE producing this event log |
| Indexed | B_LIST of B_BYTES(N) | Indexed data.                  |
| Data    | B_LIST of B_BYTES(N) | Remaining data.                |


### Merkle Patricia Trie

It's similar to [Merkle Patricia Trie](https://github.com/ethereum/wiki/wiki/Patricia-Tree)
except that it uses SHA3-256 instead of KECCAK-256.


#### Merkle List

Root hash of the list is SHA3Sum256([MPT Node](#mpt-node)) of the root.
The key for the list is B_INT encoded index of the item.


#### Merkle Proof

It's list of MPT Nodes from the root to the leaf containing the value.


#### MPT Node

MPT Node is one of followings.
* MPT Leaf
* MPT Extension
* MPT Branch


#### MPT Branch

> RLP List of followings

| Field      | Type                  | Description            |
|:-----------|:----------------------|:-----------------------|
| Link(1~16) | [MPT Link](#mpt-link) | Link to the Nth child. |
| Value      | RLP Bytes             | Stored data            |


#### MPT Leaf

> RLP List of followings

| Field  | Type      | Description                |
|:-------|:----------|:---------------------------|
| Header | RLP Bytes | [Header](#mpt-node-header) |
| Value  | RLP Bytes | Stored data                |


#### MPT Extension

> RLP List of followings

| Field  | Type                  | Description                |
|:-------|:----------------------|:---------------------------|
| Header | RLP Bytes             | [Header](#mpt-node-header) |
| Link   | [MPT Link](#mpt-link) | Link to the child.         |


#### MPT Link

> RLP Bytes or MPT Node

If encoded MPT Node is shorter than 32 bytes, then it's embedded in the link.
Otherwise, it would be RLP Bytes of SHA3Sum256([MPT Node](#mpt-node))


#### MPT Node Header

The key for the tree can be spliced into 4 bits nibbles.

| Case                        | Prefix | Path    |
|-----------------------------|--------|---------|
| Leaf with odd nibbles       | 0x3    | Nibbles |
| Leaf with even nibbles      | 0x20   | Nibbles |
| Extension with odd nibbles  | 0x1    | Nibbles |
| Extension with even nibbles | 0x00   | Nibbles |

> Examples

| Case            | Prefix | Path        | Bytes          |
|-----------------|--------|-------------|----------------|
| Leaf [A]        | 0x3    | [0xA]       | [ 0x3A ]       |
| Extension [AB]  | 0x00   | [0xAB]      | [ 0x00, 0xAB ] |
| Extension [ABC] | 0x1    | [0xA, 0xBC] | [ 0x1A, 0xBC ] |



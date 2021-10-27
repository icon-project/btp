# BTP Message Verifier for Moonbeam parchain (BMV)

## Introduction

BTP Message Verifier verify and decode Relay Message to
[BTP Message](btp.md#btp-message)s.
Relay Message is composed of both BTP Messages and with proof of
existence of BTP Messages.

## Polkadot
### Relay chain

The Relay Chain is the central chain of Polkadot. All validators of Polkadot are staked on the Relay Chain in chain's token and validate for the Relay Chain. The Relay Chain is composed of a relatively small number of transaction types that include ways to interact with the governance mechanism, parachain auctions, and participating in NPoS. The Relay Chain has deliberately minimal functionality - for instance, smart contracts are not supported. The main responsibility is to coordinate the system as a whole, including parachains. Other specific work is delegated to the parachains, which have different implementations and features.

### Parachain

Polkadot can support a number of execution slots. These slots are like cores on a computer's processor (a modern laptop's processor may have eight cores, for example). Each one of these cores can run one process at a time. Polkadot allows these slots using two subscription models: parachains and parathreads. Parachains have a dedicated slot (core) for their chain and are like a process that runs constantly. Parathreads share slots amongst a group, and are thus more like processes that need to be woken up and run less frequently.

Most of the computation that happens across the Polkadot network as a whole will be delegated to specific parachain or parathread implementations that handle various use cases. Polkadot places no constraints over what parachains can do besides that they must be able to generate a proof that can be validated by the validators assigned to the parachain. This proof verifies the state transition of the parachain. Some parachains may be specific to a particular application, others may focus on specific features like smart contracts, privacy, or scalability — still, others might be experimental architectures that are not necessarily blockchain in nature.

Polkadot provides many ways to secure a slot for a parachain slot for a particular length of time. Parathreads are part of a pool that shares slots and must-win auctions for individual blocks. Parathreads and parachains have the same API; their difference is economic. Parachains will have to reserve DOT for the duration of their slot lease; parathreads will pay on a per-block basis. Parathreads can become parachains, and vice-versa.

### Consensus

Relay chain use GRANDPA consensus algorithm to finalized block. Simply it is Proof of Work, validators and nominators stake chain token, KSM on Kusama or DOT on Polakdot. The ones have most stake and vote by nominators will become validators of relay chain. These participants play a crucial role in produce new blocks and finalize block in the Relay Chain and validate parachain block.

Validators perform two functions:

- Verifying that the information contained in an assigned set of parachain blocks is valid (such as the identities of the transacting parties and the subject matter of the contract). Each block, list of random validators, about five validators in Kusama, will be assigned to validate parachain block proof and include parachain block to relay chain block. In order to support the goal of 100 parachains, it require to a lot number of validators compare to other chain, currently 900 validators in Kusama relay chain.

- Participating in the consensus mechanism to produce the Relay Chain blocks based on validity statements from other validators. As soon as more than 2/3 of validators attest to a chain containing a certain block, all blocks leading up to that one are finalized at once. A notable distinction is that GRANDPA reaches agreements on chains rather than blocks, greatly speeding up the finalization process, even after long-term network partitioning or other networking failures. It means that we can not find finalized message, Polkadot call it `Justification` in every finalized block.

Parachain has their own block production mechanism, It call [BABE](https://docs.substrate.io/v3/advanced/consensus#babe) or [AURA](https://docs.substrate.io/v3/advanced/consensus#aura), block producers in parachain called collator. But there are no finalization process, blocks are finalized for the parachains when they are finalized by Polkadot's Relay Chain, the main chain of the system. So that in order to verify that parachain's blocks are finalized, we need to prove that their corresponding blocks are finalized on relay chain.

Because of that reason, Relay chain data are included in relay message to prove that parachain block has been finalized.

Reference: 

- [GRANDPA](https://github.com/w3f/consensus/blob/master/pdf/grandpa.pdf)
- [Security of the network](https://wiki.polkadot.network/docs/learn-security)
- [Polkadot Consensus](https://wiki.polkadot.network/docs/learn-consensus)
- [The Path of a Parachain Block](https://polkadot.network/blog/the-path-of-a-parachain-block/)

## Data structure

Both parachain and relay chain are built base on Substrate. Most of data structures of their chain are the same, such as block header, state merkle partricia trie, some of common event. Substrate using [SCALE](https://docs.substrate.io/v3/advanced/scale-codec) codec to encode data instead of RLP in Ethereum and ICON.
### Subtrate block header

| Name            | Type            | Description |
|:----------------|:----------------|:------------|
| parent_hash     | Hash            | hash of previous block     |
| number          | integer         | block number               |
| state_root      | Hash            | State MPT root             |
| extrinsics_root | Hash            | extrinsics MPT root        |
| digest          | block digest    | Digest, contains consensus message    |

How to calculate block hash:

- SCALE encoded block header
- Hash encoded data above by blake2b-256 algorithm

In Moonbeam parachain to ICON flow, relay get SCALE encoded block header of Moonbeam parachain and Kusama relay chain then included it in relay message. Para BMV decoded and calculate block hash in [here](../javascore/bmv/lib/src/main/java/foundation/icon/btp/lib/blockHeader/BlockHeader.java)

### Merkle patricia trie

Substrate modified Merkle Patricia Trie compare to the one that use in Ethereum and ICON. The following is what they modified:

- Remove extension node
- Use xxHash and Blake2 to create key
- Use SCALE encoding instead of RLP

Substrate use Merkle Patricia Trie to store state data of chain. State data includes block number, block hash, event, validators set, state data of smart contract... We can get meta data of chain to know what data the chain store in state storage.

With state_root in header and state proof from relay, BMV can verify storage to be valid or not. Pra-BMV implement code to verify MPT storage in [mpt](../javascore/bmv/lib/src/main/java/foundation/icon/btp/lib/mpt)

Reference:

- [Substrate Storage Deep Dive](https://www.shawntabrizi.com/substrate/substrate-storage-deep-dive/)
- [Querying Substrate Storage via RPC](https://www.shawntabrizi.com/substrate/querying-substrate-storage-via-rpc/)

### Justification

For GRANDPA, Justification is list signatures of validator and ancestry headers of all votes in the commit (so that we can verify the signatures).

| Name              | Type            | Description |
|:------------------|:----------------|:------------|
| round             | u64             | current vote round  |
| commit            | Commit          | vote commit         |
| votes_ancestries  | Vec<Header>     | ancestry headers    |


- Commit type: 

| Name              | Type            | Description |
|:------------------|:----------------|:------------|
| targetHash        | 256 bytes hash  | hash of finalized block  |
| targetNumber      | Block number    | number of finalized block       |
| precommits        | Vector SignedPrecommit | List of Signed Precommit    |

- SignedPrecommit type:

| Name              | Type            | Description |
|:------------------|:----------------|:------------|
| precommit         | Precommit       | infomation of signed block  |
| signature         | Signature       | signature of validator    |
| id                | Pubic key       | public key of validator  |

- Precommit type:

| Name              | Type             | Description        |
|:------------------|:-----------------|:-------------------|
| targetHash        | 256 bytes hash   | block hash that validator vote for    |
| targetNumber      | U32              | block number that validator vote for  |

Because GRANDPA votes on chains rather than blocks, we can not find Justification for every block. Substrate only store Justification for two cases:

- Latest finalized block. As soon as block n received enough 2/3 validator signatures, node store its Justification and use it to prove any block x < n to be finalized by api [proveFinality](https://polkadot.js.org/docs/substrate/rpc#provefinalitybegin-blockhash-end-blockhash-authoritiessetid-u64-optionencodedfinalityproofs). But then block m > n finalized, Justification of block n is remove and replace by Justification of block m. Substrate provide api [subscribeJustifications](https://polkadot.js.org/docs/substrate/rpc#subscribejustifications-justificationnotification) to get latest Justification of chain.

- Block has event to change validators list.

Message that validator sign to approve block to be finalized that is SCALE encoded of:

| Name              | Type             | Description        |
|:------------------|:-----------------|:-------------------|
| message           | Precommit        | infomation of signed block |
| round             | U64              | current vote round   |
| setId             | U64              | validator set id   |

Each validator set has it own set id number, starting from 0, when validator set change it increase by 1 and so on.

Relay get signatures of validator from Justification, encoded it to relay message. Moonbeam BMV verify block that has been finalized by verify relay signature, ensure enough 2/3+ 1 validator sign for block.

Reference:

- [Getting finalised block data](https://stackoverflow.com/questions/63064040/getting-finalised-block-data)
- [grandpa: always store justification for the latest finalized block ](https://github.com/paritytech/substrate/pull/8392)
- [Polkadot js](https://polkadot.js.org/docs/substrate/rpc#grandpa)

### Event

List of events with different type and structure are SCALE encoded and store in one storage of MPT. In order to decode event storage, we must start from begining of encoded bytes, get event index, determine what data contains in event data and decoded it, one by one until the end of encoded bytes. In other words, we must know structure of all events in each chains.

EventRecord structure:

| Name              | Type             | Description        |
|:------------------|:-----------------|:-------------------|
| phase             | Phase            | event phase        |
| event             | Event            | Event infomation   |
| topics            | Vec<Hash>        | list of event topics   |

Event structure:

| Name              | Type             | Description        |
|:------------------|:-----------------|:-------------------|
| index             | U16              | event index        |
| data              | Struct           | Data of event      |

For reducing code size of BMV contract, we implement separate contract to decode event. Contract only determine size of event data and decode it without specific data fields inside. We defined all type size of Moonbeam, Kusama and Edgeware event at [here](../javascore/bmv/eventDecoder/src/main/java/foundation/icon/btp/lib/EventDecoder/SizeDecoder.java)

Because each chain define their own event type and index, we can not implemented one contract to decode events of all substrate chains. Instead we create script to generate code of event decoder base on chain's metadata that implemented [build.gradle](../javascore/bmv/eventDecoder/build.gradle).

For more information how to build event decoder, please refer to [here](../javascore/bmv/Readme.md#buildEventDecoder)

BMV call `decodeEvent` method of event decoder to decode event. Method return list of eventRecord without specific event data. From para BMV contract, we only decode specific data of event that BMV need to verify and ignore others. There are three events para BMV need to decode and get specific data inside:

1. CandidateIncludeEvent, event of relay chain notify that one block of parachain has been accepted to relay chain

| Name              | Type                 | Description        |
|:------------------|:---------------------|:---------------------------|
| candidateReceipt  | CandidateReceipt     | Receipt of prachain block  |
| headData          | HeadData             | Head data of parachain      |
| coreIndex         | u16                  |       |
| groupIndex        | u16                  |       |

- Candidate receipt 

| Name              | Type                 | Description        |
|:------------------|:---------------------|:---------------------------|
| descriptor        | CandidateDescriptor  | The descriptor of the candidate block  |
| commitments_hash  | Hash                 | The hash of the encoded commitments made as a result of candidate execution  |

- CandidateDescriptor:

| Name                           | Type                 | Description        |
|:-------------------------------|:------------|:---------------------------|
| para_id                        | U32         | The ID of the para this is a candidate for  |
| relay_parent                   | Hash        | The hash of the relay-chain block this is executed in the context of |
| collator                       | Public key  | The collator's sr25519 public key |
| persisted_validation_data_hash | Hash        | The blake2-256 hash of the persisted validation data. These are extra parameters derived from relay-chain state that influence the validity of the block which must also be kept available for secondary checkers |
| pov_hash                       | Hash        | The blake2-256 hash of the `pov-block` |
| erasure_root                   | Hash        | The root of a block's erasure encoding Merkle tree |
| signature                      | Signature   | Signature on blake2-256 of components of this receipt: The parachain index, the relay parent, the validation data hash, and the `pov_hash` |
| para_head                      | Hash        | Hash of the para header that is being generated by this candidate |
| validation_code_hash           | Hash        | The blake2-256 hash of the validation code bytes. |


Code to decode this event implemented at [CandidateIncludedEvent.java](../javascore/bmv/lib/src/main/java/foundation/icon/btp/lib/event/CandidateIncludedEvent.java)

2. EVMLogEvent, event of parachain, it is log from EVM or Solidity smart contract.

| Name              | Type                 | Description        |
|:------------------|:---------------------|:---------------------------|
| address           | Address              | 20 bytes address of contract |
| topics            | Vec<Hash>            | List of topics of event     |
| data              | EVM event data       | Parameter logs from solidity smart contract, encoded using solidity ABI  |

- Data of BTP message event

| Name              | Type              | Description                  |
|:------------------|:------------------|:-----------------------------|
| seq               | Integer           | sequence number of message |
| next              | String            | btp address of receiver      |
| msg               | Bytes             | BTP message    |

Code to decode this event implemented at [EVMLogEvent.java](../javascore/bmv/lib/src/main/java/foundation/icon/btp/lib/event/EVMLogEvent.java)

3. NewAuthoritiesEvent, event of relay chain, notify that new validators list replace current validators list.

| Name              | Type              | Description                  |
|:------------------|:------------------|:-----------------------------|
| authority_set     | AuthorityList     | new authority list |

- Authority list: 

| Name              | Type              | Description                  |
|:------------------|:------------------|:-----------------------------|
| validator         | Public Key        | public key of address |
| weight            | u64               | weight of validator, all validators has the same weight is 1 |

Code to decode this event implemented at [NewAuthoritiesEvent.java](../javascore/bmv/lib/src/main/java/foundation/icon/btp/lib/event/NewAuthoritiesEvent.java)

Relay monitor that event and send to BMV, we will verify event data then update new validator list to db, increase validator setId by 1. Because validators included setId into vote message, relay must send NewAuthoritiesEvent at the last block of RelayBlockUpdates with validator's signatures of that block, before update any relay block higher. 

For example:

- Given:
    - current setId store in BMV: 10
    - current block number: 100
- When:
    - has newAuthorities in block 110
    - current setId increase to 101 at block 111
- Then:
    - If relay submit to BMV relay block from 100-130 and votes of block 130, BMV will throw error `verify signature for invalid validator set id` because block 130 validator sign for setId 11 but in BMV still store setId 10.
    - In this case relay should send relay block from 100-110, votes of block 110 and newAuthorities in block 110, BMV will update new authorities list and setId to db and then next block signatures will be verify properly.
    - As mentioned in Justification session, validators vote always stored in block that has change validators list, so that relay can get validtor's signature form that block

There are the case that relay may submit the same `newAuthorities` event for many times, to prevent that case, we store last block of relay chain that update validator set in [BMV](../javascore/bmv/parachain/src/main/java/foundation/icon/btp/bmv/parachain/BMV.java) line 46. Each time relay push newAuthorities event, BMV get that compare relay block number with last block of relay chain that update validator set, if it already updated, BMV ignore it, line 465-468.

## Relay message structure

### RelayMessage:

| Name                | Type            | Description                  |
|:--------------------|:----------------|:-----------------------------|
| paraBlockUpdates        | List of Bytes   | list of RLP encoded of ParaBlockUpdate |
| blockProof          | Bytes           | RLP encoded of BlockProof |
| stateProof          | List of Bytes   | List of RLP encoded of StateProof |

Code to decode RelayMessage implemented in [RelayMessage.java](../javascore/bmv/parachain/src/main/java/foundation/icon/btp/bmv/parachain/lib/relayMessage/RelayMessage.java)

### ParaBlockUpdate:

| Name                | Type            |       Description              |
|:--------------------|:----------------|:-------------------------------|
| paraBlockHeader         | bytes           | SCALE encoded of para chain block header  |
| relayChainData      | bytes           | RLP encoded of RelayChainData |

Code to decode ParaBlockUpdate implemented in [ParaBlockUpdate.java](../javascore/bmv/parachain/src/main/java/foundation/icon/btp/bmv/parachain/lib/blockUpdate/ParaBlockUpdate.java)

Relay chain data only need for last block in paraBlockUpdates, if it included in other blocks, it will be ignore.

There is the case that data of both chains are large, and it not able to execute in one transaction. We allow relay to update relay block first, in this case paraBlockUpdates only contains one item and para block header is null, only contains relayChainData. At first, for more transparent, We propose to change separate relay chain data and para chain data, but it will break current interface of relay message, so we come back to edit current relay message structure like that.

### BlockProof:

| Name                | Type            |       Description              |
|:--------------------|:----------------|:-------------------------------|
| blockHeader         | bytes           | SCALE encoded of block header that want to prove by MTA |
| height              | bytes           | MTA height of relay use to generate witness |
| witness             | bytes           | list of hash of leaf point to prove that block include in Merkle tree accumulator, low level leaf first |

Code to decode and verify BlockProof implemented in [BlockProof.java](../javascore/bmv/lib/src/main/java/foundation/icon/btp/lib/blockProof/BlockProof.java)

The cases that need BlockProof:

1. Last block of paraBlockUpdates that not includes in last block of relayBlockUpdates. In this case, relay send blockProof of relay block that contains event include parachain block

Example:

- Given:
   - relay block height in BMV 1000
   - para block height in BMV 500
- When:
   - last updating relay block height 1010
   - last updating para block height 505
   - para block 505 included in block 1008 of relay chain
- Then:
   - relay submit to BMV:
      - Relay block update from 1000-1010
      - Votes of block 1010
      - Relay BlockProof of block 1008
      - State proof of relay block 1008, contains CandidateIncluded event
      - ParaBlockUpdates from 500-505

2. Last block of paraBlockUpdates that includes in block of relay chain that already updated in previous transaction, In this case, relay don't need to send relayBlockUpdates, relay only send blockProof of relay block that contains event include parachain block.

Example:

- Given:
   - relay block height in BMV 1010
   - para block height in BMV 500
- When:
   - last updating para block height 505
   - para block 505 included in block 1008 of relay chain
- Then:
   - relay submit to BMV:
      - Relay BlockProof of block 1008
      - State proof of relay block 1008, contains CandidateIncluded event
      - ParaBlockUpdates from 500-505

3. BTP message not includes in last block of paraBlockUpdates. In this case, relay send blockProof of para block that contains EVM log event.

Example:

- Given:
   - para block height in BMV 500
- When:
   - last updating para block height 515
   - BTP message contains in block 510 of para
- Then:
   - relay submit to BMV:
      - `ParaBlockUpdates` from 500-515
      - Para BlockProof of block 510
      - State proof of para block 510, contains EVM log event of source BMC contract

4. BTP message includes in parachain block that already updated in previous transaction. In this case, relay don't need to update paraBlockUpdates, relay only need to send blockProof of para block that contains EVM log event.

Example:

- Given:
   - para block height in BMV 530
- When:
   - BTP message contains in block 510 of para
- Then:
   - relay submit to BMV:
      - Para BlockProof of block 510
      - State proof of para block 510, contains EVM log event of source BMC contract

### StateProof:

| Name                | Type            |       Description              |
|:--------------------|:----------------|:-------------------------------|
| key                 | bytes           | Key of MPT storage  |
| proofs              | List of bytes   | Proof to prove MPT storage, get from api, https://polkadot.js.org/docs/substrate/rpc#getreadproofkeys-vecstoragekey-at-blockhash-readproof, data of MPT node (branch node, leaf node) to prove that storage |

Code to decode and verify StateProof implemented in [StateProof.java](../javascore/bmv/lib/src/main/java/foundation/icon/btp/lib/stateProof/StateProof.java)

### RelayChainData:

| Name                | Type            | Description                  |
|:--------------------|:----------------|:-----------------------------|
| relayBlockUpdates   | List of Bytes   | list of RLP encoded of RelayBlockUpdate |
| blockProof          | Bytes           | RLP encoded of BlockProof |
| stateProof          | List of Bytes   | List of RLP encoded of StateProof |

Code to decode RelayChainData implemented in [RelayChainData.java](../javascore/bmv/parachain/src/main/java/foundation/icon/btp/bmv/parachain/lib/relayChainData/RelayChainData.java)

As mentioned in polkadot consensus session, in order to verify that parachain's blocks are finalized, we need to prove that their corresponding blocks are finalized on relay chain. That's why we need relay to subbmit relay chain data here.

### RelayBlockUpdate:

| Name                | Type            | Description                  |
|:--------------------|:----------------|:-----------------------------|
| blockHeader         | Bytes           | SCALE encoded of relay block header |
| votes               | Bytes           | RLP encode of Votes, signatures of validators with fields that included in vote |

**note** votes only require for last block of RelayBlockUpdates list, if it exist in other blocks, it will be ignore

Code to decode RelayBlockUpdate implemented in [RelayBlockUpdate.java](../javascore/bmv/parachain/src/main/java/foundation/icon/btp/bmv/parachain/lib/blockUpdate/RelayBlockUpdate.java)

### Votes:

| Name                | Type                 | Description                  |
|:--------------------|:---------------------|:-----------------------------|
| voteMessage         | Bytes                | SCALE codec of Vote Message |
| signatures          | ValidatorSignature   | list RLP of ValidatorSignature |

**note** that Votes only contain in RelayBlockUpdate, ParaBlockUpdate has no vote, and it finalization depend on RelayChainData

Relay send validator signature and vote message to BMV in RelayBlockUpdate, BMV do the following step to validate:

1. Decode vote message, implemented in [Votes.java](../javascore/bmv/lib/src/main/java/foundation/icon/btp/lib/votes/Votes.java) line 53-62

2. Validate vote message, targetHash, targetNumber, currentSetId equals to current validate block and setId. Code implemented in [Votes.java](../javascore/bmv/lib/src/main/java/foundation/icon/btp/lib/votes/Votes.java) line 66-76

3. Verify each signature, check that signer is contain in current validator list and no signatures is duplicated. Code implemented in [Votes.java](../javascore/bmv/lib/src/main/java/foundation/icon/btp/lib/votes/Votes.java) line 79-94.

4. Check that number of valid signatures greater than 2/3 number of current validators

5. Relay block finalized if all conditions above satisfy

### ValidatorSignature:

| Name                | Type            | Description                  |
|:--------------------|:----------------|:-----------------------------|
| signature           | Bytes           | Signature of validator for relay chain block |
| validator           | Bytes           | 32 byte public key of validator |


Code to decode and ValidatorSignature implemented in [ValidatorSignature.java](../javascore/bmv/lib/src/main/java/foundation/icon/btp/lib/votes/ValidatorSignature.java)

## handleRelayMessage

We will go through step to step from BMV receive relay message from relay, verify message and return BTP message to BMC. 

### decode Base64:

- Code is implemented in [BMV](../javascore/bmv/parachain/src/main/java/foundation/icon/btp/bmv/parachain/BMV.java#L202) line 200-206
- throw `DECODE_ERROR = 37;`, if can not decode

### Decode RLP:

- Decode RLP with structure as defined above

- Code is implemented in [BMV](../javascore/bmv/parachain/src/main/java/foundation/icon/btp/bmv/parachain/BMV.java#L212) line 208-214

- throw `DECODE_ERROR = 37;`, if can not decode

### Check that relay message has block update or block proof

- If relay message missing both block update and block proof, it can not prove any data that valid in parachain. So we have nothing to do with that.

- Code is implemented in [BMV](../javascore/bmv/parachain/src/main/java/foundation/icon/btp/bmv/parachain/BMV.java#L216) line 216 - 218
- throw `BMV_ERROR = 25;`, message `invalid RelayMessage not exists BlockUpdate and BlockProof`

### Verify parachain block data

Code to verify parachain block data implementd in function `verifyParaChainBlock`. This function return `stateRoot` and `lastHeight` of expected block that contain btp message if exist. 

There are two cases that going to be happened here: 

1. Relay want to sync both relay chain block and parachain block in one relay message

- Code is implemented at [BMV](../javascore/bmv/parachain/src/main/java/foundation/icon/btp/bmv/parachain/BMV.java) line 403-433
- Relay message contains list of parachain blocks
- BMV compare previous hash of para block n + 1 with hash of para block n, to make sure they are in the same chain. For the first block of list, we compare it with the last para block hash that store in db in previous transaction.
- Last block in list contains relay chain data to prove that list parachain block has been included in finalized chain of relay, 
- BMV pass relay chain data to `verifyRelayChainBlock` function, this function verify relay chain block update and block proof of relay and return `stateRoot` and `lastHeight` of expected block contain event of relay chain if exist. Detail about what function do will be explain later.
- BMV pass BlockVerifyResult (`stateRoot` and `lastHeight`) to `verifyRelayChainState` function, this function use MPT to verify event storage return hash of parachain block that has been included in relay chain if it exists. Detail about what function do will be explain later.
- BMV compare block hash return from `verifyRelayChainState` function with last block hash of parachain block update, if it is the same parachain blocks prove to be finalized.

2. In case data of both chain is large, and not able process it in one transaction. Relay just want to update relay chain block without parachain block. 

- Implemented at [BMV](../javascore/bmv/parachain/src/main/java/foundation/icon/btp/bmv/parachain/BMV.java) line 395-401
- Relay message contains para block updates with only one item. In this item, para block header null and only contains relay chain data.
- The same with follow 1, but we don't need to verify any parachain block in this case, just use two function `verifyRelayChainBlock` and `verifyRelayChainState` to verify relay chain data.

After verify relay and para chain blocks are valid, BMV verify block proof of parachain chain if it exists. Code to verify BlockProof implemented at [BMV](../javascore/bmv/parachain/src/main/java/foundation/icon/btp/bmv/parachain/BMV.java) line 437-442

### Verify relay chain data

As mentioned above, there are two function to verify relay chain data `verifyRelayChainBlock` and `verifyRelayChainState`.

1. verifyRelayChainBlock

- Code is implemented at [BMV](../javascore/bmv/parachain/src/main/java/foundation/icon/btp/bmv/parachain/BMV.java) line 348-387
- First, verify relay chain blocks are on the same chain, we compare hash of block n + 1 with hash of block n of relay chain block updates. For the first block of list, we compare it with the last para block hash that store in db in previous transaction.
- To make sure that blocks has been finalized, we verify signatures of validator is valid and enough 2/3 + 1 validators signed.
- Verify block proof of relay chain if exist.
- return stateRoot and lastHeight

2. verifyRelayChainState

- Code is implemented at [BMV](../javascore/bmv/parachain/src/main/java/foundation/icon/btp/bmv/parachain/BMV.java) line 453-490
- First, we use MPT to verify state proof and get encoded storage.
- If storage is event, call relay event decoder contract to decode event. 
- Loop through all events and find if there are any NewAuthorities or CandidateIncluded event.
- In case of NewAuthorities event, we update new validators list, inscrease validator set id, last update validator set block number to db
- In case of CandidateIncluded event, we check that it include block of target parachain by compare its parachain id with target parachain id in db. If it is our target parachain, return hash of included block.

### Verify parachain state proof and return BTP message to BMC

- Implemented at [BMV](../javascore/bmv/parachain/src/main/java/foundation/icon/btp/bmv/parachain/BMV.java) line 226-262
- First, we use MPT to verify state proof and get encoded storage.
- If storage is event, call parachain event decoder contract to decode event
- Loop through all events and find if there are any EVM log event.
- If it contains EVM log event, BMV continue to check: 
    - It's BTP message event by compare evm topic
    - Event was logged by BMC address of source chain
    - Sequence number of event equal to current sequence of BMC + 1
    - Add BTP message to list if all conditions satisfy
- Return BTP message list to BMC
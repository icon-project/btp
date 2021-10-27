# Binance Smart Chain BTP Message Verifier(BMV)

## Introduction

This document describes the verification and validation procedure specific to BTP Message Verifier for Binance Smart Chain, 
this follows the design of BTP protocol outlined in [BTP Message](btp.md#btp-message)

## Proof of Staked Authority

The implementation of BSC(Binance Smart Chain) consensus engine a modified version of [Clique](https://eips.ethereum.org/EIPS/eip-225) PoA
consensus protocol, known as `Parlia`.

BSC's consensus combines DPoS and PoA for consensus, such that:

1. Blocks are produced by a limited set of validators (currently 21 validators)
2. Validators take turns to produce blocks in a PoA manner (similar to Ethereum’s Clique consensus design)
3. Validator set are elected in and out based on a staking based governance

For more details on BSC consensus, please refer to the [whitepaper](https://github.com/binance-chain/whitepaper/blob/12237099ad5e7d2e33641445531a3f25dc1ebe78/WHITEPAPER.md)

## BSC Message Verifier

1. Decode RelayMessage and extract the blockheader hash at a given height
2. Verify the block header hash using the stored Merkle Tree Accumulator
    - Check if block height is the same as MTA next height
3. Verify the validator set against provided `SealedHeader`
    - Verify the coinbase signature
4. Verify Receipt Proofs and extract valid receipt which includes the Event Log for the message transfer
    - Verify receipt inclusion against header receiptRoot and receipts Merkle Patricia Trie

```go
// Header represents a block header in the Ethereum blockchain.
type Header struct {
	ParentHash  common.Hash
	UncleHash   common.Hash
	Coinbase    common.Address
	Root        common.Hash
	TxHash      common.Hash
	ReceiptHash common.Hash
	Bloom       []byte
	Difficulty  uint64
	Number      uint64
	GasLimit    uint64
	GasUsed     uint64
	Time        uint64
	Extra       []byte
	MixDigest   common.Hash
	Nonce       types.BlockNonce
}
```

### Receipt Proof

Every block header maintains structure which contains three states in Trie structure known as Modified [Merkel Patricia Trie](https://eth.wiki/en/fundamentals/patricia-tree) in Ethereum:

- Receipts trie
- Transactions trie
- State trie

BSC Relay [Receiver](https://github.com/icon-project/btp/blob/btp_web3labs/cmd/btpsimple/module/bsc/receiver.go#L150) reconstructs the proof trie encoded specification. 
BMV Message Verifier in turn decodes the proofs which includes the receipts trie and [verifies](https://github.com/icon-project/btp/blob/btp_web3labs/javascore/bmv/src/main/java/foundation/icon/btp/bmv/BTPMessageVerifier.java#L91)
the trie against header `ReceiptHash`. 

```go
type ReceiptProof struct {
	Index       int
	Proof       []byte
	EventProofs []*module.EventProof
}
```

### ValidatorSet

Binance Smart Chain relies on the extra security of Binance Chain network. Binance chain does the staking and governance parts for BSC,
and ValidatorSet change, double sign slash of BSC is updated through interchain communication.

ValidatorSet is updated after each epoch block (Epoch is set to 200 in mainnent), BMR queries ValidatorSet via the [TendermintLightClient](https://github.com/icon-project/btp/blob/btp_web3labs/cmd/btpsimple/module/bsc/receiver.go#L82) contract  

```go
type ConsensusStates struct {
	PreValidatorSetChangeHeight uint64
	AppHash                     [32]byte
	CurValidatorSetHash         [32]byte
	NextValidatorSet            []byte
}
```

#### Validator Signature Verification: 
- Verify the signature of the coinbase is in extraData of the blockheader
- The coinbase should be the signer

**Note:** 
The signature of the coinbase is in extraData of the blockheader, the structure of extraData is: epoch block. 32 bytes of extraVanity + N*{20 bytes of validator address} + 65 bytes of signature. none epoch block. 32 bytes of extraVanity + 65 bytes of signature. The signed content is the Keccak256 of RLP encoded of the block header. See example [here](https://github.com/binance-chain/bsc/blob/955c78bde05c756fe30a9e6ecf8bed5091d9f62e/consensus/parlia/parlia.go#L158).

```go
type BlockUpdate struct {
   BlockHeader []byte
   Validators  []byte
   SealedHeader []byte // Original BSC (EVM) encoded header including the validator signature
}
```

```go
signature := header.Extra[len(header.Extra)-extraSeal:]

// Recover the public key and the Ethereum address
pubkey, err := crypto.Ecrecover(SealHash(header, chainId).Bytes(), signature)
if err != nil {
    return common.Address{}, err
}
var signer common.Address
copy(signer[:], crypto.Keccak256(pubkey[1:])[12:])
```

## Relay Message Structure
### Relay Message:
| Name          |      Type      |  Description                        |
|---------------|:--------------:|:------------------------------------|
| blockUpdates  |  list of bytes | list of RLP encoded of BlockUpdate  |
| blockProof    |    bytes       | RLP encoded of BlockProof           |
| receiptProofs | list of bytes  | list of RLP encoded of ReceiptProof |
Code to decode RelayMessage implemented in [RelayMessage.java](https://github.com/icon-project/btp/blob/btp_web3labs/javascore/bmv/src/main/java/foundation/icon/btp/bmv/types/RelayMessage.java)
### BlockUpdate:
| Name           |      Type      |  Description                 |
|----------------|:--------------:|:-----------------------------|
| blockHeader    |    bytes       | RLP encoded of BlockHeader   |
| nextValidators | bytes of bytes | bytes of bytes               |
| validatorList  |    bytes       | RLP encoded of ValidatorList |
| sealedHeader   |    bytes       | BSC (EVM) encoded header bytes with the sealed validator hash in extraData     |
Code to decode BlockUpdate implemented in [BlockUpdate.java](https://github.com/icon-project/btp/blob/btp_web3labs/javascore/bmv/src/main/java/foundation/icon/btp/bmv/types/BlockUpdate.java)
### BlockProof:
| Name           |      Type      |  Description                |
|----------------|:--------------:|:----------------------------|
| blockHeader    |    bytes       | RLP encoded of BlockHeader that want to prove by MTA|
| blockWitness   |    bytes       | RLP encoded of BlockWitness, list of hash of leaf point to prove that block include in Merkle tree accumulator, low level leaf first |
Code to decode BlockProof implemented in [BlockProof.java](https://github.com/icon-project/btp/blob/btp_web3labs/javascore/bmv/src/main/java/foundation/icon/btp/bmv/types/BlockProof.java)
### ReceiptProof:
| Name           |      Type      |  Description                           |
|----------------|:--------------:|:---------------------------------------|
| index          |    int         | ...                                    |
| mptKey         |    bytes       | ...                                    |
| eventProofs    | list of bytes  | ...                                    |
| events         | list of bytes  | list of RLP encoded of ReceiptEventLog |
| mptProofs      | list of bytes  | ...                                    |
Code to decode ReceiptProof implemented in [ReceiptProof.java](https://github.com/icon-project/btp/blob/btp_web3labs/javascore/bmv/src/main/java/foundation/icon/btp/bmv/types/ReceiptProof.java)
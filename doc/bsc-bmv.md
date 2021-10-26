# Binance Smart Chain BTP Message Verifier(BMV)

## Introduction

This document will describe the BTP Message Verification specific to BSC Message Verifier, this follows the BTP protocol
outlined in [BTP Message](btp.md#btp-message)s

## BSC Consensus

The implementation of BSC(Binance Smart Chain) consensus engine is based on modified version of Clique PoA
consensus protocol, known as Parlia. 

## Message Verifier

1. Decode RelayMessage and extract the blockheader hash at a given height
2. Verify the block header hash using the stored Merkle Tree Accumulator
3. Verify the validator set against provided `SealedHeader`
4. Verify Receipt Proofs and extract valid receipt which includes the Event Log for the message tranfer

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

```go
type ReceiptProof struct {
	Index       int
	Proof       []byte
	EventProofs []*module.EventProof
}
```

### ValidatorSet

```go
type ConsensusStates struct {
	PreValidatorSetChangeHeight uint64
	AppHash                     [32]byte
	CurValidatorSetHash         [32]byte
	NextValidatorSet            []byte
}

type BlockUpdate struct {
   BlockHeader []byte
   Validators  []byte
   SealedHeader []byte // Original BSC (EVM) encoded header including the validator signature
}

```


- Binance chain does the staking and governance parts for BSC.
- ValidatorSet change, double sign slash of BSC is updated through interchain communication.

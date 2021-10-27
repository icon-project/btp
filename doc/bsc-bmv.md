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
    - check if block height is the same as MTA next height
3. Verify the validator set against provided `SealedHeader`
    - verify the coinbase signature
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

#### Validator Signature Verification: 
- Verify the signature of the coinbase is in extraData of the blockheader
- The coinbase should be the signer

**Note:** 
The signature of the coinbase is in extraData of the blockheader, the structure of extraData is: epoch block. 32 bytes of extraVanity + N*{20 bytes of validator address} + 65 bytes of signature. none epoch block. 32 bytes of extraVanity + 65 bytes of signature. The signed content is the Keccak256 of RLP encoded of the block header. See example [here](https://github.com/binance-chain/bsc/blob/955c78bde05c756fe30a9e6ecf8bed5091d9f62e/consensus/parlia/parlia.go#L158).

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
| votes          |    bytes       | RLP encoded of Votes         |
| nextValidators | bytes of bytes | bytes of bytes               |
| validatorList  |    bytes       | RLP encoded of ValidatorList |
| evmHeader      |    bytes       | not encoded header bytes     |
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
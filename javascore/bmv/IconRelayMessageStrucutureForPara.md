```python
# block header of Substrate chain
BlockHeader {
    parentHash: bytes,
    number: CompactU32,
    stateRoot: Hash,
    extrinsicsRoot: Hash,
    digest: bytes
}

# validator will SCALE encode that message and then sign it.
VoteMessage {
    message: {
        # Scale enum type, ref https://substrate.dev/docs/en/knowledgebase/advanced/codec#enumerations-tagged-unions
        _enum: {
            Prevote: {
                targetHash: "HASH",
                targetNumber: "u32"
            },
            Precommit: {
                targetHash: "HASH",
                targetNumber: "u32"
            },
            PrimaryPropose: {
                targetHash: "HASH",
                targetNumber: "u32"
            }
        }
    },
    round: "u64",
    setId: "u64",
}

ValidatorSignature {
  # 64 bytes signature
  signature: bytes,
  # 32 byte public key of validator
  validator: bytes
}

# validator's signatures to prove that block's confirmed
Votes {
    # SCALE codec of VoteMessage
    voteMessage: bytes
    # list RLP of ValidatorSignature
    signatures: [bytes]
}

# blocks to sync from destination chain
RelayBlockUpdate {
  # header of updating block
  # SCALE encode of BlockHeader
  blockHeader: bytes,

  # only require for last block of list
  # signatures of validators with fields that included in vote
  # RLP encode of Votes
  votes: bytes
}

# blocks to sync from destination chain
ParaBlockUpdate {
  # header of updating block
  # SCALE encode of BlockHeader
  blockHeader: bytes,

  # only require for last block of list
  # signatures of validators with fields that included in vote
  # RLP encode of RelayChainData
  relayChainData: bytes
}

StateProof {
  # key of storage
  key: bytes
  # get from api, https://polkadot.js.org/docs/substrate/rpc#getreadproofkeys-vecstoragekey-at-blockhash-readproof
  # data of MPT node (branch node, leaf node) to prove that storage, from top to bottom
  proofs: [bytes]
}

# block contain event with proofs
BlockProof {
  # SCALE codec of block header
  # Header of block contains event
  blockHeader: bytes,

  height: "u32" # Merkle tree accumulator height when get witness
  witness: [bytes] # list of hash of leaf point to prove that block include in Merkle tree accumulator, low level leaf first
}

RelayChainData {
  # optional, for BMV to synchronize block from relay chain
  # list of RLP encode of RelayBlockUpdate
  # If it is omitted, an EMPTY_HEADER (0xC0) will be attached
  relayBlockUpdates: [bytes]

  # optional, in case of event in previous updated block is missing, it used to prove that block included in source chain
  # RLP endcode of BlockProof
  # If it is omitted, an EMPTY_HEADER (0xF800) will be attached
  blockProof: bytes,

  # required
  # needed event: parasInclusion.CandidateIncluded, grandpa.NewAuthorities 
  # proofs to prove that state storage in merkle patricia trie of relay chain
  # a list of RLP encode of StateProof
  stateProof: bytes
}

# message from Relay
RelayMessage {
  # optional, for BMV to synchronize block from srouce chain
  # list of RLP encode of ParaBlockUpdate
  # If it is omitted, an EMPTY_HEADER (0xC0) will be attached
  paraBlockUpdates: [bytes],

  # optional, in case of event in previous updated block is missing, it used to prove that block included in source chain
  # RLP endcode of BlockProof
  # If it is omitted, an EMPTY_HEADER (0xF800) will be attached
  blockProof: bytes,

  # optional, in case of have any event to update from source BMC
  # proofs to prove that state storage in merkle patricia trie
  # a list of RLP encode of StateProof
  # This field also can be omitted. If so, 0xC0 will be attached
  stateProof: [bytes]
}

_msg = urlsafe_b64decode(RLP_ENCODE(RelayMessage)) # message from relayer to BMC
```

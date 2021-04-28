BlockHeader {
  version: int
  height: int # block number
  timestamp: int
  proposer: bytes
  prev_hash: bytes
  vote_hash: bytes
  next_validators_hash: bytes # hash of next validator address
  patch_tx_hash: bytes
  tx_hash: bytes
  logs_bloom: bytes
  RLP_ENCODE({
    state_hash: bytes
    patch_receipt_hash: bytes
    receipt_hash: bytes # root of event logs trie
  }
  # may include more or less fields depend on what BlockHeader structure of chain
  # RLPEncode might or might not include in the BlockHeader
  # If it is omitted, an EMPTY_HEADE (0xF800) will be attached
  # logs_bloom can also be empty. In that case, an EMPTY_HEADER (0xC0) will be attached
  # patch_tx_hash, tx_hash, patch_receipt_hash, and receipt_hash can also be omitted
  # If these fields are empty, an EMPTY_HEADER (0xF800) will be attached
}

# validator's signatures to prove that block's confirmed
Votes {
  round: int
  block_part_set_id: [int, bytes] # this is a list. It can be called as a struct in Solidity
  [{
    timestamp: int,
    signature: bytes
  }]  # ====> This is an array of list/struct, NOT an array of RLPEncode
  # include all fields that validator included to signature
}

# to prove that event include in event Merkle Patricia Trie
EventProof {
  # index of event
  index: int,

  # data of MPT node (extension node, branch node) to prove that event log, from top to bottom
  proofs: RLP_ENCODE([bytes]) # This is a flatten bytes array of a sophicated tree
}

# blocks to sync from destination chain
BlockUpdate {
  # header of updating block
  # RLP encode of BlockHeader
  blockHeader: bytes,

  # signatures of validators with fields that included in vote
  # RLP encode of Votes
  votes: bytes,

  # list addresses of next validators, to validate signature of next block
  nextValidator: RLP_ENCODE([bytes]) # address of validators
  # RLPEncode([bytes]) can be omitted. If so, 0xF800 will be attached
}

ReceiptProof {
  # index of transaction receipt
  index: int,

  # data of MPT node (extension node, branch node) to prove that transaciton receipt, from top to bottom
  proof: RLP_ENCODE([bytes])

  # event to synchronize with proofs that it is included in MPT
  # list of EventProof [EventProof]
  eventProofs: [bytes]
}

# block contain event with proofs
BlockProof {
  # Header of block contains event
  blockHeader: bytes,
  blockWitness: {
    height: int # Merkle tree accumulator height when get witness
    witness: [bytes] # list of hash of leaf point to prove that block include in Merkle tree accumulator, low level leaf first
  }
}

# message from Relay
RelayMessage {
  # optional, for BMV to synchronize block from srouce chain
  # list of RLP encode of BlockUpdate
  # If it is omitted, an EMPTY_HEADER (0xC0) will be attached
  blockUpdates: [bytes],

  # optional, in case of event in previous updated block is missing, it used to prove that block included in source chain
  # RLP endcode of BlockProof
  # If it is omitted, an EMPTY_HEADER (0xF800) will be attached
  blockProof: bytes,

  # optional, in case of have any event to update from source BMC
  # proofs to prove that receipt in transaction receipt merkle patricia trie
  # a list of RLP encode of BlockProof
  # This field also can be omitted. If so, 0xC0 will be attached
  receiptProof: bytes
}

_msg = urlsafe_b64decode(RLP_ENCODE(RelayMessage)) # message from relayer to BMC
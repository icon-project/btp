# Knowledge

## Overall

Votes -> BlockHeader -> Receipt/Log MerkleProof -> Event Proof (BTP event)

## Practical byzantine fault tolerance (pbft)
    - Votes: how validators/block producers confirm the finality of a block
    - Votes: proofs depends on each chain implementation

## Merkle Proof: 
    - Merkle Patricia Trie
    - Merkle Accumulator
    - Using storage hash to prove Event Log with Merkle Proof

## Encoding
    - RLP and Null-RLP from ICON
    - SCALE from Polkadot
    - Reflect API of Golang

## Cryptography
    - How keystore works
    - How to unlock keystore with password
    - How to sign ICON and Ethereum transaction with private key in Golang

## BMC interaction
    - BMCGetStatusLink properties

## BMC rotation algorithm
    - Round robin algorithm

## JSON-RPC
    - Differentiate with HTTP and Websocket client
    - How to add new method existing client in Golang

## Polkadot
    - Finality mechanism of Relaychain and Parachain
    - How to interfact Ethereum compatiable Layer-2 of Polkadot - Frontier
    - How to encode/decode with metadata
    - How to get EVM logs from Parachain system logs

## ICON

## Concurrency in Golang:
    - How to use Channel to communicate among Goroutine
    - How to use Mutex
    - How to use WaitGroup

## Database:
    - LevelDB Embedded database Golang

## Golang Frameworks:
    - Cobra, Viper CLI framework
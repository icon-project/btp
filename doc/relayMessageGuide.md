### relay update new blocks from source chain without event log from source BMC

```python
# block num   70      71     72     73     74     75     76     77     78     79     80 
#           |------|------|------|------|------|------|------|------|------|------|------|
#                |  
#         lastBMVUpdatedBlock
```

1. Read block header in front of last BMV updated block (block 71, 72, ....)
2. Get validator signatures of above block
3. get list validators of next block (empty if not change)

```python
# message from Relay
RelayMessage {
  # list of RLP encode of BlockUpdate, from block 71 to 80
  blockUpdates: [bytes],

  blockProof: bytes, # empty
  receiptProof: [bytes] # empty
}
```

### relay update new blocks from source chain and event log is on last updated block

```python
# block num   70      71     72     73     74     75     76     77     78     79     80 
#           |------|------|------|------|------|------|------|------|------|------|------|
#                |                                                                   |
#         lastBMVUpdatedBlock                                           event log of BMC in this block (80)
```

1. Read block header in front of last BMV updated block (block 71, 72, ....)
2. Get validator signatures of above block
3. get list validators of next block (empty if not change)
4. Get receipt proof
5. Get event proof

```python
# message from Relay
RelayMessage {
  # list of RLP encode of BlockUpdate, from block 71 to 80
  blockUpdates: [bytes],

  blockProof: bytes, # empty

  # receipt proof and event proof in block 80
  receiptProof: bytes
}
```

### relay update event logs of BMC that missing

```python
# block num   70      71     72     73     74     75     76     77     78     79     80 
#           |------|------|------|------|------|------|------|------|------|------|------|
#                                    |                                               |  
#                            block has missing event                        lastBMVUpdatedBlock (80)
```

1. Read block header of block has missing event logs (block 73)
2. Get proof to prove that block included in merkle tree acummulator
4. Get receipt proof
5. Get event proof

```python
# message from Relay
RelayMessage {
  # empty
  blockUpdates: [bytes],

  # merkle tree acummulator proof of block 73
  blockProof: bytes,

  # receipt proof and event proof in block 73
  receiptProof: bytes
}
```


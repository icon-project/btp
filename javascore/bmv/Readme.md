# deploy bmv score

## Deploy BMC smart contract

```
goloop rpc --uri http://localhost:9082/api/v3 sendtx deploy bmc.zip \
    --key_store godWallet.json --key_password gochain \
    --nid 3 --step_limit 13610920001 \
    --content_type application/zip \
    --param _net="0x07.icon"
```

- get transaction result and score address:

```
goloop rpc --uri http://localhost:9082/api/v3 txresult 0x3ce80b7352481385fecdbc91e4d064869abf97cc3bfb25e997a946c3870e6db9
```

## Deploy event decoder contract, use one of comands below
```
goloop rpc --uri http://localhost:9082/api/v3 sendtx deploy ./eventDecoder/build/libs/eventDecoder-optimized.jar \
    --key_store godWallet.json --key_password gochain \
    --nid 3 --step_limit 13610920010 \
    --content_type application/java

gradle eventDecoder:deployToLocal -P keystoreName=/Users/leclevietnam/mwork/btp/icon-bmv/godWallet.json -P keystorePass=gochain
```

## Deploy bmv contract

### Parameter:

- `mtaOffset`: offset of Merkle Tree Accumulator, block height BMV start to sync block
- `bmc`: address of BMC score
- `net`: network that BMV handle, (Edgeware.frontier)
- `mtaRootSize`: size of MTA roots
- `mtaCacheSize`: size of mta cache
- `mtaIsAllowNewerWitness`: is allow to verify newer witness (client MTA height higher than contract MTA height)
- `lastBlockHash`: hash of previous block (mtaOffset), BMV use this to check previous hash of updating block equal to last block hash
- `encodedValidators`: `Base64(RLP.encode(List<byte[]> validatorPublicKey))`, encoded of validators list
- `eventDecoderAddress`: address of event decoder score
- `currentSetId`: current validator set id

```
goloop rpc --uri http://localhost:9082/api/v3 sendtx deploy ./bmv/build/libs/BMV-optimized.jar \
    --key_store godWallet.json --key_password gochain \
    --nid 3 --step_limit 3519157719 \
    --content_type application/java \
    --param mtaOffset=0x28 \
    --param bmc=cxc332681329f869cbfb1840d71d57033eebccc682 \
    --param net=Edgeware.frontier \
    --param mtaRootSize=0x10 \
    --param mtaCacheSize=0x10 \
    --param mtaIsAllowNewerWitness=0x1 \
    --param lastBlockHash=0xf94a5e262b7192fb951813c50de551761fcc13f2493f41a2f0105cb931cedd89 \
    --param encodedValidators=4aCI3DQX1QWOxLRQPgwS6hoKib4gD-mJIkI9QzQBT6aw7g \
    --param eventDecoderAddress=cxf66548780f0e57a316ff7d1b84a1b3869b6445c6 \
    --param currentSetId=0x0

gradle bmv:deployToLocal -P keystoreName=/Users/leclevietnam/mwork/btp/icon-bmv/godWallet.json -P keystorePass=gochain
```

## get encode of Merkle tree accumulator

- MTA encode struct

```
mta: {
    long height,
    long offset,
    int rootSize,
    int cacheSize,
    boolean isAllowNewerWitness,
    byte[] lastBlockHash,
    List<byte[]> roots,
    List<byte[]> caches
}

result = Base64.encode(RLP.encode(mta))
```

```bash
goloop rpc --uri http://localhost:9082/api/v3 call --to cx41c3699326f166e281a5be4f91e1330ca049aeeb --method mta
```

## get list of merkle tree accummulator roots
```bash
goloop rpc --uri http://localhost:9082/api/v3 call --to cx41c3699326f166e281a5be4f91e1330ca049aeeb --method mtaRoot
```

## get current mta height

```bash
goloop rpc --uri http://localhost:9082/api/v3 call --to cx41c3699326f166e281a5be4f91e1330ca049aeeb --method mtaHeight
```

## get current mta caches

```bash
goloop rpc --uri http://localhost:9082/api/v3 call --to cx41c3699326f166e281a5be4f91e1330ca049aeeb --method mtaCaches
```

## register link to BMC

```bash
goloop rpc --uri http://localhost:9082/api/v3 sendtx call --to cxc332681329f869cbfb1840d71d57033eebccc682 --method addLink --key_store godWallet.json --step_limit 10000000000 --nid 3 --key_password gochain --param _link=btp://Edgeware.frontier/08425D9Df219f93d5763c3e85204cb5B4cE33aAa
```

## Configure link

```bash
goloop rpc --uri http://localhost:9082/api/v3 sendtx call --to cxc332681329f869cbfb1840d71d57033eebccc682 --method setLink --key_store godWallet.json --step_limit 10000000000 --nid 3 --key_password gochain --param _link=btp://Edgeware.frontier/08425D9Df219f93d5763c3e85204cb5B4cE33aAa --param _block_interval=0x3e8 --param _max_agg=0x10 --param _delay_limit=3
```

## Retrieve properties of link

```bash
goloop rpc --uri http://localhost:9082/api/v3 call --to cxc332681329f869cbfb1840d71d57033eebccc682 --method getStatus --param _link=btp://Edgeware.frontier/08425D9Df219f93d5763c3e85204cb5B4cE33aAa
```

## Register Relayer and Relay

- add relayer:

```bash
goloop rpc --uri http://localhost:9082/api/v3 sendtx call --to cxc332681329f869cbfb1840d71d57033eebccc682 --method addRelayer --key_store godWallet.json --step_limit 10000000000 --nid 3 --key_password gochain --param _addr=hxb6b5791be0b5ef67063b3c10b840fb81514db2fd --param _desc="edgeware relayer"
```

- add relay:
```bash
goloop rpc --uri http://localhost:9082/api/v3 sendtx call --to cxc332681329f869cbfb1840d71d57033eebccc682 --method addRelay --key_store godWallet.json --step_limit 10000000000 --nid 3 --key_password gochain --param _link=btp://Edgeware.frontier/08425D9Df219f93d5763c3e85204cb5B4cE33aAa --param _addr=hxb6b5791be0b5ef67063b3c10b840fb81514db2fd
```
## retrieve list of registered relayers

```bash
goloop rpc --uri http://localhost:9082/api/v3 call --to cxc332681329f869cbfb1840d71d57033eebccc682 --method getRelayers
```

- retrieve list of registered relay of link:

```bash
goloop rpc --uri http://localhost:9082/api/v3 call --to cxc332681329f869cbfb1840d71d57033eebccc682 --method getRelays --param _link=btp://Edgeware.frontier/08425D9Df219f93d5763c3e85204cb5B4cE33aAa
```
## call handleRelayMessage of BMC contract

```bash
goloop rpc --uri http://localhost:9082/api/v3 sendtx call --to cxc332681329f869cbfb1840d71d57033eebccc682 --method handleRelayMessage --key_store godWallet.json --step_limit 10000000000 --nid 3 --key_password gochain --param _prev=btp://Edgeware.frontier/08425D9Df219f93d5763c3e85204cb5B4cE33aAa --param _msg=
```

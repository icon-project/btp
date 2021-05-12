#### deploy to local

1. Deploy event decoder contract, use one of comands below
```
goloop rpc --uri http://localhost:9082/api/v3 sendtx deploy ./eventDecoder/build/libs/EventDecoderScore-optimized.jar \
    --key_store godWallet.json --key_password gochain \
    --nid 3 --step_limit 1361092001 \
    --content_type application/java

gradle eventDecoder:deployToLocal -P keystoreName=/Users/leclevietnam/mwork/btp/icon-bmv/godWallet.json -P keystorePass=gochain
```

2. Deploy bmv contract, use one of comands below
```
goloop rpc --uri http://localhost:9082/api/v3 sendtx deploy ./bmv/build/libs/BMV-optimized.jar \
    --key_store godWallet.json --key_password gochain \
    --nid 3 --step_limit 13610920010 \
    --content_type application/java \
    --param mtaOffset=0xa78 \
    --param bmc=hxb6b5791be0b5ef67063b3c10b840fb81514db2fd \
    --param net=0x1234.icon \
    --param mtaRootSize=0x10 \
    --param mtaCacheSize=0x10 \
    --param mtaIsAllowNewerWitness=0x1 \
    --param lastBlockHash=0x3fa04f87dc0cb872f5555bea778035d9ddc7e0e36465223ec26408bd7b979b99 \
    --param encodedValidators=4aCI3DQX1QWOxLRQPgwS6hoKib4gD-mJIkI9QzQBT6aw7g \
    --param eventDecoderAddress=cxd2fed8780aba08e946d967037b748d998f292cbd \
    --param currentSetId=0x11

gradle bmv:deployToLocal -P keystoreName=/Users/leclevietnam/mwork/btp/icon-bmv/godWallet.json -P keystorePass=gochain
```

2. get tx result
```bash
goloop rpc --uri http://localhost:9082/api/v3 txresult 0x0f5a6bd4e432b0755e0179fe7901df6a49731891b9c85c3e7fc4345e7acfe708
```

3. get list of base 64 encoded of RLP merkle tree accumulator
```bash
goloop rpc --uri http://localhost:9082/api/v3 call --to cx3a20c1b4901dfc4787b0f17f48f4dad1a8347b19 --method mta
```

3. get list of merkle tree accummulator roots
```bash
goloop rpc --uri http://localhost:9082/api/v3 call --to cx3a20c1b4901dfc4787b0f17f48f4dad1a8347b19 --method mtaRoot
```

4. get current mta height

```bash
goloop rpc --uri http://localhost:9082/api/v3 call --to cx3a20c1b4901dfc4787b0f17f48f4dad1a8347b19 --method mtaHeight
```

5. get current mta caches

```bash
goloop rpc --uri http://localhost:9082/api/v3 call --to cx3a20c1b4901dfc4787b0f17f48f4dad1a8347b19 --method mtaCaches
```

6. call handleRelayMessage
```bash
goloop rpc --uri http://localhost:9082/api/v3 sendtx call --to cx3a20c1b4901dfc4787b0f17f48f4dad1a8347b19 --method handleRelayMessage --key_store godWallet.json --step_limit 10000000000 --nid 3 --key_password gochain --param bmc=btp://0x1234.icon/hxb6b5791be0b5ef67063b3c10b840fb81514db2fd --param prev=btp://0x1234.icon/08425D9Df219f93d5763c3e85204cb5B4cE33aAa --param seq=0x6e --param msg=
```

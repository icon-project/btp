# build bmv score

## Requirements

Gradle  6.7.1
Java OpenJDK 11
goloop/gochain 0.9.7 or higher

## build event decoder SCORE

BMV smart contracts consists of Event decoder score that exists per chain and BMV score (either Sovereign chain type or Relay chain and Parachain type). According to the chain you're trying to connect, to build a complete BMV score, you should build Event decoder score (or use predefined ones) and choose BMV score type.

Each chain has different type of event and data structure, we need meta data file to know what modules and data structure these chain has. Event decoder code will be genereated base on meta data file.

### predefined metaData

we have predefined meta data for Kusama, Edgeware and Moonriver chain. It's stored in `./eventDecoder/KusamaMetaData.json`, `./eventDecoder/EdgewareMetaData.json` and `./eventDecoder/MoonriverMetaData.json`. To build score for these chains use the following command:

```bash
cd eventDecoder
gradle buildKusamaDecoder
gradle buildEdgewareDecoder
gradle buildMoonriverDecoder
```

`KusamaEventDecoder-optimized.jar`, `EdgewareEventDecoder-optimized.jar`, `MoonriverDecoder-optimized.jar` file will be generated in `eventDecoder/build/libs/`

### get meta data of chain

In case that you need to build event decoder for other parachains rather than there chain above, you can use two ways bellow to get meta data file.

#### get through typescript helper

We have set up yarn project to get metadata through typescript.

1. Go to helper directory

```bash
cd javascore/bmv/helper
```

2. Install yarn package

```bash
yarn install
```

3. Specify chain endpoint, open file `javascore/bmv/helper/getMetaData.ts`, replace `CHAIN_ENDPOINT` in line 10 with your chain endpoint

4. Run `yarn getMetaData` in `javascore/bmv/helper` directory

```bash
cd javascore/bmv/helper

yarn getMetaData
```

### build event decoder with meta data

1. Store meta data in `metadata.json` file
2. Get metaData file path
3. execute 
```bash
cd eventDecoder
gradle loadMetaData -PmetaDataFilePath=${path of your meta data file}
gradle optimizedJar
```

*Note* Substrate base chain may redefine data default type. For example, moonbeam has modify default AccountId (32 bytes) to EthereumAccountId (20 bytes). To adapt that change, we provide `accountIdSize` to specify size of account id

```bash
cd eventDecoder
gradle loadMetaData -PmetaDataFilePath=${path of your meta data file} -PaccountIdSize=20
gradle optimizedJar
```

`eventDecoder-optimized.jar` file will be generated in `eventDecoder/build/libs/`

## build BMV SCORE
### build BMV SCORE for sovereign chain

```bash
cd sovereignChain
gradle optimizedJar
```

`SovereignChain-BMV-optimized.jar` file will be generated in `sovereignChain/build/libs/`

### build BMV SCORE for para chain

```bash
cd parachain
gradle optimizedJar
```

`parachain-BMV-optimized.jar` file will be generated in `parachain/build/libs/`

## run integration test

Before running integration test, make sure local icon node is running. Follow https://github.com/icon-project/gochain-local to know how to start your local node.

We expect that your local ICON node using port 9082 for RPC and default godWallet.json. If you change any default configuration, please update it to `testinteg/conf/env.props` file.
### sovereign chain intergration test

Data generated for test is base on Edgeware chain, to be albe to test on other chain, plase update test data generator and event decoder score first. To run intergration please build Edgeware event decoder first

```bash
# build edgeware event decoder score
cd eventDecoder
gradle buildEdgewareDecoder

# run integration test
cd sovereignChain
gradle integrationTest
```
### para chain intergration test

Data generated for test is base on Kusama relay chain and Moonriver parachains, to be albe to test on other chain, plase update test data generator and event decoder score first. To run intergration please build Kusma and Moonriver event decoder first

```bash
# build Kusama and Moonriver event decoder score
cd eventDecoder
gradle buildKusamaDecoder
gradle buildMoonriverDecoder

# run integration test
cd parachain
gradle integrationTest
```

# deploy BMV score

## deploy sovereign chain BMV
### Deploy BMC smart contract

```
goloop rpc --uri http://localhost:9082/api/v3 sendtx deploy bmc.zip \
    --key_store godWallet.json --key_password gochain \
    --nid 3 --step_limit 13610920001 \
    --content_type application/zip \
    --param _net="0x07.icon"
```

- get transaction result and score address:

```
goloop rpc --uri http://localhost:9082/api/v3 txresult 0x2a297fc118728494ed284845af620c1c3395a228cfc60983474fd5e04423b468
```

### Deploy event decoder contract, use one of comands below

*note* score file name can be changed

```
goloop rpc --uri http://localhost:9082/api/v3 sendtx deploy ./eventDecoder/build/libs/EdgewareEventDecoder-optimized.jar \
    --key_store godWallet.json --key_password gochain \
    --nid 3 --step_limit 13610920010 \
    --content_type application/java
```

### Deploy bmv contract

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
goloop rpc --uri http://localhost:9082/api/v3 sendtx deploy ./sovereignChain/build/libs/SovereignChain-BMV-optimized.jar \
    --key_store godWallet.json --key_password gochain \
    --nid 3 --step_limit 3519157719 \
    --content_type application/java \
    --param mtaOffset=0x28 \
    --param bmc=cxaba68cb5d3dbcd0ae1ca626a85f2ddbfa3be6559 \
    --param net=0x123.edge \
    --param mtaRootSize=0x10 \
    --param mtaCacheSize=0x10 \
    --param mtaIsAllowNewerWitness=0x1 \
    --param lastBlockHash=0xf94a5e262b7192fb951813c50de551761fcc13f2493f41a2f0105cb931cedd89 \
    --param encodedValidators=4aCI3DQX1QWOxLRQPgwS6hoKib4gD-mJIkI9QzQBT6aw7g \
    --param eventDecoderAddress=cx4b577d3b165c14b313294c6babef37fcb3ae7e7d \
    --param currentSetId=0x0
```

### get encode of Merkle tree accumulator

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
goloop rpc --uri http://localhost:9082/api/v3 call --to cxf3e336ff003356e3ee3873c31c929e6c01ef739b --method mta
```

### get list of merkle tree accummulator roots
```bash
goloop rpc --uri http://localhost:9082/api/v3 call --to cxf3e336ff003356e3ee3873c31c929e6c01ef739b --method mtaRoot
```

### get current mta height

```bash
goloop rpc --uri http://localhost:9082/api/v3 call --to cxf3e336ff003356e3ee3873c31c929e6c01ef739b --method mtaHeight
```

### get current mta caches

```bash
goloop rpc --uri http://localhost:9082/api/v3 call --to cxf3e336ff003356e3ee3873c31c929e6c01ef739b --method mtaCaches
```

### register link to BMC

```bash
goloop rpc --uri http://localhost:9082/api/v3 sendtx call --to cxf3e336ff003356e3ee3873c31c929e6c01ef739b --method addLink --key_store godWallet.json --step_limit 10000000000 --nid 3 --key_password gochain --param _link=btp://0x123.edge/08425D9Df219f93d5763c3e85204cb5B4cE33aAa
```

### Configure link

```bash
goloop rpc --uri http://localhost:9082/api/v3 sendtx call --to cxf3e336ff003356e3ee3873c31c929e6c01ef739b --method setLink --key_store godWallet.json --step_limit 10000000000 --nid 3 --key_password gochain --param _link=btp://0x123.edge/08425D9Df219f93d5763c3e85204cb5B4cE33aAa --param _block_interval=0x3e8 --param _max_agg=0x10 --param _delay_limit=3
```

### Retrieve properties of link

```bash
goloop rpc --uri http://localhost:9082/api/v3 call --to cxf3e336ff003356e3ee3873c31c929e6c01ef739b --method getStatus --param _link=btp://0x123.edge/08425D9Df219f93d5763c3e85204cb5B4cE33aAa
```

### Register Relayer and Relay

- add relayer:

```bash
goloop rpc --uri http://localhost:9082/api/v3 sendtx call --to cxf3e336ff003356e3ee3873c31c929e6c01ef739b --method addRelayer --key_store godWallet.json --step_limit 10000000000 --nid 3 --key_password gochain --param _addr=hxb6b5791be0b5ef67063b3c10b840fb81514db2fd --param _desc="edgeware relayer"
```

- add relay:
```bash
goloop rpc --uri http://localhost:9082/api/v3 sendtx call --to cxf3e336ff003356e3ee3873c31c929e6c01ef739b --method addRelay --key_store godWallet.json --step_limit 10000000000 --nid 3 --key_password gochain --param _link=btp://0x123.edge/08425D9Df219f93d5763c3e85204cb5B4cE33aAa --param _addr=hxb6b5791be0b5ef67063b3c10b840fb81514db2fd
```
### retrieve list of registered relayers

```bash
goloop rpc --uri http://localhost:9082/api/v3 call --to cxf3e336ff003356e3ee3873c31c929e6c01ef739b --method getRelayers
```

- retrieve list of registered relay of link:

```bash
goloop rpc --uri http://localhost:9082/api/v3 call --to cxf3e336ff003356e3ee3873c31c929e6c01ef739b --method getRelays --param _link=btp://0x123.edge/08425D9Df219f93d5763c3e85204cb5B4cE33aAa
```
### call handleRelayMessage of BMC contract

```bash
goloop rpc --uri http://localhost:9082/api/v3 sendtx call --to cxf3e336ff003356e3ee3873c31c929e6c01ef739b --method handleRelayMessage --key_store godWallet.json --step_limit 10000000000 --nid 3 --key_password gochain --param _prev=btp://0x123.edge/08425D9Df219f93d5763c3e85204cb5B4cE33aAa --param _msg=
```

## deploy para chain BMV
### Deploy BMC smart contract

```
goloop rpc --uri http://localhost:9082/api/v3 sendtx deploy bmc.zip \
    --key_store godWallet.json --key_password gochain \
    --nid 3 --step_limit 13610920001 \
    --content_type application/zip \
    --param _net="0x3.icon"
```

- get transaction result and score address:

```
goloop rpc --uri http://localhost:9082/api/v3 txresult 0x6fd5dd9f2b678bcfd635c9c9886eea13c41ae3dacf55567d3fbdde6adca9ec10
```

### Deploy event decoder contract for parachain and relay chain, use one of comands below

*note* change score `jar` file name according to relay event decoder and para decoder file.

```
goloop rpc --uri http://localhost:9082/api/v3 sendtx deploy ./eventDecoder/build/libs/KusamaEventDecoder-optimized.jar \
    --key_store godWallet.json --key_password gochain \
    --nid 3 --step_limit 13610920010 \
    --content_type application/java

goloop rpc --uri http://localhost:9082/api/v3 sendtx deploy ./eventDecoder/build/libs/MoonriverEventDecoder-optimized.jar \
    --key_store godWallet.json --key_password gochain \
    --nid 3 --step_limit 13610920010 \
    --content_type application/java
```

### Deploy bmv contract

### Parameter:

- `relayMtaOffset`: offset of Merkle Tree Accumulator, block height BMV start to sync block of relay chain
- `paraMtaOffset`: offset of Merkle Tree Accumulator, block height BMV start to sync block of para chain
- `bmc`: address of BMC score
- `net`: network that BMV handle (of parachain), (example 0x501.pra)
- `mtaRootSize`: size of MTA roots use for both parachain and relay chain
- `mtaCacheSize`: size of mta cache use for both parachain and relay chain
- `mtaIsAllowNewerWitness`: is allow to verify newer witness (client MTA height higher than contract MTA height)
- `relayLastBlockHash`: hash of previous block (relayMtaOffset) of relay chain, BMV use this to check previous hash of updating block equal to last block hash
- `paraLastBlockHash`: hash of previous block (paraMtaOffset) of para chain, BMV use this to check previous hash of updating block equal to last block hash
- `encodedValidators`: `Base64(RLP.encode(List<byte[]> validatorPublicKey))`, encoded of validators list of relay chain
- `relayEventDecoderAddress`: address of event decoder score for relay chain
- `paraEventDecoderAddress`: address of event decoder score for para chain
- `relayCurrentSetId`: current validator set id of relay chain
- `paraChainId`: para chain id

```
goloop rpc --uri http://localhost:9082/api/v3 sendtx deploy ./parachain/build/libs/parachain-BMV-optimized.jar \
    --key_store godWallet.json --key_password gochain \
    --nid 3 --step_limit 3519157719 \
    --content_type application/java \
    --param relayMtaOffset=0x48 \
    --param paraMtaOffset=0x38 \
    --param bmc=cxdf018cda3b8f041b2468775412f683e2650dac62 \
    --param net=0x501.pra \
    --param mtaRootSize=0x10 \
    --param mtaCacheSize=0x10 \
    --param mtaIsAllowNewerWitness=0x1 \
    --param relayLastBlockHash=0xf94a5e262b7192fb951813c50de551761fcc13f2493f41a2f0105cb931cedd89 \
    --param paraLastBlockHash=0xcd9932baef2bc7fc9bca5155594394c96e15491e6eb45587df66fbafe43a3dbc \
    --param encodedValidators=4aCI3DQX1QWOxLRQPgwS6hoKib4gD-mJIkI9QzQBT6aw7g \
    --param relayEventDecoderAddress=cx567425fd292e032c79617ebeb38acf2e34b01497 \
    --param paraEventDecoderAddress=cxa6525b38f8bf513fdd55c506f7cf6bc95d0d30a0 \
    --param relayCurrentSetId=0x0 \
    --param paraChainId=0x3e9
```

### get encode of Merkle tree accumulator

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
goloop rpc --uri http://localhost:9082/api/v3 call --to cxb46cc4f33b80a5a6e8312968e325f8834f9fc810 --method paraMta
goloop rpc --uri http://localhost:9082/api/v3 call --to cx0fe6bff6d229bf78d365534d0a20ddd9c5683f22 --method relayMta
```

### get list of merkle tree accummulator roots
```bash
goloop rpc --uri http://localhost:9082/api/v3 call --to cx0fe6bff6d229bf78d365534d0a20ddd9c5683f22 --method paraMtaRoot
goloop rpc --uri http://localhost:9082/api/v3 call --to cx0fe6bff6d229bf78d365534d0a20ddd9c5683f22 --method relayMtaRoot
```

### get current mta height

```bash
goloop rpc --uri http://localhost:9082/api/v3 call --to cx0fe6bff6d229bf78d365534d0a20ddd9c5683f22 --method paraMtaHeight
goloop rpc --uri http://localhost:9082/api/v3 call --to cx0fe6bff6d229bf78d365534d0a20ddd9c5683f22 --method relayMtaHeight
```

### get current mta caches

```bash
goloop rpc --uri http://localhost:9082/api/v3 call --to cx0fe6bff6d229bf78d365534d0a20ddd9c5683f22 --method relayMtaCaches
goloop rpc --uri http://localhost:9082/api/v3 call --to cx0fe6bff6d229bf78d365534d0a20ddd9c5683f22 --method paraMtaCaches
```

### register link to BMC

```bash
goloop rpc --uri http://localhost:9082/api/v3 sendtx call --to cx9e8a72eb07ae70bbfb8f5b835a20cb25688a549c --method addLink --key_store godWallet.json --step_limit 10000000000 --nid 3 --key_password gochain --param _link=btp://moonriver.parachain/08425D9Df219f93d5763c3e85204cb5B4cE33aAa
```

### Configure link

```bash
goloop rpc --uri http://localhost:9082/api/v3 sendtx call --to cx9e8a72eb07ae70bbfb8f5b835a20cb25688a549c --method setLink --key_store godWallet.json --step_limit 10000000000 --nid 3 --key_password gochain --param _link=btp://0x501.pra/08425D9Df219f93d5763c3e85204cb5B4cE33aAa --param _block_interval=0x3e8 --param _max_agg=0x10 --param _delay_limit=3
```

### Retrieve properties of link

```bash
goloop rpc --uri http://localhost:9082/api/v3 call --to cx9e8a72eb07ae70bbfb8f5b835a20cb25688a549c --method getStatus --param _link=btp://0x501.pra/08425D9Df219f93d5763c3e85204cb5B4cE33aAa
```

### Register Relayer and Relay

- add relayer:

```bash
goloop rpc --uri http://localhost:9082/api/v3 sendtx call --to cx9e8a72eb07ae70bbfb8f5b835a20cb25688a549c --method addRelayer --key_store godWallet.json --step_limit 10000000000 --nid 3 --key_password gochain --param _addr=hxb6b5791be0b5ef67063b3c10b840fb81514db2fd --param _desc="moonbeam parachain relayer"
```

- add relay:
```bash
goloop rpc --uri http://localhost:9082/api/v3 sendtx call --to cx9e8a72eb07ae70bbfb8f5b835a20cb25688a549c --method addRelay --key_store godWallet.json --step_limit 10000000000 --nid 3 --key_password gochain --param _link=btp://0x501.pra/08425D9Df219f93d5763c3e85204cb5B4cE33aAa --param _addr=hxb6b5791be0b5ef67063b3c10b840fb81514db2fd
```
### retrieve list of registered relayers

```bash
goloop rpc --uri http://localhost:9082/api/v3 call --to cx9e8a72eb07ae70bbfb8f5b835a20cb25688a549c --method getRelayers
```

- retrieve list of registered relay of link:

```bash
goloop rpc --uri http://localhost:9082/api/v3 call --to cx9e8a72eb07ae70bbfb8f5b835a20cb25688a549c --method getRelays --param _link=btp://0x501.pra/08425D9Df219f93d5763c3e85204cb5B4cE33aAa
```
### call handleRelayMessage of BMC contract

```bash
goloop rpc --uri http://localhost:9082/api/v3 sendtx call --to cx0fe6bff6d229bf78d365534d0a20ddd9c5683f22 --method handleRelayMessage --key_store godWallet.json --step_limit 10000000000 --nid 3 --key_password gochain --param _prev=btp://0x501.pra/08425D9Df219f93d5763c3e85204cb5B4cE33aAa --param _msg=
```

# How to deploy contracts to networks

## Prerequisites

Please check your installation with [build](build.md) prerequisities and make sure running these deployments on a computer/server have a stable network condition

## Solidity

### Prepare environment for deployment

Notes: This deployment will require user, to use raw text private key as environment variable, make sure you don't let any party access to process manager while deployment process is running. And clear up the shell history after deployment


1. `PRIVATE_KEYS`

User will prepare this key with raw test and add it for each deploy command to avoid export it to other process, this key will use for all deployment. For example:

```bash
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" BMC_BTP_NET="0x507.pra" truffle migrate --network moonbase
```

2. `SOLIDITY_DIST_DIR`

```bash
make dist-sol
export SOLIDITY_DIST_DIR=${PWD}/build/contracts/solidity
```

### Deploy BMC

```bash
yarn install --production --cwd $SOLIDITY_DIST_DIR/bmc
rm -rf $SOLIDITY_DIST_DIR/bmc/.openzeppelin $SOLIDITY_DIST_DIR/bmc/build

truffle compile --all --working_directory $SOLIDITY_DIST_DIR/bmc

# @param
# - BMC_BTP_NET: Chain ID and name of a network that BMC is going to deploy on, e.g. 
# To get Chain ID. Depends on chain 
# curl https://rpc.testnet.moonbeam.network \
#    -X POST \
#    -H "Content-Type: application/json" \
#    -d '{"jsonrpc":"2.0","method":"eth_chainId","params": [],"id":1}'
# {"jsonrpc":"2.0","result":"0x507","id":1}
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" \
BMC_BTP_NET="0x507.pra" \
truffle migrate --network moonbase --working_directory $SOLIDITY_DIST_DIR/bmc

### Output
> Warning: possible unsupported (undocumented in help) command line option: --working_directory

Compiling your contracts...
===========================
> Everything is up to date, there is nothing to compile.



Starting migrations...
======================
> Network name:    'moonbase'
> Network id:      1287
> Block gas limit: 15000000 (0xe4e1c0)


1_deploy_bmc.js
===============

   Deploying 'BMCManagement'
   -------------------------
   > transaction hash:    0xbea40fa8b109640c0f5f8262f0bc947189f9e9b67fdd1ad5ce1a1e085d4bd281
   > Blocks: 1            Seconds: 20
   > contract address:    0x1c2b9AC5EF15b91e10591C57dfAa9f911d6F561f
   > block number:        813641
   > block timestamp:     1632368442
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             525.244294493360370548
   > gas used:            5036784 (0x4cdaf0)
   > gas price:           20 gwei
   > value sent:          0 ETH
   > total cost:          0.10073568 ETH


   Deploying 'ProxyAdmin'
   ----------------------
   > transaction hash:    0x8924900a42a74e4291f5d5c9801b66b66464b5a0561cfe1f07af9f14360a852c
   > Blocks: 1            Seconds: 16
   > contract address:    0x4642233D82232960B2A05469867f98f7181fc9Da
   > block number:        813643
   > block timestamp:     1632368466
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             525.234640093360370548
   > gas used:            482720 (0x75da0)
   > gas price:           20 gwei
   > value sent:          0 ETH
   > total cost:          0.0096544 ETH


   Deploying 'TransparentUpgradeableProxy'
   ---------------------------------------
   > transaction hash:    0xd2009f5e64c00afe77f8c2609e264cfb3423d85842eeab7e6e7fba346e628be9
   > Blocks: 1            Seconds: 16
   > contract address:    0xC933Dbb5C348D8675F3E2A8A34F49817f038d723
   > block number:        813645
   > block timestamp:     1632368490
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             525.221445753360370548
   > gas used:            659717 (0xa1105)
   > gas price:           20 gwei
   > value sent:          0 ETH
   > total cost:          0.01319434 ETH


   Deploying 'BMCPeriphery'
   ------------------------
   > transaction hash:    0xa3c2743454e2e493fb86d908ac755d2d3fb5899e57d86eb12dd9eca923d098ea
   > Blocks: 1            Seconds: 16
   > contract address:    0x94F167c2ce860a21f711b48eAB285DaFbb6da856
   > block number:        813647
   > block timestamp:     1632368514
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             525.128153153360370548
   > gas used:            4664630 (0x472d36)
   > gas price:           20 gwei
   > value sent:          0 ETH
   > total cost:          0.0932926 ETH


   Deploying 'TransparentUpgradeableProxy'
   ---------------------------------------
   > transaction hash:    0x0b216bd7f3b5f6161485028a278fd2e95b83f5cbfe7eccd009c2242b2623442f
   > Blocks: 1            Seconds: 16
   > contract address:    0x0DaAAb07b078fd9cC8ebc7f980aeFd0e2aBa2Ec6
   > block number:        813649
   > block timestamp:     1632368538
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             525.113319093360370548
   > gas used:            741703 (0xb5147)
   > gas price:           20 gwei
   > value sent:          0 ETH
   > total cost:          0.01483406 ETH

   > Saving artifacts
   -------------------------------------
   > Total cost:          0.23171108 ETH


Summary
=======
> Total deployments:   5
> Final cost:          0.23171108 ETH


```

### Deploy NativeCoin BSH

```bash
yarn install --production --cwd $SOLIDITY_DIST_DIR/bsh
rm -rf $SOLIDITY_DIST_DIR/bsh/.openzeppelin $SOLIDITY_DIST_DIR/bsh/build

truffle compile --all --working_directory $SOLIDITY_DIST_DIR/bsh

# @params:
# - BMC_PERIPHERY_ADDRESS: an address on chain of BMCPeriphery contract
# This address is queried after deploying BMC contracts
# For example: BMC_PERIPHERY_ADDRESS = 0x5CC307268a1393AB9A764A20DACE848AB8275c46
# - BSH_COIN_NAME: a native coin name of Moonbase Alpha Testnet Network - DEV
# - BSH_COIN_FEE: a charging fee ratio of each request, e.g. 100/10000 = 1%
# - BSH_SERVICE: a service name of BSH contract, e.g. 'CoinTransfer'
# This service name is unique in one network. And it must be registered to BMC contract to activate
# BMC contract checks its service name whether it's already existed
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" \
BMC_PERIPHERY_ADDRESS="0x0DaAAb07b078fd9cC8ebc7f980aeFd0e2aBa2Ec6" \
BSH_COIN_URL="https://moonbeam.network/" \
BSH_SERVICE="nativecoin" \
BSH_COIN_NAME="DEV" \
BSH_COIN_FEE="100" \
BSH_FIXED_FEE="500000" \
truffle migrate --network moonbase --working_directory $SOLIDITY_DIST_DIR/bsh

### Output
Compiling your contracts...
===========================
> Everything is up to date, there is nothing to compile.



Starting migrations...
======================
> Network name:    'moonbase'
> Network id:      1287
> Block gas limit: 15000000 (0xe4e1c0)


1_deploy_bsh.js
===============

   Deploying 'BSHCore'
   -------------------
   > transaction hash:    0xe46886ed5e57276d71220c955358b98fbc18879b90d42566ee91dfb157e1d2dc
   > Blocks: 2            Seconds: 17
   > contract address:    0x72b499eBf8d3DCd105C5C993BEEe2c063D1a0a88
   > block number:        799727
   > block timestamp:     1632195978
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             554.162390421485464167
   > gas used:            4740289 (0x4854c1)
   > gas price:           20 gwei
   > value sent:          0 ETH
   > total cost:          0.09480578 ETH

   Deploying 'BSHPeriphery'
   ------------------------
   > transaction hash:    0xf687360744786ff28ac070e6c9af1afe3ef0fe4d779875c935a757fe2a72edbe
   > Blocks: 2            Seconds: 16
   > contract address:    0xf420840CC519B048152799d2A6Ec79aC0151c34C
   > block number:        799731
   > block timestamp:     1632196026
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             554.069466121485464167
   > gas used:            3756820 (0x395314)
   > gas price:           20 gwei
   > value sent:          0 ETH
   > total cost:          0.0751364 ETH


   Deploying 'TransparentUpgradeableProxy'
   ---------------------------------------
   > transaction hash:    0xf883d4ca59150dfff286e833a0f01e147df51a914db6b0f7376d5f07fe8bc2c7
   > Blocks: 1            Seconds: 12
   > contract address:    0xb1fCF2074bB3bAEDF48DF816c7F82C2dA3C19A06
   > block number:        799751
   > block timestamp:     1632196266
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             554.002980381485464167
   > gas used:            889395 (0xd9233)
   > gas price:           20 gwei
   > value sent:          0 ETH
   > total cost:          0.0177879 ETH


   Deploying 'TransparentUpgradeableProxy'
   ---------------------------------------
   > transaction hash:    0x07a9ab174a20e00d11f3437746de26020800a4f5b0d833f12816607df12a9fa0
   > Blocks: 1            Seconds: 16
   > contract address:    0x8Bc43638284f8Acf446Ede6d352EC7ea50B5305f
   > block number:        799753
   > block timestamp:     1632196290
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             553.989702241485464167
   > gas used:            663907 (0xa2163)
   > gas price:           20 gwei
   > value sent:          0 ETH
   > total cost:          0.01327814 ETH

   > Saving artifacts
   -------------------------------------
   > Total cost:          0.03106604 ETH


Summary
=======
> Total deployments:   2
> Final cost:          0.03106604 ETH

```

### Deloy BMV

```bash
yarn install --production --cwd $SOLIDITY_DIST_DIR/bmv
rm -rf $SOLIDITY_DIST_DIR/bmv/.openzeppelin $SOLIDITY_DIST_DIR/bmv/build

truffle compile --all --working_directory $SOLIDITY_DIST_DIR/bmv

# @params
# - BMC_PERIPHERY_ADDRESS: an address on chain of BMCPeriphery contract
# This address is queried after deploying BMC contracts
# For example: BMC_PERIPHERY_ADDRESS = 0x5CC307268a1393AB9A764A20DACE848AB8275c46
# - BMV_ICON_NET: Chain ID and name of a network that BMV is going to verify BTP Message
# - BMV_ICON_INIT_OFFSET: a block height when ICON-BMC was deployed
# - BMV_ICON_LASTBLOCK_HASH: a hash of the above block
# - BMV_ICON_ENCODED_VALIDATORS: a result of ICON JSON-RPC method `icx_getDataByHash` with the input is
# PreviousBlockHeader.NextValidatorHash. So, to get this param for block N, you must get BlockHeader of N - 1
# User can execute to ge the result : 
#
# make iconvalidators
# 
# GOLOOP_RPC_URI="https://btp.net.solidwallet.io/api/v3/icon_dex" bin/iconvalidators build 3453901
#
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" \
BMC_PERIPHERY_ADDRESS="0x0DaAAb07b078fd9cC8ebc7f980aeFd0e2aBa2Ec6" \
BMV_ICON_NET="0x42.icon" \
BMV_ICON_ENCODED_VALIDATORS="0xf8589500edeb1b82b94d548ec440df553f522144ca83fb8d9500d63c4c73b623e97f67407d687af4efcfe486a51595007882dace25ff7e947d3a25178a2a1162874cfddc95000458d8b6f649a9e005963dc9a72669c89ed52d85" \
BMV_ICON_INIT_OFFSET="3453901" \
BMV_ICON_INIT_ROOTSSIZE="10" \
BMV_ICON_INIT_CACHESIZE="10" \
BMV_ICON_LASTBLOCK_HASH="0x76f0f0d5a5fa51e90a95076bd572469350de8ca42cfccb848a6584ecde35cf5c" \
truffle migrate --network moonbase --working_directory $SOLIDITY_DIST_DIR/bmv

### Output
> Warning: possible unsupported (undocumented in help) command line option: --working_directory

Compiling your contracts...
===========================
> Everything is up to date, there is nothing to compile.



Starting migrations...
======================
> Network name:    'moonbase'
> Network id:      1287
> Block gas limit: 15000000 (0xe4e1c0)


1_deploy_bmv.js
===============

   Deploying 'DataValidator'
   -------------------------
   > transaction hash:    0x1e938175d619a7e919292e563f0c63fed30897abed7aaa601872ae91f3960c0a
   > Blocks: 2            Seconds: 24
   > contract address:    0x66c91AC416A76De7fAea5186Dd9e2862D8d31772
   > block number:        813688
   > block timestamp:     1632369006
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             525.069954813360370548
   > gas used:            2120414 (0x205ade)
   > gas price:           20 gwei
   > value sent:          0 ETH
   > total cost:          0.04240828 ETH


   Deploying 'TransparentUpgradeableProxy'
   ---------------------------------------
   > transaction hash:    0x98c41eeb7b2f8cc57045e773b0466f72a77e265d54ea9b077a47f1f0fb46d423
   > Blocks: 1            Seconds: 16
   > contract address:    0xe97906D3BDEFea724bA00aadf234969Fca1B8782
   > block number:        813690
   > block timestamp:     1632369030
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             525.057603653360370548
   > gas used:            617558 (0x96c56)
   > gas price:           20 gwei
   > value sent:          0 ETH
   > total cost:          0.01235116 ETH


   Deploying 'BMV'
   ---------------
   > transaction hash:    0xe400bf09228bf317f378a4022dc2594c28dbefb0eb249258763b4d7963938f81
   > Blocks: 2            Seconds: 20
   > contract address:    0xFAF49E2602b31B35687c13c6BA9565Fa0D5990B1
   > block number:        813692
   > block timestamp:     1632369054
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             524.965505153360370548
   > gas used:            4604925 (0x4643fd)
   > gas price:           20 gwei
   > value sent:          0 ETH
   > total cost:          0.0920985 ETH


   Deploying 'TransparentUpgradeableProxy'
   ---------------------------------------
   > transaction hash:    0x2393e7e5dba599bd1ef839cfb4ad7d9ee60168f6d33b9e98a025777384f93c0b
   > Blocks: 1            Seconds: 16
   > contract address:    0x4e3435c8ae2bE8Fe5E67c2d7f44e4E32a1e5d159
   > block number:        813694
   > block timestamp:     1632369078
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             524.942983933360370548
   > gas used:            1126061 (0x112ead)
   > gas price:           20 gwei
   > value sent:          0 ETH
   > total cost:          0.02252122 ETH

   > Saving artifacts
   -------------------------------------
   > Total cost:          0.16937916 ETH


Summary
=======
> Total deployments:   4
> Final cost:          0.16937916 ETH


```

### Configure Contracts

```bash
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" \
NEXTLINK_BTP_NET="0x42.icon" \
CURRENTLINK_BMV_ADDRESS="0x4e3435c8ae2bE8Fe5E67c2d7f44e4E32a1e5d159" \
truffle exec $SOLIDITY_DIST_DIR/bmc/scripts/add_verifier.js --network moonbase --working_directory $SOLIDITY_DIST_DIR/bmc

### Output

{
  tx: '0x99cf2d64fabe389cbd882896b9310913a73438bd1d23a52c3e846ceee45a2dec',
  receipt: {
    blockHash: '0xd62a099421c5301bb39044f59a60c234af3e2e6fb6153b489150afbf203d56c3',
    blockNumber: 813716,
    contractAddress: null,
    cumulativeGasUsed: 112146,
    from: '0x4bb718cb404787bf97bb012bb08096602fb9544b',
    gasUsed: 91146,
    logs: [],
    logsBloom: '0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000',
    status: true,
    to: '0xc933dbb5c348d8675f3e2a8a34f49817f038d723',
    transactionHash: '0x99cf2d64fabe389cbd882896b9310913a73438bd1d23a52c3e846ceee45a2dec',
    transactionIndex: 1,
    rawLogs: []
  },
  logs: []
}
```

```bash
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" \
NEXTLINK_BTP_ADDRESS="btp://0x42.icon/cx744270a51947966520d29f4d584115bb473f3dbc" \
NEXTLINK_BLOCK_INTERVAL=12 \
NEXTLINK_ROTATION_MAX_AGGERATION=10 \
NEXTLINK_ROTATION_DELAY_LIMIT=3 \
RELAY_ADDRESSES="0x1e9eb23057adf446777c7fab1a6c565cb8977ab5" \
truffle exec $SOLIDITY_DIST_DIR/bmc/scripts/add_link_set_link_add_relay.js --network moonbase --working_directory $SOLIDITY_DIST_DIR/bmc

### Output

{
  tx: '0x0174808ca26f158f8cb3732cd5c70c82b6ee5a8fa9ce745f2a1bc37f0a325228',
  receipt: {
    blockHash: '0x16a78f3ebecfe1fd914877be0497f20db8eb497a668287fd92e1eded143a880f',
    blockNumber: 813723,
    contractAddress: null,
    cumulativeGasUsed: 851807,
    from: '0x4bb718cb404787bf97bb012bb08096602fb9544b',
    gasUsed: 436359,
    logs: [],
    logsBloom: '0x00000002000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000040000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000080000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000010000000000000000000000000000004000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000',
    status: true,
    to: '0xc933dbb5c348d8675f3e2a8a34f49817f038d723',
    transactionHash: '0x0174808ca26f158f8cb3732cd5c70c82b6ee5a8fa9ce745f2a1bc37f0a325228',
    transactionIndex: 10,
    rawLogs: [ [Object] ]
  },
  logs: []
}
{
  tx: '0xd2928a6d641da39f2e589c3f5e2f6ce5697cd09bd0f7a0c922f4ebdfbd359ba7',
  receipt: {
    blockHash: '0xb3abad866d4532f1d27b50d77aeeedebcb5e614ab5e03ebfc711b59314b0199c',
    blockNumber: 813725,
    contractAddress: null,
    cumulativeGasUsed: 179122,
    from: '0x4bb718cb404787bf97bb012bb08096602fb9544b',
    gasUsed: 179122,
    logs: [],
    logsBloom: '0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000',
    status: true,
    to: '0xc933dbb5c348d8675f3e2a8a34f49817f038d723',
    transactionHash: '0xd2928a6d641da39f2e589c3f5e2f6ce5697cd09bd0f7a0c922f4ebdfbd359ba7',
    transactionIndex: 0,
    rawLogs: []
  },
  logs: []
}
{
  tx: '0x3aa247bfbdbca7fd8c83931245995dc15eccb5a879b9cb19a27e5df2c850dcd5',
  receipt: {
    blockHash: '0x584546a04017cf388a68b05a1eb69bf9f8281098cde15f498de0b9eca46bf503',
    blockNumber: 813737,
    contractAddress: null,
    cumulativeGasUsed: 94482,
    from: '0x4bb718cb404787bf97bb012bb08096602fb9544b',
    gasUsed: 94482,
    logs: [],
    logsBloom: '0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000',
    status: true,
    to: '0xc933dbb5c348d8675f3e2a8a34f49817f038d723',
    transactionHash: '0x3aa247bfbdbca7fd8c83931245995dc15eccb5a879b9cb19a27e5df2c850dcd5',
    transactionIndex: 0,
    rawLogs: []
  },
  logs: []
}
```

```bash
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" \
CURRENTLINK_BSH_SERVICENAME="nativecoin" \
CURRENTLINK_BSH_ADDRESS="0xc06BE3322f7118B14843861B424698646875CE64" \
truffle exec $SOLIDITY_DIST_DIR/bmc/scripts/add_bsh_service.js --network moonbase --working_directory $SOLIDITY_DIST_DIR/bmc

### Output
{
  tx: '0x7cf789c3ba1b677235614c09427547745fdc68e05697d4d9e24f702ed11baeee',
  receipt: {
    blockHash: '0xc891ff86a8c66e78eeb3f71c9ded1370f5d42a920f5643ede1aa7980fee78516',
    blockNumber: 813745,
    contractAddress: null,
    cumulativeGasUsed: 277785,
    from: '0x4bb718cb404787bf97bb012bb08096602fb9544b',
    gasUsed: 91061,
    logs: [],
    logsBloom: '0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000',
    status: true,
    to: '0xc933dbb5c348d8675f3e2a8a34f49817f038d723',
    transactionHash: '0x7cf789c3ba1b677235614c09427547745fdc68e05697d4d9e24f702ed11baeee',
    transactionIndex: 4,
    rawLogs: []
  },
  logs: []
}****
```

```bash
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" \
NEXTLINK_BTP_NATIVECOIN_NAME="ICX" \
truffle exec $SOLIDITY_DIST_DIR/bsh/scripts/register_coin.js --network moonbase --working_directory $SOLIDITY_DIST_DIR/bsh  
```

### JAVAScore

TODO

### Transfer ICX to DEV

```bash
USER_PRIVATE_KEYS="${YOUR_USER_PRIVATE_KEY}" \
truffle console --network moonbase --working_directory $SOLIDITY_DIST_DIR/bsh

> Warning: possible unsupported (undocumented in help) command line option: --working_directory
truffle(moonbase)> let bshCore = await BSHCore.deployed()
undefined
> await bshCore.setApprovalForAll(bshCore.address, true, {from: accounts[0]})
{
  tx: '0xce8c1b6b4cf9a6788f869e584973ddb21ffce22f4f95ba2c6a2538d350e568ac',
  receipt: {
    blockHash: '0xa5bfef384db239bee42a8008950d8159652a67907092f3914e92f527eaecba62',
    blockNumber: 814003,
    contractAddress: null,
    cumulativeGasUsed: 256065,
    from: '0xd07d078373be60dd10e35f352559ef1f25029daf',
    gasUsed: 48341,
    logs: [ [Object] ],
    logsBloom: '0x00000000000000000000000000000000000002000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002000000000000000000000000400000000000000000000000000000000000100000000000000000040000000000000000000000000000000000000000000000000000000000000000000000000000000000000000002000000000000000000000000000040000000000000000000004000000000000000000000000000000000000200800000000000000008000000000000000000000000000000000000000',
    status: true,
    to: '0xf798331b1d2000d483977593ee6e447e4760c63f',
    transactionHash: '0xce8c1b6b4cf9a6788f869e584973ddb21ffce22f4f95ba2c6a2538d350e568ac',
    transactionIndex: 5,
    rawLogs: [ [Object] ]
  },
  logs: [
    {
      address: '0xf798331b1d2000D483977593ee6E447e4760c63F',
      blockHash: '0xa5bfef384db239bee42a8008950d8159652a67907092f3914e92f527eaecba62',
      blockNumber: 814003,
      logIndex: 4,
      removed: false,
      transactionHash: '0xce8c1b6b4cf9a6788f869e584973ddb21ffce22f4f95ba2c6a2538d350e568ac',
      transactionIndex: 5,
      transactionLogIndex: '0x0',
      id: 'log_aba7fc0d',
      event: 'ApprovalForAll',
      args: [Result]
    }
  ]
}
> truffle(moonbase)> await bshCore.transferNativeCoin("btp://0x42.icon/cx871afa3bd0adfa139f68ac88819da53ad44ef077", {value: 1000000000000000000})
{
  tx: '0xd48e3dc4382221d839edc8988309d2f51d6c2c24d61854602bfe5ee4aa945848',
  receipt: {
    blockHash: '0x8239fd93348272db330c1e3901b78be4112ab61d19519ad4a2e808d88df0ca4f',
    blockNumber: 814054,
    contractAddress: null,
    cumulativeGasUsed: 549972,
    from: '0xd07d078373be60dd10e35f352559ef1f25029daf',
    gasUsed: 528972,
    logs: [],
    logsBloom: '0x00000002000000000800000000000000000000000000000000000000000000000001000000000000000000000000000000000000040000000000000000000000000000000000000000000001000000000000000000000000000000000000000000000000000000040000080000400000000020000000000000000000000000100000000000000000040000000000000000000000000000000010000000000000000000000000000004000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000',
    status: true,
    to: '0xf798331b1d2000d483977593ee6e447e4760c63f',
    transactionHash: '0xd48e3dc4382221d839edc8988309d2f51d6c2c24d61854602bfe5ee4aa945848',
    transactionIndex: 1,
    rawLogs: [ [Object], [Object] ]
  },
  logs: []
}

```
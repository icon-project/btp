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
> Warning: possible unsupported (undocumented in help) command line option(s): --working_directory

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
   > transaction hash:    0x3b70b66444835aa3b2f95f52cf4309302a2b770a37e756c19a1fc37c20b33e42
   > Blocks: 5            Seconds: 72
   > contract address:    0x241678456eDf8700B8546e2B8f1111cfC7454434
   > block number:        1041103
   > block timestamp:     1635316512
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             833.439356922469765665
   > gas used:            5036796 (0x4cdafc)
   > gas price:           1 gwei
   > value sent:          0 ETH
   > total cost:          0.005036796 ETH


   Deploying 'ProxyAdmin'
   ----------------------
   > transaction hash:    0x1bcb6bd2f0fb3dff70fe023fbce4de29ccb812d492d4059bc01bc36b87c0086c
   > Blocks: 27           Seconds: 425
   > contract address:    0x7C1aCb94154483FD02954A669C15da46E7Df1994
   > block number:        1041131
   > block timestamp:     1635316950
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             833.438874202469765665
   > gas used:            482720 (0x75da0)
   > gas price:           1 gwei
   > value sent:          0 ETH
   > total cost:          0.00048272 ETH


   Deploying 'TransparentUpgradeableProxy'
   ---------------------------------------
   > transaction hash:    0x924159796c4b99e295e8e78ee9c0838c95923ffd932bbeae2881231b6c158a7c
   > Blocks: 6            Seconds: 92
   > contract address:    0x95AAd731c97B93f92a205311687469769BC0Ec53
   > block number:        1041138
   > block timestamp:     1635317058
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             833.438214497469765665
   > gas used:            659705 (0xa10f9)
   > gas price:           1 gwei
   > value sent:          0 ETH
   > total cost:          0.000659705 ETH


   Deploying 'BMCPeriphery'
   ------------------------
   > transaction hash:    0xd6df2350c6140c4042e7e5741f9572b1731f74f76adc2d6285be0bb994426903
   > Blocks: 20           Seconds: 261
   > contract address:    0xE6c9A0e1593de940A81a719529ED793f349D55cE
   > block number:        1041158
   > block timestamp:     1635317328
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             833.433549867469765665
   > gas used:            4664630 (0x472d36)
   > gas price:           1 gwei
   > value sent:          0 ETH
   > total cost:          0.00466463 ETH


   Deploying 'TransparentUpgradeableProxy'
   ---------------------------------------
   > transaction hash:    0xece8046174286068ee877ae715d6fdd01a3ec4fef5692787cf0b0d010a352042
   > Blocks: 5            Seconds: 52
   > contract address:    0x6047341C88B9A45957E9Ad68439cA941D464c706
   > block number:        1041164
   > block timestamp:     1635317400
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             833.432808668469765665
   > gas used:            741199 (0xb4f4f)
   > gas price:           1 gwei
   > value sent:          0 ETH
   > total cost:          0.000741199 ETH

   > Saving artifacts
   -------------------------------------
   > Total cost:          0.01158505 ETH


Summary
=======
> Total deployments:   5
> Final cost:          0.01158505 ETH


```

### Get BMC Periphery proxy address

```bash
truffle console --network moonbase --working_directory $SOLIDITY_DIST_DIR/bmc
> Warning: possible unsupported (undocumented in help) command line option(s): --working_directory
truffle(moonbase)> let bmcP = await BMCPeriphery.deployed()
bmundefined
truffle(moonbase)> bmcP.address
'0x6047341C88B9A45957E9Ad68439cA941D464c706'
truffle(moonbase)> .exit
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
BMC_PERIPHERY_ADDRESS="0x6047341C88B9A45957E9Ad68439cA941D464c706" \
BSH_COIN_URL="https://moonbeam.network/" \
BSH_SERVICE="nativecoin" \
BSH_COIN_NAME="DEV" \
BSH_COIN_FEE="100" \
BSH_FIXED_FEE="500000" \
truffle migrate --network moonbase --working_directory $SOLIDITY_DIST_DIR/bsh

### Output
> Warning: possible unsupported (undocumented in help) command line option(s): --working_directory

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
   > transaction hash:    0x8c7dba484873a17d6f40e2a2accb837ea28b4f64653cab9b838505ebd910321a
   > Blocks: 2            Seconds: 16
   > contract address:    0xBdeBcF714c299A10308fdbCfE6cEaff785110D0E
   > block number:        1041264
   > block timestamp:     1635318768
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             833.395349962469765665
   > gas used:            4740289 (0x4854c1)
   > gas price:           2 gwei
   > value sent:          0 ETH
   > total cost:          0.009480578 ETH


   Deploying 'TransparentUpgradeableProxy'
   ---------------------------------------
   > transaction hash:    0xa5f58e39767f28e68044fec5b8f4ad511b2e688d30b98c89d065990bc8a73bd1
   > Blocks: 2            Seconds: 28
   > contract address:    0x378DFcf210F93E2d6E1ef5C0d357Be5E6478581E
   > block number:        1041267
   > block timestamp:     1635318810
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             833.393571172469765665
   > gas used:            889395 (0xd9233)
   > gas price:           2 gwei
   > value sent:          0 ETH
   > total cost:          0.00177879 ETH


   Deploying 'BSHPeriphery'
   ------------------------
   > transaction hash:    0x2c7b55e4b6b120df701cef7e516f690355c8747b7483c98c88f92808fee9f35b
   > Blocks: 2            Seconds: 28
   > contract address:    0x759F7953825f3EF26Dc2E121b57Fd5b61654751E
   > block number:        1041270
   > block timestamp:     1635318852
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             833.386057532469765665
   > gas used:            3756820 (0x395314)
   > gas price:           2 gwei
   > value sent:          0 ETH
   > total cost:          0.00751364 ETH


   Deploying 'TransparentUpgradeableProxy'
   ---------------------------------------
   > transaction hash:    0x09b1c18a82e59b659143fb64d556c7296c9e12a26c060295da08a715bcd29b22
   > Blocks: 1            Seconds: 12
   > contract address:    0x0077e7c6e32201081359d7dB8976139037935332
   > block number:        1041273
   > block timestamp:     1635318888
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             833.384729718469765665
   > gas used:            663907 (0xa2163)
   > gas price:           2 gwei
   > value sent:          0 ETH
   > total cost:          0.001327814 ETH

   > Saving artifacts
   -------------------------------------
   > Total cost:         0.020100822 ETH


Summary
=======
> Total deployments:   4
> Final cost:          0.020100822 ETH

```

### Get BSH Periphery proxy address

```bash
truffle console --network moonbase --working_directory $SOLIDITY_DIST_DIR/bsh
> Warning: possible unsupported (undocumented in help) command line option(s): --working_directory
truffle(moonbase)> let bshP = await BSHPeriphery.deployed()
undefined
truffle(moonbase)> bshP.address
'0x0077e7c6e32201081359d7dB8976139037935332'
truffle(moonbase)> .exit
```


### Deloy BMV

For example the next BTP address is: `btp://0x42.icon/cx924f8f84efd1fde23356da451ed226590bca81c9`
And the offset is: `5121400`

```bash
yarn install --production --cwd $SOLIDITY_DIST_DIR/bmv
rm -rf $SOLIDITY_DIST_DIR/bmv/.openzeppelin $SOLIDITY_DIST_DIR/bmv/build

truffle compile --all --working_directory $SOLIDITY_DIST_DIR/bmv

# @params
# - BMC_PERIPHERY_ADDRESS: an address on chain of BMCPeriphery contract
# This address is queried after **deploying** BMC contracts
# For example: BMC_PERIPHERY_ADDRESS = 0x6047341C88B9A45957E9Ad68439cA941D464c706
# - BMV_ICON_NET: Chain ID and name of a network that BMV is going to verify BTP Message
# - BMV_ICON_INIT_OFFSET: a block height when ICON-BMC was deployed
# - BMV_ICON_LASTBLOCK_HASH: a hash of the above block
# - BMV_ICON_ENCODED_VALIDATORS: a result of ICON JSON-RPC method `icx_getDataByHash` with the input is
# PreviousBlockHeader.NextValidatorHash. So, to get this param for block N, you must get BlockHeader of N - 1
# User can execute to ge the result : 
#
# make iconvalidators
# 
# GOLOOP_RPC_URI="https://btp.net.solidwallet.io/api/v3/icon_dex" bin/iconvalidators build 5121400
#
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" \
BMC_PERIPHERY_ADDRESS="0x6047341C88B9A45957E9Ad68439cA941D464c706" \
BMV_ICON_NET="0x42.icon" \
BMV_ICON_ENCODED_VALIDATORS="0xf8589500edeb1b82b94d548ec440df553f522144ca83fb8d9500d63c4c73b623e97f67407d687af4efcfe486a51595007882dace25ff7e947d3a25178a2a1162874cfddc95000458d8b6f649a9e005963dc9a72669c89ed52d85" \
BMV_ICON_INIT_OFFSET="5121400" \
BMV_ICON_INIT_ROOTSSIZE="10" \
BMV_ICON_INIT_CACHESIZE="10" \
BMV_ICON_LASTBLOCK_HASH="0x5ca410405ec96b8d30706eeaed16b709ae8fd98d4e4c1e787bb0599f5050bf8c" \
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
   > transaction hash:    0x23259df49f6021d0049e763670452254730dfb596acead6974bed6e808dee6fb
   > Blocks: 2            Seconds: 16
   > contract address:    0x1D36993A37818215D019B6C3E4f1c86e7b69b9C7
   > block number:        1041207
   > block timestamp:     1635317994
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             833.428520040469765665
   > gas used:            2120414 (0x205ade)
   > gas price:           2 gwei
   > value sent:          0 ETH
   > total cost:          0.004240828 ETH


   Deploying 'TransparentUpgradeableProxy'
   ---------------------------------------
   > transaction hash:    0xd0c6566f8b168cacb71ad4b96b3eea533c4f68c8e8c48d0ac6cb7d79e131e3ef
   > Blocks: 2            Seconds: 29
   > contract address:    0xE5A9cf37D23D781e458AF4ADd6901Bc9a4819EDb
   > block number:        1041219
   > block timestamp:     1635318144
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             833.417367332469765665
   > gas used:            617558 (0x96c56)
   > gas price:           2 gwei
   > value sent:          0 ETH
   > total cost:          0.001235116 ETH


   Deploying 'BMV'
   ---------------
   > transaction hash:    0xa39258f623f4ae8aa9719598737aef083a986d84834664aa8ba6c0e5ec1c8ab9
   > Blocks: 2            Seconds: 18
   > contract address:    0x91316D2E058b2d34E3c45Ee117BE798A97A5202c
   > block number:        1041221
   > block timestamp:     1635318180
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             833.408684856469765665
   > gas used:            4341238 (0x423df6)
   > gas price:           2 gwei
   > value sent:          0 ETH
   > total cost:          0.008682476 ETH


   Deploying 'TransparentUpgradeableProxy'
   ---------------------------------------
   > transaction hash:    0x212edf8b030c8f2317d74c83f3efa16a86ce75a9d047de494b4b3a61242ee93b
   > Blocks: 1            Seconds: 20
   > contract address:    0xD5002Ff9197e27dE02E88FF65e11656e9DccabAA
   > block number:        1041224
   > block timestamp:     1635318228
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             833.406432734469765665
   > gas used:            1126061 (0x112ead)
   > gas price:           2 gwei
   > value sent:          0 ETH
   > total cost:          0.002252122 ETH

   > Saving artifacts
   -------------------------------------
   > Total cost:         0.012169714 ETH


Summary
=======
> Total deployments:   3
> Final cost:          0.012169714 ETH


```

### Get BMV address from proxy

```bash
truffle console --network moonbase --working_directory $SOLIDITY_DIST_DIR/bmv
> Warning: possible unsupported (undocumented in help) command line option(s): --working_directory
truffle(moonbase)> let bmv = await BMV.deployed()
undefined
truffle(moonbase)> bmv.address
'0xD5002Ff9197e27dE02E88FF65e11656e9DccabAA'
truffle(moonbase)> .exit
```

### Configure Contracts

```bash
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" \
NEXTLINK_BTP_NET="0x42.icon" \
CURRENTLINK_BMV_ADDRESS="0xD5002Ff9197e27dE02E88FF65e11656e9DccabAA" \
truffle exec $SOLIDITY_DIST_DIR/bmc/scripts/add_verifier.js --network moonbase --working_directory $SOLIDITY_DIST_DIR/bmc

### Output
> Warning: possible unsupported (undocumented in help) command line option(s): --working_directory
Using network 'moonbase'.

{
  tx: '0x8358e48a09505e47224126dbc1dd02bde9787a69fceb6ea40710594af1275f62',
  receipt: {
    blockHash: '0x9dfb86c3faea8936937c81250b05c981884e0739f71fd3cd2759add147a631d4',
    blockNumber: 1041240,
    contractAddress: null,
    cumulativeGasUsed: 91134,
    from: '0x4bb718cb404787bf97bb012bb08096602fb9544b',
    gasUsed: 91134,
    logs: [],
    logsBloom: '0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000',
    status: true,
    to: '0x95aad731c97b93f92a205311687469769bc0ec53',
    transactionHash: '0x8358e48a09505e47224126dbc1dd02bde9787a69fceb6ea40710594af1275f62',
    transactionIndex: 0,
    rawLogs: []
  },
  logs: []
}
```

For example the next BTP address is: `btp://0x42.icon/cx924f8f84efd1fde23356da451ed226590bca81c9`
And the offset is: `5121400`

```bash
# Deploy on what relay you want to add
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" \
NEXTLINK_BTP_ADDRESS="btp://0x42.icon/cx924f8f84efd1fde23356da451ed226590bca81c9" \
NEXTLINK_BLOCK_INTERVAL=12 \
NEXTLINK_ROTATION_MAX_AGGERATION=10 \
NEXTLINK_ROTATION_DELAY_LIMIT=3 \
RELAY_ADDRESSES="0x1e9eb23057adf446777c7fab1a6c565cb8977ab5" \
truffle exec $SOLIDITY_DIST_DIR/bmc/scripts/add_link_set_link_add_relay.js --network moonbase --working_directory $SOLIDITY_DIST_DIR/bmc

### Output
> Warning: possible unsupported (undocumented in help) command line option(s): --working_directory
Using network 'moonbase'.

{
  tx: '0x2977c6306e35520b9bea9e75c5416cdcaae3f0833961e9097fe3dec3a1ca7ac9',
  receipt: {
    blockHash: '0x4dd19d38b9fb373e5fea7c0a7c5e69220482952bddf6177888b23f7eaea58d4f',
    blockNumber: 1041246,
    contractAddress: null,
    cumulativeGasUsed: 677711,
    from: '0x4bb718cb404787bf97bb012bb08096602fb9544b',
    gasUsed: 436359,
    logs: [],
    logsBloom: '0x00000002000000000000000000000000000000000000000000000000000000000000000000000040000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000001000000001000200000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000',
    status: true,
    to: '0x95aad731c97b93f92a205311687469769bc0ec53',
    transactionHash: '0x2977c6306e35520b9bea9e75c5416cdcaae3f0833961e9097fe3dec3a1ca7ac9',
    transactionIndex: 2,
    rawLogs: [ [Object] ]
  },
  logs: []
}
{
  tx: '0xe99b43a7622fc2d5646e582f9b1bac2ba324e545d346859a169115ad7995693b',
  receipt: {
    blockHash: '0xab9b1c3f0ce833b01aa1a2f4067ca110609e0ada9ce8dab7cfd2b74e7ef19ad9',
    blockNumber: 1041247,
    contractAddress: null,
    cumulativeGasUsed: 179122,
    from: '0x4bb718cb404787bf97bb012bb08096602fb9544b',
    gasUsed: 179122,
    logs: [],
    logsBloom: '0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000',
    status: true,
    to: '0x95aad731c97b93f92a205311687469769bc0ec53',
    transactionHash: '0xe99b43a7622fc2d5646e582f9b1bac2ba324e545d346859a169115ad7995693b',
    transactionIndex: 0,
    rawLogs: []
  },
  logs: []
}
{
  tx: '0x876c161ae5b2ac55b39a001fdcc6348e02eaa58910712675fd6d13a0645e7b55',
  receipt: {
    blockHash: '0x59a26a5ea87653a6f0a2ba35707cc83b0c03d8ad9ddec653d15d9fa3f9a71db2',
    blockNumber: 1041249,
    contractAddress: null,
    cumulativeGasUsed: 94482,
    from: '0x4bb718cb404787bf97bb012bb08096602fb9544b',
    gasUsed: 94482,
    logs: [],
    logsBloom: '0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000',
    status: true,
    to: '0x95aad731c97b93f92a205311687469769bc0ec53',
    transactionHash: '0x876c161ae5b2ac55b39a001fdcc6348e02eaa58910712675fd6d13a0645e7b55',
    transactionIndex: 0,
    rawLogs: []
  },
  logs: []
}
```

```bash
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" \
CURRENTLINK_BSH_SERVICENAME="nativecoin" \
CURRENTLINK_BSH_ADDRESS="0x0077e7c6e32201081359d7dB8976139037935332" \
truffle exec $SOLIDITY_DIST_DIR/bmc/scripts/add_bsh_service.js --network moonbase --working_directory $SOLIDITY_DIST_DIR/bmc

### Output
> Warning: possible unsupported (undocumented in help) command line option(s): --working_directory
Using network 'moonbase'.

{
  tx: '0xe6272c79c97401820ad4ca9d2352b574873f623e39c5bcd250cb98ddeb5062c0',
  receipt: {
    blockHash: '0x19285245bebeedb35ea42d904f897ce30fc5e113e1a53e907ae528e1d63a5587',
    blockNumber: 1041285,
    contractAddress: null,
    cumulativeGasUsed: 112049,
    from: '0x4bb718cb404787bf97bb012bb08096602fb9544b',
    gasUsed: 91049,
    logs: [],
    logsBloom: '0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000',
    status: true,
    to: '0x95aad731c97b93f92a205311687469769bc0ec53',
    transactionHash: '0xe6272c79c97401820ad4ca9d2352b574873f623e39c5bcd250cb98ddeb5062c0',
    transactionIndex: 1,
    rawLogs: []
  },
  logs: []
}
```

```bash
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" \
NEXTLINK_BTP_NATIVECOIN_NAME="ICX" \
truffle exec $SOLIDITY_DIST_DIR/bsh/scripts/register_coin.js --network moonbase --working_directory $SOLIDITY_DIST_DIR/bsh  

## Output

> Warning: possible unsupported (undocumented in help) command line option(s): --working_directory
Using network 'moonbase'.

{
  tx: '0xe60709b5820004b104e802f241e2d096b58f726679f61717f23d0b4f1488406a',
  receipt: {
    blockHash: '0x0e2bff43b2138568e2d67c567ed90777408787e140954b356072dfc126151444',
    blockNumber: 1041289,
    contractAddress: null,
    cumulativeGasUsed: 311949,
    from: '0x4bb718cb404787bf97bb012bb08096602fb9544b',
    gasUsed: 70597,
    logs: [],
    logsBloom: '0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000',
    status: true,
    to: '0x378dfcf210f93e2d6e1ef5c0d357be5e6478581e',
    transactionHash: '0xe60709b5820004b104e802f241e2d096b58f726679f61717f23d0b4f1488406a',
    transactionIndex: 2,
    rawLogs: []
  },
  logs: []
}
[ 'DEV', 'ICX' ]
```

#### Add another relay

**WARNING** remember this is an override update, so, get the existing relays and add new one

```bash
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" truffle console --network moonbase --working_directory $SOLIDITY_DIST_DIR/bmc

## Output
> Warning: possible unsupported (undocumented in help) command line option(s): --working_directory
truffle(moonbase)> let bmcM = await BMCManagement.deployed()
undefined
truffle(moonbase)> await bmcM.getRelays("btp://0x42.icon/cx924f8f84efd1fde23356da451ed226590bca81c9")
[
  '0x126AD520629a0152b749Af26d5fd342Cb67Ac6CE',
  '0x1e9EB23057adf446777c7FAB1A6c565cB8977Ab5'
]
truffle(moonbase)> await bmcM.addRelay("btp://0x42.icon/cx924f8f84efd1fde23356da451ed226590bca81c9", ["0x126ad520629a0152b749af26d5fd342cb67ac6ce", "0x723af972757df56573ab116bfdac775420f844a2"])
{
  tx: '0xf40332405bc4c697f0e7411c22e9679746f87bfea231c21c40df339d8a681fb1',
  receipt: {
    blockHash: '0xd8b2476fa69f46b956d1fe20738a31558d9a428ba33729a5ec2c1b631889d01d',
    blockNumber: 1041862,
    contractAddress: null,
    cumulativeGasUsed: 2401865,
    from: '0x4bb718cb404787bf97bb012bb08096602fb9544b',
    gasUsed: 42814,
    logs: [],
    logsBloom: '0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000',
    status: true,
    to: '0x95aad731c97b93f92a205311687469769bc0ec53',
    transactionHash: '0xf40332405bc4c697f0e7411c22e9679746f87bfea231c21c40df339d8a681fb1',
    transactionIndex: 2,
    rawLogs: []
  },
  logs: []
}
truffle(moonbase)> await bmcM.getRelays("btp://0x42.icon/cx924f8f84efd1fde23356da451ed226590bca81c9")
[
  '0x126AD520629a0152b749Af26d5fd342Cb67Ac6CE',
  '0x723af972757Df56573Ab116Bfdac775420F844a2'
]
```
### JAVAScore

TODO

### Transfer ICX to DEV

```bash
PRIVATE_KEYS="${YOUR_USER_PRIVATE_KEY}" \
truffle console --network moonbase --working_directory $SOLIDITY_DIST_DIR/bsh

> Warning: possible unsupported (undocumented in help) command line option: --working_directory
truffle(moonbase)> let bshCore = await BSHCore.deployed()
undefined
> await bshCore.setApprovalForAll(bshCore.address, true, {from: accounts[0]})
{
  tx: '0x2b3010a60b52559b642a6635740c301a18ab60e3fb69140a5fb7b701d636e86a',
  receipt: {
    blockHash: '0xbb12024299ee5179172e2845cf149096001789ab3e72494322e676f494a0b13d',
    blockNumber: 1041594,
    contractAddress: null,
    cumulativeGasUsed: 5164682,
    from: '0xd07d078373be60dd10e35f352559ef1f25029daf',
    gasUsed: 48353,
    logs: [ [Object] ],
    logsBloom: '0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000002000000000000000000000000c00000000000000000000000000000000000100000000000000000140010000000000000000000000000000000000002000000000000000000000000000000000000000000000000000000000000000000080000000000000000000000000000000000000000000000000000000000000000000000200800000000000000000000000000000000000000000000000000000000',
    status: true,
    to: '0x378dfcf210f93e2d6e1ef5c0d357be5e6478581e',
    transactionHash: '0x2b3010a60b52559b642a6635740c301a18ab60e3fb69140a5fb7b701d636e86a',
    transactionIndex: 2,
    rawLogs: [ [Object] ]
  },
  logs: [
    {
      address: '0x378DFcf210F93E2d6E1ef5C0d357Be5E6478581E',
      blockHash: '0xbb12024299ee5179172e2845cf149096001789ab3e72494322e676f494a0b13d',
      blockNumber: 1041594,
      logIndex: 0,
      removed: false,
      transactionHash: '0x2b3010a60b52559b642a6635740c301a18ab60e3fb69140a5fb7b701d636e86a',
      transactionIndex: 2,
      transactionLogIndex: '0x0',
      id: 'log_2dea5542',
      event: 'ApprovalForAll',
      args: [Result]
    }
  ]
}
> truffle(moonbase)> await bshCore.transferNativeCoin("btp://0x42.icon/hxb6b5791be0b5ef67063b3c10b840fb81514db2fd", {value: 1000000000000000000})
{
  tx: '0x325cd38c6da51d37a1ffa0ae26bf6d6f630cbfbce08263c0fba6ca4affb8211a',
  receipt: {
    blockHash: '0xe57a94a654ef7177aaa3abed69e90aeed105888ae130e71c8d927f60a859b751',
    blockNumber: 1041611,
    contractAddress: null,
    cumulativeGasUsed: 528972,
    from: '0xd07d078373be60dd10e35f352559ef1f25029daf',
    gasUsed: 528972,
    logs: [],
    logsBloom: '0x00000002000000000000000000000000000000000000000000000000000000000001000000000040000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000100000000000000000000000000000000000041000000000400000000000000000000000000000000040100000000000000000040000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000001000000001000200000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000',
    status: true,
    to: '0x378dfcf210f93e2d6e1ef5c0d357be5e6478581e',
    transactionHash: '0x325cd38c6da51d37a1ffa0ae26bf6d6f630cbfbce08263c0fba6ca4affb8211a',
    transactionIndex: 0,
    rawLogs: [ [Object], [Object] ]
  },
  logs: []
}

```
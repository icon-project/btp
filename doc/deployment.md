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

### WARNING

If you have error while deployment, just rerun the truffle migrate command. And, after you finished deployments please save the `.openzeppelin` folder, this is neccessary to keep the address of implementations/proxies/proxyadmin contracts.


### Notify

When you use truffle console and got the problem like, you could to only use abi interface with the contract

```
truffle console --network moonbase --working_directory $SOLIDITY_DIST_DIR/bmc
> Warning: possible unsupported (undocumented in help) command line option(s): --working_directory
truffle(moonbase)> let bmcPer = await BMCPeriphery.deployed()
Uncaught:
Error: BMCPeriphery has not been deployed to detected network (network/artifact mismatch)
    at processTicksAndRejections (internal/process/task_queues.js:95:5)
truffle(moonbase)> bmcPer = await BMCPeriphery.at("0x3e525eD7a82B87bE30cdADE89d32204cA0F1C356")
undefined
```

### Deploy BMC

```bash
yarn install --production --cwd $SOLIDITY_DIST_DIR/bmc
rm -rf $SOLIDITY_DIST_DIR/bmc/build

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
   > transaction hash:    0xb7b6461bb87f0818aa5a62af82a0783542837923547be174df053a2c4077577d
   > Blocks: 2            Seconds: 18
   > contract address:    0x8efAe8b69095b6467859496624ea31468142A2D8
   > block number:        1047640
   > block timestamp:     1635402936
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             763.373698834469765665
   > gas used:            5036796 (0x4cdafc)
   > gas price:           2 gwei
   > value sent:          0 ETH
   > total cost:          0.010073592 ETH


   Deploying 'ProxyAdmin'
   ----------------------
   > transaction hash:    0x45db47377061fbd9a1d36801cb22a06617147800a0f0af4b79025235d35d4758
   > Blocks: 2            Seconds: 16
   > contract address:    0xf070CDa0F732562e25402e6b31cE37817C43ED26
   > block number:        1047642
   > block timestamp:     1635402960
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             763.372733394469765665
   > gas used:            482720 (0x75da0)
   > gas price:           2 gwei
   > value sent:          0 ETH
   > total cost:          0.00096544 ETH


   Deploying 'TransparentUpgradeableProxy'
   ---------------------------------------
   > transaction hash:    0x5fa1efe21821b3c5048fd4543609379be19fbdfa9be4dfcb3464fb16cb793e82
   > Blocks: 2            Seconds: 16
   > contract address:    0xb3aD0707F494393A7d922F14A412E3518eD0B6bc
   > block number:        1047644
   > block timestamp:     1635402984
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             763.371413960469765665
   > gas used:            659717 (0xa1105)
   > gas price:           2 gwei
   > value sent:          0 ETH
   > total cost:          0.001319434 ETH


   Deploying 'BMCPeriphery'
   ------------------------
   > transaction hash:    0x1dcb9f5d00f3b7782c19b6c0a2c042cd6c80472ad37c16687e81815673b8ac3b
   > Blocks: 1            Seconds: 12
   > contract address:    0x4394a448c7032C93C3d7dDcde65Ef21c72804272
   > block number:        1047646
   > block timestamp:     1635403008
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             763.362084700469765665
   > gas used:            4664630 (0x472d36)
   > gas price:           2 gwei
   > value sent:          0 ETH
   > total cost:          0.00932926 ETH


   Deploying 'TransparentUpgradeableProxy'
   ---------------------------------------
   > transaction hash:    0x257611de4463ee1efd51ba2d9398bfc5c224b8fb8867d1a0452ea1f9c93eff2f
   > Blocks: 1            Seconds: 12
   > contract address:    0x3e525eD7a82B87bE30cdADE89d32204cA0F1C356
   > block number:        1047648
   > block timestamp:     1635403032
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             763.360601754469765665
   > gas used:            741473 (0xb5061)
   > gas price:           2 gwei
   > value sent:          0 ETH
   > total cost:          0.001482946 ETH

   > Saving artifacts
   -------------------------------------
   > Total cost:         0.023170672 ETH


Summary
=======
> Total deployments:   5
> Final cost:          0.023170672 ETH


```

### Get BMC proxy addresses

```bash
truffle console --network moonbase --working_directory $SOLIDITY_DIST_DIR/bmc
> Warning: possible unsupported (undocumented in help) command line option(s): --working_directory
truffle(moonbase)> let bmcPer = await BMCPeriphery.deployed()
undefined
truffle(moonbase)> bmcPer.address
'0x3e525eD7a82B87bE30cdADE89d32204cA0F1C356'
truffle(moonbase)> let bmcM = await BMCManagement.deployed()
undefined
truffle(moonbase)> bmcM.address
'0xb3aD0707F494393A7d922F14A412E3518eD0B6bc'
truffle(moonbase)> .exit

```
### Deploy NativeCoin BSH

```bash
yarn install --production --cwd $SOLIDITY_DIST_DIR/bsh
rm -rf $SOLIDITY_DIST_DIR/bsh/build

truffle compile --all --working_directory $SOLIDITY_DIST_DIR/bsh

# @params:
# - BMC_PERIPHERY_ADDRESS: an address on chain of BMCPeriphery contract
# This address is queried after deploying BMC contracts
# For example: BMC_PERIPHERY_ADDRESS = 0x3e525eD7a82B87bE30cdADE89d32204cA0F1C356
# - BSH_COIN_NAME: a native coin name of Moonbase Alpha Testnet Network - DEV
# - BSH_COIN_FEE: a charging fee ratio of each request, e.g. 100/10000 = 1%
# - BSH_SERVICE: a service name of BSH contract, e.g. 'CoinTransfer'
# This service name is unique in one network. And it must be registered to BMC contract to activate
# BMC contract checks its service name whether it's already existed
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" \
BMC_PERIPHERY_ADDRESS="0x3e525eD7a82B87bE30cdADE89d32204cA0F1C356" \
BSH_COIN_URL="https://moonbeam.network/" \
BSH_SERVICE="nativecoin" \
BSH_COIN_NAME="DEV" \
BSH_COIN_FEE="100" \
BSH_FIXED_FEE="500000" \
truffle migrate --network moonbase --working_directory $SOLIDITY_DIST_DIR/bsh

### Output
truffle migrate --network moonbase --working_directory $SOLIDITY_DIST_DIR/bsh
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
   > transaction hash:    0x7f809e38445fc33c0a6f98b8ddd0575de35dd3ff1b4325540d8069c8ff5f26c5
   > Blocks: 2            Seconds: 16
   > contract address:    0xde4C26226BD6258Cc699CE9E88cc798C21B4b94D
   > block number:        1047668
   > block timestamp:     1635403302
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             763.351025576469765665
   > gas used:            4740289 (0x4854c1)
   > gas price:           2 gwei
   > value sent:          0 ETH
   > total cost:          0.009480578 ETH


   Deploying 'TransparentUpgradeableProxy'
   ---------------------------------------
   > transaction hash:    0x06de84189e02f396c89cd59175652b2216c9f0c0d3146f6610baeb0305ff1700
   > Blocks: 2            Seconds: 24
   > contract address:    0x2a17B6814a172419a5E84d7B746aBEb95a84E76B
   > block number:        1047671
   > block timestamp:     1635403338
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             763.349246786469765665
   > gas used:            889395 (0xd9233)
   > gas price:           2 gwei
   > value sent:          0 ETH
   > total cost:          0.00177879 ETH


   Deploying 'BSHPeriphery'
   ------------------------
   > transaction hash:    0x09400aa94dbb7e16ab0aaccde2b835906b47e579962abc647b283429cf42eec6
   > Blocks: 2            Seconds: 20
   > contract address:    0x909174976471750601cBA5BcBEe24465B6D32C2d
   > block number:        1047673
   > block timestamp:     1635403368
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             763.341733146469765665
   > gas used:            3756820 (0x395314)
   > gas price:           2 gwei
   > value sent:          0 ETH
   > total cost:          0.00751364 ETH


   Deploying 'TransparentUpgradeableProxy'
   ---------------------------------------
   > transaction hash:    0x72b9aa4a36b1a843618d49ecc2fb4b26a89899bf93469c4dce93454a170dd4e1
   > Blocks: 2            Seconds: 32
   > contract address:    0xccf66A1a9D82EC13b0B2a5002EdA4dF411BE4754
   > block number:        1047675
   > block timestamp:     1635403410
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             763.340405332469765665
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
'0xccf66A1a9D82EC13b0B2a5002EdA4dF411BE4754'
truffle(moonbase)> let bshCore = await BSHCore.deployed()
undefined
truffle(moonbase)> bshCore.address
'0x2a17B6814a172419a5E84d7B746aBEb95a84E76B'
truffle(moonbase)> .exit
```


### Deploy nativecoinERC20_BSH

```bash
yarn install --production --cwd $SOLIDITY_DIST_DIR/nativecoinERC20
rm -rf $SOLIDITY_DIST_DIR/nativecoinERC20/build

truffle compile --all --working_directory $SOLIDITY_DIST_DIR/nativecoinERC20

# @params:
# - BSH_COIN_NAME: Native coin Name
# - BSH_COIN_FEE: Fees to be charged
# - BSH_FIXED_FEE: basic fixed fees
# - BSH_TOKEN_NAME: ERC20_token Name same as symbol 
# - BSH_TOKEN_SYMBOL:  ERC20_token Name
# - BSH_INITIAL_SUPPLY: inital supply of the erc20 token
# - BMC_PERIPHERY_ADDRESS: an address on chain of BMCPeriphery contract
# This address is queried after deploying BMC contracts
# For example: BMC_PERIPHERY_ADDRESS = 0x3e525eD7a82B87bE30cdADE89d32204cA0F1C356
# - BSH_SERVICE: a service name of BSH contract, e.g. 'NativeCoinIRC2BSH'
# This service name is unique in one network. And it must be registered to BMC contract to activate
# BMC contract checks its service name whether it's already existed
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" \
BSH_COIN_NAME="MOVR" \
BSH_COIN_FEE="100" \
BSH_FIXED_FEE="50000" \
BSH_TOKEN_NAME="ICX" \
BSH_TOKEN_SYMBOL="ICX" \
BSH_INITIAL_SUPPLY="100000" \
BMC_PERIPHERY_ADDRESS="0x3e525eD7a82B87bE30cdADE89d32204cA0F1C356" \
BSH_SERVICE="NativeCoinIRC2BSH" \
truffle migrate --network moonbase --working_directory $SOLIDITY_DIST_DIR/nativecoinERC20
```

### Deloy BMV

For example the next BTP address is: `btp://0x42.icon/cx11a5a7510b128e0ab16546e1493e38b2d7e299c3`
And the offset is: `5167618`

```bash
yarn install --production --cwd $SOLIDITY_DIST_DIR/bmv
rm -rf $SOLIDITY_DIST_DIR/bmv/build

truffle compile --all --working_directory $SOLIDITY_DIST_DIR/bmv

# @params
# - BMC_PERIPHERY_ADDRESS: an address on chain of BMCPeriphery contract
# This address is queried after **deploying** BMC contracts
# For example: BMC_PERIPHERY_ADDRESS = 0x6047341C88B9A45957E9Ad68439cA941D464c706
# - BMV_ICON_NET: Chain ID and name of a network that BMV is going to verify BTP Message
# - BMV_ICON_INIT_OFFSET: a block height when ICON-BMC was deployed
# - BMV_ICON_ENCODED_VALIDATORS: a result of ICON JSON-RPC method `icx_getDataByHash` with the input is
# PreviousBlockHeader.NextValidatorHash. So, to get this param for block N, you must get BlockHeader of N - 1
# User can execute to ge the result : 
#
# make iconvalidators
# 
# GOLOOP_RPC_URI="https://btp.net.solidwallet.io/api/v3/icon_dex" bin/iconvalidators build 5167618
# 
# - BMV_ICON_LASTBLOCK_HASH: a hash of the above block
# GOLOOP_RPC_URI="https://btp.net.solidwallet.io/api/v3/icon_dex" goloop rpc blockbyheight 5167618 | jq -r '.block_hash'
# Remember adding 0x 
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" \
BMC_PERIPHERY_ADDRESS="0x3e525eD7a82B87bE30cdADE89d32204cA0F1C356" \
BMV_ICON_NET="0x42.icon" \
BMV_ICON_ENCODED_VALIDATORS="0xf8589500edeb1b82b94d548ec440df553f522144ca83fb8d9500d63c4c73b623e97f67407d687af4efcfe486a51595007882dace25ff7e947d3a25178a2a1162874cfddc95000458d8b6f649a9e005963dc9a72669c89ed52d85" \
BMV_ICON_INIT_OFFSET="5167618" \
BMV_ICON_INIT_ROOTSSIZE="12" \
BMV_ICON_INIT_CACHESIZE="12" \
BMV_ICON_LASTBLOCK_HASH="0x2b666571f5bb83147d6a000326a88d0a35c9e2c7f25a88b7dfbc7325ba8ee927" \
truffle migrate --network moonbase --working_directory $SOLIDITY_DIST_DIR/bmv

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


1_deploy_bmv.js
===============

   Deploying 'DataValidator'
   -------------------------
   > transaction hash:    0x77b237b9ae256630356431b086db0ac0e628f23abead3de3c0934a6605158426
   > Blocks: 2            Seconds: 21
   > contract address:    0xE8F00eA24C53C1990bd365Ad41b1C70eE62676cc
   > block number:        1047697
   > block timestamp:     1635403686
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             763.336069806469765665
   > gas used:            2120414 (0x205ade)
   > gas price:           2 gwei
   > value sent:          0 ETH
   > total cost:          0.004240828 ETH


   Deploying 'TransparentUpgradeableProxy'
   ---------------------------------------
   > transaction hash:    0xc33701b3c3cc24ce8e700e056e28631cf7140214f1cfa61c69576765418e9191
   > Blocks: 1            Seconds: 12
   > contract address:    0x8ceD43FDF0Fe705E10965637e40b7b3F084E3c40
   > block number:        1047699
   > block timestamp:     1635403710
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             763.334834690469765665
   > gas used:            617558 (0x96c56)
   > gas price:           2 gwei
   > value sent:          0 ETH
   > total cost:          0.001235116 ETH


   Deploying 'BMV'
   ---------------
   > transaction hash:    0x7114ebec49ac1463b29a12b6c4b533b0a40cf8c65e0f3da6eb97563a9d8b4c9b
   > Blocks: 4            Seconds: 48
   > contract address:    0xbAc7d8f664c6E5CeDf9fe3335A40CB08d1100871
   > block number:        1047704
   > block timestamp:     1635403770
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             763.326152214469765665
   > gas used:            4341238 (0x423df6)
   > gas price:           2 gwei
   > value sent:          0 ETH
   > total cost:          0.008682476 ETH


   Deploying 'TransparentUpgradeableProxy'
   ---------------------------------------
   > transaction hash:    0xf715d23dd0dea30a82b5b10c526207f0033166d0f4e8369a837ba9ff294b8d3c
   > Blocks: 3            Seconds: 28
   > contract address:    0x7D25dc35670873E90bcCa0096FAEbeE576911F52
   > block number:        1047707
   > block timestamp:     1635403806
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             763.323900116469765665
   > gas used:            1126049 (0x112ea1)
   > gas price:           2 gwei
   > value sent:          0 ETH
   > total cost:          0.002252098 ETH

   > Saving artifacts
   -------------------------------------
   > Total cost:         0.016410518 ETH


Summary
=======
> Total deployments:   4
> Final cost:          0.016410518 ETH

```

### Get BMV address from proxy

```bash
truffle console --network moonbase --working_directory $SOLIDITY_DIST_DIR/bmv
> Warning: possible unsupported (undocumented in help) command line option(s): --working_directory
truffle(moonbase)> let bmv = await BMV.deployed()
undefined
truffle(moonbase)> bmv.address
'0x7D25dc35670873E90bcCa0096FAEbeE576911F52'
truffle(moonbase)> .exit
```

### Configure Contracts

```bash
# - CURRENTLINK_BMV_ADDRESS: from Get BMV address from proxy
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" \
NEXTLINK_BTP_NET="0x42.icon" \
CURRENTLINK_BMV_ADDRESS="0x7D25dc35670873E90bcCa0096FAEbeE576911F52" \
truffle exec $SOLIDITY_DIST_DIR/bmc/scripts/add_verifier.js --network moonbase --working_directory $SOLIDITY_DIST_DIR/bmc

### Output
> Warning: possible unsupported (undocumented in help) command line option(s): --working_directory
Using network 'moonbase'.

{
  tx: '0x816de0f01d6817de82bd68c0e254086ed2d58599fcc5583bb051cd8a0b76a9aa',
  receipt: {
    blockHash: '0xd06a77b0d11b89ce36e3ac6345da9fae780bf9f4d3cea500312a54782887a5ee',
    blockNumber: 1047716,
    contractAddress: null,
    cumulativeGasUsed: 91146,
    from: '0x4bb718cb404787bf97bb012bb08096602fb9544b',
    gasUsed: 91146,
    logs: [],
    logsBloom: '0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000',
    status: true,
    to: '0xb3ad0707f494393a7d922f14a412e3518ed0b6bc',
    transactionHash: '0x816de0f01d6817de82bd68c0e254086ed2d58599fcc5583bb051cd8a0b76a9aa',
    transactionIndex: 0,
    rawLogs: []
  },
  logs: []
}
```

For example the next BTP address is: `btp://0x42.icon/cx11a5a7510b128e0ab16546e1493e38b2d7e299c3`
And the offset is: `5167618`
And deployer repair the keystore with address `126ad520629a0152b749af26d5fd342cb67ac6ce`

```bash
# Deploy on what relay you want to add
# - NEXTLINK_BTP_ADDRESS: the BMC address from 0x42.icon network
# - NEXTLINK_BLOCK_INTERVAL: the block interval from the deploy network
# - NEXTLINK_ROTATION_MAX_AGGERATION: the max period of BMR rotation algorithm to 
# - NEXTLINK_ROTATION_DELAY_LIMIT: expect delay in sending transactions RelayMessage
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" \
NEXTLINK_BTP_ADDRESS="btp://0x42.icon/cx11a5a7510b128e0ab16546e1493e38b2d7e299c3" \
NEXTLINK_BLOCK_INTERVAL=12 \
NEXTLINK_ROTATION_MAX_AGGERATION=10 \
NEXTLINK_ROTATION_DELAY_LIMIT=3 \
RELAY_ADDRESSES="0x126ad520629a0152b749af26d5fd342cb67ac6ce" \
truffle exec $SOLIDITY_DIST_DIR/bmc/scripts/add_link_set_link_add_relay.js --network moonbase --working_directory $SOLIDITY_DIST_DIR/bmc

### Output
> Warning: possible unsupported (undocumented in help) command line option(s): --working_directory
Using network 'moonbase'.

{
  tx: '0x3a17d026cadeefc4c33853aa5d546ef8d88bcbb949baeb519e33c0647d33867e',
  receipt: {
    blockHash: '0xbd7497080cce34cf6d9fba69ac7e2222cc37fa600ac1bd5b85a9e6d885dc5ae9',
    blockNumber: 1047729,
    contractAddress: null,
    cumulativeGasUsed: 457359,
    from: '0x4bb718cb404787bf97bb012bb08096602fb9544b',
    gasUsed: 436359,
    logs: [],
    logsBloom: '0x00000002000000400000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004000000000000000000000000000000000000000000000000200000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000',
    status: true,
    to: '0xb3ad0707f494393a7d922f14a412e3518ed0b6bc',
    transactionHash: '0x3a17d026cadeefc4c33853aa5d546ef8d88bcbb949baeb519e33c0647d33867e',
    transactionIndex: 1,
    rawLogs: [ [Object] ]
  },
  logs: []
}
{
  tx: '0xaa1f9cb7097dddffc205fc60ed0599b4e2fab3eae184f4dabf583940a1773510',
  receipt: {
    blockHash: '0x032a2c81f570a6a62c2205de1421ff3c0a78b2cda3d024f0dda05ab18db2de98',
    blockNumber: 1047733,
    contractAddress: null,
    cumulativeGasUsed: 179122,
    from: '0x4bb718cb404787bf97bb012bb08096602fb9544b',
    gasUsed: 179122,
    logs: [],
    logsBloom: '0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000',
    status: true,
    to: '0xb3ad0707f494393a7d922f14a412e3518ed0b6bc',
    transactionHash: '0xaa1f9cb7097dddffc205fc60ed0599b4e2fab3eae184f4dabf583940a1773510',
    transactionIndex: 0,
    rawLogs: []
  },
  logs: []
}
{
  tx: '0x87913e2186b32407e259025fdacf7534fffc772358c3ff11fe4b1fbc897c704e',
  receipt: {
    blockHash: '0xe7ed1ec83d7fa93fc6b7c5d796eebfc9c024880f30817b237af7009f07010955',
    blockNumber: 1047745,
    contractAddress: null,
    cumulativeGasUsed: 94482,
    from: '0x4bb718cb404787bf97bb012bb08096602fb9544b',
    gasUsed: 94482,
    logs: [],
    logsBloom: '0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000',
    status: true,
    to: '0xb3ad0707f494393a7d922f14a412e3518ed0b6bc',
    transactionHash: '0x87913e2186b32407e259025fdacf7534fffc772358c3ff11fe4b1fbc897c704e',
    transactionIndex: 0,
    rawLogs: []
  },
  logs: []
}
```

```bash
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" \
CURRENTLINK_BSH_SERVICENAME="nativecoin" \
CURRENTLINK_BSH_ADDRESS="0xccf66A1a9D82EC13b0B2a5002EdA4dF411BE4754" \
truffle exec $SOLIDITY_DIST_DIR/bmc/scripts/add_bsh_service.js --network moonbase --working_directory $SOLIDITY_DIST_DIR/bmc

### Output
> Warning: possible unsupported (undocumented in help) command line option(s): --working_directory
Using network 'moonbase'.

{
  tx: '0xc1f4a4806bf29f9fb34d4e6e5d12f279b0676fb556fde24142c21aa7b1b71bb3',
  receipt: {
    blockHash: '0x8521521ca0ee82dc09f5080f0c8f0a330c2acad84ceef129f873a5c0e11017c3',
    blockNumber: 1047753,
    contractAddress: null,
    cumulativeGasUsed: 112049,
    from: '0x4bb718cb404787bf97bb012bb08096602fb9544b',
    gasUsed: 91049,
    logs: [],
    logsBloom: '0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000',
    status: true,
    to: '0xb3ad0707f494393a7d922f14a412e3518ed0b6bc',
    transactionHash: '0xc1f4a4806bf29f9fb34d4e6e5d12f279b0676fb556fde24142c21aa7b1b71bb3',
    transactionIndex: 1,
    rawLogs: []
  },
  logs: []
}
```
##### Add ERC20_BSH service

```bash
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" \
CURRENTLINK_BSH_SERVICENAME="NativeCoinIRC2BSH" \
CURRENTLINK_BSH_ADDRESS="0xccf66A1a9D82EC13b0B2a5002EdA4dF411BE4754" \
truffle exec $SOLIDITY_DIST_DIR/bmc/scripts/add_bsh_service.js --network moonbase --working_directory $SOLIDITY_DIST_DIR/bmc
```

```bash
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" \
NEXTLINK_BTP_NATIVECOIN_NAME="BTC" \
truffle exec $SOLIDITY_DIST_DIR/bsh/scripts/register_coin.js --network moonbase --working_directory $SOLIDITY_DIST_DIR/bsh  

## Output

> Warning: possible unsupported (undocumented in help) command line option(s): --working_directory
Using network 'moonbase'.

{
  tx: '0x1abea0a03be823bd172c04b4b68dc1d9b8ed45db43b4c12ef262eac1cb6f66bb',
  receipt: {
    blockHash: '0x457271a8b995bd270b8db2c6b71409f89149ec9f016a8c289262896859faa7e4',
    blockNumber: 1047758,
    contractAddress: null,
    cumulativeGasUsed: 70597,
    from: '0x4bb718cb404787bf97bb012bb08096602fb9544b',
    gasUsed: 70597,
    logs: [],
    logsBloom: '0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000',
    status: true,
    to: '0x2a17b6814a172419a5e84d7b746abeb95a84e76b',
    transactionHash: '0x1abea0a03be823bd172c04b4b68dc1d9b8ed45db43b4c12ef262eac1cb6f66bb',
    transactionIndex: 0,
    rawLogs: []
  },
  logs: []
}
[ 'DEV', 'ICX' ]
```

Register BTC

```
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" \
NEXTLINK_BTP_NATIVECOIN_NAME="BTC" \
truffle exec $SOLIDITY_DIST_DIR/bsh/scripts/register_coin.js --network moonbase --working_directory $SOLIDITY_DIST_DIR/bsh
> Warning: possible unsupported (undocumented in help) command line option(s): --working_directory
Using network 'moonbase'.

{
  tx: '0xa5397a30220fb2fab7f52b844c5ccec707afe5049336dce59e64228359449aeb',
  receipt: {
    blockHash: '0xb371b5edb22242a7656a454ef1cf5f4308e432d6a6b383b26c67a8cc15f2480f',
    blockNumber: 1048430,
    contractAddress: null,
    cumulativeGasUsed: 133597,
    from: '0x4bb718cb404787bf97bb012bb08096602fb9544b',
    gasUsed: 70597,
    logs: [],
    logsBloom: '0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000',
    status: true,
    to: '0x2a17b6814a172419a5e84d7b746abeb95a84e76b',
    transactionHash: '0xa5397a30220fb2fab7f52b844c5ccec707afe5049336dce59e64228359449aeb',
    transactionIndex: 3,
    rawLogs: []
  },
  logs: []
}
[ 'DEV', 'ICX', 'BTC' ]
```

#### Add another relay

**WARNING** remember this is an override update, so, get the existing relays and add new one

```bash
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" truffle console --network moonbase --working_directory $SOLIDITY_DIST_DIR/bmc

## Output
> Warning: possible unsupported (undocumented in help) command line option(s): --working_directory
truffle(moonbase)> let bmcM = await BMCManagement.deployed()
undefined
truffle(moonbase)> await bmcM.getRelays("btp://0x42.icon/cx11a5a7510b128e0ab16546e1493e38b2d7e299c3")
truffle(moonbase)> await bmcM.addRelay("btp://0x42.icon/cx11a5a7510b128e0ab16546e1493e38b2d7e299c3", ["0x126ad520629a0152b749af26d5fd342cb67ac6ce"])
{
  tx: '0x87913e2186b32407e259025fdacf7534fffc772358c3ff11fe4b1fbc897c704e',
  receipt: {
    blockHash: '0xe7ed1ec83d7fa93fc6b7c5d796eebfc9c024880f30817b237af7009f07010955',
    blockNumber: 1047745,
    contractAddress: null,
    cumulativeGasUsed: 94482,
    from: '0x4bb718cb404787bf97bb012bb08096602fb9544b',
    gasUsed: 94482,
    logs: [],
    logsBloom: '0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000',
    status: true,
    to: '0xb3ad0707f494393a7d922f14a412e3518ed0b6bc',
    transactionHash: '0x87913e2186b32407e259025fdacf7534fffc772358c3ff11fe4b1fbc897c704e',
    transactionIndex: 0,
    rawLogs: []
  },
  logs: []
}
truffle(moonbase)> await bmcM.getRelays("btp://0x42.icon/cx11a5a7510b128e0ab16546e1493e38b2d7e299c3")
[ '0x126AD520629a0152b749Af26d5fd342Cb67Ac6CE' ]
```

### Redeploy new BMV

For example the next BTP address is: `btp://0x42.icon/cx11a5a7510b128e0ab16546e1493e38b2d7e299c3`
And the offset is: `5174702`. Why `5174702`? Because the next BTPMessage is on `5174703`, so we have to redeploy new BMV with the offset smaller than the next BTPMessage to prevent the system fails.

```bash
yarn install --production --cwd $SOLIDITY_DIST_DIR/bmv
rm -rf $SOLIDITY_DIST_DIR/bmv/build

truffle compile --all --working_directory $SOLIDITY_DIST_DIR/bmv

# @params
# - BMC_PERIPHERY_ADDRESS: an address on chain of BMCPeriphery contract
# This address is queried after **deploying** BMC contracts
# For example: BMC_PERIPHERY_ADDRESS = 0x6047341C88B9A45957E9Ad68439cA941D464c706
# - BMV_ICON_NET: Chain ID and name of a network that BMV is going to verify BTP Message
# - BMV_ICON_INIT_OFFSET: a block height when ICON-BMC was deployed
# - BMV_ICON_ENCODED_VALIDATORS: a result of ICON JSON-RPC method `icx_getDataByHash` with the input is
# PreviousBlockHeader.NextValidatorHash. So, to get this param for block N, you must get BlockHeader of N - 1
# User can execute to ge the result : 
#
# make iconvalidators
# 
# GOLOOP_RPC_URI="https://btp.net.solidwallet.io/api/v3/icon_dex" bin/iconvalidators build 5167618
# 
# - BMV_ICON_LASTBLOCK_HASH: a hash of the above block
# GOLOOP_RPC_URI="https://btp.net.solidwallet.io/api/v3/icon_dex" goloop rpc blockbyheight 5167618 | jq -r '.block_hash'
# Remember adding 0x 
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" \
BMC_PERIPHERY_ADDRESS="0x3e525eD7a82B87bE30cdADE89d32204cA0F1C356" \
BMV_ICON_NET="0x42.icon" \
BMV_ICON_ENCODED_VALIDATORS="0xf8589500edeb1b82b94d548ec440df553f522144ca83fb8d9500d63c4c73b623e97f67407d687af4efcfe486a51595007882dace25ff7e947d3a25178a2a1162874cfddc95000458d8b6f649a9e005963dc9a72669c89ed52d85" \
BMV_ICON_INIT_OFFSET="5174702" \
BMV_ICON_INIT_ROOTSSIZE="12" \
BMV_ICON_INIT_CACHESIZE="12" \
BMV_ICON_LASTBLOCK_HASH="0xcdaf4a7ad9fb77bac8e9c6cf296233c3cfba789ffbb4f2fb3412a3266951e7be" \
truffle migrate --network moonbase --working_directory $SOLIDITY_DIST_DIR/bmv

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


1_deploy_bmv.js
===============

   Deploying 'DataValidator'
   -------------------------
   > transaction hash:    0xa309cfc09dab73e63c602d40218fe43ce26e17b1b5ebc5316d9cda16bd5e5261
   > Blocks: 3            Seconds: 44
   > contract address:    0xa6a147af24D6416B3CC8E91372af1eaf477c0E4f
   > block number:        1054138
   > block timestamp:     1635495480
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             313.319527535469765665
   > gas used:            2120414 (0x205ade)
   > gas price:           1 gwei
   > value sent:          0 ETH
   > total cost:          0.002120414 ETH


   Deploying 'ProxyAdmin'
   ----------------------
   > transaction hash:    0x4205479afe06571c7495479cfddd64ea5d17d82eb606e312bbc9ff38a7b5b03a
   > Blocks: 13           Seconds: 192
   > contract address:    0x1EccC74336c47f4f696Ffd210E4884C1F480bB94
   > block number:        1054152
   > block timestamp:     1635495684
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             313.319044815469765665
   > gas used:            482720 (0x75da0)
   > gas price:           1 gwei
   > value sent:          0 ETH
   > total cost:          0.00048272 ETH


   Deploying 'TransparentUpgradeableProxy'
   ---------------------------------------
   > transaction hash:    0x2a35d39fb9e36071fc7d1f02761b0a9c9842877b5a211c874ff3f0c78d895f6a
   > Blocks: 3            Seconds: 37
   > contract address:    0xD20d590dd12fF328d836D0F87D30464c20bF8f20
   > block number:        1054155
   > block timestamp:     1635495726
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             313.318427257469765665
   > gas used:            617558 (0x96c56)
   > gas price:           1 gwei
   > value sent:          0 ETH
   > total cost:          0.000617558 ETH


   Deploying 'BMV'
   ---------------
   > transaction hash:    0x00ae73c60709dc17aeb704c5eb8f7ae907cfacb0d444c6427292c9673b0e7090
   > Blocks: 2            Seconds: 32
   > contract address:    0x192db7A8AE41A712314eD8ab7d16910C16A3B72E
   > block number:        1054158
   > block timestamp:     1635495762
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             313.314086019469765665
   > gas used:            4341238 (0x423df6)
   > gas price:           1 gwei
   > value sent:          0 ETH
   > total cost:          0.004341238 ETH


   Deploying 'TransparentUpgradeableProxy'
   ---------------------------------------
   > transaction hash:    0x0a1c3a18ce5f57467496b155431b44e8b0d9e11986371a9c453d0fd8abe734da
   > Blocks: 19           Seconds: 244
   > contract address:    0x22cFb9cb119DCBfBEBae735ece2AbC3b9e40be81
   > block number:        1054177
   > block timestamp:     1635496020
   > account:             0x4bb718Cb404787BF97bB012Bb08096602fb9544B
   > balance:             313.312959958469765665
   > gas used:            1126061 (0x112ead)
   > gas price:           1 gwei
   > value sent:          0 ETH
   > total cost:          0.001126061 ETH

   > Saving artifacts
   -------------------------------------
   > Total cost:         0.008687991 ETH


Summary
=======
> Total deployments:   5
> Final cost:          0.008687991 ETH
```

- Extract BMV address

```bash
truffle console --network moonbase --working_directory $SOLIDITY_DIST_DIR/bmv
> Warning: possible unsupported (undocumented in help) command line option(s): --working_directory
truffle(moonbase)> let bmv = await BMV.deployed()
undefined
truffle(moonbase)> bmv.address
'0x22cFb9cb119DCBfBEBae735ece2AbC3b9e40be81'
truffle(moonbase)> .exit
```

- Add new verifier - BMV

```bash
PRIVATE_KEYS="${YOUR_PRIVATE_KEY}" truffle console --network moonbase --working_directory $SOLIDITY_DIST_DIR/bmc
truffle(moonbase)> let bmcM = await BMCManagement.at("0xb3aD0707F494393A7d922F14A412E3518eD0B6bc")
undefined
truffle(moonbase)> bmcM.removeVerifier("0x42.icon")
truffle(moonbase)> bmcM.addVerifier("0x42.icon", "0x22cFb9cb119DCBfBEBae735ece2AbC3b9e40be81")
{
  tx: '0x78e99c900a286ec74743381d2934ed75b7e6e8f2a8f2dcb363d051c57114317e',
  receipt: {
    blockHash: '0x337047c156d1242726b30046f7c25e89ae0e859f517ab0c8d905b9703418669f',
    blockNumber: 1054203,
    contractAddress: null,
    cumulativeGasUsed: 1018760,
    from: '0x4bb718cb404787bf97bb012bb08096602fb9544b',
    gasUsed: 91146,
    logs: [],
    logsBloom: '0x00000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000',
    status: true,
    to: '0xb3ad0707f494393a7d922f14a412e3518ed0b6bc',
    transactionHash: '0x78e99c900a286ec74743381d2934ed75b7e6e8f2a8f2dcb363d051c57114317e',
    transactionIndex: 32,
    rawLogs: []
  },
  logs: []
}
truffle(moonbase)> .exit
```

### JAVAScore

#### Deploy BMC

```
goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ sendtx deploy btp/javascore/bmc/build/libs/bmc-0.1.0-optimized.jar \
    --key_store godWallet.json --key_password gochain \
    --nid 66 --step_limit 13610920001 \
    --content_type application/java \
    --param _net="0x42.icon"

goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ txresult 0x3d9eb499c6744447033dad678d6e0d2f01542d227b56647c67b061a107b20a14
```

#### Deploy CPS

```
goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ sendtx deploy btp/javascore/fee_aggregation/CPFTreasury.zip \
    --key_store godWallet.json --key_password gochain \
    --nid 66 --step_limit 13610920001 \
    --content_type application/zip

goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ txresult 0x998ddf8593155f1790f7cec11483616503807adc5dcd974b799b092eebc54b6c
```

#### Deploy Fee Aggregator

```
goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ sendtx deploy btp/javascore/fee_aggregation/build/libs/fee-aggregation-system-1.0-optimized.jar --key_store godWallet.json --key_password gochain --nid 0x42 --step_limit 3519157719 --content_type application/java --param _cps_address=cx2cfe2207351afac374150fbd64540c3f797b4ce7 --param _band_protocol_address=cx2cfe2207351afac374150fbd64540c3f797b4ce7

goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ txresult 0x6e3dc5e88c0f3992ef9531299eeb97bec52bd96da1f7475d63f7bc4aed897847
```

#### Set Fee Aggregator address to BMC contract
```
goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ sendtx call --to cx11a5a7510b128e0ab16546e1493e38b2d7e299c3 --method setFeeAggregator --param _addr=cx37465423027f2dd3cc961d8b37a6ad04ddb17138 --key_store godWallet.json --key_password gochain --nid 0x42 --step_limit 3519157719

goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ txresult 0xbca569ad3a21f0e9d5b42412b8873452f8ec1f16dd3774df81c527179fb7c4ef
```

#### Deploy Event Decoder and Para BMV

```bash
cd btp/javascore/bmv/helper
yarn deployParaBMV
```

- Update `.env` file:

```
BMC_ADDRESS=cx11a5a7510b128e0ab16546e1493e38b2d7e299c3
DST_NET_ADDRESS=0x507.pra
# wss://wss-relay.testnet.moonbeam.network 
RELAY_ENDPOINT=wss://wss-relay.testnet.moonbeam.network 
# wss://moonbase.moonbeam.elara.patract.io 
PARA_ENDPOINT=wss://wss.testnet.moonbeam.network

RELAY_CHAIN_OFFSET=2214750
PARA_CHAIN_OFFSET=1047648
MTA_ROOT_SIZE=16
MTA_CACHE_SIZE=16
MTA_IS_ALLOW_WITNESS=true

ICON_ENDPOINT=https://btp.net.solidwallet.io/api/v3
ICON_KEYSTORE_PATH=/Users/leclevietnam/mwork/btp/javascore/bmv/godWallet.json
ICON_KEYSTORE_PASSWORD=gochain
ICON_NID=66
```

- Run script to deploy event decoder and BMV:

```
$ yarn deployParaBMV

yarn run v1.22.10
warning package.json: No license field
$ yarn ts-node src/deployParaBMV.ts
warning package.json: No license field
$ /Users/leclevietnam/mwork/btp/javascore/bmv/helper/node_modules/.bin/ts-node src/deployParaBMV.ts
2021-10-28 13:48:00        METADATA: Unknown types found, no types for AssetRegistrarMetadata, AssetType, AuthorId, BlockV0, Collator2, CollatorSnapshot, CurrencyId, ExitQ, InflationInfo, Nominator2, NominatorAdded, OrderedSet, ParachainBondConfig, RegistrationInfo, RelayChainAccountId, RewardInfo, RoundInfo, VestingBlockNumber
 Relay genesis hash:  0xe1ea3ab1d46ba8f4898b6b4b9c54ffc05282d299f89e84bd0fd08067758c9443
 Relay chain name:  "Moonbase Relay Testnet"
 Para genesis hash:  0x91bc6e169807aaa54802737e1c504b2577d4fafedd5a02c10293b1cd60e39527
 Para chain name:  "Moonbase Alpha"
 Get metadata of relay chain...
 Build event decoder for relay chain...
 Deploy relay chain event decoder...
------ transactionId:  0x10a294d5bfd654bef930cd09ca42302376e3e6ae3220c4e7be4db71c82c01e4d
 Get metadata of para chain...
 Build event decoder for para chain...
 Deploy para chain event decoder...
------ transactionId:  0x81743bd67cef1d6848911c4bf58db47f8b3e39232a646246e170a486ac315b3e
 Build para chain BMV...
 Deploy para chain BMV...
------ transactionId:  0x6122c412df795945fca8b638fd6588fb686273e6f8735da2d679eb39773fc690

                --------------------- DONE ----------------------
- Relay event decoder score address:         cx2e0ba47b2df8fc78af75001e1eb06732ea71dfde
- Para event decoder score address:          cx447ee0788b359e571a283f415a820b1977bf0713
- Para chain bmv score address:    cxb358e6a851909bd3215e1ee42cd79f0a676b65cc
```

#### Set Para BMV to BMC contract:

```bash
goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ sendtx call --to cx11a5a7510b128e0ab16546e1493e38b2d7e299c3 --method addVerifier --param _net=0x507.pra --param _addr=cxb358e6a851909bd3215e1ee42cd79f0a676b65cc --key_store godWallet.json --key_password gochain --nid 0x42 --step_limit 3519157719

goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ txresult 0xf52cce4260af2b92210ddaa9d8035e95d7a2e440ffc8f921fdb651fdf1f3f064
```

#### Add parachain link to BMC:

```bash
goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ sendtx call --to cx11a5a7510b128e0ab16546e1493e38b2d7e299c3 --method addLink --param _link=btp://0x507.pra/0x3e525eD7a82B87bE30cdADE89d32204cA0F1C356 --key_store godWallet.json --key_password gochain --nid 0x42 --step_limit 3519157719

goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ txresult 0x58c83e52228eef9f0fe9b3059b0f04b8361d3c7c65a1aa4dadaf07a5ffb2671a
```

#### Config parachain link of BMC:

```bash
goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ sendtx call --to cx11a5a7510b128e0ab16546e1493e38b2d7e299c3 --method setLinkRotateTerm --param _link=btp://0x507.pra/0x3e525eD7a82B87bE30cdADE89d32204cA0F1C356 --param _block_interval=12 --param _max_agg=10 --key_store godWallet.json --key_password gochain --nid 0x42 --step_limit 3519157719

goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ txresult 0x1b7c9b73bf066bdaac269713a1084723f8407c0a0f3a87eae2a78a64b4516749

goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ sendtx call --to cx11a5a7510b128e0ab16546e1493e38b2d7e299c3 --method setLinkDelayLimit --param _link=btp://0x507.pra/0x3e525eD7a82B87bE30cdADE89d32204cA0F1C356 --param _value=2 --key_store godWallet.json --key_password gochain --nid 0x42 --step_limit 3519157719

goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ txresult 0x2cb56e3a91ecc43c4268fc1eab50859b09daefd2656b4c741ec4c11a6eece00e
```

#### add relay address to parachain link of BMC:

```bash
goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ sendtx call --to cx11a5a7510b128e0ab16546e1493e38b2d7e299c3 --method addRelay --param _link=btp://0x507.pra/0x3e525eD7a82B87bE30cdADE89d32204cA0F1C356 --param _addr=hx2dbd4f999f0e6b3c017c029d569dd86950c23104 --key_store godWallet.json --key_password gochain --nid 0x42 --step_limit 3519157719

goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ txresult 0xdabca08cf388c374bf61e05dd32a2d6bde0f37e2d1225a447a144662ecc73b13
```

#### Deploy BSH

```bash
IRC2_SERIALIZED=$(xxd -p javascore/irc2Tradeable/build/libs/irc2Tradeable-0.1.0-optimized.jar | tr -d '\n')
goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ sendtx deploy btp/javascore/nativecoin/build/libs/nativecoin-0.1.0-optimized.jar \
    --key_store godWallet.json --key_password gochain \
    --nid 0x42 --step_limit 3519157719 \
    --content_type application/java \
    --param _irc31=cxc1e92e175e1e5f98edf62b192ae051caae994a97 \
    --param _name=ICX \
    --param _serializedIrc2=$IRC2_SERIALIZED

goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ txresult 0xdd53327a0f5b5e2b433c49ec43d8c9f45b54295de81b3cd74db99be75257810c
```

#### Deploy IRC2 token Jar

```bash
goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ sendtx deploy btp/javascore/nativecoin/build/libs/irc2-0.1.0-optimized.jar \
    --key_store godWallet.json --key_password gochain \
    --nid 0x42 --step_limit 3519157719 \
    --content_type application/java \
    --param _name=MOVR \
    --param _symbol=MOVR \
    --param _initialSupply=0x186A0 \
    --param _decimals=0x12

goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ txresult 0xdd53327a0f5b5e2b433c49ec43d8c9f45b54295de81b3cd74db99be75257810c
```


#### Deploy IRC2_BSH

```bash
goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ sendtx deploy btp/javascore/nativecoin/build/libs/nativecoinIRC2-0.1.0-optimized.jar \
    --key_store godWallet.json --key_password gochain \
    --nid 0x42 --step_limit 3519157719 \
    --content_type application/java \
    --param _bmc=cx11a5a7510b128e0ab16546e1493e38b2d7e299c3 \
    --param _irc2=cx11a5a7510b128e0ab16546e1493e38b2d7e299c3 \
    --param _name=ICX \
    --param _tokenName=MOVR

goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ txresult 0xdd53327a0f5b5e2b433c49ec43d8c9f45b54295de81b3cd74db99be75257810c
```

#### Deploy IRC2 token Jar

```bash
goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ sendtx deploy btp/javascore/nativecoin/build/libs/irc2-0.1.0-optimized.jar \
    --key_store godWallet.json --key_password gochain \
    --nid 0x42 --step_limit 3519157719 \
    --content_type application/java \
    --param _name=MOVR \
    --param _symbol=MOVR \
    --param _initialSupply=0x186A0 \
    --param _decimals=0x12

goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ txresult 0xdd53327a0f5b5e2b433c49ec43d8c9f45b54295de81b3cd74db99be75257810c
```


#### Deploy IRC2_BSH

```bash
goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ sendtx deploy btp/javascore/nativecoin/build/libs/nativecoinIRC2-0.1.0-optimized.jar \
    --key_store godWallet.json --key_password gochain \
    --nid 0x42 --step_limit 3519157719 \
    --content_type application/java \
    --param _bmc=cx11a5a7510b128e0ab16546e1493e38b2d7e299c3 \
    --param _irc2=cx11a5a7510b128e0ab16546e1493e38b2d7e299c3 \
    --param _name=ICX \
    --param _tokenName=MOVR

goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ txresult 0xdd53327a0f5b5e2b433c49ec43d8c9f45b54295de81b3cd74db99be75257810c
```

#### Register coin names to BSH

```bash
goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ sendtx call --to cx047d8cd08015a75deab90ef5f9e0f6878d5563bd --method register --param _name=DEV --param _symbol=DEV --param _decimals=18 --key_store godWallet.json --key_password gochain --nid 0x42 --step_limit 3519157719

# get txresult and score address
goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ txresult 0xa6cacdc4a8783f62dd981999f3ab7c08340618c8f01e8f5c84369e15c72831d9

# get IRC2 address by coin name
goloop rpc --uri http://127.0.0.1:9082/api/v3 call --to cxe737e1bcf7b2eb9a6c8dc96d59e0b8da26e57558 --method coinAddress --param _coinName=DEV

goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ sendtx call --to cx047d8cd08015a75deab90ef5f9e0f6878d5563bd --method register --param _name=BTC --key_store godWallet.json --key_password gochain --nid 0x42 --step_limit 3519157719

goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ txresult 0x41845171790b40df01dd2838e39569020ff24498034ac5c54eaeb28e33488d39
```

#### Set BSH fee ratio

```bash
goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ sendtx call --to cx047d8cd08015a75deab90ef5f9e0f6878d5563bd --method setFeeRatio --param _feeNumerator=1000 --key_store godWallet.json --key_password gochain --nid 0x42 --step_limit 3519157719

goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ txresult 0xddfb4763b746b44b8ec4540824425dc2b6dcf760b4630f5f3a2611e4de82690b
```

#### Set IRC2_BSH fee ratio

```bash
goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ sendtx call --to cx047d8cd08015a75deab90ef5f9e0f6878d5563bd --method setFeeRatio --param _feeNumerator=100 --key_store godWallet.json --key_password gochain --nid 0x42 --step_limit 3519157719

goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ txresult 0xddfb4763b746b44b8ec4540824425dc2b6dcf760b4630f5f3a2611e4de82690b
```

#### Add BSH service to BMC

```bash
goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ sendtx call --to cx11a5a7510b128e0ab16546e1493e38b2d7e299c3 --method addService --param _svc=nativecoin --param _addr=cx047d8cd08015a75deab90ef5f9e0f6878d5563bd --key_store godWallet.json --key_password gochain --nid 0x42 --step_limit 3519157719

goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ txresult 0x9ba88bbb9907cf1fe29ff2b50706c97d6fd08d09ea6394138444c8d34b40dd3d
```

#### Add IRC2_BSH service to BMC

```bash
goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ sendtx call --to cx11a5a7510b128e0ab16546e1493e38b2d7e299c3 --method addService --param _svc=NativeCoinIRC2BSH --param _addr=cx047d8cd08015a75deab90ef5f9e0f6878d5563bd --key_store godWallet.json --key_password gochain --nid 0x42 --step_limit 3519157719

goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ txresult 0x9ba88bbb9907cf1fe29ff2b50706c97d6fd08d09ea6394138444c8d34b40dd3d
```

#### Add BSH address to owner of IRC31 contract

```bash
goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ sendtx call --to cxc1e92e175e1e5f98edf62b192ae051caae994a97 --method addOwner \
      --key_store godWallet.json --key_password gochain \
      --nid 0x42 --step_limit 3519157719 \
      --param _addr=cx047d8cd08015a75deab90ef5f9e0f6878d5563bd

goloop rpc --uri https://btp.net.solidwallet.io/api/v3/ txresult 0x3f2ec91804204a9e3e3faa5f40b4674b72b217257eb389b4ccc13c703ef5f5f7
```

## Transfer coin using IRC2_BSH

### Transfer ICX from ICON to Moonbeam

```bash
goloop rpc --uri https://berlin.net.solidwallet.io/api/v3 sendtx call --to cx8a05039c1c1da936d279e276a25c4fa66154bebd --method transferNativeCoin --param _to=btp://0x507.pra/0x0e367f147682237a0Bc1c839a2a4a1b2c28Bd77C --value 1000000000000000000 --key_store daniel111.ks.json --key_password abc12345 --nid 0x7 --step_limit 3519157719
```

### Transfer DEV from Moonbeam to ICON

```bash
eth contract:send --network https://moonbeam-alpha.api.onfinality.io/public erc2Bshcore@0xC0bDA7E7Cb3f0277748aF59F1c639BE7589bE4Ec 'transferNativeCoin("btp://0x7.icon/hxdd7cc765bb90ef63fca515e362feb3cce3f63ec7")' --pk YOUR_PRIVATE_KEY --gas 6721975 --value 1000000000000000000
```

### Transfer ICX from Moonbeam to ICON

1. Approve for Moonbeam BSH to transfer ICX token

```bash
eth contract:send --network https://moonbeam-alpha.api.onfinality.io/public erc2Bshcore@0xC0bDA7E7Cb3f0277748aF59F1c639BE7589bE4Ec 'approve("0xC0bDA7E7Cb3f0277748aF59F1c639BE7589bE4Ec", "100000000000000000")' --pk YOUR_PRIVATE_KEY --gas 6721975
```

2. Transfer ERC20 ICX token to address on ICON

```bash
eth contract:send --network https://moonbeam-alpha.api.onfinality.io/public erc2Bshcore@0xC0bDA7E7Cb3f0277748aF59F1c639BE7589bE4Ec 'transferWrappedCoin("ICX","1000000000000000000", "btp://0x7.icon/hxc00a6d2d1e9ee0686704e0b6eec75d0f2c095b39")' --pk YOUR_PRIVATE_KEY --gas 6721975
```

### Transfer DEV from ICON to Moonbeam

1. Deposit IRC2 DEV token to BSH

```bash
goloop rpc --uri https://berlin.net.solidwallet.io/api/v3 sendtx call --to cx824f3b2f2a8f59ac3d281b1b9bc295e051be5274 --method transfer --param _to=cx8a05039c1c1da936d279e276a25c4fa66154bebd --param _value=100000000000000000 --key_store daniel111.ks.json --nid 0x7 --step_limit 3519157719 --key_password abc12345
```

2. Transfer IRC2 DEV token to address on Moonbeam

```bash
goloop rpc --uri https://berlin.net.solidwallet.io/api/v3 sendtx call --to cx8a05039c1c1da936d279e276a25c4fa66154bebd --method transfer --param _coinName=DEV --param _value=100000000000000000 --param _to=btp://0x507.pra/0x0e367f147682237a0Bc1c839a2a4a1b2c28Bd77C --key_store daniel111.ks.json --nid 0x7 --step_limit 3519157719 --key_password abc12345
```

## Transfer coin using BSH factory

### Check ICX balance on ICON

```bash
goloop rpc --uri https://berlin.net.solidwallet.io/api/v3 balance hxc00a6d2d1e9ee0686704e0b6eec75d0f2c095b39
```

### Check DEV balance on ICON

1. Call BSH score to get IRC2 contract address of DEV token

```bash
goloop rpc --uri https://berlin.net.solidwallet.io/api/v3 call --to cx5bab2d3aa3eed9b0a2d9445d18ec5812155ff6f1 --method coinAddress --param _coinName=DEV
"cxa7f01ce9e0901836eba39e243448837314b48549"
```

2. Call IRC2 score to get balance

```bash
goloop rpc --uri https://berlin.net.solidwallet.io/api/v3 call --to cxd815dd0290c1536ed11a94239d0219046b2a5c31 --method balanceOf --param _owner=hxc00a6d2d1e9ee0686704e0b6eec75d0f2c095b39
```

### Check DEV balance on Moonbeam

```bash
eth address:balance --network https://moonbeam-alpha.api.onfinality.io/public 0x0e367f147682237a0Bc1c839a2a4a1b2c28Bd77C
```

### Check ICX balance on Moonbeam

1. Call BSH contract to get ERC20 contract address of ICX token

```bash
eth contract:call --network https://moonbeam-alpha.api.onfinality.io/public erc2Bshcore@0x7d4567B7257cf869B01a47E8cf0EDB3814bDb963 'coinId("ICX")'
0xc994Cdd31F92b778b87d7CdeA77dA3Ffa2e4f3d6
```

2. Call ERC20 contract to get balance

```
eth contract:call --network https://moonbeam-alpha.api.onfinality.io/public ERC20@0xc994Cdd31F92b778b87d7CdeA77dA3Ffa2e4f3d6 'balanceOf("0x0e367f147682237a0Bc1c839a2a4a1b2c28Bd77C")'
```

### Transfer ICX from ICON to Moonbeam

```
goloop rpc --uri https://berlin.net.solidwallet.io/api/v3 sendtx call --to cx8a05039c1c1da936d279e276a25c4fa66154bebd --method transferNativeCoin --param _to=btp://0x507.pra/0x0e367f147682237a0Bc1c839a2a4a1b2c28Bd77C --value 1000000000000000000 --key_store daniel111.ks.json --key_password abc12345 --nid 0x7 --step_limit 3519157719
```

### Transfer DEV from Moonbeam to ICON

```
eth contract:send --network https://moonbeam-alpha.api.onfinality.io/public erc2Bshcore@0x7d4567B7257cf869B01a47E8cf0EDB3814bDb963 'transferNativeCoin("btp://0x7.icon/hxdd7cc765bb90ef63fca515e362feb3cce3f63ec7")' --pk YOUR_PRIVATE_KEY --gas 6721975 --value 1000000000000000000
```

### Transfer ICX from Moonbeam to ICON

1. Call ERC20 contract to approve for Moonbeam BSH to transfer ICX token

```bash
eth contract:send --network https://moonbeam-alpha.api.onfinality.io/public ERC20@0xC0bDA7E7Cb3f0277748aF59F1c639BE7589bE4Ec 'approve("0xC0bDA7E7Cb3f0277748aF59F1c639BE7589bE4Ec", "100000000000000000")' --pk YOUR_PRIVATE_KEY --gas 6721975
```

2. Call BSH Core to transfer ERC20 ICX token to address on ICON

```bash
eth contract:send --network https://moonbeam-alpha.api.onfinality.io/public erc2Bshcore@0xC0bDA7E7Cb3f0277748aF59F1c639BE7589bE4Ec 'transfer("ICX","1000000000000000000", "btp://0x7.icon/hxc00a6d2d1e9ee0686704e0b6eec75d0f2c095b39")' --pk YOUR_PRIVATE_KEY --gas 6721975
```

### Transfer DEV from ICON to Moonbeam

1. Call IRC2 score to aprove DEV token to BSH

```bash
goloop rpc --uri https://berlin.net.solidwallet.io/api/v3 sendtx call --to cx824f3b2f2a8f59ac3d281b1b9bc295e051be5274 --method approve --param spender=cx8a05039c1c1da936d279e276a25c4fa66154bebd --param amount=100000000000000000 --key_store daniel111.ks.json --nid 0x7 --step_limit 3519157719 --key_password abc12345
```

2. Call BSH to transfer DEV to Moonbeam address

```bash
goloop rpc --uri https://berlin.net.solidwallet.io/api/v3 sendtx call --to cx8a05039c1c1da936d279e276a25c4fa66154bebd --method transfer --param _coinName=DEV --param _value=100000000000000000 --param _to=btp://0x507.pra/0x0e367f147682237a0Bc1c839a2a4a1b2c28Bd77C --key_store daniel111.ks.json --nid 0x7 --step_limit 3519157719 --key_password abc12345
```
# TokenTransfer BSH

ICON Blockchain Transmission Protocol for Binance Smart Chain

The original Libraries from the repository below has been modified or used as in original to suit our needs.
Credits to ICONDAO's team : Please refer https://github.com/icon-project/btp/tree/icondao/solidity/bsh for more information.

## Requirements

- Node >= 10.x

Install dependencies from the project path:

```
$ npm install -g truffle
$ npm install
```

To run a local development node using Truffle
```
$ truffle develop
```
In another new terminal, run 

``` truffle test --network development test/token-bsh.js ``` 

##### To test BEP20 against BSC
Ensure there are two accounts created with balance in the BSC node.

For More details on BSC setup
https://docs.binance.org/smart-chain/developer/deploy/local.html
https://docs.binance.org/smart-chain/developer/fullnode.html

1. start up the local BSC node 
2. add the private keys of the two accounts in the truffle-config.js 
3. run the command to test
``` truffle test --network testnet test/bep20-token-bsh.js ```

$ ganache-cli -l 12000000 --allowUnlimitedContractSize

SET Env variable

BSH_TOKEN_FEE=1
BMC_PERIPHERY_ADDRESS=0x503c16557325C0FCDf5fAB9e31B7ec29F9690066
BSH_SERVICE=TokenBSH

run the mirgate with:
BSH_TOKEN_FEE=1 BMC_PERIPHERY_ADDRESS=0x503c16557325C0FCDf5fAB9e31B7ec29F9690066  BSH_SERVICE=TokenBSH truffle migrate --compile-all --network bsc

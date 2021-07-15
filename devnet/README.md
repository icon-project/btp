# BSC Private Network Setup

This directory contains instructions and genesis private Binance Smart Chain testnet equivalent.

## Preparation

1. Create genesis
````shell
./bsc/build/bin/geth init --datadir node1 devnet/genesis.json
```` 
2. Start signer node
````shell
./bsc/build/bin/geth --datadir node1 --syncmode 'full' --port 30312 --rpc --rpcaddr 'localhost' --rpccorsdomain "*" --rpcport 8545 --ws --wsport 8546 --rpcapi 'personal,eth,net,web3,txpool,miner' --networkid 97 --gasprice '1' --unlock '0x48948297C3236ec3eA6c95f4eEc22fDb18255E55' --password pass.txt --mine --allow-insecure-unlock
```` 


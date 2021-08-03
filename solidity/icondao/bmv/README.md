## Set up
Node >= 10.x
```
$ node --version
v15.12.0
```
Install tools
```
$ npm install --global yarn truffle@5.3.0
```
Install dependencies
```
$ yarn
```

## Test
1. Run in a background process or seperate terminal window
```
$ ./bsc/build/bin/geth --datadir node1 --syncmode 'full' --port 30312 --rpc --rpcaddr 0.0.0.0 --rpccorsdomain "*" --rpcport 8545 --ws --wsaddr 0.0.0.0 --wsport 8546 --rpcapi 'personal,eth,net,web3,txpool,miner' --networkid 97 --unlock '0x48948297C3236ec3eA6c95f4eEc22fDb18255E55' --password pass.txt --mine --allow-insecure-unlock
```
2. Compile contracts
```
$ yarn contract:compile
```
3. Run unit and integration test
```
$ yarn test
```
-  Run specific test
```
$ yarn test:unit
$ yarn test:integration
```
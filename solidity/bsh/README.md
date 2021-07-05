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
Install dev dependencies
```
$ yarn
```

## Test
1. Run in a background process or seperate terminal window
```
// Testing with Moonbeam local testnet
$ docker run --rm -p 9933:9933 -p 9944:9944 purestake/moonbeam:v0.8.0 --dev --ws-external --rpc-external
```
2. Run test
```
$ truffle compile --all
//  If testing with Moonbeam local testnet
$ yarn test     
```
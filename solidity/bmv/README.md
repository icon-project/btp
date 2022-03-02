## Set up
Node >= 10.x
```
$ node --version
v15.12.0
```
Install tools
```
$ npm install --global yarn truffle@5.3.13
```
Install dependencies
```
$ yarn
```

## Test
1. Run in a background process or seperate terminal window
```
$ docker run --rm -p 9933:9933 -p 9944:9944 purestake/moonbeam:v0.9.2 --dev --ws-external --rpc-external
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
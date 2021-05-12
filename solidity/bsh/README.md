## Set up
Node >= 10.x
```
$ node --version
v15.12.0
```
Install tools
```
$ npm install --global yarn truffle ganache-cli
```
Install dev dependencies
```
$ yarn
```

## Test
1. Run in a background process or seperate terminal window
```
$ ganache-cli -l 12000000 --allowUnlimitedContractSize
```
2. Run test
```
$ yarn test
```
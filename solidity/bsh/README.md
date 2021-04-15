## NativeCoinBSH


### TODO


### Setup

Be sure to have node >= 10.x

```
$ node --version
v15.12.0
```

Install dev dependencies:

```
$ npm install -g truffle
$ npm install -g ganache-cli
$ npm install
```

### Compile

```
$ npm run compile
```
### Test

Open a terminal window
```
$ ganache-cli -l 12000000 --allowUnlimitedContractSize
```
Open another terminal window
```
$ npm test
```
### Format

```
$ npm run prettier

```

### Deploy


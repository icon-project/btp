const EthUtil = require('ethereumjs-util')
const EthTx = require('ethereumjs-tx').Transaction
// signed tx
const signedTx = "0xf86b808504a817c800825208942890228d4478e2c3b0ebf5a38479e3396c1d6074872386f26fc100008029a0520e5053c1b573d747f823a0b23d52e5a619298f46cd781d677d0e5e78fbc750a075be461137c2c2a5594beff76ecb11a215384c574a7e5b620dba5cc63b0a0f13"
// Create a tx object from signed tx 
const tx = new EthTx(signedTx,  { chain: 'ropsten'})
// Get an address of sender
const address = EthUtil.bufferToHex(tx.getSenderAddress())
// get a public key of sender
const publicKey = EthUtil.bufferToHex(tx.getSenderPublicKey())
console.log(address)
// => 0x89c24a88bad4abe0a4f5b2eb5a86db1fb323832c
console.log(publicKey)
// => 0xfff49b58b83104ff16875452852466a46c7169ba4e368d11830c9170624e0a9509080a05a38c18841718ea4fc13483ac467d3e2d728d41ff16b73b9c943734f8
const Web3 = require('web3')
var Personal = require('web3-eth-personal')
const ethers = require('ethers')

const web3 = new Web3('ws://localhost:9944')
const personal = new Personal('http://34.125.31.100:8545')
const provider = new ethers.providers.JsonRpcProvider('http://localhost:9933')

function hex_to_ascii(str1) {
  var hex  = str1.toString();
  var str = '';
  for (var n = 0; n < hex.length; n += 2) {
    str += String.fromCharCode(parseInt(hex.substr(n, 2), 16));
  }
  return str;
}

async function reason(hash) {
  console.log('tx hash:', hash)
  console.log('provider:', process.env.WEB3_URL)

  let tx = await provider.getTransaction(hash)
  if (!tx) {
    console.log('tx not found')
  } else {
    let code = await provider.call(tx, tx.blockNumber)
    let reason = hex_to_ascii(code.substr(138))
    console.log('revert reason:', reason)
  }
}

let owner = "0xf24FF3a9CF04c71Dbc94D0b566f7A27B94566cac"

async function reason(hash) {
  console.log('tx hash:', hash)
  let tx = await provider.getTransaction(hash)
  if (!tx) {
    console.log('tx not found')
  } else {
    let code = await provider.call(tx, tx.blockNumber)
    console.log(code)
    let reason = hex_to_ascii(code.substr(138))
    console.log('revert reason:', reason)
  }
}

async function main() {
  //const accounts = await web3.eth.getAccounts()
  //await personal.unlockAccount(owner, "Perlia0", 60*60*12)
  let balance = await  web3.eth.getBalance(owner)
  console.log("Owner Balance: ", web3.utils.fromWei(balance ,"ether"))
 
  console.log(await web3.eth.getTransactionReceipt("0x5702521ec3e6f67c1c5a4ef74e11c62b3a75552c1116fc06aa6af0f7b36bb1aa"))
  reason("0x5702521ec3e6f67c1c5a4ef74e11c62b3a75552c1116fc06aa6af0f7b36bb1aa") 

}

main().then(r => console
    .log("Done."))
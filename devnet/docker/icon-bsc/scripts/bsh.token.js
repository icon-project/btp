const BSHProxy = artifacts.require('BSHProxy')
const BEP20TKN = artifacts.require('BEP20TKN');
var fs = require('fs');
module.exports = async function (callback) {
  try {
    var argv = require('minimist')(process.argv.slice(2), { string: 'addr' });
    const bshProxy = await BSHProxy.deployed();
    const bep20tkn = await BEP20TKN.deployed();
    let tx;
    switch (argv["method"]) {
      case "registerToken":
        console.log("registerToken", argv.name)
        tx = await bshProxy.register(argv.name, argv.symbol, 18, 100, argv.addr)
        //console.log(await bshProxy.tokenNames())
        console.log(tx)
        break;
      case "fundBSH":
        console.log("fundBSH", argv.addr)
        tx = await bep20tkn.transfer(argv.addr, web3.utils.toWei("100", 'ether'))
        console.log(tx)
        var bal = await bep20tkn.balanceOf(argv.addr)
        console.log("BSH Balance" + bal)
        break;
      case "getBalance":
        var balance = await bep20tkn.balanceOf(argv.addr)
        //var balance=web3.utils.fromWei(await bep20tkn.balanceOf(argv.addr),"ether")
        //console.log("Balance:" + balance);
        var bal = await web3.utils.fromWei(balance, "ether")
        console.log("Balance: " + bal)
        break;
      case "approve":
        console.log("Approving BSH to use Bob's tokens")
        var balance = await bep20tkn.approve(argv.addr, web3.utils.toWei("" + argv.amount, 'ether'))
        break;
      case "transfer":
        console.log("Init BTP transfer of " + web3.utils.toWei("" + argv.amount, 'ether') + " wei to " + argv.to)
        tx = await bshProxy.transfer("ETH", web3.utils.toWei("" + argv.amount, 'ether'), argv.to)
        console.log(tx)
        break;
      case "calculateTransferFee":
        let result = await bshProxy.calculateTransferFee(bep20tkn.address, web3.utils.toWei("" + argv.amount, 'ether'))
        console.log("amount:" + result.value)
        console.log("fee:" + result.fee)
        break;
      default:
        console.error("Bad input for method, ", argv)
    }
  }
  catch (error) {
    console.log(error)
  }
  callback()
}
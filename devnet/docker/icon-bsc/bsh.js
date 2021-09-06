const BSHProxy = artifacts.require('BSHProxy')
const BEP20TKN = artifacts.require('BEP20TKN');
module.exports = async function (callback) {
  try {
    var argv = require('minimist')(process.argv.slice(2), { string: 'addr' });
    const bshProxy = await BSHProxy.deployed();
    const bep20tkn = await BEP20TKN.deployed();
    let tx;
    switch (argv["method"]) {
      case "registerToken":
        console.log("registerToken", argv.name)
        await bshProxy.register(argv.name, argv.symbol, 18, 1, argv.addr)
        console.log(await bshProxy.tokenNames())
        break;
      case "fundBSH":
        console.log("fundBSH", argv.addr)
        await bep20tkn.transfer(argv.addr, argv.amount)
        var bal = await bep20tkn.balanceOf(argv.addr)
        console.log("BSH Balance" + bal)
        break;
      case "getBalance":
        console.log("getBalance", argv.addr)
        var bal = await bep20tkn.balanceOf(argv.addr)
        console.log("User Balance: " + bal)
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
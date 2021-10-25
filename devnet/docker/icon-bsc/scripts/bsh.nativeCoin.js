const BSHCore = artifacts.require("BSHCore");
module.exports = async function (callback) {
  try {
    var argv = require('minimist')(process.argv.slice(2), { string: ['addr', 'from'] });
    const bshCore = await BSHCore.deployed();
    let tx;
    switch (argv["method"]) {
      case "register":
        console.log("registerCoin", argv.name)
        tx = await bshCore.register(argv.name);
        //console.log(await bshCore.coinNames())
        console.log(tx)
        break;
      case "getBalanceOf":
        var balance = await bshCore.getBalanceOf(argv.addr, argv.name);
        //console.log("balance of user:" + argv.addr + " = " + balance._usableBalance);
        var bal = await web3.utils.fromWei(balance._usableBalance, "ether")
        console.log(bal)
        break;
      case "transferNativeCoin":
        console.log("Init BTP native transfer of " + web3.utils.toWei("" + argv.amount, 'ether') + " wei to " + argv.to)
        tx = await bshCore.transferNativeCoin(argv.to, { from: argv.from, value: web3.utils.toWei("" + argv.amount, 'ether') })
        console.log(tx)
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
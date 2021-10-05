const BSHCore = artifacts.require("BSHCore");
module.exports = async function (callback) {
  try {
    var argv = require('minimist')(process.argv.slice(2), { string: 'addr' });
    const bshCore = await BSHCore.deployed();
    let tx;
    switch (argv["method"]) {
      case "register":
        console.log("registerCoin", argv.name)
        await bshCore.register(argv.name);
        console.log(await bshCore.coinNames())
        break;
      case "getBalanceOf":
        var balance = await bshCore.getBalanceOf(argv.addr, argv.name);
        //console.log("balance of user:" + argv.addr + " = " + balance._usableBalance);
        var bal=await web3.utils.fromWei(balance._usableBalance,"ether")
        console.log(bal)
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
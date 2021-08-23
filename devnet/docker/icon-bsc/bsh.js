const BSHProxy = artifacts.require('BSHProxy')
module.exports = async function (callback) {
  try {
    var argv = require('minimist')(process.argv.slice(2), { string: 'addr' });
    const bshProxy = await BSHProxy.deployed();
    let tx;
    switch (argv["method"]) {
      case "registerToken":
        console.log("registerToken", argv.name)
        await bshProxy.register(argv.name, argv.symbol, 18, 1, argv.addr)
        console.log(await bshProxy.tokenNames())
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
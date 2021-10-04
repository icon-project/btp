const BMV = artifacts.require('BMV')
module.exports = async function (callback) {
  try {
    var argv = require('minimist')(process.argv.slice(2), { string: 'addr', string: 'lasthash' });
    const bmv = await BMV.deployed();
    console.log(bmv.address)
    let tx;
    switch (argv["method"]) {
      case "setMTA":
        console.log("setMTA", argv.offset, argv.lasthash)
        let tx = await bmv.setNewMTA(argv.offset, argv.lasthash)
        console.log(await bmv.getStatus())
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
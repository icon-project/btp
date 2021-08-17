const BMCManagement = artifacts.require('BMCManagement');
module.exports = async function(callback) {
  try {
    var argv = require('minimist')(process.argv.slice(2), { string: 'addr' });
    const bmcManagement = await BMCManagement.deployed();
    let tx;
    switch (argv["method"]) {
      case "addVerifier":
        console.log("Add verifier ", argv.net, argv.addr)
        tx = await bmcManagement.addVerifier(argv.net, argv.addr);
        let verifiers = await bmcManagement.getVerifiers();
        console.log(verifiers)
        break;
      case "addLink":
        console.log("Add link ", argv.link)
        tx = await bmcManagement.addLink(argv.link);
        console.log("links: ", await bmcManagement.getLinks())
        console.log("Set link")
        await bmcManagement.setLink(argv.link, argv.blockInterval, argv.maxAggregation, argv.delayLimit);
        break;
      case "addRelay":
        console.log("Add relay ", argv.link)
        let relays = [argv.addr]
        await bmcManagement.addRelay(argv.link, relays)
        console.log(await bmcManagement.getRelays(argv.link))
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
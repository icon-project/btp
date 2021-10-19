const BMCManagement = artifacts.require('BMCManagement');
module.exports = async function (callback) {
  try {
    var argv = require('minimist')(process.argv.slice(2), { string: 'addr' });
    const bmcManagement = await BMCManagement.deployed();
    let tx;
    switch (argv["method"]) {
      case "addVerifier":
        console.log("Add verifier ", argv.net, argv.addr)
        tx = await bmcManagement.addVerifier(argv.net, argv.addr);
        //let verifiers = await bmcManagement.getVerifiers();
        console.log(tx)
        break;
      case "addLink":
        console.log("Add link ", argv.link)
        tx = await bmcManagement.addLink(argv.link);
        console.log(tx)
        console.log("Set link")
        tx = await bmcManagement.setLink(argv.link, argv.blockInterval, argv.maxAggregation, argv.delayLimit);
        console.log(tx)
        break;
      case "addRelay":
        console.log("Add relay ", argv.link)
        let relays = [argv.addr]
        tx = await bmcManagement.addRelay(argv.link, relays)
        //console.log(await bmcManagement.getRelays(argv.link))
        console.log(tx)
        break;
      case "addService":
        console.log("Add Service ", argv.name)
        tx = await bmcManagement.addService(argv.name, argv.addr)
        //console.log(await bmcManagement.getServices())
        console.log(tx)
        break;
      case "updateRxSeq":
        await bmcManagement.updateLinkRxSeq(argv.link, argv.value)
        console.log(await bmcManagement.getLinkRxSeq(argv.link))
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
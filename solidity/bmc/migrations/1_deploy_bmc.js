const BMCManagement = artifacts.require('BMCManagement');
const BMCPeriphery = artifacts.require('BMCPeriphery');
const { deployProxy } = require('@openzeppelin/truffle-upgrades');
const fs = require('fs')

module.exports = async function (deployer, network) {
  if (network !== "development") {
    await deployProxy(BMCManagement, { deployer });
    await deployProxy(BMCPeriphery, [process.env.BMC_PRA_NET, BMCManagement.address], { deployer });

    const bmcManagement = await BMCManagement.deployed();
    await bmcManagement.setBMCPeriphery(BMCPeriphery.address);

    let filename = process.env.CONFIG_DIR + "/bmc.moonbeam"
    fs.writeFileSync(filename, BMCPeriphery.address, function (err, data) {
      if (err) {
        return console.log(err);
      }
    });
  }
};
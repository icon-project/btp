const BMCManagement = artifacts.require('BMCManagement');
const BMCPeriphery = artifacts.require('BMCPeriphery');
const { deployProxy } = require('@openzeppelin/truffle-upgrades');

module.exports = async function (deployer, network) {
  if (network !== "development") {
    await deployProxy(BMCManagement, { deployer });
    await deployProxy(BMCPeriphery, [process.env.BMC_PRA_NET, BMCManagement.address], { deployer });

    const bmcManagement = await BMCManagement.deployed();
    await bmcManagement.setBMCPeriphery(BMCPeriphery.address);
  }
};
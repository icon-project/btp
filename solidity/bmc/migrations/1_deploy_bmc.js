const BMCManagement = artifacts.require('BMCManagement');
const BMCPeriphery = artifacts.require('BMCPeriphery');

module.exports = async function (deployer, network) {
  if (network !== "development") {
    await deployer.deploy(BMCManagement);
    await deployer.deploy(BMCPeriphery, process.env.BMC_PRA_NET, BMCManagement.address);

    const bmcManagement = await BMCManagement.deployed();
    await bmcManagement.setBMCPeriphery(BMCPeriphery.address);
  }
};
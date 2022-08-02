const BMCManagement = artifacts.require('BMCManagement');
const BMCPeriphery = artifacts.require('BMCPeriphery');
const { deployProxy } = require('@openzeppelin/truffle-upgrades');

module.exports = async function (deployer, network) {
    if (network !== "development") {
        await deployProxy(BMCManagement, {deployer});
        await deployProxy(BMCPeriphery, [process.env.BMC_NETWORK_ID, BMCManagement.address], {deployer});

        const bmcManagement = await BMCManagement.deployed();
        await bmcManagement.setBMCPeriphery(BMCPeriphery.address);
    } else {
        await deployer.deploy(BMCManagement);
        const bmcm = await BMCManagement.deployed();
        await bmcm.initialize();
        await deployer.deploy(BMCPeriphery);
        const BMC_NETWORK_ID=0x61
        const bmcp = await BMCPeriphery.deployed();
        await bmcp.initialize(BMC_NETWORK_ID, BMCManagement.address);
        await bmcm.setBMCPeriphery(BMCPeriphery.address);
        let btpAddress = await bmcp.getBmcBtpAddress()
        console.log(btpAddress);
    }
};

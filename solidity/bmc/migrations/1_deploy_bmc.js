const BMCManagement = artifacts.require('BMCManagement');
const BMCService = artifacts.require('BMCService');
const BMCPeriphery = artifacts.require('BMCPeriphery');
const { deployProxy } = require('@openzeppelin/truffle-upgrades');

module.exports = async function (deployer, network) {
    if (network !== "development") {
        await deployProxy(
            BMCManagement,
            {deployer});
        await deployProxy(
            BMCService,
            [
                BMCManagement.address
            ],
            {deployer});
        await deployProxy(
            BMCPeriphery,
            [
                process.env.BMC_NETWORK_ID,
                BMCManagement.address,
                BMCService.address
            ],
            {deployer});

        const bmcm = await BMCManagement.deployed();
        await bmcm.setBMCService(BMCService.address);
        await bmcm.setBMCPeriphery(BMCPeriphery.address);

        const bmcs = await BMCService.deployed();
        await bmcs.setBMCPeriphery(BMCPeriphery.address);
    } else {
        await deployer.deploy(BMCManagement);
        const bmcm = await BMCManagement.deployed();
        await bmcm.initialize();

        await deployer.deploy(BMCService);
        const bmcs = await BMCService.deployed();
        await bmcs.initialize(BMCManagement.address);

        await deployer.deploy(BMCPeriphery);
        const BMC_NETWORK_ID=0x61
        const bmcp = await BMCPeriphery.deployed();
        await bmcp.initialize(BMC_NETWORK_ID, BMCManagement.address, BMCService.address);

        await bmcm.setBMCPeriphery(BMCPeriphery.address);
        await bmcm.setBMCService(BMCService.address);
        await bmcs.setBMCPeriphery(BMCPeriphery.address);

        let btpAddress = await bmcp.getBtpAddress()
        console.log(btpAddress);
    }
};

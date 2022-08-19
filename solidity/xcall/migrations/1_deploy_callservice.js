const CallService = artifacts.require('CallService');
const { deployProxy } = require('@openzeppelin/truffle-upgrades');

module.exports = async function (deployer, network) {
    if (network !== "development") {
        await deployProxy(CallService, [process.env.BMC], {deployer});
    } else {
        await deployer.deploy(CallService);
        const callService = await CallService.deployed();
        await callService.initialize(process.env.BMC);
    }
};

const CallService = artifacts.require('CallService');
const DAppProxySample = artifacts.require('DAppProxySample');
const { deployProxy } = require('@openzeppelin/truffle-upgrades');

module.exports = async function (deployer, network) {
    if (network !== "development") {
        await deployProxy(DAppProxySample, [CallService.address], {deployer});
    } else {
        await deployer.deploy(DAppProxySample);
        const dAppProxySample = await DAppProxySample.deployed();
        await dAppProxySample.initialize(CallService.address);
    }
};

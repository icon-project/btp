var mockBmc = artifacts.require("MockBMC");
var bmv = artifacts.require("BMV");

const testData = require("../test/data");

module.exports =  async function(deployer) {
    // Test BMV
    await deployer.deploy(mockBmc, testData.praNet);
    await deployer.deploy(
        bmv,
        mockBmc.address,
        testData.iconNet,
        testData.encodedValidators,
        testData.initOffset,
        testData.lastBlockHash
    );
};
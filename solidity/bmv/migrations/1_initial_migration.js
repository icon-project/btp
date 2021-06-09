const mockBmc = artifacts.require("MockBMC");
const bmv = artifacts.require("BMV");
const subBmv = artifacts.require("DataValidator");
const { deployProxy } = require('@openzeppelin/truffle-upgrades');

const testData = require("../test/data");

module.exports =  async function(deployer) {
    // Test BMV
    await deployer.deploy(mockBmc, testData.praNet);
    await deployProxy(subBmv, [], { deployer });
    await deployProxy(
        bmv,
        [
            mockBmc.address,
            subBmv.address,
            testData.iconNet,
            testData.encodedValidators,
            testData.initOffset,
            testData.lastBlockHash
        ],
        { deployer }
    );
};
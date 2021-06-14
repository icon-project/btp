const BMV = artifacts.require("BMV");
const SUB_BMV = artifacts.require("DataValidator");
const { deployProxy } = require('@openzeppelin/truffle-upgrades');

module.exports = async function (deployer, network) {
  if (network !== "development") {
    await deployProxy(SUB_BMV, { deployer });
    await deployProxy(
      BMV,
      [
        process.env.BMC_CONTRACT_ADDRESS,
        SUB_BMV.address,
        process.env.BMV_ICON_NET,
        process.env.BMV_ICON_ENCODED_VALIDATORS,
        parseInt(process.env.BMV_ICON_INIT_OFFSET),
        process.env.BMV_ICON_LASTBLOCK_HASH,
      ],
      { deployer }
    );
  } else {
    const testData = require("../test/data");
    const BMC = artifacts.require("MockBMC");
    await deployer.deploy(BMC, testData.praNet);
    await deployProxy(SUB_BMV, [], { deployer });
    await deployProxy(
      BMV,
      [
        BMC.address,
        SUB_BMV.address,
        testData.iconNet,
        testData.encodedValidators,
        testData.initOffset,
        testData.lastBlockHash
      ],
      { deployer }
    );
  }
};
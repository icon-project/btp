
const BSHImpl = artifacts.require("BSHImpl");
const BSHProxy = artifacts.require("BSHProxy");
const { deployProxy } = require('@openzeppelin/truffle-upgrades');
const Web3 = require('web3');

const TruffleConfig = require('../truffle-config');
module.exports = async function (deployer, network) {
  if (network !== "development") {
    await deployProxy(BSHProxy, [parseInt(process.env.BSH_TOKEN_FEE)], { deployer });
    await deployProxy(BSHImpl, [process.env.BMC_PERIPHERY_ADDRESS, BSHProxy.address, process.env.BSH_SERVICE], { deployer });
    const bshProxy = await BSHProxy.deployed();
    await bshProxy.updateBSHImplementation(BSHImpl.address);
  }
};

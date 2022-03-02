const BSHPeriphery = artifacts.require("BSHPeriphery");
const BSHCore = artifacts.require("BSHCore");
const { deployProxy } = require('@openzeppelin/truffle-upgrades');

module.exports = async function (deployer, network) {
  if (network !== "development" && network !== "dev") {
    let INITIAL_SUPPLY = web3.utils.toBN(web3.utils.toWei(""+process.env.BSH_INITIAL_SUPPLY,"ether"))
    await deployProxy(BSHCore, [ process.env.BSH_COIN_NAME, parseInt(process.env.BSH_COIN_FEE), parseInt(process.env.BSH_FIXED_FEE), process.env.BSH_TOKEN_NAME, process.env.BSH_TOKEN_SYMBOL, INITIAL_SUPPLY], { deployer });
    await deployProxy(BSHPeriphery, [process.env.BMC_PERIPHERY_ADDRESS, BSHCore.address, process.env.BSH_SERVICE], { deployer });
    const bshCore = await BSHCore.deployed();
    await bshCore.updateBSHPeriphery(BSHPeriphery.address);
  }
};

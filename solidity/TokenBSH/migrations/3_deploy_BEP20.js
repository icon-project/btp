
const BEP20TKN = artifacts.require("BEP20TKN");

const TruffleConfig = require('../truffle-config');
module.exports = async function (deployer, network) {
  if (network !== "development") {
    await deployer.deploy(BEP20TKN);
  }
};
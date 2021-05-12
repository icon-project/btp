const BMC = artifacts.require("MockBMC");
const MockBMV = artifacts.require("MockBMV");

var btp_network = '1234.pra';

module.exports = async function (deployer) {
  await deployer.deploy(MockBMV);
  await deployer.deploy(BMC, btp_network);
};
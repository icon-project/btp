const MockBSHPeriphery= artifacts.require("MockBSHPeriphery");
const BSHPeriphery = artifacts.require("BSHPeriphery");
const BSHCore = artifacts.require("BSHCore");
const BMC = artifacts.require("MockBMC");
const Holder = artifacts.require("Holder");
const AnotherHolder = artifacts.require("AnotherHolder");
const NotPayable = artifacts.require("NotPayable");
const NonRefundable = artifacts.require("NonRefundable");
const Refundable = artifacts.require("Refundable");

var btp_network = '1234.pra';

module.exports = async function (deployer) {
  await deployer.deploy(Holder);
  await deployer.deploy(AnotherHolder);
  await deployer.deploy(NotPayable);
  await deployer.deploy(NonRefundable);
  await deployer.deploy(Refundable);
  await deployer.deploy(BMC, btp_network);
  await deployer.deploy(BSHPeriphery);
  await deployer.deploy(BSHCore);
  await deployer.deploy(MockBSHPeriphery);
};
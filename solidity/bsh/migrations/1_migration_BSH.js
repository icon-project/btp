// const BSH = artifacts.require("NativeCoinBSH");
const BSH = artifacts.require("MockBSH");
const BMC = artifacts.require("MockBMC");
const Holder = artifacts.require("Holder");
const NotPayable = artifacts.require("NotPayable");
const NonRefundable = artifacts.require("NonRefundable");
const Refundable = artifacts.require("Refundable");

var btp_network = '1234.pra';
var service_name = 'Coin/WrappedCoin';
var native_coin_name = 'PARA';
var symbol = 'PRA';
var decimal = 0;
var fee = 10000;

module.exports = async function (deployer) {
  await deployer.deploy(Holder);
  await deployer.deploy(NotPayable);
  await deployer.deploy(NonRefundable);
  await deployer.deploy(Refundable);
  var bmc = await deployer.deploy(BMC, btp_network);
  await deployer.deploy(
    BSH,
    bmc.address,
    service_name,
    native_coin_name,
    symbol,
    decimal,
    fee
  );
};
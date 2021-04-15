const Mock = artifacts.require("Mock");
const Holder = artifacts.require("Holder");
const NotPayable = artifacts.require("NotPayable");

var btp_network = 'btp://1234.pra';
var service_name = 'Coin/WrappedCoin';
var native_coin_name = 'PARA';
var symbol = 'PRA';
var decimal = 0;

module.exports = function (deployer) {
  deployer.deploy(Holder);
  deployer.deploy(NotPayable);    
  deployer.deploy(Mock, 
    btp_network,
    service_name,
    native_coin_name,
    symbol,
    decimal
  );
};
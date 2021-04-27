const Mock = artifacts.require("Mock");
const BEP20Mock = artifacts.require("BEP20Mock");
const Holder = artifacts.require("Holder");

var service_name = 'TokenBSH';
var token_name = 'CAKE';
var symbol = 'CAKE';
var decimals = 0;
module.exports = function (deployer) {
  deployer.deploy(Mock,
    service_name);
  deployer.deploy(BEP20Mock,
    service_name, token_name, symbol, decimals);
  deployer.deploy(Holder);
};

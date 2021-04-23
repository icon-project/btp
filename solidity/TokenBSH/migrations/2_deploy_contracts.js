const Mock = artifacts.require("Mock");
const Token = artifacts.require("Token");

var service_name = 'TokenBSH';
var token_name = 'CAKE';
var symbol = 'CAKE';
module.exports = function (deployer) {
  deployer.deploy(Mock,
    service_name);
  deployer.deploy(Token,
    token_name, symbol);
};

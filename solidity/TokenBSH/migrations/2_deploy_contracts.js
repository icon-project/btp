const Mock = artifacts.require("Mock");
const BMC = artifacts.require("BMC");
//const BEP20Mock = artifacts.require("BEP20Mock"); 
const PrecompilesMock = artifacts.require("PrecompilesMock");
const ERC20TKN = artifacts.require("ERC20TKN");
const BEP20TKN = artifacts.require("BEP20TKN");

var btp_network = 'btp://bsc';
var service_name = 'TokenBSH';
var token_name = 'CAKE';
var symbol = 'CAKE';
var decimals = 0;
module.exports = async function (deployer) {
  await deployer.deploy(BMC, btp_network);
  let bmcInstance = await BMC.deployed()
  console.log("BMC address:" + bmcInstance.address)
  await deployer.deploy(Mock, bmcInstance.address,
    service_name, btp_network);
  /*await deployer.deploy(BEP20Mock, bmcInstance.address,
    service_name, token_name, symbol, decimals);*/
  await deployer.deploy(ERC20TKN);
  await deployer.deploy(BEP20TKN);

  await deployer.deploy(PrecompilesMock);
};

const BMCMock = artifacts.require("BMCMock"); 
const BSH = artifacts.require("TokenBSH");
const PrecompilesMock = artifacts.require("PrecompilesMock");
const ERC20TKN = artifacts.require("ERC20TKN");
const BEP20TKN = artifacts.require("BEP20TKN");

var btp_network = 'btp://bsc';
var service_name = 'TokenBSH';
var token_name = 'CAKE';
var symbol = 'CAKE';
var decimals = 0;
module.exports = async function (deployer) {
  await deployer.deploy(BMCMock, btp_network);
  let bmcInstance = await BMCMock.deployed()
  await deployer.deploy(BSH, bmcInstance.address,
    service_name);
  await deployer.deploy(ERC20TKN);
  await deployer.deploy(BEP20TKN);
  await deployer.deploy(PrecompilesMock);
};

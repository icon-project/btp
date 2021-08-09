

const BSHProxy = artifacts.require("BSHProxy");
const BSHImpl = artifacts.require("BSHImpl");
const BSHProxyUpdate = artifacts.require("BSHProxyUpdate");
const BSHImplUpdate = artifacts.require("BSHImplUpdate");
const BMC = artifacts.require("BMCMock");
const ERC20TKN = artifacts.require("ERC20TKN");
const truffleAssert = require('truffle-assertions');
const { deployProxy, upgradeProxy } = require('@openzeppelin/truffle-upgrades');

contract('BSC BSH Proxy Contract Management tests', (accounts) => {
    var btp_network = 'btp://bsc';
    var _svc = 'TokenBSH';
    var _net = 'bsc';
    var tokenName = 'ETH'
    var symbol = 'ETH'
    var fees = 1
    var decimals = 18
    var transferAmount = 100;
    var _bmcBSC = 'btp://bsc/0x1234567812345678';
    let bshProxy, bshImpl, token;
    before(async () => {
        accounts = await web3.eth.getAccounts();
        bmc = await BMC.new(btp_network);
        token = await ERC20TKN.new();

        bshProxy = await deployProxy(BSHProxy, [fees]);
        bshImpl = await deployProxy(BSHImpl, [bmc.address, bshProxy.address, _svc]);
        await bshProxy.updateBSHImplementation(bshImpl.address);
        bshProxyV2 = await upgradeProxy(bshProxy.address, BSHProxyUpdate);
        bshImplV2 = await upgradeProxy(bshImpl.address, BSHImplUpdate);

        await bmc.setBSH(bshImpl.address);
        await bmc.addService(_svc, bshImpl.address);
        await bmc.addVerifier(_net, accounts[1]);
        await bmc.addLink(_bmcBSC);
    });

    it('Scenario 1: Re-initialize bshProxyV2 - Revert', async () => {
        await truffleAssert.reverts(
            bshProxyV2.initialize(fees),
            "Initializable: contract is already initialized"
        );
    });

    it('Scenario 2:  Re-initialize bshImplV2 - Revert', async () => {
        await truffleAssert.reverts(
            bshImplV2.initialize(bmc.address, bshProxyV2.address, _svc),
            "Initializable: contract is already initialized"
        );
    });

});

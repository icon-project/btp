

const BSHProxy = artifacts.require("BSHProxy");
const BSHImpl = artifacts.require("BSHImpl");
const BSHProxyUpdate = artifacts.require("BSHProxyUpdate");
const BSHImplUpdate = artifacts.require("BSHImplUpdate");
const BMC = artifacts.require("BMCMock");
const ERC20TKN = artifacts.require("ERC20TKN");
const truffleAssert = require('truffle-assertions');
const { deployProxy, upgradeProxy } = require('@openzeppelin/truffle-upgrades');

contract('BSC BSH Proxy Contract Management tests', (accounts) => {
    var btp_network = '0x97.bsc';
    var _svc = 'TokenBSH';
    var _net = '0x03.icon';
    var tokenName = 'ETH'
    var symbol = 'ETH'
    var fees = 1
    var decimals = 18
    var transferAmount = 100;
    var _bmcICON = 'btp://0x03.icon/0x1234567812345678';
    let bshProxy, bshImpl, token, bshProxyV2, bshImplV2;
    before(async () => {
        accounts = await web3.eth.getAccounts();
        bmc = await BMC.new(btp_network);
        token = await ERC20TKN.new();

        bshProxy = await deployProxy(BSHProxy, [fees]);
        bshImpl = await deployProxy(BSHImpl, [bmc.address, bshProxy.address, _svc]);
        await bshProxy.updateBSHImplementation(bshImpl.address);
        await bmc.setBSH(bshImpl.address);
        await bmc.addService(_svc, bshImpl.address);
        await bmc.addVerifier(_net, accounts[1]);
        await bmc.addLink(_bmcICON);
    });

    it(`Scenario 1: Register Token - by Owner - Success`, async () => {
        await bshProxy.register(tokenName, symbol, decimals, fees, token.address);
        var tokeNames = await bshProxy.tokenNames();
        assert.equal(tokeNames.length, 1, "The size of the token names should be 1");
    });

    it('Scenario 2: Register Token - not an owner - Revert', async () => {
        await truffleAssert.reverts(
            bshProxy.register(tokenName, symbol, decimals, fees, token.address, { from: accounts[1] }),
            "Unauthorized"
        );
    });

    it('Scenario 3: Upgrade Contracts - success', async () => {
        bshProxyV2 = await upgradeProxy(bshProxy.address, BSHProxyUpdate);
        bshImplV2 = await upgradeProxy(bshImpl.address, BSHImplUpdate);

    });

    it('Scenario 4: Re-initialize bshProxyV2 - Revert', async () => {
        await truffleAssert.reverts(
            bshProxyV2.initialize(fees),
            "Initializable: contract is already initialized"
        );
    });

    it('Scenario 5:  Re-initialize bshImplV2 - Revert', async () => {
        await truffleAssert.reverts(
            bshImplV2.initialize(bmc.address, bshProxyV2.address, _svc),
            "Initializable: contract is already initialized"
        );
    });

    it(`Scenario 1:state should remain- Register new Token - Upgraded Contract -  by Owner - Success`, async () => {
        await bshProxyV2.register("CAKE", symbol, decimals, fees, token.address);
        var tokeNames = await bshProxyV2.tokenNames();
        assert.equal(tokeNames.length, 2, "The size of the token names should be 2 after upgrade");
    });

});

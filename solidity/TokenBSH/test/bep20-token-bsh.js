const BEP20Mock = artifacts.require("BEP20Mock");
const Holder = artifacts.require("Holder");
const truffleAssert = require('truffle-assertions');
const BMC = artifacts.require("BMC");

var _svc = 'TokenBSH';
var _net = 'bsc';
var tokenName = 'CAKE'


contract('Receiving BEP20 from ICON blockchain', function () {
    let mock, accounts, token;
    beforeEach(async () => {
        token = await Holder.deployed();        
        bmc = await BMC.deployed();        
        mock = await BEP20Mock.deployed();
        accounts = await web3.eth.getAccounts();
        web3.eth.defaultAccount = accounts[0];
    });

    it("Scenario 1: Receiving address is an invalid address - fail", async () => {
        var _from = '0x12345678';
        var _value = 5
        var _to = '0x1234567890123456789';
        console.log("Mock Address"+mock.address)
        await bmc.addService(_svc, mock.address);
        await bmc.addVerifier(_net, accounts[1]);
        var transfer = await mock.handleRequestWithStringAddress(
            _net, _svc, _from, _to, tokenName, _value
        );
    });


    it("Scenario 2: All requirements are qualified - Success", async () => {
        var _from = '0x12345678';
        var _value = 5
        await mock.register(tokenName, token.address);
        var balanceBefore = await mock.balanceOf(token.address);
        var transfer = await mock.handleRequest(
            _net, _svc, _from, token.address, tokenName, _value
        );
        var balanceAfter = await mock.balanceOf(token.address);
        assert(
            web3.utils.hexToNumber(balanceAfter) ==
            web3.utils.hexToNumber(balanceBefore) + 5, "Invalid balance after handle request"
        );
    });
});

contract('Sending BEP20 to ICON blockchain', function () {
    let mock, accounts, token;
    beforeEach(async () => {
        mock = await BEP20Mock.deployed();
        token = await Holder.deployed();
        accounts = await web3.eth.getAccounts()
        bmc = await BMC.deployed();
        web3.eth.defaultAccount = accounts[0];
    });


    it("Scenario 1: User creates a transfer, but a token_name has not yet registered - fail", async () => {
        var _to = '0x1234567890123456789';
        var balance = 20;

        await token.addBSHContract(mock.address);
        await token.setApprove(mock.address, 10);
        await truffleAssert.reverts(
            token.callTransfer(tokenName, 5, _to),
            "Token is not registered"
        );
    });

    it("Scenario 2: User has an account with insufficient balance - fail", async () => {
        var _to = '0x1234567890123456789';
        var balance = 10;
        await mock.setBalance(token.address, balance);
        await mock.register(tokenName, token.address);
        await truffleAssert.reverts(
            token.callTransfer(tokenName, 15, _to),
            "transfer amount exceeds balance"
        );
    });

    it("Scenario 3: User transfers to an invalid BTP address - fail", async () => {
        var _to = 'btp://bsc:0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        await truffleAssert.fails(
            token.callTransfer(tokenName, 5, _to),           
        );
    });

    it("Scenario 4: User requests to transfer an invalid amount - fail", async () => {
        var _to = 'btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        await truffleAssert.reverts(
            token.callTransfer(tokenName, 0, _to),
            "Invalid amount specified"
        );
    });

    it("Scenario 5: All requirements are qualified and BSH initiates Transfer start - Success", async () => {
        var _to = 'btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        var balanceBefore = await mock.getBalanceOf(token.address, tokenName)       
        await bmc.addService(_svc, mock.address);
        await bmc.addVerifier(_net, accounts[1]);
        await token.callTransfer(tokenName, 5, _to)
        var balanceafter = await mock.getBalanceOf(token.address, tokenName)
        assert(
            web3.utils.hexToNumber(balanceafter[1]) ==
            web3.utils.hexToNumber(balanceBefore[1]) + 5, "Error response Handler failed "
        );
    });

    it("Scenario 6:All requirements are qualified and BSH receives a failed message - Success", async () => {
        var _code = 1;
        var _msg = 'Transfer failed'
        var _to = 'btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        var balanceBefore = await mock.getBalanceOf(token.address, tokenName)
        await mock.handleResponse(_net, _svc, 0, _code, _msg)
        var balanceAfter = await mock.getBalanceOf(token.address, tokenName)
        //Since the balance is returned back to the token Holder due to failure
        assert(
            web3.utils.hexToNumber(balanceAfter[1]) + 5 ==
            web3.utils.hexToNumber(balanceBefore[1]), "Error response Handler failed "
        );
    });

    it("Scenario 7:All requirements are qualified and BSH receives a successful message - Success", async () => {
        var _to = 'btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        await token.callTransfer(tokenName, 5, _to);
        var balanceBefore = await mock.getBalanceOf(token.address, tokenName)
        await mock.handleResponse(_net, _svc, 0, 0, "Transfer Success")
        var balanceAfter = await mock.getBalanceOf(token.address, tokenName)
        //Reason: the amount is burned from the tokenBSH and locked balance is reduced for the set amount
        assert(
            web3.utils.hexToNumber(balanceAfter[1]) + 5 ==
            web3.utils.hexToNumber(balanceBefore[1]), "Error response Handler failed "
        );
    });


});
 
contract('BEP20 - Complete flow tests', function () {
    let mock, accounts, token;
    beforeEach(async () => {
        mock = await BEP20Mock.deployed();
        token = await Holder.deployed();
        accounts = await web3.eth.getAccounts()
        web3.eth.defaultAccount = accounts[0];
        bmc = await BMC.deployed();
    });

    it("should register BEP20 Token", async () => {
        var _to = 'btp://bsc/0x12345678';
        var tokeNames = await mock.tokenNames();
        assert.equal(tokeNames.length, 0, "The size of the token names should be 0");
        await mock.setBalance(token.address, 999999999999999);
        let services=await bmc.getServices();
        console.log(services)
        let verifiers=await bmc.getVerifiers();
        console.log(verifiers)
        await bmc.addService(_svc, mock.address);
        await bmc.addVerifier(_net, accounts[1]);
        await mock.register("CAKE", token.address);

        var tokeNames = await mock.tokenNames();
        assert.equal(tokeNames.length, 1, "The size of the token names should be 1");

        var balanceBefore = await mock.balanceOf(token.address);
        await token.addBSHContract(mock.address);
        await token.setApprove(mock.address, 99999999999999);
        await token.callTransfer("CAKE", 10, _to)
        var balanceAfter = await mock.balanceOf(token.address);
        assert(
            web3.utils.hexToNumber(balanceAfter) == web3.utils.hexToNumber(balanceBefore) - 10, "Wrong balance after transfer"
        );
    });

});
 

contract('BEP20 - Basic BSH unit tests', function () {
    let mock, accounts, token;
    beforeEach(async () => {
        mock = await BEP20Mock.deployed();
        token = await Holder.deployed();
        accounts = await web3.eth.getAccounts()
        bmc = await BMC.deployed();
    });

    it("1. Register Coin - With Permission - Success", async () => {
        var output = await mock.tokenNames();
        await mock.register(tokenName, token.address);
        output = await mock.tokenNames();
        assert(
            output[0] === tokenName, "Invalid token name after registration"
        );
    });

    it('2. Register Coin - Without Permission - Failure', async () => {
        var _name = "ICON";
        var _symbol = "ICX";
        var _decimal = 0;

        await truffleAssert.reverts(
            mock.register(tokenName, token.address, { from: accounts[1] }),
            "No permission"
        );
    });

    it('3. Register Coin - Token already exists - Failure', async () => {
        //await mock.register(tokenName,token.address)  
        await truffleAssert.reverts(
            mock.register(tokenName, token.address),
            "Token with same name exists already"
        );
    });

    /*  it("Scenario 2: Receiving contract is not BEP20 - fail", async () => {
     var _from = '0x12345678';
     var _value = 5
     var _to = '0x1234567890123456789'; 
     await mock.register(token, token.address);
     var transfer = await mock.transferRequestWithStringAddress(
         _net, _svc, _from, _to, token, _value
     );
 });
*/
});

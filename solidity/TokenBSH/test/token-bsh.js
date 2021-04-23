const Mock = artifacts.require("Mock");
const Token = artifacts.require("Token");
const truffleAssert = require('truffle-assertions');

var _svc = 'tokenBSH';
var _net = 'bsc';
var tokenName = 'CAKE'



contract('Receiving ERC20 from ICON blockchain', function () {
    let mock, accounts, token;
    beforeEach(async () => {
        mock = await Mock.deployed();
        token = await Token.deployed();
        accounts = await web3.eth.getAccounts()
    });

    it("Scenario 1: Receiving address is an invalid address - fail", async () => {
        var _from = '0x12345678';
        var _value = 5
        var _to = '0x1234567890123456789';
        var transfer = await mock.handleRequestWithStringAddress(
            _net, _svc, _from, _to, tokenName, _value
        );
    });

 
    it("Scenario 3: All requirements are qualified - Success", async () => {
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
            web3.utils.hexToNumber(balanceBefore) + 5
        );
    });
});

contract('Sending ERC20 to ICON blockchain', function () {
    let mock, accounts, token;
    beforeEach(async () => {
        mock = await Mock.deployed();
        token = await Token.deployed();
        accounts = await web3.eth.getAccounts()
    });


    it("Scenario 1: User creates a transfer, but a token_name has not yet registered - fail", async () => {
        var _to = '0x1234567890123456789';
        var balance = 20;

        await token.addBSHContract(mock.address);
        await token.setApprove(mock.address, 10);
        await truffleAssert.reverts(
            token.callTransfer(tokenName, 5, _to),
            "VM Exception while processing transaction: revert Token is not registered -- Reason given: Token is not registered."
        );
    });

    it("Scenario 2: User has an account with insufficient balance - fail", async () => {
        var _to = '0x1234567890123456789';
        var balance = 10;
        await mock.setBalance(token.address, balance);
        await mock.register(tokenName, token.address);
        await truffleAssert.reverts(
            token.callTransfer(tokenName, 15, _to),
            "VM Exception while processing transaction: revert ERC20: transfer amount exceeds balance -- Reason given: ERC20: transfer amount exceeds balance."
        );
    });

    it("Scenario 3: User transfers to an invalid BTP address - fail", async () => {
        var _to = 'btp://bsc:0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        await truffleAssert.reverts(
            token.callTransfer(tokenName, 5, _to),
            "VM Exception while processing transaction: revert"
        );
    });

    it("Scenario 4: User requests to transfer an invalid amount - fail", async () => {
        var _to = 'btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        await truffleAssert.reverts(
            token.callTransfer(tokenName, 0, _to),
            "VM Exception while processing transaction: revert Invalid amount specified. -- Reason given: Invalid amount specified."
        );
    });

    it("Scenario 5: All requirements are qualified and BSH initiates Transfer start - Success", async () => {
        var _to = 'btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        var balanceBefore = await mock.getBalanceOf(token.address, tokenName)        
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


contract('Complete flow tests', function () {
    let mock, accounts, token;
    beforeEach(async () => {
        mock = await Mock.deployed();
        token = await Token.deployed();
        accounts = await web3.eth.getAccounts()
    });

    it("should register ERC20 Token", async () => {
        var _to = 'btp://bsc/0x12345678';
        var tokeNames = await mock.tokenNames();
        assert.equal(tokeNames.length, 0, "The size of the token names should be 0");
        await mock.setBalance(token.address, 999999999999999);

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


contract('Basic BSH unit tests', function () {
    let mock, accounts, token;
    beforeEach(async () => {
        mock = await Mock.deployed();
        token = await Token.deployed();
        accounts = await web3.eth.getAccounts()
    });

    it("Register Coin - With Permission - Success", async () => {
        var output = await mock.tokenNames(); 
        await mock.register(tokenName,token.address);
        output = await mock.tokenNames();        
        assert(
            output[0] === tokenName , "Invalid token name after registration"
        );
    });

    it('Register Coin - Without Permission - Failure', async () => {
        var _name = "ICON";
        var _symbol = "ICX";
        var _decimal = 0;
        
        await truffleAssert.reverts(
            mock.register(tokenName,token.address, {from: accounts[1]}),
            "VM Exception while processing transaction: revert No permission -- Reason given: No permission"
        );
    }); 

    it('Register Coin - Token already exists - Failure', async () => {      
        await truffleAssert.reverts(
            mock.register(tokenName,token.address),
            "VM Exception while processing transaction: revert Token with same name exists already. -- Reason given: Token with same name exists already.."
        );
    }); 

       /*  it("Scenario 2: Receiving contract is not ERC20 - fail", async () => {
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

const Mock = artifacts.require("Mock");
const Holder = artifacts.require("Holder");
const BMC = artifacts.require("BMC");
const ERC20TKN = artifacts.require("ERC20TKN");
const truffleAssert = require('truffle-assertions');

var _svc = 'TokenBSH';
var _net = 'bsc';
var tokenName = 'ETH'



contract('Receiving ERC20 from ICON blockchain', function () {
    let mock, accounts, token;
    beforeEach(async () => {
        mock = await Mock.deployed();
        token = await ERC20TKN.deployed();
        bmc = await BMC.deployed();
        accounts = await web3.eth.getAccounts()
    });

    it("Scenario 1: Receiving address is an invalid address - fail", async () => {
        var _from = '0x12345678';
        var _value = 5
        var _to = '0x1234567890123456789';
        console.log(mock.address)
        await bmc.addService(_svc, mock.address);
        await bmc.addVerifier(_net, accounts[1]);
        var transfer = await mock.handleRequestWithStringAddress(
            _net, _svc, _from, _to, tokenName, _value
        );
    });
    //todo it('Receive Request Token Mint - Invalid Token Name - Failure', async () => {

    it("Scenario 2: All requirements are qualified - Success", async () => {
        var _from = '0x12345678';
        var _value = 5
        await mock.register(tokenName, token.address);
        var balanceBefore = await mock.getBalanceOf(accounts[0], tokenName);
        var transfer = await mock.handleRequest(
            _net, _svc, _from, accounts[0], tokenName, _value
        );
        var balanceAfter = await mock.getBalanceOf(accounts[0], tokenName);
        // let bshBal=await token.balanceOf(accounts[0]);
        // console.log("Balance of"+accounts[0]+" after the transfer:"+bshBal);
        assert(
            web3.utils.hexToNumber(balanceAfter._usableBalance) ==
            web3.utils.hexToNumber(balanceBefore._usableBalance) + 5, "Locked balance after is not greater than sent amount" + balanceAfter._usableBalance
        );
    });
});


contract('Sending ERC20 to ICON blockchain', function () {
    let mock, accounts, token;
    beforeEach(async () => {
        mock = await Mock.deployed();
        console.log("Mock Address:" + mock.address)
        token = await ERC20TKN.deployed();
        console.log("Token Address:" + token.address)
        accounts = await web3.eth.getAccounts()
        console.log("Admin:" + accounts[0])
    });


    it("Scenario 1: User creates a transfer, but a token_name has not yet registered - fail", async () => {
        var _to = '0x1234567890123456789';
        var balance = 20;

        await truffleAssert.reverts(
            mock.transfer(tokenName, 5, _to),
            "VM Exception while processing transaction: revert Token is not registered -- Reason given: Token is not registered."
        );
    });

    it("Scenario 2: User has not approved the transfer - fail", async () => {
        var _to = 'btp://iconee/0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        await mock.register(tokenName, token.address);
        //await token.approve(mock.address,15); 
        await truffleAssert.reverts(
            mock.transfer(tokenName, 15, _to),
            "transfer amount exceeds allowance"
        );
    });

    it("Scenario 3: User transfers to an invalid BTP address - fail", async () => {
        var _to = 'btp://bsc:0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        await truffleAssert.reverts(
            mock.transfer(tokenName, 5, _to),
            "VM Exception while processing transaction: revert"
        );
    });

    it("Scenario 4: User requests to transfer an invalid amount - fail", async () => {
        var _to = 'btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        await truffleAssert.reverts(
            mock.transfer(tokenName, 0, _to),
            "VM Exception while processing transaction: revert Invalid amount specified. -- Reason given: Invalid amount specified."
        );
    });

    it("Scenario 5: All requirements are qualified and BSH initiates Transfer start - Success", async () => {
        var _to = 'btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        var balanceBefore = await mock.getBalanceOf(accounts[0], tokenName)
        //console.log(web3.utils.hexToNumber(balanceBefore._lockedBalance))
        await bmc.addService(_svc, mock.address);
        await bmc.addVerifier(_net, accounts[1]);
        await token.approve(mock.address, 5);
        await mock.transfer(tokenName, 5, _to)
        var balanceafter = await mock.getBalanceOf(accounts[0], tokenName)
        let bshBal = await token.balanceOf(mock.address);
        console.log("Balance of" + mock.address + " after the transfer:" + bshBal);
        //console.log( web3.utils.hexToNumber(balanceafter._lockedBalance))
        assert(
            web3.utils.hexToNumber(balanceafter._lockedBalance) ==
            web3.utils.hexToNumber(balanceBefore._lockedBalance) + 5, "Initiate transfer failed"
        );
    });

    it("Scenario 6:All requirements are qualified and BSH receives a failed message - Success", async () => {
        var _code = 1;
        var _msg = 'Transfer failed'
        var _to = 'btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        var balanceBefore = await mock.getBalanceOf(accounts[0], tokenName)
        await mock.handleResponse(_net, _svc, 0, _code, _msg)
        var balanceAfter = await mock.getBalanceOf(accounts[0], tokenName)
        //Since the balance is returned back to the token Holder due to failure
        assert(
            web3.utils.hexToNumber(balanceAfter[1]) + 5 ==
            web3.utils.hexToNumber(balanceBefore[1]), "Error response Handler failed "
        );
    });

    it("Scenario 7:All requirements are qualified and BSH receives a successful message - Success", async () => {
        var _code = 0;
        var _to = 'btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        await token.approve(mock.address, 5);
        await mock.transfer(tokenName, 5, _to)
        var balanceBefore = await mock.getBalanceOf(accounts[0], tokenName)
        await mock.handleResponse(_net, _svc, 0, _code, "Transfer Success")
        var balanceAfter = await mock.getBalanceOf(accounts[0], tokenName)
        //Reason: the amount is burned from the tokenBSH and locked balance is reduced for the set amount
        assert(
            web3.utils.hexToNumber(balanceAfter[1]) + 5 ==
            web3.utils.hexToNumber(balanceBefore[1]), "Error response Handler failed "
        );
    });
});


contract('ERC20 - Complete flow tests', function () {
    let mock, accounts, token;
    beforeEach(async () => {
        mock = await Mock.deployed();
        token = await ERC20TKN.deployed();
        accounts = await web3.eth.getAccounts()
    });

    it("should register & transfer ERC20 Token", async () => {
        var _to = 'btp://bsc/0x12345678';
        var tokeNames = await mock.tokenNames();
        assert.equal(tokeNames.length, 0, "The size of the token names should be 0");
        await bmc.addService(_svc, mock.address);
        await bmc.addVerifier(_net, accounts[1]);
        await mock.register(tokenName, token.address);

        var tokeNames = await mock.tokenNames();
        assert.equal(tokeNames.length, 1, "The size of the token names should be 1");

        var balanceBefore = await mock.getBalanceOf(accounts[0], tokenName)
        //await token.addBSHContract(mock.address); 
        await token.approve(mock.address, 10);
        await mock.transfer(tokenName, 10, _to)
        var balanceAfter = await mock.getBalanceOf(accounts[0], tokenName)
        console.log(web3.utils.hexToNumber(balanceAfter._lockedBalance));
        console.log(web3.utils.hexToNumber(balanceBefore._lockedBalance));
        assert(
            web3.utils.hexToNumber(balanceAfter._lockedBalance) - 10 == web3.utils.hexToNumber(balanceBefore._lockedBalance), "Wrong balance after transfer"
        );
    });

});


contract('ERC20 - Basic BSH unit tests', function () {
    let mock, accounts, token;
    beforeEach(async () => {
        mock = await Mock.deployed();
        token = await ERC20TKN.deployed();
        accounts = await web3.eth.getAccounts()
    });

    it("Register Coin - With Permission - Success", async () => {
        var output = await mock.tokenNames();
        await mock.register(tokenName, token.address);
        output = await mock.tokenNames();
        assert(
            output[0] === tokenName, "Invalid token name after registration"
        );
    });

    it('Register Coin - Without Permission - Failure', async () => {
        var _name = "ICON";
        var _symbol = "ICX";
        var _decimal = 0;

        await truffleAssert.reverts(
            mock.register(tokenName, token.address, { from: accounts[1] }),
            "VM Exception while processing transaction: revert No permission -- Reason given: No permission"
        );
    });

    it('Register Coin - Token already exists - Failure', async () => {
        await truffleAssert.reverts(
            mock.register(tokenName, token.address),
            "VM Exception while processing transaction: revert Token with same name exists already. -- Reason given: Token with same name exists already.."
        );
    });

    //TODO: new testcases 1. add different owner and perform transfer
    // 2. withdraw test case


});

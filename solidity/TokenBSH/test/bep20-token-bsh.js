const BEP20Mock = artifacts.require("Mock");
const Holder = artifacts.require("Holder");
const truffleAssert = require('truffle-assertions');
const BMC = artifacts.require("BMC");
const BEP20TKN = artifacts.require("BEP20TKN");
var _svc = 'TokenBSH';
var _net = 'bsc';
var tokenName = 'ETH'

var symbol = 'ETH'
var fees = 1
var decimals = 18
var transferAmount = 100;


contract('Receiving BEP20 from ICON blockchain', function () {
    let mock, accounts, token;
    beforeEach(async () => {
        token = await BEP20TKN.deployed();
        bmc = await BMC.deployed();
        mock = await BEP20Mock.deployed();
        accounts = await web3.eth.getAccounts();
        web3.eth.defaultAccount = accounts[0];
    });

    it("Scenario 1: Receiving address is an invalid address - fail", async () => {
        var _from = '0x12345678';
        var _value = 5
        var _to = '0x1234567890123456789';
        console.log("Mock Address" + mock.address)
        await bmc.addService(_svc, mock.address);
        await bmc.addVerifier(_net, accounts[1]);
        await mock.handleRequestWithStringAddress(
            _net, _svc, _from, _to, tokenName, _value
        );
        //check the event logs for the invalid address error
    });


    it("Scenario 2: All requirements are qualified - Success", async () => {
        var _from = '0x12345678';
        await mock.register(tokenName, symbol, decimals, fees, token.address);
        var balanceBefore = await mock.getBalanceOf(accounts[0], tokenName);
        await mock.handleRequest(
            _net, _svc, _from, accounts[0], tokenName, transferAmount
        );
        var balanceAfter = await mock.getBalanceOf(accounts[0], tokenName);
        var amountAndFee = await mock.calculateTransferFee(token.address, transferAmount);
        assert(
            web3.utils.hexToNumber(balanceAfter._usableBalance) ==
            web3.utils.hexToNumber(balanceBefore._usableBalance) + (transferAmount - amountAndFee.fee), "Locked balance after is not greater than sent amount" + balanceAfter._usableBalance
        );
    });
});

contract('Sending BEP20 to ICON blockchain', function () {
    let mock, accounts, token;
    beforeEach(async () => {
        mock = await BEP20Mock.deployed();
        token = await BEP20TKN.deployed();
        accounts = await web3.eth.getAccounts()
        bmc = await BMC.deployed();
        web3.eth.defaultAccount = accounts[0];
    });


    it("Scenario 1: User creates a transfer, but a token_name has not yet registered - fail", async () => {
        var _to = '0x1234567890123456789';
        await truffleAssert.reverts(
            mock.transfer(tokenName, transferAmount, _to),
            "Token is not registered"
        );
    });

    it("Scenario 2: User has an account with insufficient balance - fail", async () => {
        var _to = '0x1234567890123456789';
        await mock.register(tokenName, symbol, decimals, fees, token.address);
        await truffleAssert.reverts(
            mock.transfer(tokenName, transferAmount + 50, _to),
            "transfer amount exceeds allowance"
        );
    });

    it("Scenario 3: User transfers to an invalid BTP address - fail", async () => {
        var _to = 'btp://bsc:0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        await truffleAssert.fails(
            mock.transfer(tokenName, transferAmount, _to)
        );
    });

    it("Scenario 4: User requests to transfer an invalid amount - fail", async () => {
        var _to = 'btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        await truffleAssert.reverts(
            mock.transfer(tokenName, 0, _to),
            "Invalid amount specified"
        );
    });

    it("Scenario 5: All requirements are qualified and BSH initiates Transfer start - Success", async () => {
        var _to = 'btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        var balanceBefore = await mock.getBalanceOf(accounts[0], tokenName)
        await bmc.addService(_svc, mock.address);
        await bmc.addVerifier(_net, accounts[1]);
        await token.approve(mock.address, transferAmount);
        await mock.transfer(tokenName, transferAmount, _to)
        var balanceafter = await mock.getBalanceOf(accounts[0], tokenName)
        let bshBal = await token.balanceOf(mock.address);
        var amountAndFee = await mock.calculateTransferFee(token.address, transferAmount);
        console.log("Balance of" + mock.address + " after the transfer:" + bshBal);
        assert(
            web3.utils.hexToNumber(balanceafter._lockedBalance) ==
            web3.utils.hexToNumber(balanceBefore._lockedBalance) + (transferAmount - amountAndFee.fee), "Initiate transfer failed"
        );
    });

    it("Scenario 6:All requirements are qualified and BSH receives a failed message - Success", async () => {
        var _code = 1;
        var _msg = 'Transfer failed'
        var _to = 'btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        var balanceBefore = await mock.getBalanceOf(accounts[0], tokenName)
        await mock.handleResponse(_net, _svc, 0, _code, _msg)
        var balanceAfter = await mock.getBalanceOf(accounts[0], tokenName)

        var amountAndFee = await mock.calculateTransferFee(token.address, transferAmount);
        //Since the balance is returned back to the token Holder due to failure
        assert(
            web3.utils.hexToNumber(balanceAfter[1]) + (transferAmount - amountAndFee.fee) ==
            web3.utils.hexToNumber(balanceBefore[1]), "Error response Handler failed "
        );
    });

    it("Scenario 7:All requirements are qualified and BSH receives a successful message - Success", async () => {
        var _to = 'btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        var _code = 0;
        await token.approve(mock.address, transferAmount);
        await mock.transfer(tokenName, transferAmount, _to)
        var balanceBefore = await mock.getBalanceOf(accounts[0], tokenName)
        await mock.handleResponse(_net, _svc, 0, _code, "Transfer Success")
        var balanceAfter = await mock.getBalanceOf(accounts[0], tokenName)
        var amountAndFee = await mock.calculateTransferFee(token.address, transferAmount);
        //Reason: the amount is burned from the tokenBSH and locked balance is reduced for the set amount
        assert(
            web3.utils.hexToNumber(balanceAfter[1]) + (transferAmount - amountAndFee.fee) ==
            web3.utils.hexToNumber(balanceBefore[1]), "Error response Handler failed "
        );
        var accumulatedFees = await mock.getAccumulatedFees();
        assert(accumulatedFees[0].value == web3.utils.hexToNumber(amountAndFee.fee), "The Accumulated fee is not equal to the calculated fees from transfer amount");
    });


});

contract('BEP20 - Complete flow tests', function () {
    let mock, accounts, token;
    beforeEach(async () => {
        mock = await BEP20Mock.deployed();
        token = await BEP20TKN.deployed();
        accounts = await web3.eth.getAccounts()
        web3.eth.defaultAccount = accounts[0];
        bmc = await BMC.deployed();
    });

    it("should register BEP20 Token", async () => {
        var _to = 'btp://bsc/0x12345678';
        var _code = 0;
        var tokeNames = await mock.tokenNames();
        assert.equal(tokeNames.length, 0, "The size of the token names should be 0");
        await bmc.addService(_svc, mock.address);
        await bmc.addVerifier(_net, accounts[1]);
        await mock.register(tokenName, symbol, decimals, fees, token.address);

        var tokeNames = await mock.tokenNames();
        assert.equal(tokeNames.length, 1, "The size of the token names should be 1");


        var balanceBefore = await mock.getBalanceOf(accounts[0], tokenName)
        console.log("Locked Balance Before Transfer" + web3.utils.hexToNumber(balanceBefore._lockedBalance));

        //await token.addBSHContract(mock.address); 
        await token.approve(mock.address, transferAmount);
        await mock.transfer(tokenName, transferAmount, _to)

        var balanceAfter = await mock.getBalanceOf(accounts[0], tokenName)
        var amountAndFee = await mock.calculateTransferFee(token.address, transferAmount);
        console.log("Locked Balance After Transfer:" + web3.utils.hexToNumber(balanceAfter._lockedBalance));
        //The locked balance before TRANSFER (0) should be equal to (Locked balance after - transfer amount - fee)(0)

        assert(
            (web3.utils.hexToNumber(balanceAfter._lockedBalance) - (transferAmount - amountAndFee.fee)) == web3.utils.hexToNumber(balanceBefore._lockedBalance), "Wrong balance after transfer"
        );
        balanceBefore = balanceAfter;
        await mock.handleResponse(_net, _svc, 0, _code, "Transfer Success")
        balanceAfter = await mock.getBalanceOf(accounts[0], tokenName)
        console.log("Locked Balance After Handle Response:" + web3.utils.hexToNumber(balanceAfter._lockedBalance));
        //Reason: the amount is burned from the tokenBSH and locked balance is reduced for the set amount
        // the locked balance after Transfer should be zero (previous balance- transfer amount)
        assert(
            web3.utils.hexToNumber(balanceAfter[1]) ==
            web3.utils.hexToNumber(balanceBefore[1]) - (transferAmount - amountAndFee.fee), "Success response Handler failed "
        );

        var accumulatedFees = await mock.getAccumulatedFees();
        assert(accumulatedFees[0].value == web3.utils.hexToNumber(amountAndFee.fee), "The Accumulated fee is not equal to the calculated fees from transfer amount");
        //execute handle gather fee
        await mock.handleGatherFee(_to);
    });

});


contract('BEP20 - Basic BSH unit tests', function () {
    let mock, accounts, token;
    beforeEach(async () => {
        mock = await BEP20Mock.deployed();
        token = await BEP20TKN.deployed();
        accounts = await web3.eth.getAccounts()
        bmc = await BMC.deployed();
    });

    it("1. Register Coin - With Permission - Success", async () => {
        var output = await mock.tokenNames();
        await mock.register(tokenName, symbol, decimals, fees, token.address);
        output = await mock.tokenNames();
        assert(
            output[0] === tokenName, "Invalid token name after registration"
        );
    });

    it('2. Register Coin - Without Permission - Failure', async () => {
        await truffleAssert.reverts(
            mock.register(tokenName, symbol, decimals, fees, token.address, { from: accounts[1] }),
            "No permission"
        );
    });

    it('3. Register Coin - Token already exists - Failure', async () => {
        //await mock.register(tokenName,token.address)  
        await truffleAssert.reverts(
            mock.register(tokenName, symbol, decimals, fees, token.address),
            "Token with same name exists already"
        );
    });


});


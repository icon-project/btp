const Mock = artifacts.require("Mock");
const Holder = artifacts.require("Holder");
const BMC = artifacts.require("BMC");
const ERC20TKN = artifacts.require("ERC20TKN");
const truffleAssert = require('truffle-assertions');

var _svc = 'TokenBSH';
var _net = 'bsc';
var tokenName = 'ETH'
var symbol = 'ETH'
var fees = 1
var decimals = 18
var transferAmount = 100;



contract('ERC20 - Complete flow tests', function () {
    let mock, accounts, token;
    beforeEach(async () => {
        mock = await Mock.deployed();
        token = await ERC20TKN.deployed();
        accounts = await web3.eth.getAccounts();
        bmc = await BMC.deployed();
    });

    it("should register & transfer ERC20 Token", async () => {
        var _to = 'btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        var _code = 0;

        //###-Pre-reqs
        var tokeNames = await mock.tokenNames();
        assert.equal(tokeNames.length, 0, "The size of the token names should be 0");
        await bmc.addService(_svc, mock.address);
        await bmc.addVerifier(_net, accounts[1]);

        //###-Register token
        await mock.register(tokenName, symbol, decimals, fees, token.address);
        var tokeNames = await mock.tokenNames();
        assert.equal(tokeNames.length, 1, "The size of the token names should be 1");
        var balanceBefore = await mock.getBalanceOf(accounts[0], tokenName)
        console.log("Locked Balance Before Transfer" + web3.utils.hexToNumber(balanceBefore._lockedBalance));

        //###-tranfer token from user to bsh
        await token.approve(mock.address, transferAmount);
        await mock.transfer(tokenName, transferAmount, _to)
        var balanceAfter = await mock.getBalanceOf(accounts[0], tokenName)
        var amountAndFee = await mock.calculateTransferFee(token.address, transferAmount);
        console.log("Locked Balance After Transfer:" + web3.utils.hexToNumber(balanceAfter._lockedBalance));
        //The locked balance before TRANSFER (0) should be equal to (Locked balance after - transfer amount - fee)(0)
        assert(
            (web3.utils.hexToNumber(balanceAfter._lockedBalance) - (transferAmount - amountAndFee.fee)) ==
            web3.utils.hexToNumber(balanceBefore._lockedBalance),
            "Wrong balance after transfer"
        );

        //###-mint token request
        balanceBefore = await mock.getBalanceOf(accounts[1], tokenName);
        await mock.handleRequest(
            _net, _svc, accounts[0], accounts[1], tokenName, transferAmount
        );
        balanceAfter = await mock.getBalanceOf(accounts[1], tokenName);
        assert(
            web3.utils.hexToNumber(balanceAfter._usableBalance) ==
            web3.utils.hexToNumber(balanceBefore._usableBalance) + (transferAmount - amountAndFee.fee),
            "Locked balance after is not greater than sent amount" + balanceAfter._usableBalance
        );

        //###- withdraw token test
        balanceBefore = await token.balanceOf(accounts[1]);
        console.log("Balance of account1 beofre withdraw" + balanceBefore);
        await mock.withdraw(tokenName, amountAndFee.value, { from: accounts[1] });
        balanceAfter = await token.balanceOf(accounts[1]);
        console.log("Balance of account1 after withdraw" + balanceAfter);
        assert(
            web3.utils.hexToNumber(balanceBefore) + amountAndFee.value ==
            web3.utils.hexToNumber(balanceAfter),
            "Balance of the receiver is not credited with withdrawn amount"
        );

        //###-handle transfer success response
        balanceBefore = await mock.getBalanceOf(accounts[0], tokenName);;
        await mock.handleResponse(_net, _svc, 0, _code, "Transfer Success")
        balanceAfter = await mock.getBalanceOf(accounts[0], tokenName)
        console.log("Locked Balance After Handle Response:" + web3.utils.hexToNumber(balanceAfter._lockedBalance));
        //Reason: the amount is burned from the tokenBSH and locked balance is reduced for the set amount
        // the locked balance after Transfer should be zero (previous balance- transfer amount)
        assert(
            web3.utils.hexToNumber(balanceAfter[1]) ==
            web3.utils.hexToNumber(balanceBefore[1]) - (transferAmount - amountAndFee.fee),
            "Success response Handler failed "
        );

        //###-handle accumulated fees
        var accumulatedFees = await mock.getAccumulatedFees();
        assert(
            accumulatedFees[0].value ==
            web3.utils.hexToNumber(amountAndFee.fee),
            "The Accumulated fee is not equal to the calculated fees from transfer amount"
        );

        //###-handle gather fee request
        await mock.handleGatherFee(_to);
    });

});


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
        var _to = '0x1234567890123456789';
        await bmc.addService(_svc, mock.address);
        await bmc.addVerifier(_net, accounts[1]);
        await mock.handleRequestWithStringAddress(
            _net, _svc, _from, _to, tokenName, transferAmount
        );
        //check the event logs for the invalid address error
    });

    //todo it('Receive Request Token Mint - Invalid Token Name - Failure', async () => {

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
            web3.utils.hexToNumber(balanceBefore._usableBalance) + (transferAmount - amountAndFee.fee),
            "Locked balance after is not greater than sent amount" + balanceAfter._usableBalance
        );
    });
});


contract('Sending ERC20 to ICON blockchain', function () {
    let mock, accounts, token;
    beforeEach(async () => {
        mock = await Mock.deployed();
        //console.log("Mock Address:" + mock.address)
        token = await ERC20TKN.deployed();
        //console.log("Token Address:" + token.address)
        accounts = await web3.eth.getAccounts()
        //console.log("Admin:" + accounts[0])
        bmc = await BMC.deployed();
    });


    it("Scenario 1: User creates a transfer, but a token_name has not yet registered - fail", async () => {
        var _to = '0x1234567890123456789';
        await truffleAssert.reverts(
            mock.transfer(tokenName, transferAmount, _to),
            "VM Exception while processing transaction: revert Token is not registered -- Reason given: Token is not registered."
        );
    });

    it("Scenario 2: User has not approved the transfer - fail", async () => {
        var _to = 'btp://iconee/0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        await mock.register(tokenName, symbol, decimals, fees, token.address);
        //await token.approve(mock.address,15);
        await truffleAssert.reverts(
            mock.transfer(tokenName, transferAmount + 50, _to),
            "transfer amount exceeds allowance"
        );
    });

    it("Scenario 3: User transfers to an invalid BTP address - fail", async () => {
        var _to = 'btp://bsc:0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        await truffleAssert.reverts(
            mock.transfer(tokenName, transferAmount, _to),
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
        await bmc.addService(_svc, mock.address);
        await bmc.addVerifier(_net, accounts[1]);
        await token.approve(mock.address, transferAmount);
        await mock.transfer(tokenName, transferAmount, _to)
        var balanceafter = await mock.getBalanceOf(accounts[0], tokenName)
        let bshBal = await token.balanceOf(mock.address);
        var amountAndFee = await mock.calculateTransferFee(token.address, transferAmount);
        console.log("Balance of" + mock.address + " after the transfer:" + bshBal);
        //console.log( web3.utils.hexToNumber(balanceafter._lockedBalance))
        assert(
            web3.utils.hexToNumber(balanceafter._lockedBalance) ==
            web3.utils.hexToNumber(balanceBefore._lockedBalance) + (transferAmount - amountAndFee.fee),
            "Initiate transfer failed"
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
        var _code = 0;
        var _to = 'btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        await token.approve(mock.address, transferAmount);
        await mock.transfer(tokenName, transferAmount, _to)
        var balanceBefore = await mock.getBalanceOf(accounts[0], tokenName)
        await mock.handleResponse(_net, _svc, 0, _code, "Transfer Success")
        var balanceAfter = await mock.getBalanceOf(accounts[0], tokenName)
        var amountAndFee = await mock.calculateTransferFee(token.address, transferAmount);
        //Reason: the amount is burned from the tokenBSH and locked balance is reduced for the set amount
        assert(
            web3.utils.hexToNumber(balanceAfter[1]) + (transferAmount - amountAndFee.fee) ==
            web3.utils.hexToNumber(balanceBefore[1]),
            "Error response Handler failed "
        );
        var accumulatedFees = await mock.getAccumulatedFees();
        assert(
            accumulatedFees[0].value ==
            web3.utils.hexToNumber(amountAndFee.fee),
            "The Accumulated fee is not equal to the calculated fees from transfer amount");
    });
});


contract('ERC20 - Basic BSH unit tests', function () {
    let mock, accounts, token;
    beforeEach(async () => {
        mock = await Mock.deployed();
        token = await ERC20TKN.deployed();
        accounts = await web3.eth.getAccounts()
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
        await truffleAssert.reverts(
            mock.register(tokenName, symbol, decimals, fees, token.address),
            "VM Exception while processing transaction: revert Token with same name exists already. -- Reason given: Token with same name exists already.."
        );
    });

    //TODO: new testcases 1. add different owner and perform transfer 

});


//Check the ACL of functions
//Check with multiple tokens & multiple owners
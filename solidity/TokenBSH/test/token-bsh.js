const BSH = artifacts.require("TokenBSH");
const BMC = artifacts.require("BMCMock");
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
    let bsh, accounts, token;
    beforeEach(async () => {
        bsh = await BSH.deployed();
        token = await ERC20TKN.deployed();
        accounts = await web3.eth.getAccounts();
        bmc = await BMC.deployed();
    });

    it("should register & transfer ERC20 Token", async () => {
        var _to = 'btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        var _code = 0;

        //###-Pre-reqs
        var tokeNames = await bsh.tokenNames();
        assert.equal(tokeNames.length, 0, "The size of the token names should be 0");
        await bmc.addService(_svc, bsh.address);
        await bmc.setBSH(bsh.address);
        await bmc.addVerifier(_net, accounts[1]);

        //###-Register token
        await bsh.register(tokenName, symbol, decimals, fees, token.address);
        var tokeNames = await bsh.tokenNames();
        assert.equal(tokeNames.length, 1, "The size of the token names should be 1");
        var balanceBefore = await bsh.getBalanceOf(accounts[0], tokenName)
        console.log("Locked Balance Before Transfer" + web3.utils.hexToNumber(balanceBefore._lockedBalance));

        //###-tranfer token from user to bsh
        await token.approve(bsh.address, transferAmount);
        await bsh.transfer(tokenName, transferAmount, _to)
        var balanceAfter = await bsh.getBalanceOf(accounts[0], tokenName)
        var amountAndFee = await bsh.calculateTransferFee(token.address, transferAmount);
        console.log("Locked Balance After Transfer:" + web3.utils.hexToNumber(balanceAfter._lockedBalance));
        //The locked balance before TRANSFER (0) should be equal to (Locked balance after - transfer amount - fee)(0)
        assert(
            (web3.utils.hexToNumber(balanceAfter._lockedBalance) - (transferAmount - amountAndFee.fee)) ==
            web3.utils.hexToNumber(balanceBefore._lockedBalance),
            "Wrong balance after transfer"
        );

        //###-mint token request
        //balanceBefore = await bsh.getBalanceOf(accounts[1], tokenName);
        balanceBefore = await token.balanceOf(accounts[1])
        await bmc.handleRequest(
            _net, _svc, accounts[0], accounts[1], tokenName, transferAmount - amountAndFee.fee
        );
        /* balanceAfter = await token.balanceOf(accounts[1])
         //balanceAfter = await bsh.getBalanceOf(accounts[1], tokenName);
         console.log("Balance Before" + balanceBefore);
         console.log("Balance After" + balanceAfter);
         console.log(amountAndFee);
         assert(
             web3.utils.hexToNumber(balanceAfter) ==
             web3.utils.hexToNumber(balanceBefore) + (transferAmount - amountAndFee.fee),
             "Locked balance after is not greater than sent amount" + balanceAfter._usableBalance
         );*/

        //###- withdraw token test
        /* balanceBefore = await token.balanceOf(accounts[1]);
         console.log("Balance of account1 beofre withdraw" + balanceBefore);
         await bsh.withdraw(tokenName, amountAndFee.value, { from: accounts[1] });
         balanceAfter = await token.balanceOf(accounts[1]);
         console.log("Balance of account1 after withdraw" + balanceAfter);
         assert(
             web3.utils.hexToNumber(balanceBefore) + amountAndFee.value ==
             web3.utils.hexToNumber(balanceAfter),
             "Balance of the receiver is not credited with withdrawn amount"
         );
             */
        //###-handle transfer success response
        /*  balanceBefore = await bsh.getBalanceOf(accounts[0], tokenName);;
          await bmc.handleResponse(_net, _svc, 0, _code, "Transfer Success")
          balanceAfter = await bsh.getBalanceOf(accounts[0], tokenName)
          console.log("Locked Balance After Handle Response:" + web3.utils.hexToNumber(balanceAfter._lockedBalance));
          //Reason: the amount is burned from the tokenBSH and locked balance is reduced for the set amount
          // the locked balance after Transfer should be zero (previous balance- transfer amount)
          assert(
              web3.utils.hexToNumber(balanceAfter[1]) ==
              web3.utils.hexToNumber(balanceBefore[1]) - (transferAmount - amountAndFee.fee),
              "Success response Handler failed "
          );
  
          //###-handle accumulated fees
          var accumulatedFees = await bsh.getAccumulatedFees();
          assert(
              accumulatedFees[0].value ==
              web3.utils.hexToNumber(amountAndFee.fee),
              "The Accumulated fee is not equal to the calculated fees from transfer amount"
          );
  
          //###-handle gather fee request
          await bmc.handleFeeGathering(_to, _svc);
          accumulatedFees = await bsh.getAccumulatedFees();
          console.log(accumulatedFees);
          assert.equal(accumulatedFees[0].value, 0, "The accumulated fees should be zero after handleGather fee");*/
    });

});

/*
contract('Receiving ERC20 from ICON blockchain', function () {
    let bsh, accounts, token;
    beforeEach(async () => {
        bsh = await BSH.deployed();
        token = await ERC20TKN.deployed();
        bmc = await BMC.deployed();
        accounts = await web3.eth.getAccounts()
    });

    it("Scenario 1: Receiving address is an invalid address - fail", async () => {
        var _from = '0x12345678';
        var _to = '0x1234567890123456789';
        await bmc.addService(_svc, bsh.address);
        await bmc.setBSH(bsh.address);
        await bmc.addVerifier(_net, accounts[1]);
        await bmc.handleRequestWithStringAddress(
            _net, _svc, _from, _to, tokenName, transferAmount
        );
        //check the event logs for the invalid address error
    });

    //todo it('Receive Request Token Mint - Invalid Token Name - Failure', async () => {

    it("Scenario 2: All requirements are qualified - Success", async () => {
        var _from = '0x12345678';
        //set initial bsh balance
        await token.transfer(bsh.address, transferAmount);
        await bsh.register(tokenName, symbol, decimals, fees, token.address);
        var amountAndFee = await bsh.calculateTransferFee(token.address, transferAmount);
        var balanceBefore = await token.balanceOf(accounts[1])
        var amount = transferAmount - amountAndFee.fee
        await bmc.handleRequest(
            _net, _svc, _from, accounts[1], tokenName, amount
        );
        var balanceAfter = await token.balanceOf(accounts[1])
        console.log("Balance Before" + balanceBefore);
        console.log("Balance After" + balanceAfter);
        console.log(amountAndFee);
        assert(
            web3.utils.hexToNumber(balanceAfter) ==
            web3.utils.hexToNumber(balanceBefore) + amount,
            "Locked balance after is not greater than sent amount"
        );
    });
});


contract('Sending ERC20 to ICON blockchain', function () {
    let bsh, accounts, token;
    beforeEach(async () => {
        bsh = await BSH.deployed();
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
            bsh.transfer(tokenName, transferAmount, _to),
            "VM Exception while processing transaction: revert Token is not registered -- Reason given: Token is not registered."
        );
    });

    it("Scenario 2: User has not approved the transfer - fail", async () => {
        var _to = 'btp://iconee/0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        await bsh.register(tokenName, symbol, decimals, fees, token.address);
        //await token.approve(mock.address,15);
        await truffleAssert.reverts(
            bsh.transfer(tokenName, transferAmount + 50, _to),
            "transfer amount exceeds allowance"
        );
    });

    it("Scenario 3: User transfers to an invalid BTP address - fail", async () => {
        var _to = 'btp://bsc:0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        await truffleAssert.reverts(
            bsh.transfer(tokenName, transferAmount, _to),
            "VM Exception while processing transaction: revert"
        );
    });

    it("Scenario 4: User requests to transfer an invalid amount - fail", async () => {
        var _to = 'btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        await truffleAssert.reverts(
            bsh.transfer(tokenName, 0, _to),
            "VM Exception while processing transaction: revert Invalid amount specified. -- Reason given: Invalid amount specified."
        );
    });

    it("Scenario 5: All requirements are qualified and BSH initiates Transfer start - Success", async () => {
        var _to = 'btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        var balanceBefore = await bsh.getBalanceOf(accounts[0], tokenName)
        await bmc.addService(_svc, bsh.address);
        await bmc.setBSH(bsh.address);
        await bmc.addVerifier(_net, accounts[1]);
        await token.approve(bsh.address, transferAmount);
        await bsh.transfer(tokenName, transferAmount, _to)
        var balanceafter = await bsh.getBalanceOf(accounts[0], tokenName)
        let bshBal = await token.balanceOf(bsh.address);
        var amountAndFee = await bsh.calculateTransferFee(token.address, transferAmount);
        console.log("Balance of" + bsh.address + " after the transfer:" + bshBal);
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
        var balanceBefore = await bsh.getBalanceOf(accounts[0], tokenName)
        await bmc.handleResponse(_net, _svc, 0, _code, _msg)
        var balanceAfter = await bsh.getBalanceOf(accounts[0], tokenName)
        var amountAndFee = await bsh.calculateTransferFee(token.address, transferAmount);
        //Since the balance is returned back to the token Holder due to failure
        assert(
            web3.utils.hexToNumber(balanceAfter[1]) + (transferAmount - amountAndFee.fee) ==
            web3.utils.hexToNumber(balanceBefore[1]), "Error response Handler failed "
        );
    });

    it("Scenario 7:All requirements are qualified and BSH receives a successful message - Success", async () => {
        var _code = 0;
        var _to = 'btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        await token.approve(bsh.address, transferAmount);
        await bsh.transfer(tokenName, transferAmount, _to)
        var balanceBefore = await bsh.getBalanceOf(accounts[0], tokenName)
        await bmc.handleResponse(_net, _svc, 1, _code, "Transfer Success")
        var balanceAfter = await bsh.getBalanceOf(accounts[0], tokenName)
        var amountAndFee = await bsh.calculateTransferFee(token.address, transferAmount);
        //Reason: the amount is burned from the tokenBSH and locked balance is reduced for the set amount
        assert(
            web3.utils.hexToNumber(balanceAfter[1]) + (transferAmount - amountAndFee.fee) ==
            web3.utils.hexToNumber(balanceBefore[1]),
            "Error response Handler failed "
        );
        var accumulatedFees = await bsh.getAccumulatedFees();
        assert(
            accumulatedFees[0].value ==
            web3.utils.hexToNumber(amountAndFee.fee),
            "The Accumulated fee is not equal to the calculated fees from transfer amount");
    });
});


contract('ERC20 - Basic BSH unit tests', function () {
    let bsh, accounts, token;
    beforeEach(async () => {
        bsh = await BSH.deployed();
        token = await ERC20TKN.deployed();
        accounts = await web3.eth.getAccounts()
    });

    it("1. Register Token - With Permission - Success", async () => {
        var output = await bsh.tokenNames();
        await bsh.register(tokenName, symbol, decimals, fees, token.address);
        output = await bsh.tokenNames();
        assert(
            output[0] === tokenName, "Invalid token name after registration"
        );
    });

    it('2. Register Token - Without Permission - Failure', async () => {
        await truffleAssert.reverts(
            bsh.register(tokenName, symbol, decimals, fees, token.address, { from: accounts[1] }),
            "No permission"
        );
    });

    it('3. Register Token - Token already exists - Failure', async () => {
        await truffleAssert.reverts(
            bsh.register(tokenName, symbol, decimals, fees, token.address),
            "VM Exception while processing transaction: revert Token with same name exists already. -- Reason given: Token with same name exists already.."
        );
    });

    //TODO: new testcases 1. add different owner and perform transfer

});*/

//Check the ACL of functions
//Check with multiple tokens & multiple owners
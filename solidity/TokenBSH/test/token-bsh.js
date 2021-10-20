

const BSHProxy = artifacts.require("BSHProxy");
const BSHImpl = artifacts.require("BSHImpl");
const BMC = artifacts.require("BMCMock");
const ERC20TKN = artifacts.require("ERC20TKN");
const truffleAssert = require('truffle-assertions');

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
    let bshProxy, bshImpl, token;
    before(async () => {
        accounts = await web3.eth.getAccounts();
        console.log(accounts[0]);
        console.log(web3.eth.getBalance(accounts[0]));
        bmc = await BMC.new(btp_network);
        token = await ERC20TKN.new();
        bshProxy = await BSHProxy.new();
        bshImpl = await BSHImpl.new();
        await bshProxy.initialize(fees);
        await bshImpl.initialize(bmc.address, bshProxy.address, _svc);
        await bmc.addService(_svc, bshImpl.address);
        await bmc.setBSH(bshImpl.address);
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

    it('Scenario 3: Register Token - Token already exists - Revert', async () => {
        await truffleAssert.reverts(
            bshProxy.register(tokenName, symbol, decimals, fees, token.address),
            "TokenExists"
        );
    });

    it('Scenario 4: update BSH Implementation - by owner - success ', async () => {
        await bshProxy.updateBSHImplementation(bshImpl.address);
    });

    it('Scenario 5: update BSH Implementation - Not an owner - Revert ', async () => {
        await truffleAssert.reverts(
            bshProxy.updateBSHImplementation(bshImpl.address, { from: accounts[1] }),
            "Unauthorized"
        );
    });

    it('Scenario 6: update BSH Implementation - by Owner + pending requests - Revert', async () => {
        var _to = 'btp://0x03.icon/0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        await token.approve(bshProxy.address, transferAmount);
        await bshProxy.transfer(tokenName, transferAmount, _to);
        await truffleAssert.reverts(
            bshProxy.updateBSHImplementation(accounts[2]),
            "HasPendingRequest"
        );
        //TODO: Clear pending request
        var REPONSE_HANDLE_SERVICE = 2;
        var RC_OK = 0;
        await bmc.response(REPONSE_HANDLE_SERVICE, _net, _svc, 1, RC_OK, "");
    });

    it('Scenario 7: update FeeRatio  - by owner - success', async () => {
        var new_fee = 200;
        await bshProxy.setFeeRatio(new_fee);
    });

    it('Scenario 8: update FeeRatio  - Not an owner - Revert', async () => {
        var new_fee = 2;
        await truffleAssert.reverts(
            bshProxy.setFeeRatio(new_fee, { from: accounts[1] }),
            "Unauthorized"
        );
    });

    it('Scenario 9: update FeeRatio - Fee Numerator is higher than Fee Denominator - Revert', async () => {
        var new_fee = 11000;
        await truffleAssert.reverts(
            bshProxy.setFeeRatio(new_fee),
            "InvalidSetting"
        );
    });

    it('Scenario 10: check if the token registered - valid token name - returns true - success', async () => {
        var result = await bshProxy.isTokenRegisterd(tokenName);
        assert(
            result === true
        );
    });

    it('Scenario 11: check if the token registered - in valid tokenname- returns false - success', async () => {
        var result = await bshProxy.isTokenRegisterd("CAKE");
        assert(
            result === false
        );
    });

    it('Scenario 12: Add new owner - by non-owner - revert', async () => {
        var existingOwners = await bshProxy.getOwners();
        await truffleAssert.reverts(
            bshProxy.addOwner(accounts[1], { from: accounts[2] }),
            "Unauthorized"
        );
        var isNewOwnerValid = await bshProxy.isOwner(accounts[1]);
        assert(
            existingOwners.length === 1 && isNewOwnerValid === false
        );
    });

    it('Scenario 13: Add new owner - by owner - success', async () => {
        await bshProxy.addOwner(accounts[1]);
        var newOwners = await bshProxy.getOwners();
        var isNewOwnerValid = await bshProxy.isOwner(accounts[1]);
        assert(
            newOwners.length === 2 && isNewOwnerValid === true
        );
    });


    it('Scenario 14: update BSH Implementation - by new Owner - Success', async () => {
        var _to = 'btp://0x03.icon/0xa36a32c114ee13090e35cb086459a690f5c1f8e8';
        var newBSHImpl = await BSHImpl.new();
        await bshProxy.updateBSHImplementation(newBSHImpl.address, { from: accounts[1] });
    });


    it('Scenario 15: remove owner - by non-owner - Revert', async () => {
        await truffleAssert.reverts(
            bshProxy.removeOwner(accounts[0], { from: accounts[2] }),
            "Unauthorized"
        );
        var newOwners = await bshProxy.getOwners();
        var isremovedOwnerValid = await bshProxy.isOwner(accounts[0]);
        assert(
            newOwners.length === 2 && isremovedOwnerValid === true
        );
    });

    it('Scenario 16: remove owner - by owner - Success', async () => {
        await bshProxy.removeOwner(accounts[0], { from: accounts[1] });
        var newOwners = await bshProxy.getOwners();
        var isremovedOwnerValid = await bshProxy.isOwner(accounts[0]);
        assert(
            newOwners.length === 1 && isremovedOwnerValid === false
        );
    });

    it('Scenario 17: remove owner - only one owner - by owner - Success', async () => {
        await truffleAssert.reverts(
            bshProxy.removeOwner(accounts[1], { from: accounts[1] }),
            "LastOwner"
        );
        var newOwners = await bshProxy.getOwners();
        var isremovedOwnerValid = await bshProxy.isOwner(accounts[1]);
        assert(
            newOwners.length === 1 && isremovedOwnerValid === true
        );
    });

    it('Scenario 18:  update FeeRatio  - By removed owner - Revert', async () => {
        var new_fee = 2;
        await truffleAssert.reverts(
            bshProxy.setFeeRatio(new_fee, { from: accounts[0] }),
            "Unauthorized"
        );
    });
});

contract('Sending ERC20 to ICON blockchain', function () {

    var btp_network = '0x97.bsc';
    var _svc = 'TokenBSH';
    var _net = '0x03.icon';
    var tokenName = 'ETH'
    var symbol = 'ETH'
    var fees = 100
    var decimals = 18
    var amount = web3.utils.toBN(10);
    var transferAmount = web3.utils.toWei(amount, "ether");
    var _bmcICON = 'btp://0x03.icon/0x1234567812345678';
    let bshProxy, bshImpl, token, bmcBtpAdd;
    var RC_ERR = 1;
    before(async () => {
        accounts = await web3.eth.getAccounts();
        bmc = await BMC.new(btp_network);
        token = await ERC20TKN.new();
        bshProxy = await BSHProxy.new();
        bshImpl = await BSHImpl.new();
        await bshProxy.initialize(fees);
        await bshImpl.initialize(bmc.address, bshProxy.address, _svc);
        await bshProxy.updateBSHImplementation(bshImpl.address);
        await bmc.setBSH(bshImpl.address);
        await bmc.addService(_svc, bshImpl.address);
        await bmc.addVerifier(_net, accounts[1]);
        await bmc.addLink(_bmcICON);
        bmcBtpAdd = await bmc.getBmcBtpAddress();
    });

    it("Scenario 1: User creates a transfer, but a token_name has not yet registered - fail", async () => {
        var _to = '0x1234567890123456789';
        await truffleAssert.reverts(
            bshProxy.transfer(tokenName, transferAmount, _to),
            "UnRegisteredToken"
        );
    });

    it("Scenario 2: User has not approved the transfer - fail", async () => {
        var _to = 'btp://0x03.icon/hxb6b5791be0b5ef67063b3c10b840fb81514db2fd';
        await bshProxy.register(tokenName, symbol, decimals, fees, token.address);
        //await token.approve(mock.address,15);
        await truffleAssert.reverts(
            bshProxy.transfer(tokenName, transferAmount + 50, _to),
            "transfer amount exceeds allowance"
        );
    });

    it("Scenario 3: User transfers to an invalid BTP address - fail", async () => {
        var _to = 'btp://0x03.icon:hxb6b5791be0b5ef67063b3c10b840fb81514db2fd';
        await truffleAssert.reverts(
            bshProxy.transfer(tokenName, transferAmount, _to),
            "revert"
        );
    });

    it("Scenario 4: User requests to transfer an invalid amount - fail", async () => {
        var _to = 'btp://0x03.icon/hxb6b5791be0b5ef67063b3c10b840fb81514db2fd';
        await truffleAssert.reverts(
            bshProxy.transfer(tokenName, 0, _to),
            "InvalidAmount"
        );
    });

    it("Scenario 5: All requirements are qualified and BSH initiates Transfer start - Success", async () => {
        var _to = 'btp://0x03.icon/hxb6b5791be0b5ef67063b3c10b840fb81514db2fd';
        let bshBalBefore = await token.balanceOf(bshProxy.address);
        var balanceBefore = await bshProxy.getBalanceOf(accounts[0], tokenName)
        await token.approve(bshProxy.address, transferAmount);
        var txn = await bshProxy.transfer(tokenName, transferAmount, _to)
        //console.log(txn);
        var balanceafter = await bshProxy.getBalanceOf(accounts[0], tokenName)
        let bshBal = await token.balanceOf(bshProxy.address);
        var amountAndFee = await bshProxy.calculateTransferFee(token.address, transferAmount);
        // console.log("Balance of" + bshProxy.address + " before the transfer:" + bshBalBefore);
        // console.log(""+amountAndFee.value);
        // console.log(""+amountAndFee.fee);
        // console.log("Balance of" + bshProxy.address + " after the transfer:" + bshBal);
        // console.log( web3.utils.fromWei(balanceafter._lockedBalance,"ether")) 
        // console.log(balanceafter._lockedBalance.toString()) 
        // console.log(balanceBefore._lockedBalance.add(transferAmount).sub(amountAndFee.fee).toString()) 
        assert(
            balanceafter._lockedBalance.toString() ==
            balanceBefore._lockedBalance.add(transferAmount).sub(amountAndFee.fee).toString(),
            "Initiate transfer failed"
        );
    });

    it("Scenario 6: All requirements are qualified and BSH receives RESPONSE_HANDLE_SERVICE a failed message - Success", async () => {
        var _code = 1;
        var _msg = 'Transfer failed'
        var _to = 'btp://0x03.icon/hxb6b5791be0b5ef67063b3c10b840fb81514db2fd';
        var balanceBefore = await bshProxy.getBalanceOf(accounts[0], tokenName)
        await bmc.handleResponse(_net, _svc, 1, _code, _msg)
        var balanceAfter = await bshProxy.getBalanceOf(accounts[0], tokenName)
        var amountAndFee = await bshProxy.calculateTransferFee(token.address, transferAmount);
        //console.log(balanceAfter)
        // @dev check the balance is returned back to the token Holder due to failure
        // locked balance for this particular amount is released
        assert(
            balanceAfter._lockedBalance.add(transferAmount).sub(amountAndFee.fee).toString() ==
            balanceBefore._lockedBalance.toString(), "Error response Handler failed "
        );

        //Check if the amount is updated in refundable balance(value+accumulated fees) for the user to withdraw later
        assert(
            balanceAfter._refundableBalance.toString() ==
            balanceBefore._refundableBalance.add(transferAmount)
        )
    });

    it("Scenario 7: Withdraw the refundable balance - invalid Token Name", async () => {
        await truffleAssert.reverts(
            bshProxy.withdraw("ICX", transferAmount),
            "UnRegisteredToken"
        );
    });

    it("Scenario 8: Withdraw the refundable balance - Zero amount", async () => {
        await truffleAssert.reverts(
            bshProxy.withdraw(tokenName, 0),
            "InvalidAmount"
        );
    });

    it("Scenario 9: Withdraw the refundable balance - Amount exceeds the refundable balance", async () => {
        var excessAmount = transferAmount + 10
        await truffleAssert.reverts(
            bshProxy.withdraw(tokenName, excessAmount),
            "InsufficientBalance"
        );
    });

    it("Scenario 10: Withdraw the refundable balance - Success", async () => {
        var balanceBefore = await token.balanceOf(accounts[0]);
        await bshProxy.withdraw(tokenName, transferAmount)
        var balanceAfter = await token.balanceOf(accounts[0]);
        assert(
            balanceBefore.add(transferAmount).toString() ==
            balanceAfter.toString(), "Token Balance after withdraw did not get credited with transfer amount"
        );
    });


    it("Scenario 11: All requirements are qualified and BSH receives a RESPONSE_UNKNOWN Message - Success", async () => {
        var _code = 1;
        await bmc.handleUnknownBTPResponse(_net, _svc, 3, _code, "UNKNOWN_TYPE")
        const events = await bshImpl.getPastEvents('ResponseUnknownType');
        assert(
            events[0].args._from ==
            _net, "Invalid Event or No event emitted"
        );
    });

    it("Scenario 12: All requirements are qualified and BSH receives a invalid service Message - Success", async () => {
        var _code = 1;
        var mockOutputToAssert = await bmc.buildBTPInvalidRespMessage(bmcBtpAdd, _bmcICON, _svc, 0, RC_ERR, "UNKNOWN_TYPE")
        let output = await bmc.handleInvalidBTPResponse(_net, _svc, 0, _code, "INVALID_TYPE")
        //TODO: check why there is a difference in the mockoutput & actual output
        /*console.log(mockOutputToAssert)
        console.log(output.logs[0].args._msg)         
        assert(
            output.logs[0].args._msg.toString() == mockOutputToAssert.toString(),
            "The invalid service message not emitted"
        ); */
    });

    it("Scenario 13: All requirements are qualified and BSH receives RESPONSE_HANDLE_SERVICE with a successful message - Success", async () => {
        var _code = 0;
        var _to = 'btp://0x03.icon/hxb6b5791be0b5ef67063b3c10b840fb81514db2fd';
        await token.approve(bshProxy.address, transferAmount);
        await bshProxy.transfer(tokenName, transferAmount, _to)
        var balanceBefore = await bshProxy.getBalanceOf(accounts[0], tokenName)
        await bmc.handleResponse(_net, _svc, 2, _code, "Transfer Success")
        var balanceAfter = await bshProxy.getBalanceOf(accounts[0], tokenName)
        var amountAndFee = await bshProxy.calculateTransferFee(token.address, transferAmount);
        //Reason: the amount is burned from the tokenBSH and locked balance is reduced for the set amount
        assert(
            balanceAfter[1].add(transferAmount).sub(amountAndFee.fee).toString() ==
            balanceBefore[1].toString(),
            "Error response Handler failed "
        );
        var accumulatedFees = await bshProxy.getAccumulatedFees();
        assert(
            accumulatedFees[0].value.toString() ==
            amountAndFee.fee.toString(),
            "The Accumulated fee is not equal to the calculated fees from transfer amount");
    });
});

contract('Receiving ERC20 from ICON blockchain', function () {
    var btp_network = '0x97.bsc';
    var _svc = 'TokenBSH';
    var _net = '0x03.icon';
    var tokenName = 'ETH'
    var symbol = 'ETH'
    var fees = 100
    var decimals = 18
    var transferAmount = 100;
    var _bmcICON = 'btp://0x03.icon/0x1234567812345678';
    let bshProxy, bshImpl, token, bmcBtpAdd;
    var RC_ERR = 1;
    before(async () => {
        accounts = await web3.eth.getAccounts();
        bmc = await BMC.new(btp_network);
        token = await ERC20TKN.new();
        bshProxy = await BSHProxy.new();
        bshImpl = await BSHImpl.new();
        await bshProxy.initialize(fees);
        await bshImpl.initialize(bmc.address, bshProxy.address, _svc);
        await bshProxy.updateBSHImplementation(bshImpl.address);
        await bmc.setBSH(bshImpl.address);
        await bmc.addService(_svc, bshImpl.address);
        await bmc.addVerifier(_net, accounts[1]);
        await bmc.addLink(_bmcICON);
        bmcBtpAdd = await bmc.getBmcBtpAddress();
        //await bshProxy.register(tokenName, symbol, decimals, fees, token.address);
    });

    it("Scenario 1: Receiving address is an invalid address - fail", async () => {
        var _from = '0x12345678';
        var _to = '0x1234567812345678';
        var mockOutputToAssert = await bmc.buildBTPRespMessage(bmcBtpAdd, _bmcICON, _svc, 0, RC_ERR, "Invalid Address")
        var output = await bmc.handleTransferReqStrAddr(
            _from, _to, _net, _svc, tokenName, transferAmount
        );
        //check the event logs for the invalid address error
        //TODO: change the log index to 0 after removing the debug emit event from the contracts
        assert(
            output.logs[0].args._msg == mockOutputToAssert
        );

    });

    it('Scenario 2: Receive Request Token Mint - Invalid Token Name - Failure', async () => {
        var _from = '0x12345678';
        var mockOutputToAssert = await bmc.buildBTPRespMessage(bmcBtpAdd, _bmcICON, _svc, 0, RC_ERR, 'Unregistered Token');

        var output = await bmc.handleTransferReq(
            _from, accounts[1], _net, _svc, tokenName, transferAmount
        );
        //TODO: change the log index to 0 after removing the debug emit event from the contracts
        assert(
            output.logs[0].args._msg === mockOutputToAssert
        );
    });

    it('Scenario 3: Receive Request Token Mint - Insufficient funds with BSH - Failure', async () => {
        var _from = '0x12345678';
        var _value = "10000000000000000000"
        await bshProxy.register(tokenName, symbol, decimals, fees, token.address);
        var mockOutputToAssert = await bmc.buildBTPRespMessage(bmcBtpAdd, _bmcICON, _svc, 0, RC_ERR, 'ERC20: transfer amount exceeds balance');
        var output = await bmc.handleTransferReq(
            _from, accounts[1], _net, _svc, tokenName, _value
        );
        //TODO: change the log index to 0 after removing the debug emit event from the contracts
        assert(
            output.logs[0].args._msg === mockOutputToAssert
        );
    });

    it("Scenario 4: All requirements are qualified - Success", async () => {
        var _from = '0x12345678';
        //set initial bsh balance
        transferAmount = "10000000000000000000"
        var amountAndFee = await bshProxy.calculateTransferFee(token.address, transferAmount);
        var balanceBefore = await token.balanceOf(accounts[1]);
        var amount = transferAmount - amountAndFee.fee;
        await token.transfer(bshProxy.address, transferAmount);
        var bshBalance = await token.balanceOf(bshProxy.address);

        await bmc.handleTransferReq(
            _from, accounts[1], _net, _svc, tokenName, "" + amount
        );

        var balanceAfter = await token.balanceOf(accounts[1])
        // console.log("Balance Before" + balanceBefore.toString());
        // console.log("Balance After" + balanceAfter.toString());
        // console.log("Balance After in eth" + await web3.utils.fromWei(balanceAfter, "ether"));
        assert(
            balanceAfter.toString() ==
            balanceBefore.add(web3.utils.toBN(amount)).toString(),
            "Locked balance after is not greater than sent amount"
        );
    });
});

//TODO: fee aggregation tests
// type code comments
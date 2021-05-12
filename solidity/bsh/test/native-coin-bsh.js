// const BSH = artifacts.require("NativeCoinBSH");
const BSH = artifacts.require("MockBSH");
const BMC = artifacts.require("MockBMC");
const Holder = artifacts.require("Holder");
const NotPayable = artifacts.require("NotPayable");
const NonRefundable = artifacts.require("NonRefundable");
const Refundable = artifacts.require("Refundable");
const { assert } = require('chai');
const truffleAssert = require('truffle-assertions');

contract('BSH Basic Unit Tests', () => {
    let bsh, bmc, accounts;
    var native_coin_name = 'PARA';
    var _name = "ICON";
    var _symbol = "ICX";
    var _decimal = 0;
    var _fee = 10000;
    before(async () => {
        bsh = await BSH.deployed();
        bmc = await BMC.deployed();
        accounts = await web3.eth.getAccounts()
    });

    it('Register Coin - With Permission - Success', async () => {
        var output = await bsh.coinNames();
        await bsh.register(_name, _symbol, _decimal, _fee);
        output = await bsh.coinNames();
        assert(
            output[0] === native_coin_name && output[1] === 'ICON'
        );
    });
    
    it('Register Coin - Without Permission - Failure', async () => {   
        await truffleAssert.reverts(
            bsh.register(_name, _symbol, _decimal, _fee, {from: accounts[1]}),
            "No permission"
        );
    }); 

    it('Register Coin - Token existed - Failure', async () => {
        await truffleAssert.reverts(
            bsh.register(_name, _symbol, _decimal, _fee),
            "Token existed"
        );
    }); 

    it('Coin Query - Valid Supported Coin - Success', async () => {
        var _name1 = "wBTC";    var _name2 = "Ethereum";
        var _symbol1 = "wBTC";   var _symbol2 = "ETH";
        var _decimals1 = 0;     var _decimals2 = 0;
        var _fee1 = 1;          var _fee2 = 1;
        await bsh.register(_name1, _symbol1, _decimals1, _fee1);
        await bsh.register(_name2, _symbol2, _decimals2, _fee2);

        var _query = "ICON";
        var id = await bmc.hashCoinName(_query);
        var result = await bsh.coinOf(_query);
        assert(
            result.id === web3.utils.BN(id).toString() && result.symbol === 'ICX'
        );
    }); 

    it('Coin Query - Not Supported Coin - Success', async () => {
        var _query = "EOS";
        var result = await bsh.coinOf(_query);
        assert(
            result.id == 0 && result.symbol === '' && result.decimals == 0 && result.feeNumerator == 0
        );
    }); 
});

contract('BSH Transfer Coin Unit Tests', () => {
    let bsh, bmc, nonrefundable, refundable, accounts;
    var service = 'Coin/WrappedCoin';               var _svc = 'Token';
    var _net = '1234.iconee';                       var _to = 'btp://1234.iconee/0x12345678';
    var RC_OK = 0;                                  var RC_ERR = 1;    
    var _amt1 = 1000;                               var _amt2 = 999999999999;                       
    var _amt3 = 10000;                              var deposit = 100000;
    var _native = 'PARA';                           var REPONSE_HANDLE_SERVICE = 2;

    before(async () => {
        bsh = await BSH.deployed();
        bmc = await BMC.deployed();
        nonrefundable = await NonRefundable.deployed();
        refundable = await Refundable.deployed();
        accounts = await web3.eth.getAccounts();
        await bmc.setBSH(bsh.address);
        await bmc.approveService(service);
        await bmc.addVerifier(_net, accounts[1]);
    });

    it('Transfer Native Coin to side chain - From Account - Success', async () => {
        var balanceBefore = await bsh.getBalanceOf(accounts[0], _native);
        var output = await bsh.transfer(_to, {from: accounts[0], value: _amt1});
        var balanceAfter = await bsh.getBalanceOf(accounts[0], _native);
        // truffleAssert.prettyPrintEmittedEvents(output);
        var chargedFee = Math.floor(_amt1 * 1 / 100);
        truffleAssert.eventEmitted(output, 'TransferStart', (ev) => {
            return ev._from === accounts[0] && ev._to === _to && ev._sn == 0 &&
                ev._assetDetails.length == 1 &&
                ev._assetDetails[0].coinName === 'PARA' && 
                ev._assetDetails[0].value == _amt1 - chargedFee &&
                ev._assetDetails[0].fee == chargedFee
        });
        assert(
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() == 0 && 
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() == _amt1
        );
    });

    it('BSH Handle BTP Success Response', async () => {
        var balanceBefore = await bsh.getBalanceOf(accounts[0], _native);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net, service, 0, RC_OK, "");
        var balanceAfter = await bsh.getBalanceOf(accounts[0], _native);

        assert(
            web3.utils.BN(balanceBefore._lockedBalance) == _amt1 && 
            web3.utils.BN(balanceAfter._lockedBalance) == 0
        );
    });

    it('Transfer Native Coin to side chain - From Account - Success', async () => {
        var balanceBefore = await bsh.getBalanceOf(accounts[0], _native);
        var output = await bsh.transfer(_to, {from: accounts[0], value: _amt2});
        var balanceAfter = await bsh.getBalanceOf(accounts[0], _native);
        var chargedFee = Math.floor(_amt2 / 100);
        truffleAssert.eventEmitted(output, 'TransferStart', (ev) => {
            return ev._from === accounts[0] && ev._to === _to && ev._sn == 1 &&
                ev._assetDetails.length == 1 &&
                ev._assetDetails[0].coinName === 'PARA' && 
                ev._assetDetails[0].value == _amt2 - chargedFee &&
                ev._assetDetails[0].fee == chargedFee
        });

        assert(
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() == 0 && 
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() == _amt2
        );
    });

    it('BSH Handle BTP Error Response - Refundable account', async () => {
        var balanceBefore = await bsh.getBalanceOf(accounts[0], _native);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net, service, 1, RC_ERR, "");
        var balanceAfter = await bsh.getBalanceOf(accounts[0], _native);

        assert(
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() == _amt2 && 
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() == 0 &&
            web3.utils.BN(balanceAfter._refundableBalance).toNumber() == 0
        );
    });

    it('Transfer Native Coin to side chain - From Non-Refundable Contract - Success', async () => {
        await nonrefundable.deposit({from: accounts[2], value: deposit});
        var balanceBefore = await bsh.getBalanceOf(nonrefundable.address, _native);
        await nonrefundable.transfer(bsh.address, _to, _amt3);
        var balanceAfter = await bsh.getBalanceOf(nonrefundable.address, _native);

        assert(
            web3.utils.BN(balanceBefore._usableBalance).toNumber() == 
                web3.utils.BN(balanceAfter._usableBalance).toNumber() + _amt3 &&
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() == 0 && 
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() == _amt3
        );
    });

    it('BSH Handle BTP Error Response - Non-Refundable Contract', async () => {
        var balanceBefore = await bsh.getBalanceOf(nonrefundable.address, _native);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net, service, 2, RC_ERR, "");
        var balanceAfter = await bsh.getBalanceOf(nonrefundable.address, _native);

        assert(
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() == _amt3 && 
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() == 0 &&
            web3.utils.BN(balanceAfter._usableBalance).toNumber() == web3.utils.BN(balanceBefore._usableBalance).toNumber() &&
            web3.utils.BN(balanceAfter._refundableBalance).toNumber() == _amt3
        );
    });

    it('Transfer Native Coin to side chain - From Refundable Contract - Success', async () => {
        await refundable.deposit({from: accounts[2], value: deposit});
        var balanceBefore = await bsh.getBalanceOf(refundable.address, _native);
        await refundable.transfer(bsh.address, _to, _amt3);
        var balanceAfter = await bsh.getBalanceOf(refundable.address, _native);

        assert(
            web3.utils.BN(balanceBefore._usableBalance).toNumber() == 
                web3.utils.BN(balanceAfter._usableBalance).toNumber() + _amt3 &&
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() == 0 && 
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() == _amt3
        );
    });

    it('BSH Handle BTP Error Response - Refundable Contract', async () => {
        var balanceBefore = await bsh.getBalanceOf(refundable.address, _native);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net, service, 3, RC_ERR, "");
        var balanceAfter = await bsh.getBalanceOf(refundable.address, _native);

        assert(
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() == _amt3 && 
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() == 0 &&
            web3.utils.BN(balanceAfter._usableBalance).toNumber() == 
                web3.utils.BN(balanceBefore._usableBalance).toNumber() + _amt3 &&
            web3.utils.BN(balanceAfter._refundableBalance).toNumber() == 0
        );
    });

    it('Transfer Native Coin to side chain - Invalid BTP Address format - Failure', async () => {
        var invalid_destination = '1234.iconee/0x12345678';
        var err = 'Error: Returned error: VM Exception while processing transaction: invalid opcode';
        var rsp = '';
        try {
            await bsh.transfer(invalid_destination, {from: accounts[0], value: 1000});
        }catch (error) {
            rsp = error.toString();
        }
        // await truffleAssert.fails(
        //     bsh.transfer(_to, {from: accounts[0], value: 1000}),
        //     // "invalid opcode"
        //     truffleAssert.ErrorType.INVALID_OPCODE
        // ); 
        assert(rsp == err);
    });

    it('Transfer Native Coin to side chain - Amount is Zero - Failure' , async () => {
        await truffleAssert.reverts(
            bsh.transfer(_to, {from: accounts[0], value: 0}),
            "Invalid amount"
        ); 
    });

    it('Transfer Native Coin to side chain - Charge Fee is Zero - Failure' , async () => {
        await truffleAssert.reverts(
            bsh.transfer(_to, {from: accounts[0], value: 10}),
            "Invalid amount"
        ); 
    });

    it('Transfer Native Coin to side chain - Invalid Network/Network Address Not Supported - Failure' , async () => {
        var invalid_destination = 'btp://1234.eos/0x12345678';
        await truffleAssert.reverts(
            bsh.transfer(invalid_destination, {from: accounts[1], value: 1000000}),
            "BMCRevertNotExistsBMV"
        ); 
    });
});

contract('BSH Transfer Token Unit Tests', () => {
    let bsh, bmc, holder, accounts;
    var service = 'Coin/WrappedCoin';
    var _net = '1234.iconee';
    var _svc = 'Token';
    var _symbol = 'ICX';                                var _name = 'ICON';              
    var _decimals = 0;                                  var _fee = 10000;
    var _from = '0x12345678';
    var _value = 999999999999999;                       
    var REPONSE_HANDLE_SERVICE = 2;
    var RC_OK = 0;                                      var RC_ERR = 1;
    before(async () => {
        bmc = await BMC.deployed();
        bsh = await BSH.deployed();
        holder = await Holder.deployed();
        accounts = await web3.eth.getAccounts();
        await bmc.setBSH(bsh.address);
        await bmc.approveService(service);
        await bmc.addVerifier(_net, accounts[1]);
        await holder.addBSHContract(bsh.address);
        await bsh.register(_name, _symbol, _decimals, _fee);
        await bmc.transferRequestWithAddress(
            _net, service, _from, holder.address, _name, _value
        );
    });

    it('Transfer Token to side chain - Success', async () => {
        var _to = 'btp://1234.iconee/0x12345678';
        // var coin = await bsh.coinOf(_name);
        var _value = 1000;
        var balanceBefore = await bsh.getBalanceOf(holder.address, _name);
        await holder.setApprove(bsh.address);
        await holder.callTransfer(_name, _value, _to);
        var balanceAfter = await bsh.getBalanceOf(holder.address, _name);
        assert(
            web3.utils.BN(balanceAfter._usableBalance).toNumber() == 
                web3.utils.BN(balanceBefore._usableBalance).toNumber() - 1000 &&
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() == 0 &&    
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() == 1000
        );
    });

    it('BSH Handle BTP Success Response', async () => {
        var _value = 1000;
        var balanceBefore = await bsh.getBalanceOf(holder.address, _name);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net, service, 0, RC_OK, "");
        var balanceAfter = await bsh.getBalanceOf(holder.address, _name);

        assert(
            web3.utils.BN(balanceBefore._lockedBalance) == _value && 
            web3.utils.BN(balanceAfter._lockedBalance) == 0
        );
    });

    it('Transfer Token to side chain - Big amount - Success', async () => {
        var _to = 'btp://1234.iconee/0x12345678';
        // var coin = await bsh.coinOf(_name);
        var _value = 100000000000000;
        var balanceBefore = await bsh.getBalanceOf(holder.address, _name);
        await holder.setApprove(bsh.address);
        await holder.callTransfer(_name, _value, _to);
        var balanceAfter = await bsh.getBalanceOf(holder.address, _name);
        assert(
            web3.utils.BN(balanceAfter._usableBalance).toNumber() == 
                web3.utils.BN(balanceBefore._usableBalance).toNumber() - _value &&
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() == 0 &&    
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() == _value
        );
    });

    it('BSH Handle BTP Error Response - Refundable Contract', async () => {
        var _value = 100000000000000;
        var balanceBefore = await bsh.getBalanceOf(holder.address, _name);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net, service, 1, RC_ERR, "");
        var balanceAfter = await bsh.getBalanceOf(holder.address, _name);

        assert(
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() == _value && 
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() == 0 &&
            web3.utils.BN(balanceAfter._usableBalance).toNumber() == 
                web3.utils.BN(balanceBefore._usableBalance).toNumber() + _value &&
            web3.utils.BN(balanceAfter._refundableBalance).toNumber() == 0
        );
    });

    it('Transfer Token to side chain - Invalid BTP Address format - Failure', async () => {
        var _to = '1234.iconee/0x12345678';
        var _name = 'ICON';

        var coin = await bsh.coinOf(_name);
        var balanceBefore = await bsh.balanceOf(holder.address, coin.id);
        await holder.setApprove(bsh.address);
        await truffleAssert.reverts(
            holder.callTransfer(_name, 2000, _to),
            "revert"
        ); 
        var balanceAfter = await bsh.balanceOf(holder.address, coin.id);
        assert(
            web3.utils.BN(balanceAfter).toNumber() == web3.utils.BN(balanceBefore).toNumber()
        );
    });

    it('Transfer Token to side chain - Amount is Zero - Failure', async () => {
        var _to = '1234.iconee/0x12345678';
        var _name = 'ICON';

        var coin = await bsh.coinOf(_name);
        var balanceBefore = await bsh.balanceOf(holder.address, coin.id);
        await holder.setApprove(bsh.address);
        await truffleAssert.reverts(
            holder.callTransfer(_name, 0, _to),
            "Invalid amount"
        ); 
        var balanceAfter = await bsh.balanceOf(holder.address, coin.id);
        assert(
            web3.utils.BN(balanceAfter).toNumber() == web3.utils.BN(balanceBefore).toNumber()
        );
    });

    it('Transfer Token to side chain - Charge Fee is Zero - Failure', async () => {
        var _to = '1234.iconee/0x12345678';
        var _name = 'ICON';

        var coin = await bsh.coinOf(_name);
        var balanceBefore = await bsh.balanceOf(holder.address, coin.id);
        await holder.setApprove(bsh.address);
        await truffleAssert.reverts(
            holder.callTransfer(_name, 10, _to),
            "Invalid amount"
        ); 
        var balanceAfter = await bsh.balanceOf(holder.address, coin.id);
        assert(
            web3.utils.BN(balanceAfter).toNumber() == web3.utils.BN(balanceBefore).toNumber()
        );
    });

    it('Transfer Token to side chain - Invalid Network/Network Not Supported - Failure', async () => {
        var _to = 'btp://1234.eos/0x12345678';
        var _name = 'ICON';

        var coin = await bsh.coinOf(_name);
        var balanceBefore = await bsh.balanceOf(holder.address, coin.id);
        await holder.setApprove(bsh.address);
        await truffleAssert.reverts(
            holder.callTransfer(_name, 1000, _to),
            "BMCRevertNotExistsBMV"
        ); 
        var balanceAfter = await bsh.balanceOf(holder.address, coin.id);
        assert(
            web3.utils.BN(balanceAfter).toNumber() == web3.utils.BN(balanceBefore).toNumber()
        );
    });
});

contract('BSH Handles BTP Message - Receive PARA Coin Mint request - Unit Tests', () => {
    let bmc, bsh, accounts, notpayable;
    var service = 'Coin/WrappedCoin';
    var _net = '1234.iconee';
    var _svc = 'Token';
    var _coin = 'PARA';
    var _to = 'btp://1234.iconee/0x12345678';
    before(async () => {
        bmc = await BMC.deployed();
        bsh = await BSH.deployed();
        notpayable = await NotPayable.deployed();
        accounts = await web3.eth.getAccounts();
        await bmc.setBSH(bsh.address);
        await bmc.approveService(service);
        await bmc.addVerifier(_net, accounts[1]);
        await bsh.transfer(_to, {from: accounts[0], value: 100000000});
    });

    it('Receive Request PARA Coin Mint - Success', async () => { 
        var _from = '0x12345678';
        var _value = 12345;
        var _chargedFee = Math.floor(_value / 100);
        var balanceBefore = await bmc.getBalance(accounts[1]);
        await bmc.transferRequestWithAddress(
            _net, service, _from, accounts[1], _coin, _value
        );
        var balanceAfter = await bmc.getBalance(accounts[1]);
        assert(
            web3.utils.BN(balanceAfter).toString() == 
                web3.utils.BN(balanceBefore).add(new web3.utils.BN(_value)).toString()
        );
        // truffleAssert.prettyPrintEmittedEvents(transfer);
    });

    //  BSH receives a Request Coin Transfer, but a receiving address is an invalid address
    //  BSH responses a REQUEST_SERVICE with RC_ERR (code = 1, 
    //  message = error returned when trying to parse a string address)
    it('Receive Request PARA Coin Mint - Invalid address - Failure', async () => {
        var _from = '0x12345678';
        var _value = 1000;
        var _chargedFee = Math.floor(_value / 100);
        var _address = '0x1234567890123456789';
        await truffleAssert.reverts( 
            bmc.transferRequestWithStringAddress(
            _net, service, _from, _address, _coin, _value),
            'Invalid address'
        );
        // truffleAssert.prettyPrintEmittedEvents(transfer);
        // truffleAssert.eventEmitted(transfer, 'Message', (ev) => {
        //     return ev._msg.toString() === msg;
        // });
    });

    //  mock receives a Request Coin Transfer, but receiving address is not a payable contract
    //  mock responses a REQUEST_SERVICE with RC_ERR (code = 1, 
    //  message = error returned when trying to parse a string address)
    it('Receive Request PARA Coin Mint - Not Payable Contract receiver - Failure', async () => {
        var _from = '0x12345678';
        var _value = 1000;
        var _chargedFee = Math.floor(_value / 100);
        var balanceBefore = await bmc.getBalance(notpayable.address);
        await truffleAssert.reverts(
            bmc.transferRequestWithAddress(
            _net, service, _from, notpayable.address, _coin, _value),
            'Transfer failed'
        );
        // truffleAssert.prettyPrintEmittedEvents(transfer);
        var balanceAfter = await bmc.getBalance(notpayable.address);
        assert(
            web3.utils.BN(balanceAfter).toNumber() == 
                web3.utils.BN(balanceBefore).toNumber()
        );
    });
});

contract('BSH Handles BTP Message - Receive Token Mint request - Unit Tests', () => {
    let bmc, bsh, holder, notpayable, accounts;
    var service = 'Coin/WrappedCoin';
    var _name = 'ICON';
    var _symbol = 'ICX';
    var _decimals = 0;
    var _fee = 10000;
    var _net = '1234.iconee';
    var _svc = 'Token';
    before(async () => {
        bmc = await BMC.deployed();
        bsh = await BSH.deployed();
        holder = await Holder.deployed();
        notpayable = await NotPayable.deployed();
        accounts = await web3.eth.getAccounts();
        await bmc.setBSH(bsh.address);
        await bmc.approveService(service);
        await bmc.addVerifier(_net, accounts[1]);
        await holder.addBSHContract(bsh.address);
        await bsh.register(_name, _symbol, _decimals, _fee);
    });

    it('Receive Request Token Mint - ERC1155Holder/Receiver Contract - Success', async () => { 
        var _from = '0x12345678';
        var _value = 2500;
        var _chargedFee = Math.floor(_value / 100);
        var coin = await bsh.coinOf(_name);
        var balanceBefore = await bsh.balanceOf(holder.address, coin.id);
        await bmc.transferRequestWithAddress(
            _net, service, _from, holder.address, _name, _value
        );
        var balanceAfter = await bsh.balanceOf(holder.address, coin.id);
        // truffleAssert.prettyPrintEmittedEvents(transfer);
        assert(
            web3.utils.BN(balanceAfter).toNumber() == 
                web3.utils.BN(balanceBefore).toNumber() + 2500
        );
    });

    it('Receive Request Token Mint - Account - Success', async () => { 
        var _from = '0x12345678';
        var _value = 5500;
        var _chargedFee = Math.floor(_value / 100);
        var coin = await bsh.coinOf(_name);
        var balanceBefore = await bsh.balanceOf(accounts[1], coin.id);
        await bmc.transferRequestWithAddress(
            _net, service, _from, accounts[1], _name, _value
        );
        var balanceAfter = await bsh.balanceOf(accounts[1], coin.id);
        // truffleAssert.prettyPrintEmittedEvents(transfer);
        assert(
            web3.utils.BN(balanceAfter).toNumber() == 
                web3.utils.BN(balanceBefore).toNumber() + 5500
        );
    });

    //  mock receives a Request Token Transfer, but Token name is not supported
    //  mock responses a REQUEST_SERVICE with RC_ERR (code = 1, message = 'Invalid Coin')
    //  Balance of holder.address remains unchanged (balance = 2500 as in previous test)
    it('Receive Request Token Mint - Invalid Token Name - Failure', async () => {
        var _from = '0x12345678';
        var _value = 3000;
        var _chargedFee = Math.floor(_value / 100);
        var _tokenName = 'Ethereum';

        var coin = await bsh.coinOf(_tokenName);
        var balanceBefore = await bsh.balanceOf(holder.address, coin.id);
        await truffleAssert.reverts(
            bmc.transferRequestWithAddress(
            _net, service, _from, holder.address, _tokenName, _value),
            'Invalid token'
        );
        // truffleAssert.prettyPrintEmittedEvents(transfer);
        var balanceAfter = await bsh.balanceOf(holder.address, coin.id);
        assert(
            web3.utils.BN(balanceAfter).toNumber() == 
                web3.utils.BN(balanceBefore).toNumber()
        );
    });

    //  mock receives a Request Token Transfer, but a receiving address is a invalid format address
    //  mock responses a REQUEST_SERVICE with RC_ERR (code = 1, 
    //  message = error returned when trying to parse a string address)
    it('Receive Request Token Mint - Invalid format address - Failure', async () => {
        var _from = '0x12345678';
        var _value = 1000;
        var _chargedFee = Math.floor(_value / 100);
        var _address = '0x1234567890123456789';

        await truffleAssert.reverts( 
            bmc.transferRequestWithStringAddress(
                _net, service, _from, _address, _name, _value),
            'Invalid address'    
        );
        // truffleAssert.prettyPrintEmittedEvents(transfer);
    });

    //  mock receives a Request Token Transfer, but a receiving address is NOT ERC1155Holder/Receiver
    //  mock responses a REQUEST_SERVICE with RC_ERR (code = 1, 
    //  message = error returned when trying to parse a string address)
    //  Balance of holder.address remains unchanged (balance = 2500 as in previous test)
    it('Receive Request Token Mint - Receiver not ERC1155Holder/Receiver - Failure', async () => {
        var _from = '0x12345678';
        var _value = 1000;
        var _chargedFee = Math.floor(_value / 100);
        var coin = await bsh.coinOf(_name);
        var balanceBefore = await bsh.balanceOf(notpayable.address, coin.id);
        await truffleAssert.reverts( 
            bmc.transferRequestWithAddress(
                _net, service, _from, notpayable.address, _name, _value),
            'Mint failed'    
        );
        // truffleAssert.prettyPrintEmittedEvents(transfer);
        var balanceAfter = await bsh.balanceOf(notpayable.address, coin.id);
        assert(
            web3.utils.BN(balanceAfter).toNumber() == 
                web3.utils.BN(balanceBefore).toNumber()
        );
    });
});

contract('BSH Handle Fee Aggregation', () => {
    let bsh, bmc, holder, accounts;
    var service = 'Coin/WrappedCoin';
    var _net1 = '1234.iconee';
    var _net2 = '1234.binance';
    var _svc = 'Token';
    var _native = 'PARA';
    var _txAmt = 10000;

    var _symbol1 = 'ICX';                                var _symbol2 = 'BNC';
    var _decimals1 = 0;                                  var _decimals2 = 0;
    var _fee1 = 10000;                                   var _fee2 = 10000;

    var _name1 = 'ICON';                                var _name2 = 'BINANCE';        
    var _from1 = '0x12345678';                          var _from2 = '0x12345678';
    var _value1 = 999999999999999;                      var _value2 = 999999999999999;
    var _chargedFee1 = Math.floor(_value1 / 100);       var _chargedFee2 = Math.floor(_value1 / 100);

    var _to1 = 'btp://1234.iconee/0x12345678';          var _to2 = 'btp://1234.binance/0x12345678';
    var _txAmt1 = 1000000;                              var _txAmt2 = 5000000;
    var RC_OK = 0;                                      var RC_ERR = 1;                                                         
    var _sn1 = 1;                                       var _sn2 = 2;
    var REPONSE_HANDLE_SERVICE = 2; 
    
    var _name3 = 'ETHEREUM';                             var _name4 = 'TRON'; 
    var _symbol3 = 'ETH';                                var _symbol4 = 'TRX';
    var _decimals3 = 0;                                  var _decimals4 = 0;
    var _fee3 = 10000;                                   var _fee4 = 10000;
    before(async () => {
        bmc = await BMC.deployed();
        bsh = await BSH.deployed();
        holder = await Holder.deployed();
        accounts = await web3.eth.getAccounts();
        await bmc.setBSH(bsh.address);
        await bmc.approveService(service);
        await bmc.addVerifier(_net1, accounts[1]);
        await bmc.addVerifier(_net2, accounts[2]);
        await holder.addBSHContract(bsh.address);
        await bsh.register(_name1, _symbol1, _decimals1, _fee1);
        await bsh.register(_name2, _symbol2, _decimals2, _fee2);
        await bsh.register(_name3, _symbol3, _decimals3, _fee3);
        await bsh.register(_name4, _symbol4, _decimals4, _fee4);
        await bmc.transferRequestWithAddress(
            _net1, service, _from1, holder.address, _name1, _value1
        );
        await bmc.transferRequestWithAddress(
            _net2, service, _from2, holder.address, _name2, _value2
        );
        await bsh.transfer(_to1, {from: accounts[0], value: _txAmt});
        await bmc.response(REPONSE_HANDLE_SERVICE, _net1, service, 0, RC_OK, "");
        await holder.setApprove(bsh.address);
        await holder.callTransfer(_name1, _txAmt1, _to1);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net1, service, _sn1, RC_OK, "");
        await holder.callTransfer(_name2, _txAmt2, _to2);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net2, service, _sn2, RC_OK, "");
    });

    it('Query Aggregation Fee', async () => {
        var aggregationFee = await bsh.getAccumulatedFees();
        assert(
            aggregationFee.length == 5 &&
            aggregationFee[0].coinName == 'PARA' && aggregationFee[0].value == 100 &&
            aggregationFee[1].coinName == 'ICON' && aggregationFee[1].value == 10000 &&
            aggregationFee[2].coinName == 'BINANCE' && aggregationFee[2].value == 50000 &&
            aggregationFee[3].coinName == 'ETHEREUM' && aggregationFee[3].value == 0 &&
            aggregationFee[4].coinName == 'TRON' && aggregationFee[4].value == 0
        );
    });

    it('BSH Handle Gather Fee Request - Not from BMC - Failure', async () => {
        var FA1Before = await bsh.getFAOf(_native);     //  state Aggregation Fee of each type of Coins
        var FA2Before = await bsh.getFAOf(_name1);
        var FA3Before = await bsh.getFAOf(_name2);
        await truffleAssert.reverts( 
            bsh.handleGatherFee(_to1, {from: accounts[1]}),
            'Unauthorized'    
        );
        var FA1After = await bsh.getFAOf(_native);
        var FA2After = await bsh.getFAOf(_name1);
        var FA3After = await bsh.getFAOf(_name2);
        // truffleAssert.prettyPrintEmittedEvents(event);
        var fees = await bsh.getFees(_sn2 + 1);     //  get pending Aggregation Fee list
        assert(
            web3.utils.BN(FA1Before).toNumber() == web3.utils.BN(FA1After).toNumber() && 
            web3.utils.BN(FA2Before).toNumber() == web3.utils.BN(FA2After).toNumber() &&
            web3.utils.BN(FA3Before).toNumber() == web3.utils.BN(FA3After).toNumber() &&
            fees.length == 0
        );
    });

    //  Before: 
    //      + state Aggregation Fee of each type of Coins are set
    //      + pendingAggregation Fee list is empty
    //  After: 
    //      + all states of Aggregation Fee are push into pendingAggregation Fee list
    //      + state Aggregation Fee of each type of Coins are reset
    it('BSH Handle Gather Fee Request - Success', async () => {
        var FA1Before = await bsh.getFAOf(_native);     //  state Aggregation Fee of each type of Coins
        var FA2Before = await bsh.getFAOf(_name1);
        var FA3Before = await bsh.getFAOf(_name2);
        await bmc.gatherFee(_to1);
        var FA1After = await bsh.getFAOf(_native);
        var FA2After = await bsh.getFAOf(_name1);
        var FA3After = await bsh.getFAOf(_name2);
        var fees = await bsh.getFees(_sn2 + 1);     //  get pending Aggregation Fee list
        assert(
            FA1Before == Math.floor(_txAmt / 100) && 
            FA2Before == Math.floor(_txAmt1 / 100) &&
            FA3Before == Math.floor(_txAmt2 / 100) &&
            FA1After == 0 && FA2After == 0 && FA3After == 0 && 
            fees[0].coinName == _native && fees[0].value == Math.floor(_txAmt / 100) &&
            fees[1].coinName == _name1 && fees[1].value == Math.floor(_txAmt1 / 100) &&
            fees[2].coinName == _name2 && fees[2].value == Math.floor(_txAmt2 / 100)
        );
    });

    it('BSH Handle Gather Fee Successful Response', async () => {
        var feesBefore = await bsh.getFees(_sn2 + 1);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net1, service, _sn2+1, RC_OK, "");
        var feesAfter = await bsh.getFees(_sn2 + 1);
        assert(
            feesBefore.length == 3 &&
            feesBefore[0].coinName == _native && feesBefore[0].value == Math.floor(_txAmt / 100) &&
            feesBefore[1].coinName == _name1 && feesBefore[1].value == Math.floor(_txAmt1 / 100) &&
            feesBefore[2].coinName == _name2 && feesBefore[2].value == Math.floor(_txAmt2 / 100) &&
            feesAfter.length == 0
        );
    });

    it('BSH Handle Gather Fee Error Response', async () => {
        var _amt1 = 2000000;                    var _amt2 = 6000000;
        var _sn3 = _sn2 + 2;                    var _sn4 = _sn2 + 3;
        await holder.callTransfer(_name1, _amt1, _to1);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net1, service, _sn3, RC_OK, "");
        await holder.callTransfer(_name2, _amt2, _to2);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net2, service, _sn4, RC_OK, "");
        await bmc.gatherFee(_to1);

        var FA1Before = await bsh.getFAOf(_name1);
        var FA2Before = await bsh.getFAOf(_name2);
        var feesBefore = await bsh.getFees(_sn2 + 4);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net1, service, _sn2+4, RC_ERR, "");
        var FA1After = await bsh.getFAOf(_name1);
        var FA2After = await bsh.getFAOf(_name2);
        var feesAfter = await bsh.getFees(_sn2 + 4);
        assert(
            feesBefore.length == 2 &&
            feesBefore[0].coinName == _name1 && feesBefore[0].value == Math.floor(_amt1 / 100) &&
            feesBefore[1].coinName == _name2 && feesBefore[1].value == Math.floor(_amt2 / 100) &&
            FA1Before == 0 && FA2Before == 0 &&
            feesAfter.length == 0 &&
            FA1After == Math.floor(_amt1 / 100) && FA2After == Math.floor(_amt2 / 100)
        );
    });
});
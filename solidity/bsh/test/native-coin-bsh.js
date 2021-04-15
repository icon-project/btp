const Mock = artifacts.require('Mock');
const Holder = artifacts.require("Holder");
const NotPayable = artifacts.require("NotPayable");
const truffleAssert = require('truffle-assertions');

var native_coin_name = 'PARA';
contract('BSH Basic Unit Tests', () => {
    let mock, accounts;
    beforeEach(async () => {
        mock = await Mock.deployed();
        accounts = await web3.eth.getAccounts()
    });

    // it('Should deploy', async () => {
    //     console.log('BSH Address: ', mock.address);
    // });

    it('Register Coin - With Permission - Success', async () => {
        var _name = "ICON";
        var _symbol = "ICX";
        var _decimal = 0;
        
        var output = await mock.coinNames();
        // console.log('Supported Coins (before): ', output);
        await mock.register(_name, _symbol, _decimal);
        output = await mock.coinNames();
        // console.log('Supported Coins (after): ', output);
        assert(
            output[0] === native_coin_name && output[1] === 'ICON'
        );
    });
    
    it('Register Coin - Without Permission - Failure', async () => {
        var _name = "ICON";
        var _symbol = "ICX";
        var _decimal = 0;
        
        await truffleAssert.reverts(
            mock.register(_name, _symbol, _decimal, {from: accounts[1]}),
            "VM Exception while processing transaction: revert No permission -- Reason given: No permission"
        );
    }); 

    it('Register Coin - Token existed - Failure', async () => {
        var _name = "ICON";
        var _symbol = "ICX";
        var _decimal = 0;
        await truffleAssert.reverts(
            mock.register(_name, _symbol, _decimal),
            "VM Exception while processing transaction: revert Token existed -- Reason given: Token existed"
        );
    }); 

    it('Coin Query - Valid Supported Coin - Success', async () => {
        var _name1 = "wBTC";    var _name2 = "Ethereum";
        var _symbol1 = "wBTC";   var _symbol2 = "ETH";
        var _decimals1 = 0;     var _decimals2 = 0;

        var _query = "ICON";
        await mock.register(_name1, _symbol1, _decimals1);
        await mock.register(_name2, _symbol2, _decimals2);

        var id = await mock.hashCoinName(_query);
        var result = await mock.coinOf(_query);
        assert(
            result.id === web3.utils.BN(id).toString() && result.symbol === 'ICX'
        );
    }); 

    it('Coin Query - Not Supported Coin - Success', async () => {
        var _query = "EOS";

        var result = await mock.coinOf(_query);
        // console.log('Query result: ', result);
        assert(
            result.id == 0, result.symbol === '', result.decimal == 0
        );
    }); 
});

var service = 'Coin/WrappedCoin';
var _net = '1234.iconee';
contract('BSH Transfer Coin Unit Tests', () => {
    let mock, accounts;
    beforeEach(async () => {
        mock = await Mock.deployed();
        accounts = await web3.eth.getAccounts();
    });

    // it('Should deploy', async () => {
    //     console.log('Mock Address: ', mock.address);
    // });

    it('Transfer Native Coin to side chain - Success', async () => {
        var _to = 'btp://1234.iconee/0x12345678';
        await mock.addService(service, mock.address);
        await mock.addVerifier(_net, accounts[1]);
        var output = await mock.transfer(_to, {from: accounts[0], value: 1000});
        
        // truffleAssert.prettyPrintEmittedEvents(output);

        truffleAssert.eventEmitted(output, 'TransferStart', (ev) => {
            return ev._from === accounts[0] && ev._to === _to && 
                ev._sn == 0 && ev._coinName === 'PARA' && ev._value == 1000;
        });
    });

    it('Transfer Native Coin to side chain - Success', async () => {
        var _to = 'btp://1234.iconee/0x83109993';
        var output = await mock.transfer(_to, {from: accounts[0], value: 999999999999});
        
        // truffleAssert.prettyPrintEmittedEvents(output);

        truffleAssert.eventEmitted(output, 'TransferStart', (ev) => {
            return ev._from === accounts[0] && ev._to === _to && 
                ev._sn == 1 && ev._coinName === 'PARA' && ev._value == 999999999999;
        });
    });

    it('Transfer Native Coin to side chain - Invalid BTP Address format - Failure', async () => {
        var _to = '1234.iconee/0x12345678';

        await truffleAssert.reverts(
            mock.transfer(_to, {from: accounts[0], value: 1000}),
            "VM Exception while processing transaction: revert"
        ); 
    });

    it('Transfer Native Coin to side chain - Amount is Zero - Failure' , async () => {
        var _to = 'btp://1234.iconee/0x12345678';

        await truffleAssert.reverts(
            mock.transfer(_to, {from: accounts[0], value: 0}),
            "VM Exception while processing transaction: revert Invalid amount -- Reason given: Invalid amount"
        ); 
    });

    //  In this test case, mock registers mock with its service name
    //  But, BMV Service (network address) has not yet registered
    //  Thus, the REQUEST_COIN_TRANSFER from mock prones to fail
    //  The revert() is called.
    it('Transfer Native Coin to side chain - Invalid Network/Network Address Not Supported - Failure' , async () => {
        var _to = 'btp://1234.eos/0x12345678';

        var temp = await mock.getServices();
        // console.log(temp);
        await truffleAssert.reverts(
            mock.transfer(_to, {from: accounts[1], value: 1000000}),
            "VM Exception while processing transaction: revert Invalid network -- Reason given: Invalid network"
        ); 
    });
});

var service = 'Coin/WrappedCoin';
var _net = '1234.iconee';
contract('BSH Transfer Token Unit Tests', () => {
    let mock, holder, accounts;
    beforeEach(async () => {
        mock = await Mock.deployed();
        holder = await Holder.deployed();
        accounts = await web3.eth.getAccounts();
    });

    // it('Should deploy', async () => {
    //     console.log('Mock Address: ', mock.address);
    //     console.log('Holder Address: ', holder.address);
    // });

    it('Transfer Token to side chain - Success', async () => {
        var _to = 'btp://1234.iconee/0x12345678';
        var _name = 'ICON';
        var _symbol = 'ICX';
        var _decimal = 0;
        await mock.addService(service, mock.address);
        await mock.addVerifier(_net, accounts[1]);

        await mock.register(_name, _symbol, _decimal);
        var coin = await mock.coinOf(_name);
        await mock.setBalance(holder.address, coin.id, 100000);
        var balanceBefore = await mock.balanceOf(holder.address, coin.id);
        // console.log('Balance (before): ', web3.utils.hexToNumber(balance));
        await holder.addBSHContract(mock.address);
        await holder.setApprove(mock.address);
        await holder.callTransfer(_name, 1000, _to);
        var balanceAfter = await mock.balanceOf(holder.address, coin.id);
        assert(
            balanceAfter == balanceBefore - 1000
        );
        // console.log('Balance (after): ', web3.utils.hexToNumber(balance));
    });

    it('Transfer Token to side chain - Big amount - Success', async () => {
        var _to = 'btp://1234.iconee/0x12345678';
        var _name = 'ICON';

        var coin = await mock.coinOf(_name);
        await mock.setBalance(holder.address, coin.id, 999999999999999);
        var balanceBefore = await mock.balanceOf(holder.address, coin.id);
        // console.log('Balance (before): ', web3.utils.hexToNumber(balanceBefore));
        await holder.setApprove(mock.address);
        await holder.callTransfer(_name, 100000000000000, _to);
        var balanceAfter = await mock.balanceOf(holder.address, coin.id);
        // console.log('Balance (after): ', web3.utils.hexToNumber(balance));
        assert(
            balanceAfter = balanceBefore - 100000000000000
        );
    });

    it('Transfer Token to side chain - Invalid BTP Address format - Failure', async () => {
        var _to = '1234.iconee/0x12345678';
        var _name = 'ICON';

        var coin = await mock.coinOf(_name);
        var balanceBefore = await mock.balanceOf(holder.address, coin.id);
        // console.log('Balance (before): ', web3.utils.hexToNumber(balanceBefore));
        await holder.setApprove(mock.address);
        
        await truffleAssert.reverts(
            holder.callTransfer(_name, 2000, _to),
            "VM Exception while processing transaction: revert"
        ); 
        var balanceAfter = await mock.balanceOf(holder.address, coin.id);
        // console.log('Balance (after): ', web3.utils.hexToNumber(balanceAfter));
        assert(
            web3.utils.hexToNumber(balanceAfter) == web3.utils.hexToNumber(balanceBefore)
        );
        
    });

    it('Transfer Token to side chain - Amount is Zero - Failure', async () => {
        var _to = '1234.iconee/0x12345678';
        var _name = 'ICON';

        var coin = await mock.coinOf(_name);
        var balanceBefore = await mock.balanceOf(holder.address, coin.id);
        // console.log('Balance (before): ', web3.utils.hexToNumber(balance));
        await holder.setApprove(mock.address);
        await truffleAssert.reverts(
            holder.callTransfer(_name, 0, _to),
            "VM Exception while processing transaction: revert Invalid amount -- Reason given: Invalid amount"
        ); 
        var balanceAfter = await mock.balanceOf(holder.address, coin.id);
        assert(
            web3.utils.hexToNumber(balanceAfter) == web3.utils.hexToNumber(balanceBefore) 
        );
        // console.log('Balance (after): ', web3.utils.hexToNumber(balance));
    });

    //  TODO: mock address after deploying and mock address that assigned to Mockmock
    //  is not the same. 

    it('Transfer Token to side chain - Invalid Network/Network Not Supported - Failure', async () => {
        var _to = 'btp://1234.eos/0x12345678';
        var _name = 'ICON';

        var coin = await mock.coinOf(_name);
        await mock.setBalance(holder.address, coin.id, 100000);
        var balanceBefore = await mock.balanceOf(holder.address, coin.id);
        // console.log('Balance (before): ', web3.utils.hexToNumber(balance));
        // await holder.addmockContract(mock.address);
        await holder.setApprove(mock.address);
        await truffleAssert.reverts(
            holder.callTransfer(_name, 1000, _to),
            "VM Exception while processing transaction: revert Invalid network -- Reason given: Invalid network"
        ); 
        var balanceAfter = await mock.balanceOf(holder.address, coin.id);
        assert(
            web3.utils.hexToNumber(balanceAfter) == web3.utils.hexToNumber(balanceBefore)
        );
        // console.log('Balance (after): ', web3.utils.hexToNumber(balance));
    });
});

var _net = '1234.iconee';
var _svc = 'Token';
var _coin = 'PARA';

contract('BSH Handles BTP Message - Receive PARA Coin Mint request - Unit Tests', () => {
    let mock, accounts, notpayable;
    beforeEach(async () => {
        mock = await Mock.deployed();
        notpayable = await NotPayable.deployed();
        accounts = await web3.eth.getAccounts();
    });
        
    // it('Should deploy', async () => {
    //     console.log('Mock Address: %s', mock.address);
    //     console.log('Contract Address: %s', notpayable.address);
    // });

    it('Receive Request PARA Coin Mint - Success', async () => { 
        var _from = '0x12345678';
        var _value = 12345;
        await mock.addVerifier(_net, accounts[1]);
        var balanceBefore = await mock.getBalance(accounts[1]);
        // console.log('Balance (before): ', web3.utils.BN(balance).toString());
        await mock.deposit({from: accounts[0], value: 1000000})
        var transfer = await mock.transferRequestWithAddress(
            _net, _svc, _from, accounts[1], _coin, _value
        );
        var balanceAfter = await mock.getBalance(accounts[1]);
        // console.log('Balance (after): ', web3.utils.BN(balance).toString());
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
        var _address = '0x1234567890123456789';
        var transfer = await mock.transferRequestWithStringAddress(
            _net, _svc, _from, _address, _coin, _value
        );
        // truffleAssert.prettyPrintEmittedEvents(transfer);
    });

    //  mock receives a Request Coin Transfer, but receiving address is not a payable contract
    //  mock responses a REQUEST_SERVICE with RC_ERR (code = 1, 
    //  message = error returned when trying to parse a string address)
    it('Receive Request PARA Coin Mint - Not Payable Contract receiver - Failure', async () => {
        var _from = '0x12345678';
        var _value = 1000;
        var balanceBefore = await mock.getBalance(notpayable.address);
        var transfer = await mock.transferRequestWithAddress(
            _net, _svc, _from, notpayable.address, _coin, _value
        );
        // truffleAssert.prettyPrintEmittedEvents(transfer);
        var balanceAfter = await mock.getBalance(notpayable.address);
        assert(
            web3.utils.BN(balanceAfter).toNumber() == 
                web3.utils.BN(balanceBefore).toNumber()
        );
    });
});

var _name = 'ICON';
var _symbol = 'ICX';
var _decimals = 0;
var _net = '1234.iconee';
var _svc = 'Token';

contract('BSH Handles BTP Message - Receive Token Mint request - Unit Tests', () => {
    let mock, holder, accounts;
    beforeEach(async () => {
        holder = await Holder.deployed();
        mock = await Mock.deployed();
        accounts = await web3.eth.getAccounts();
        await holder.addBSHContract(mock.address);
    });
        
    // it('Should deploy', async () => {
    //     console.log('Mock Address: %s', mock.address);
    //     console.log('Holder Address: %s', holder.address);
    // });

    it('Receive Request Token Mint - Success', async () => { 
        await mock.register(_name, _symbol, _decimals);
        await mock.addVerifier(_net, accounts[1]);
        var _from = '0x12345678';
        var _value = 2500;
        var coin = await mock.coinOf(_name);
        var balanceBefore = await mock.balanceOf(holder.address, coin.id);
        // console.log('Balance (before): ', web3.utils.hexToNumber(balance));
        var transfer = await mock.transferRequestWithAddress(
            _net, _svc, _from, holder.address, _name, _value
        );
        var balanceAfter = await mock.balanceOf(holder.address, coin.id);
        // console.log('Balance (after): ', web3.utils.hexToNumber(balance));
        // truffleAssert.prettyPrintEmittedEvents(transfer);
        assert(
            web3.utils.hexToNumber(balanceAfter) == 
                web3.utils.hexToNumber(balanceBefore) + 2500
        );
    });

    //  mock receives a Request Token Transfer, but Token name is not supported
    //  mock responses a REQUEST_SERVICE with RC_ERR (code = 1, message = 'Invalid Coin')
    //  Balance of holder.address remains unchanged (balance = 2500 as in previous test)
    it('Receive Request Token Mint - Invalid Token Name - Failure', async () => {
        var _from = '0x12345678';
        var _value = 3000;
        var _tokenName = 'Ethereum';

        var coin = await mock.coinOf(_tokenName);
        var balanceBefore = await mock.balanceOf(holder.address, coin.id);
        var transfer = await mock.transferRequestWithAddress(
            _net, _svc, _from, holder.address, _tokenName, _value
        );
        // truffleAssert.prettyPrintEmittedEvents(transfer);
        var balanceAfter = await mock.balanceOf(holder.address, coin.id);
        assert(
            web3.utils.hexToNumber(balanceAfter) == web3.utils.hexToNumber(balanceBefore)
        );
    });

    //  mock receives a Request Token Transfer, but a receiving address is a invalid format address
    //  mock responses a REQUEST_SERVICE with RC_ERR (code = 1, 
    //  message = error returned when trying to parse a string address)
    it('Receive Request Token Mint - Invalid format address - Failure', async () => {
        var _from = '0x12345678';
        var _value = 1000;
        var _address = '0x1234567890123456789';

        var transfer = await mock.transferRequestWithStringAddress(
            _net, _svc, _from, _address, _name, _value
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

        var coin = await mock.coinOf(_name);
        var balanceBefore = await mock.balanceOf(accounts[1], coin.id);
        var transfer = await mock.transferRequestWithAddress(
            _net, _svc, _from, accounts[1], _name, _value
        );
        // truffleAssert.prettyPrintEmittedEvents(transfer);
        var balanceAfter = await mock.balanceOf(accounts[1], coin.id);
        assert(
            balanceAfter = balanceBefore
        );
    });
});
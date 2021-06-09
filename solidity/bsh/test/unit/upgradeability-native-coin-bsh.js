// const BSHServiceV1 = artifacts.require("BSHServiceV1");
// const BSHServiceV2 = artifacts.require("BSHServiceV2");
// const BSHCoinV1 = artifacts.require("BSHCoinV1");
// const BSHCoinV2 = artifacts.require("BSHCoinV2");
// const BMC = artifacts.require("MockBMC");
// const Holder = artifacts.require("AnotherHolder");
// const NotPayable = artifacts.require("NotPayable");
// const NonRefundable = artifacts.require("NonRefundable");
// const Refundable = artifacts.require("Refundable");
// const { assert } = require('chai');
// const truffleAssert = require('truffle-assertions');
// const { deployProxy, upgradeProxy } = require('@openzeppelin/truffle-upgrades');

//  All of these unit tests below check upgradability of smart contract using Openzeppelin Library and SDK
//  BSHService and BSHCoin contracts have two versions
//  These two versions cover many upgradeable features:
//  - Adding additional state variables
//  - Adding additional functions
contract.skip('BSHService Basic Unit Tests - After Upgrading Contract', () => {
    let bsh_svcV1, bsh_svcV2, bsh_coinV1, bsh_coinV2, accounts;
    var native_coin_name = 'PARA';  var fee = 10;
    var service = 'Coin/WrappedCoin';  

    before(async () => {
        bmc = await BMC.deployed();
        bsh_coinV1 = await deployProxy(BSHCoinV1);
        bsh_svcV1 = await deployProxy(BSHServiceV1, [bmc.address, bsh_coinV1.address, service, native_coin_name, fee]);
        await bsh_coinV1.setBSHService(bsh_svcV1.address);
        bsh_svcV2 = await upgradeProxy(bsh_svcV1.address, BSHServiceV2);
        bsh_coinV2 = await upgradeProxy(bsh_coinV1.address, BSHCoinV2);
        accounts = await web3.eth.getAccounts();
    });

    it('Contracts code size', async () => {
        console.log('BSHServiceV2 code size: ', BSHServiceV2.deployedBytecode.length / 2);
        console.log('BSHCoinV2 code size: ', BSHCoinV2.deployedBytecode.length / 2);
    });

    it('Re-initialize BSHService Contract - Failure', async () => {
        await truffleAssert.reverts(
            bsh_svcV2.initialize(bmc.address, bsh_coinV2.address, service, native_coin_name, fee),
            "Initializable: contract is already initialized"
        );
    });

    it('Re-initialize BSHCoin Contract - Failure', async () => {
        await truffleAssert.reverts(
            bsh_coinV2.initialize(),
            "Initializable: contract is already initialized"
        );
    });

    it('Register Coin - With Permission - Success', async () => {
        var _name = "ICON";
        await bsh_svcV2.register(_name);
        output = await bsh_svcV2.coinNames();
        assert(
            output[0] === native_coin_name && output[1] === 'ICON'
        );
    });
    
    it('Register Coin - Without Permission - Failure', async () => {   
        var _name = "TRON";
        await truffleAssert.reverts(
            bsh_svcV2.register(_name, {from: accounts[1]}),
            "Ownable: caller is not the owner"
        );
    }); 

    it('Register Coin - Token existed - Failure', async () => {
        var _name = "ICON";  
        await truffleAssert.reverts(
            bsh_svcV2.register(_name),
            "ExistToken"
        );
    }); 

    it('Coin Query - Valid Supported Coin - Success', async () => {
        var _name1 = "wBTC";    var _name2 = "Ethereum";
        await bsh_svcV2.register(_name1);
        await bsh_svcV2.register(_name2);

        var _query = "ICON";
        var id = await bmc.hashCoinName(_query);
        var result = await bsh_svcV2.coinOf(_query);
        assert(
            web3.utils.BN(result).toString() === web3.utils.BN(id).toString()
        );
    }); 

    it('Coin Query - Not Supported Coin - Success', async () => {
        var _query = "EOS";
        var result = await bsh_svcV2.coinOf(_query);
        assert(
            web3.utils.BN(result).toNumber() == 0
        );
    }); 

    it('Contract Owner Transfer - Success', async () => {
        var oldOwner = await bsh_svcV2.owner();
        await bsh_svcV2.transferOwnership(accounts[1]);
        var newOwner = await bsh_svcV2.owner();
        assert(
            oldOwner == accounts[0] && newOwner == accounts[1]
        );
    }); 

    it('Register Coin - Old Owner - Failure', async () => {
        var _name3 = "TRON";    
        await truffleAssert.reverts(
            bsh_svcV2.register(_name3),
            "Ownable: caller is not the owner"
        );
        output = await bsh_svcV2.coinNames();
        assert(
            output[0] === native_coin_name && output[1] === 'ICON' &&
            output[2] === 'wBTC' && output[3] === 'Ethereum'
        );
    });
    
    it('Register Coin - New Owner - Success', async () => {   
        var _name3 = "TRON";
        await bsh_svcV2.register(_name3, {from: accounts[1]});
        output = await bsh_svcV2.coinNames();
        assert(
            output[0] === native_coin_name && output[1] === 'ICON' &&
            output[2] === 'wBTC' && output[3] === 'Ethereum' &&
            output[4] ===  'TRON'
        );
    });
});

contract.skip('BSHService Transfer Coin Unit Tests - After Upgrading Contract', () => {
    let bsh_svcV1, bsh_svcV2, bsh_coinV1, bsh_coinV2, bmc, nonrefundable, refundable, accounts;
    var service = 'Coin/WrappedCoin';               
    var _net = '1234.iconee';                       var _to = 'btp://1234.iconee/0x12345678';
    var RC_OK = 0;                                  var RC_ERR = 1;    
    var _amt1 = 1000;                               var _amt2 = 999999999999;                       
    var _amt3 = 10000;                              var deposit = 100000;
    var _native = 'PARA';   var fee = 10;                         
    var REPONSE_HANDLE_SERVICE = 2;

    before(async () => {
        bmc = await BMC.deployed();
        bsh_coinV1 = await deployProxy(BSHCoinV1);
        bsh_svcV1 = await deployProxy(BSHServiceV1, [bmc.address, bsh_coinV1.address, service, _native, fee]);
        await bmc.approveService(service);
        await bmc.setBSH(bsh_svcV1.address);
        await bsh_coinV1.setBSHService(bsh_svcV1.address);
        bsh_svcV2 = await upgradeProxy(bsh_svcV1.address, BSHServiceV2);
        bsh_coinV2 = await upgradeProxy(bsh_coinV1.address, BSHCoinV2);
        nonrefundable = await NonRefundable.deployed();
        refundable = await Refundable.deployed();
        accounts = await web3.eth.getAccounts();
        await bmc.addVerifier(_net, accounts[1]);
    });

    it('Transfer Native Coin to side chain - From Account - Success', async () => {
        var balanceBefore = await bsh_coinV2.getBalanceOf(accounts[0], _native);
        var output = await bsh_svcV2.transfer(_to, {from: accounts[0], value: _amt1});
        var balanceAfter = await bsh_coinV2.getBalanceOf(accounts[0], _native);
        var chargedFee = Math.floor(_amt1/ 1000);
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

    it('BSHService Handle BTP Success Response', async () => {
        var balanceBefore = await bsh_coinV2.getBalanceOf(accounts[0], _native);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net, service, 0, RC_OK, "");
        var balanceAfter = await bsh_coinV2.getBalanceOf(accounts[0], _native);

        assert(
            web3.utils.BN(balanceBefore._lockedBalance) == _amt1 && 
            web3.utils.BN(balanceAfter._lockedBalance) == 0
        );
    });

    it('Transfer Native Coin to side chain - From Account - Success', async () => {
        var balanceBefore = await bsh_coinV2.getBalanceOf(accounts[0], _native);
        var output = await bsh_svcV2.transfer(_to, {from: accounts[0], value: _amt2});
        var balanceAfter = await bsh_coinV2.getBalanceOf(accounts[0], _native);
        var chargedFee = Math.floor(_amt2 / 1000);
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

    it('BSHService Handle BTP Error Response - Refundable account', async () => {
        var balanceBefore = await bsh_coinV2.getBalanceOf(accounts[0], _native);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net, service, 1, RC_ERR, "");
        var balanceAfter = await bsh_coinV2.getBalanceOf(accounts[0], _native);

        assert(
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() == _amt2 && 
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() == 0 &&
            web3.utils.BN(balanceAfter._refundableBalance).toNumber() == 0
        );
    });

    it('Transfer Native Coin to side chain - From Non-Refundable Contract - Success', async () => {
        await nonrefundable.deposit({from: accounts[2], value: deposit});
        var balanceBefore = await bsh_coinV2.getBalanceOf(nonrefundable.address, _native);
        var _usableBalanceBefore = await bmc.getBalance(nonrefundable.address);
        await nonrefundable.transfer(bsh_svcV2.address, _to, _amt3);
        var balanceAfter = await bsh_coinV2.getBalanceOf(nonrefundable.address, _native);
        var _usableBalanceAfter = await bmc.getBalance(nonrefundable.address);

        assert(
            web3.utils.BN(_usableBalanceBefore).toNumber() == 
                web3.utils.BN(_usableBalanceAfter).toNumber() + _amt3 &&
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() == 0 && 
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() == _amt3
        );
    });

    it('BSHService Handle BTP Error Response - Non-Refundable Contract', async () => {
        var balanceBefore = await bsh_coinV2.getBalanceOf(nonrefundable.address, _native);
        var _usableBalanceBefore = await bmc.getBalance(nonrefundable.address);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net, service, 2, RC_ERR, "");
        var balanceAfter = await bsh_coinV2.getBalanceOf(nonrefundable.address, _native);
        var _usableBalanceAfter = await bmc.getBalance(nonrefundable.address);

        assert(
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() == _amt3 && 
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() == 0 &&
            web3.utils.BN(_usableBalanceBefore).toNumber() == web3.utils.BN(_usableBalanceAfter).toNumber() &&
            web3.utils.BN(balanceAfter._refundableBalance).toNumber() == _amt3
        );
    });

    it('Transfer Native Coin to side chain - From Refundable Contract - Success', async () => {
        await refundable.deposit({from: accounts[2], value: deposit});
        var _usableBalanceBefore = await bmc.getBalance(refundable.address);
        var balanceBefore = await bsh_coinV2.getBalanceOf(refundable.address, _native);
        await refundable.transfer(bsh_svcV2.address, _to, _amt3);
        var balanceAfter = await bsh_coinV2.getBalanceOf(refundable.address, _native);
        var _usableBalanceAfter = await bmc.getBalance(refundable.address);

        assert(
            web3.utils.BN(_usableBalanceBefore).toNumber() == 
                web3.utils.BN(_usableBalanceAfter).toNumber() + _amt3 &&
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() == 0 && 
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() == _amt3
        );
    });

    it('BSHService Handle BTP Error Response - Refundable Contract', async () => {
        var balanceBefore = await bsh_coinV2.getBalanceOf(refundable.address, _native);
        var _usableBalanceBefore = await bmc.getBalance(refundable.address);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net, service, 3, RC_ERR, "");
        var balanceAfter = await bsh_coinV2.getBalanceOf(refundable.address, _native);
        var _usableBalanceAfter = await bmc.getBalance(refundable.address);

        assert(
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() == _amt3 && 
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() == 0 &&
            web3.utils.BN(_usableBalanceAfter).toNumber() == 
                web3.utils.BN(_usableBalanceBefore).toNumber() + _amt3 &&
            web3.utils.BN(balanceAfter._refundableBalance).toNumber() == 0
        );
    });

    it('Transfer Native Coin to side chain - Invalid BTP Address format - Failure', async () => {
        var invalid_destination = '1234.iconee/0x12345678';
        await truffleAssert.reverts(
            bsh_svcV2.transfer(invalid_destination, {from: accounts[0], value: 1000}),
            "revert"
        ); 
    });

    it('Transfer Native Coin to side chain - Amount is Zero - Failure' , async () => {
        await truffleAssert.reverts(
            bsh_svcV2.transfer(_to, {from: accounts[0], value: 0}),
            "InvalidAmt"
        ); 
    });

    it('Transfer Native Coin to side chain - Charge Fee is Zero - Failure' , async () => {
        await truffleAssert.reverts(
            bsh_svcV2.transfer(_to, {from: accounts[0], value: 10}),
            "InvalidAmt"
        ); 
    });

    it('Transfer Native Coin to side chain - Invalid Network/Network Address Not Supported - Failure' , async () => {
        var invalid_destination = 'btp://1234.eos/0x12345678';
        await truffleAssert.reverts(
            bsh_svcV2.transfer(invalid_destination, {from: accounts[1], value: 1000000}),
            "BMCRevertNotExistsBMV"
        ); 
    });
});

contract.skip('BSHService Transfer Token Unit Tests - After Upgrading Contract', () => {
    let bsh_svcV1, bsh_svcV2, bsh_coinV1, bsh_coinV2, bmc, holder, accounts;
    var service = 'Coin/WrappedCoin';
    var native_coin_name = 'PARA';      var fee = 10;
    var _name = 'ICON';  
    var _net = '1234.iconee';   var _from = '0x12345678';   var _value = 999999999999999;                       
    var REPONSE_HANDLE_SERVICE = 2;     var RC_OK = 0;      var RC_ERR = 1;
    var id; 
    before(async () => {
        bmc = await BMC.deployed();
        bsh_coinV1 = await deployProxy(BSHCoinV1);
        bsh_svcV1 = await deployProxy(BSHServiceV1, [bmc.address, bsh_coinV1.address, service, native_coin_name, fee]);
        await bmc.approveService(service);
        await bmc.setBSH(bsh_svcV1.address);
        await bsh_coinV1.setBSHService(bsh_svcV1.address);
        bsh_svcV2 = await upgradeProxy(bsh_svcV1.address, BSHServiceV2);
        bsh_coinV2 = await upgradeProxy(bsh_coinV1.address, BSHCoinV2);
        await bmc.setBSH(bsh_svcV2.address);
        holder = await Holder.deployed();
        accounts = await web3.eth.getAccounts();
        await bmc.addVerifier(_net, accounts[1]);
        await holder.addBSHContract(bsh_svcV2.address, bsh_coinV2.address);
        await bsh_svcV2.register(_name);
        await bmc.transferRequestWithAddress(
            _net, service, _from, holder.address, _name, _value
        );
        id = await bsh_svcV2.coinOf(_name);
    });

    it('Transfer Token to side chain - Not set Approval - Failure', async () => {
        var _to = 'btp://1234.iconee/0x12345678';
        var _value = 1000;
        var balanceBefore = await bsh_coinV2.getBalanceOf(holder.address, _name);
        var _usableBalanceBefore = await bsh_coinV2.balanceOf(holder.address, id);
        await truffleAssert.reverts(
            holder.callTransfer(_name, _value, _to),
            "ERC1155: caller is not owner nor approved"
        ); 
        var balanceAfter = await bsh_coinV2.getBalanceOf(holder.address, _name);
        var _usableBalanceAfter = await bsh_coinV2.balanceOf(holder.address, id);
        assert(
            web3.utils.BN(_usableBalanceAfter).toNumber() == 
                web3.utils.BN(_usableBalanceBefore).toNumber() &&
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() == 0 &&    
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() == 0
        );
    });

    it('Transfer Token to side chain - Insufficient Balance - Failure', async () => {
        var _to = 'btp://1234.iconee/0x12345678';
        var _value = 9999999999999999n;
        var balanceBefore = await bsh_coinV2.getBalanceOf(holder.address, _name);
        var _usableBalanceBefore = await bsh_coinV2.balanceOf(holder.address, id);
        await holder.setApprove(bsh_coinV2.address);
        await truffleAssert.reverts(
            holder.callTransfer(_name, _value, _to),
            "ERC1155: insufficient balance for transfer"
        ); 
        var balanceAfter = await bsh_coinV2.getBalanceOf(holder.address, _name);
        var _usableBalanceAfter = await bsh_coinV2.balanceOf(holder.address, id);
        assert(
            web3.utils.BN(_usableBalanceAfter).toNumber() == 
                web3.utils.BN(_usableBalanceBefore).toNumber() &&
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() == 0 &&    
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() == 0
        );
    });

    it('Transfer Token to side chain - Invalid Token - Failure', async () => {
        var _to = 'btp://1234.iconee/0x12345678';
        var _value = 9999999999999999n;
        var _token = 'EOS';
        await holder.setApprove(bsh_coinV2.address);
        await truffleAssert.reverts(
            holder.callTransfer(_token, _value, _to),
            "InvalidToken"
        ); 
    });

    it('Transfer Token to side chain - Invalid BTP Address format - Failure', async () => {
        var _to = '1234.iconee/0x12345678';
        var _name = 'ICON';
        var balanceBefore = await bsh_coinV2.getBalanceOf(holder.address, _name);
        var _usableBalanceBefore = await bsh_coinV2.balanceOf(holder.address, id);
        await holder.setApprove(bsh_coinV2.address);
        await truffleAssert.reverts(
            holder.callTransfer(_name, 2000, _to),
            "revert"
        ); 
        var balanceAfter = await bsh_coinV2.getBalanceOf(holder.address, _name);
        var _usableBalanceAfter = await bsh_coinV2.balanceOf(holder.address, id);
        assert(
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() == 0 &&
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() == 0 &&
            web3.utils.BN(_usableBalanceAfter).toNumber() == web3.utils.BN(_usableBalanceBefore).toNumber()
        );
    });

    it('Transfer Token to side chain - Amount is Zero - Failure', async () => {
        var _to = '1234.iconee/0x12345678';
        var _name = 'ICON';
        var balanceBefore = await bsh_coinV2.getBalanceOf(holder.address, _name);
        var _usableBalanceBefore = await bsh_coinV2.balanceOf(holder.address, id);
        await holder.setApprove(bsh_coinV2.address);
        await truffleAssert.reverts(
            holder.callTransfer(_name, 0, _to),
            "InvalidAmt"
        ); 
        var balanceAfter = await bsh_coinV2.getBalanceOf(holder.address, _name);
        var _usableBalanceAfter = await bsh_coinV2.balanceOf(holder.address, id);
        assert(
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() == 0 &&
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() == 0 &&
            web3.utils.BN(_usableBalanceAfter).toNumber() == web3.utils.BN(_usableBalanceBefore).toNumber()
        );
    });

    it('Transfer Token to side chain - Charge Fee is Zero - Failure', async () => {
        var _to = '1234.iconee/0x12345678';
        var _name = 'ICON';
        var balanceBefore = await bsh_coinV2.getBalanceOf(holder.address, _name);
        var _usableBalanceBefore = await bsh_coinV2.balanceOf(holder.address, id);
        await holder.setApprove(bsh_coinV2.address);
        await truffleAssert.reverts(
            holder.callTransfer(_name, 10, _to),
            "InvalidAmt"
        ); 
        var balanceAfter = await bsh_coinV2.getBalanceOf(holder.address, _name);
        var _usableBalanceAfter = await bsh_coinV2.balanceOf(holder.address, id);
        assert(
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() == 0 &&
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() == 0 &&
            web3.utils.BN(_usableBalanceAfter).toNumber() == web3.utils.BN(_usableBalanceBefore).toNumber()
        );
    });

    it('Transfer Token to side chain - Invalid Network/Network Not Supported - Failure', async () => {
        var _to = 'btp://1234.eos/0x12345678';
        var _name = 'ICON';
        var balanceBefore = await bsh_coinV2.getBalanceOf(holder.address, _name);
        var _usableBalanceBefore = await bsh_coinV2.balanceOf(holder.address, id);
        await holder.setApprove(bsh_coinV2.address);
        await truffleAssert.reverts(
            holder.callTransfer(_name, 1000, _to),
            "BMCRevertNotExistsBMV"
        ); 
        var balanceAfter = await bsh_coinV2.getBalanceOf(holder.address, _name);
        var _usableBalanceAfter = await bsh_coinV2.balanceOf(holder.address, id);
        assert(
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() == 0 &&
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() == 0 &&
            web3.utils.BN(_usableBalanceAfter).toNumber() == web3.utils.BN(_usableBalanceBefore).toNumber()
        );
    });

    it('Transfer Token to side chain - Success', async () => {
        var _to = 'btp://1234.iconee/0x12345678';
        var _value = 1000;
        var balanceBefore = await bsh_coinV2.getBalanceOf(holder.address, _name);
        var _usableBalanceBefore = await bsh_coinV2.balanceOf(holder.address, id);
        await holder.setApprove(bsh_coinV2.address);
        await holder.callTransfer(_name, _value, _to);
        var balanceAfter = await bsh_coinV2.getBalanceOf(holder.address, _name);
        var _usableBalanceAfter = await bsh_coinV2.balanceOf(holder.address, id);
        assert(
            web3.utils.BN(_usableBalanceAfter).toNumber() == 
                web3.utils.BN(_usableBalanceBefore).toNumber() - 1000 &&
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() == 0 &&    
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() == 1000
        );
    });

    it('BSHService Handle BTP Success Response', async () => {
        var _value = 1000;
        var balanceBefore = await bsh_coinV2.getBalanceOf(holder.address, _name);
        var _usableBalanceBefore = await bsh_coinV2.balanceOf(holder.address, id);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net, service, 0, RC_OK, "");
        var balanceAfter = await bsh_coinV2.getBalanceOf(holder.address, _name);
        var _usableBalanceAfter = await bsh_coinV2.balanceOf(holder.address, id);

        assert(
            web3.utils.BN(balanceBefore._lockedBalance) == _value && 
            web3.utils.BN(balanceAfter._lockedBalance) == 0 &&
            web3.utils.BN(_usableBalanceBefore).toNumber() == web3.utils.BN(_usableBalanceAfter).toNumber()
        );
    });

    it('Transfer Token to side chain - Big amount - Success', async () => {
        var _to = 'btp://1234.iconee/0x12345678';
        var _value = 100000000000000;
        var balanceBefore = await bsh_coinV2.getBalanceOf(holder.address, _name);
        var _usableBalanceBefore = await bsh_coinV2.balanceOf(holder.address, id);
        await holder.setApprove(bsh_coinV2.address);
        await holder.callTransfer(_name, _value, _to);
        var balanceAfter = await bsh_coinV2.getBalanceOf(holder.address, _name);
        var _usableBalanceAfter = await bsh_coinV2.balanceOf(holder.address, id);
        assert(
            web3.utils.BN(_usableBalanceAfter).toNumber() == 
                web3.utils.BN(_usableBalanceBefore).toNumber() - _value &&
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() == 0 &&    
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() == _value
        );
    });

    it('BSHService Handle BTP Error Response - Refundable Contract', async () => {
        var _value = 100000000000000;
        var balanceBefore = await bsh_coinV2.getBalanceOf(holder.address, _name);
        var _usableBalanceBefore = await bsh_coinV2.balanceOf(holder.address, id);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net, service, 1, RC_ERR, "");
        var balanceAfter = await bsh_coinV2.getBalanceOf(holder.address, _name);
        var _usableBalanceAfter = await bsh_coinV2.balanceOf(holder.address, id);

        assert(
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() == _value && 
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() == 0 &&
            web3.utils.BN(_usableBalanceAfter).toNumber() == 
                web3.utils.BN(_usableBalanceBefore).toNumber() + _value &&
            web3.utils.BN(balanceAfter._refundableBalance).toNumber() == 0
        );
    });
});

contract.skip('BSHService Handles BTP Message - Receive PARA Coin Mint Request - Unit Tests - After Upgrading Contract', () => {
    let bmc, bsh_svcV1, bsh_svcV2, bsh_coinV1, bsh_coinV2, accounts, notpayable, refundable;
    var service = 'Coin/WrappedCoin';
    var _net = '1234.iconee';   var _to = 'btp://1234.iconee/0x12345678';
    var _native = 'PARA';       var fee = 10;
    
    before(async () => {
        bmc = await BMC.deployed();
        bsh_coinV1 = await deployProxy(BSHCoinV1);
        bsh_svcV1 = await deployProxy(BSHServiceV1, [bmc.address, bsh_coinV1.address, service, _native, fee]);
        await bmc.approveService(service);
        await bmc.setBSH(bsh_svcV1.address);
        await bsh_coinV1.setBSHService(bsh_svcV1.address);
        bsh_svcV2 = await upgradeProxy(bsh_svcV1.address, BSHServiceV2);
        bsh_coinV2 = await upgradeProxy(bsh_coinV1.address, BSHCoinV2);
        await bmc.setBSH(bsh_svcV2.address);
        notpayable = await NotPayable.deployed();
        refundable = await Refundable.deployed();
        accounts = await web3.eth.getAccounts();
        await bmc.addVerifier(_net, accounts[1]);
        await bsh_svcV2.transfer(_to, {from: accounts[0], value: 100000000});
    });

    it('Receive Request PARA Coin Mint - Invalid address - Failure', async () => {
        var _from = '0x12345678';
        var _value = 1000;
        var _address = '0x1234567890123456789';
        await truffleAssert.reverts( 
            bmc.transferRequestWithStringAddress(
            _net, service, _from, _address, _native, _value),
            'InvalidAddr'
        );
    });

    it('Receive Request PARA Coin Mint - Insufficient Fund - Failure', async () => { 
        var _from = '0x12345678';
        var _value = 1000000000;
        var balanceBefore = await bmc.getBalance(accounts[1]);
        await truffleAssert.reverts( 
            bmc.transferRequestWithAddress(
            _net, service, _from, accounts[1], _native, _value),
            'MintFailed'
        );
        var balanceAfter = await bmc.getBalance(accounts[1]);
        assert(
            web3.utils.BN(balanceAfter).toString() == web3.utils.BN(balanceBefore).toString()
        );
    });

    it('Receive Request PARA Coin Mint - Not Payable Contract Receiver - Failure', async () => {
        var _from = '0x12345678';
        var _value = 1000;
        var balanceBefore = await bmc.getBalance(notpayable.address);
        await truffleAssert.reverts(
            bmc.transferRequestWithAddress(
            _net, service, _from, notpayable.address, _native, _value),
            'MintFailed'
        );
        var balanceAfter = await bmc.getBalance(notpayable.address);
        assert(
            web3.utils.BN(balanceAfter).toNumber() == web3.utils.BN(balanceBefore).toNumber()
        );
    });

    it('Receive Request PARA Coin Mint - Account - Success', async () => { 
        var _from = '0x12345678';
        var _value = 12345;
        var balanceBefore = await bmc.getBalance(accounts[1]);
        await bmc.transferRequestWithAddress(
            _net, service, _from, accounts[1], _native, _value
        );
        var balanceAfter = await bmc.getBalance(accounts[1]);
        assert(
            web3.utils.BN(balanceAfter).toString() == web3.utils.BN(balanceBefore).add(new web3.utils.BN(_value)).toString()
        );
    });

    it('Receive Request PARA Coin Mint - Payable Contract Receiver - Success', async () => { 
        var _from = '0x12345678';
        var _value = 23456;
        var balanceBefore = await bmc.getBalance(refundable.address);
        await bmc.transferRequestWithAddress(
            _net, service, _from, refundable.address, _native, _value
        );
        var balanceAfter = await bmc.getBalance(refundable.address);
        assert(
            web3.utils.BN(balanceAfter).toNumber() == web3.utils.BN(balanceBefore).toNumber() + _value
        );
    });
});

contract.skip('BSHService Handles BTP Message - Receive Token Mint Request - Unit Tests - After Upgrading Contract', () => {
    let bmc, bsh_svcV1, bsh_svcV2, bsh_coinV1, bsh_coinV2, holder, notpayable, accounts;
    var service = 'Coin/WrappedCoin';
    var native_coin_name = 'PARA';  var fee = 10;
    var _name = 'ICON';
    var _net = '1234.iconee';       var _from = '0x12345678';
    var id;
    before(async () => {
        bmc = await BMC.deployed();
        bsh_coinV1 = await deployProxy(BSHCoinV1);
        bsh_svcV1 = await deployProxy(BSHServiceV1, [bmc.address, bsh_coinV1.address, service, native_coin_name, fee]);
        await bmc.approveService(service);
        await bmc.setBSH(bsh_svcV1.address);
        await bsh_coinV1.setBSHService(bsh_svcV1.address);
        bsh_svcV2 = await upgradeProxy(bsh_svcV1.address, BSHServiceV2);
        bsh_coinV2 = await upgradeProxy(bsh_coinV1.address, BSHCoinV2);
        await bmc.setBSH(bsh_svcV2.address);
        holder = await Holder.deployed();
        notpayable = await NotPayable.deployed();
        accounts = await web3.eth.getAccounts();
        await bmc.addVerifier(_net, accounts[1]);
        await holder.addBSHContract(bsh_svcV2.address, bsh_coinV2.address);
        await bsh_svcV2.register(_name);
        id = await bsh_svcV2.coinOf(_name);
    });

    it('Receive Request Token Mint - Invalid format address - Failure', async () => {
        var _value = 1000;
        var _address = '0x1234567890123456789';
        await truffleAssert.reverts( 
            bmc.transferRequestWithStringAddress(
                _net, service, _from, _address, _name, _value),
            'InvalidAddr'    
        );
    });

    it('Receive Request Token Mint - Receiver not ERC1155Holder/Receiver - Failure', async () => {
        var _value = 1000;
        var balanceBefore = await bsh_coinV2.balanceOf(notpayable.address, id);
        await truffleAssert.reverts( 
            bmc.transferRequestWithAddress(
                _net, service, _from, notpayable.address, _name, _value),
            'MintFailed'    
        );
        var balanceAfter = await bsh_coinV2.balanceOf(notpayable.address, id);
        assert(
            web3.utils.BN(balanceAfter).toNumber() == web3.utils.BN(balanceBefore).toNumber()
        );
    });

    it('Receive Request Token Mint - Invalid Token Name - Failure', async () => {
        var _value = 3000;
        var _tokenName = 'Ethereum';
        var invalid_coin_id = await bsh_svcV2.coinOf(_tokenName);
        var balanceBefore = await bsh_coinV2.balanceOf(holder.address, invalid_coin_id);
        await truffleAssert.reverts(
            bmc.transferRequestWithAddress(
            _net, service, _from, holder.address, _tokenName, _value),
            'InvalidToken'
        );
        var balanceAfter = await bsh_coinV2.balanceOf(holder.address, invalid_coin_id);
        assert(
            web3.utils.BN(balanceAfter).toNumber() == web3.utils.BN(balanceBefore).toNumber()
        );
    });

    it('Receive Request Token Mint - ERC1155Holder/Receiver Contract - Success', async () => { 
        var _value = 2500;
        var balanceBefore = await bsh_coinV2.balanceOf(holder.address, id);
        await bmc.transferRequestWithAddress(
            _net, service, _from, holder.address, _name, _value
        );
        var balanceAfter = await bsh_coinV2.balanceOf(holder.address, id);
        assert(
            web3.utils.BN(balanceAfter).toNumber() == web3.utils.BN(balanceBefore).toNumber() + 2500
        );
    });

    it('Receive Request Token Mint - Account - Success', async () => { 
        var _value = 5500;
        var balanceBefore = await bsh_coinV2.balanceOf(accounts[1], id);
        await bmc.transferRequestWithAddress(
            _net, service, _from, accounts[1], _name, _value
        );
        var balanceAfter = await bsh_coinV2.balanceOf(accounts[1], id);
        assert(
            web3.utils.BN(balanceAfter).toNumber() == web3.utils.BN(balanceBefore).toNumber() + 5500
        );
    });
});

contract.skip('BSHService Handle Fee Aggregation - After Upgrading Contract', () => {
    let bsh_svcV1, bsh_svcV2, bsh_coinV1, bsh_coinV2, bmc, holder, accounts;
    var service = 'Coin/WrappedCoin';
    var _native = 'PARA';      var fee = 10;
    var _name1 = 'ICON';    var _name2 = 'BINANCE';     var _name3 = 'ETHEREUM';    var _name4 = 'TRON';
    var _net1 = '1234.iconee';                          var _net2 = '1234.binance';                               
    var _from1 = '0x12345678';                          var _from2 = '0x12345678';
    var _value1 = 999999999999999;                      var _value2 = 999999999999999;
    var _to1 = 'btp://1234.iconee/0x12345678';          var _to2 = 'btp://1234.binance/0x12345678';
    var _txAmt = 10000;                                 var _txAmt1 = 1000000;                              var _txAmt2 = 5000000;
    var RC_OK = 0;                                      var RC_ERR = 1;                                                         
    var _sn1 = 1;                                       var _sn2 = 2;
    var REPONSE_HANDLE_SERVICE = 2;                                           
    before(async () => {
        bmc = await BMC.deployed();
        bsh_coinV1 = await deployProxy(BSHCoinV1);
        bsh_svcV1 = await deployProxy(BSHServiceV1, [bmc.address, bsh_coinV1.address, service, _native, fee]);
        await bmc.approveService(service);
        await bmc.setBSH(bsh_svcV1.address);
        await bsh_coinV1.setBSHService(bsh_svcV1.address);
        bsh_svcV2 = await upgradeProxy(bsh_svcV1.address, BSHServiceV2);
        bsh_coinV2 = await upgradeProxy(bsh_coinV1.address, BSHCoinV2);
        await bmc.setBSH(bsh_svcV2.address);
        holder = await Holder.deployed();
        accounts = await web3.eth.getAccounts();
        await bmc.addVerifier(_net1, accounts[1]);
        await bmc.addVerifier(_net2, accounts[2]);
        await holder.addBSHContract(bsh_svcV2.address, bsh_coinV2.address);
        await bsh_svcV2.register(_name1);
        await bsh_svcV2.register(_name2);
        await bsh_svcV2.register(_name3);
        await bsh_svcV2.register(_name4);
        await bmc.transferRequestWithAddress(
            _net1, service, _from1, holder.address, _name1, _value1
        );
        await bmc.transferRequestWithAddress(
            _net2, service, _from2, holder.address, _name2, _value2
        );
        await bsh_svcV2.transfer(_to1, {from: accounts[0], value: _txAmt});
        await bmc.response(REPONSE_HANDLE_SERVICE, _net1, service, 0, RC_OK, "");
        await holder.setApprove(bsh_coinV2.address);
        await holder.callTransfer(_name1, _txAmt1, _to1);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net1, service, _sn1, RC_OK, "");
        await holder.callTransfer(_name2, _txAmt2, _to2);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net2, service, _sn2, RC_OK, "");
    });

    it('Query Aggregation Fee', async () => {
        var aggregationFee = await bsh_coinV2.getAccumulatedFees();
        assert(
            aggregationFee.length == 5 &&
            aggregationFee[0].coinName == 'PARA' && aggregationFee[0].value == 10 &&
            aggregationFee[1].coinName == 'ICON' && aggregationFee[1].value == 1000 &&
            aggregationFee[2].coinName == 'BINANCE' && aggregationFee[2].value == 5000 &&
            aggregationFee[3].coinName == 'ETHEREUM' && aggregationFee[3].value == 0 &&
            aggregationFee[4].coinName == 'TRON' && aggregationFee[4].value == 0
        );
    });

    it('BSHService Handle Gather Fee Request - Not from BMC - Failure', async () => {
        var FA1Before = await bsh_svcV2.getAggregationFeeOf(_native);     //  state Aggregation Fee of each type of Coins
        var FA2Before = await bsh_svcV2.getAggregationFeeOf(_name1);
        var FA3Before = await bsh_svcV2.getAggregationFeeOf(_name2);
        await truffleAssert.reverts( 
            bsh_svcV2.handleFeeGathering(_to1, service, {from: accounts[1]}),
            'Unauthorized'    
        );
        var FA1After = await bsh_svcV2.getAggregationFeeOf(_native);
        var FA2After = await bsh_svcV2.getAggregationFeeOf(_name1);
        var FA3After = await bsh_svcV2.getAggregationFeeOf(_name2);
        var fees = await bsh_svcV2.getFees(_sn2 + 1);     //  get pending Aggregation Fee list
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
    it('BSHService Handle Gather Fee Request - Success', async () => {
        var FA1Before = await bsh_svcV2.getAggregationFeeOf(_native);     //  state Aggregation Fee of each type of Coins
        var FA2Before = await bsh_svcV2.getAggregationFeeOf(_name1);
        var FA3Before = await bsh_svcV2.getAggregationFeeOf(_name2);
        await bmc.gatherFee(_to1, service);
        var FA1After = await bsh_svcV2.getAggregationFeeOf(_native);
        var FA2After = await bsh_svcV2.getAggregationFeeOf(_name1);
        var FA3After = await bsh_svcV2.getAggregationFeeOf(_name2);
        var fees = await bsh_svcV2.getFees(_sn2 + 1);     //  get pending Aggregation Fee list
        assert(
            FA1Before == Math.floor(_txAmt / 1000) && 
            FA2Before == Math.floor(_txAmt1 / 1000) &&
            FA3Before == Math.floor(_txAmt2 / 1000) &&
            FA1After == 0 && FA2After == 0 && FA3After == 0 && 
            fees[0].coinName == _native && fees[0].value == Math.floor(_txAmt / 1000) &&
            fees[1].coinName == _name1 && fees[1].value == Math.floor(_txAmt1 / 1000) &&
            fees[2].coinName == _name2 && fees[2].value == Math.floor(_txAmt2 / 1000)
        );
    });

    it('BSHService Handle Gather Fee Successful Response', async () => {
        var feesBefore = await bsh_svcV2.getFees(_sn2 + 1);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net1, service, _sn2+1, RC_OK, "");
        var feesAfter = await bsh_svcV2.getFees(_sn2 + 1);
        assert(
            feesBefore.length == 3 &&
            feesBefore[0].coinName == _native && feesBefore[0].value == Math.floor(_txAmt / 1000) &&
            feesBefore[1].coinName == _name1 && feesBefore[1].value == Math.floor(_txAmt1 / 1000) &&
            feesBefore[2].coinName == _name2 && feesBefore[2].value == Math.floor(_txAmt2 / 1000) &&
            feesAfter.length == 0
        );
    });

    it('BSHService Handle Gather Fee Error Response', async () => {
        var _amt1 = 2000000;                    var _amt2 = 6000000;
        var _sn3 = _sn2 + 2;                    var _sn4 = _sn2 + 3;
        await holder.callTransfer(_name1, _amt1, _to1);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net1, service, _sn3, RC_OK, "");
        await holder.callTransfer(_name2, _amt2, _to2);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net2, service, _sn4, RC_OK, "");
        await bmc.gatherFee(_to1, service);

        var FA1Before = await bsh_svcV2.getAggregationFeeOf(_name1);
        var FA2Before = await bsh_svcV2.getAggregationFeeOf(_name2);
        var feesBefore = await bsh_svcV2.getFees(_sn2 + 4);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net1, service, _sn2+4, RC_ERR, "");
        var FA1After = await bsh_svcV2.getAggregationFeeOf(_name1);
        var FA2After = await bsh_svcV2.getAggregationFeeOf(_name2);
        var feesAfter = await bsh_svcV2.getFees(_sn2 + 4);
        assert(
            feesBefore.length == 2 &&
            feesBefore[0].coinName == _name1 && feesBefore[0].value == Math.floor(_amt1 / 1000) &&
            feesBefore[1].coinName == _name2 && feesBefore[1].value == Math.floor(_amt2 / 1000) &&
            FA1Before == 0 && FA2Before == 0 &&
            feesAfter.length == 0 &&
            FA1After == Math.floor(_amt1 / 1000) && FA2After == Math.floor(_amt2 / 1000)
        );
    });
});

contract.skip('BSHCoin Basic Unit Tests - After Upgrading Contract', () => {
    let bmc, bsh_svcV1, bsh_svcV2, bsh_coinV1, bsh_coinV2, notpayable, accounts;
    var service = 'Coin/WrappedCoin';
    var native_coin_name = 'PARA';  var fee = 10;
    var _name = 'ICON';     _to = 'btp://1234.iconee/0x12345678';   _net = '1234.iconee'   
    var id;
    before(async () => {
        bmc = await BMC.deployed();
        bsh_coinV1 = await deployProxy(BSHCoinV1);
        bsh_svcV1 = await deployProxy(BSHServiceV1, [bmc.address, bsh_coinV1.address, service, native_coin_name, fee]);
        accounts = await web3.eth.getAccounts();
        await bmc.approveService(service);
        await bmc.setBSH(bsh_svcV1.address);
        await bmc.addVerifier(_net, accounts[1]);
        await bsh_coinV1.setBSHService(bsh_svcV1.address);
        bsh_svcV2 = await upgradeProxy(bsh_svcV1.address, BSHServiceV2);
        bsh_coinV2 = await upgradeProxy(bsh_coinV1.address, BSHCoinV2);
        await bmc.setBSH(bsh_svcV2.address);
        holder = await Holder.deployed();
        notpayable = await NotPayable.deployed();
        await holder.addBSHContract(bsh_svcV2.address, bsh_coinV2.address);
        await bsh_svcV2.register(_name);
        id = await bsh_svcV2.coinOf(_name);
    });

    it('BSHCoin receives Mint Token Request - From BSHService Contract - Success', async () => { 
        var _value = 2500;
        var balanceBefore = await bsh_coinV2.balanceOf(holder.address, id);
        await bsh_svcV2.mint(holder.address, id, _value);
        var balanceAfter = await bsh_coinV2.balanceOf(holder.address, id);
        assert(
            web3.utils.BN(balanceAfter).toNumber() == web3.utils.BN(balanceBefore).toNumber() + 2500
        );
    });

    // it('BSHCoin receives Mint Token Request - From Contract Owner - Success', async () => { 
    //     var _value = 5500;
    //     var balanceBefore = await bsh_coinV2.balanceOf(holder.address, coin.id);
    //     await bsh_coinV2.mint(holder.address, coin.id, _value);
    //     var balanceAfter = await bsh_coinV2.balanceOf(holder.address, coin.id);
    //     assert(
    //         web3.utils.BN(balanceAfter).toNumber() == web3.utils.BN(balanceBefore).toNumber() + 5500
    //     );
    // });

    it('BSHCoin receives Mint Token Request - Not From BSHService Contract Nor Owner - Failure', async () => { 
        var _value = 30000;
        var balanceBefore = await bsh_coinV2.balanceOf(holder.address, id);
        await truffleAssert.reverts( 
            bsh_coinV2.refund(holder.address, id, _value, false, {from: accounts[1]}),
            'Unauthorized'    
        );
        var balanceAfter = await bsh_coinV2.balanceOf(holder.address, id);
        assert(
            web3.utils.BN(balanceAfter).toNumber() == web3.utils.BN(balanceBefore).toNumber()
        );
    });

    it('BSHCoin receives Mint Token Request - Mint Token to an Account - Success', async () => { 
        var _value = 5500;
        var balanceBefore = await bsh_coinV2.balanceOf(accounts[1], id);
        await bsh_svcV2.mint(accounts[1], id, _value);
        var balanceAfter = await bsh_coinV2.balanceOf(accounts[1], id);
        assert(
            web3.utils.BN(balanceAfter).toNumber() == web3.utils.BN(balanceBefore).toNumber() + 5500
        );
    });

    it('BSHCoin receives Mint Token Request - Mint Token to a Non ERC1155Holder/Receiver Contract - Failure', async () => { 
        var _value = 5500;
        var balanceBefore = await bsh_coinV2.balanceOf(notpayable.address, id);
        await truffleAssert.reverts( 
            bsh_svcV2.mint(notpayable.address, id, _value),
            'ERC1155: transfer to non ERC1155Receiver implementer'    
        );
        var balanceAfter = await bsh_coinV2.balanceOf(notpayable.address, id);
        assert(
            web3.utils.BN(balanceAfter).toNumber() == web3.utils.BN(balanceBefore).toNumber()
        );
    });

    it('BSHCoin receives Burn Token Request - From BSHService Contract - Success', async () => { 
        var _value = 1500;
        await holder.setApprove(bsh_coinV2.address);
        await holder.callTransfer(_name, _value, _to);
        var balanceBefore = await bsh_coinV2.balanceOf(bsh_coinV2.address, id);
        await bsh_svcV2.burn(id, _value);
        var balanceAfter = await bsh_coinV2.balanceOf(bsh_coinV2.address, id);
        assert(
            web3.utils.BN(balanceAfter).toNumber() == web3.utils.BN(balanceBefore).toNumber() - 1500
        );
    });

    // it('BSHCoin receives Burn Token Request - From Contract Owner - Success', async () => { 
    //     var _value = 1000;
    //     var balanceBefore = await bsh_coinV2.balanceOf(holder.address, id);
    //     await bsh_coinV2.burn(holder.address, id, _value);
    //     var balanceAfter = await bsh_coinV2.balanceOf(holder.address, id);
    //     assert(
    //         web3.utils.BN(balanceAfter).toNumber() == web3.utils.BN(balanceBefore).toNumber() - 1000
    //     );
    // });

    it('BSHCoin receives Burn Token Request - Not From BSHService Contract Nor Owner - Failure', async () => { 
        var _value = 30000;
        var balanceBefore = await bsh_coinV2.balanceOf(bsh_coinV2.address, id);
        await truffleAssert.reverts( 
            bsh_coinV2.burn(id, _value, {from: accounts[1]}),
            'Unauthorized'    
        );
        var balanceAfter = await bsh_coinV2.balanceOf(bsh_coinV2.address, id);
        assert(
            web3.utils.BN(balanceAfter).toNumber() == web3.utils.BN(balanceBefore).toNumber()
        );
    });

    it('BSHCoin receives Burn Token Request - Exceed Amount - Success', async () => { 
        var _value = 6000;
        var balanceBefore = await bsh_coinV2.balanceOf(bsh_coinV2.address, id);
        await truffleAssert.reverts( 
            bsh_svcV2.burn(id, _value),
            'ERC1155: burn amount exceeds balance'    
        );
        var balanceAfter = await bsh_coinV2.balanceOf(bsh_coinV2.address, id);
        assert(
            web3.utils.BN(balanceAfter).toNumber() == web3.utils.BN(balanceBefore).toNumber()
        );
    });

    it('Contract Owner Transfer - Success', async () => {
        var oldOwner = await bsh_coinV2.owner();
        await bsh_coinV2.transferOwnership(accounts[1]);
        var newOwner = await bsh_coinV2.owner();
        assert(
            oldOwner == accounts[0] && newOwner == accounts[1]
        );
    }); 

    // it('BSHCoin receives Mint Token Request - Old Owner - Failure', async () => { 
    //     var _value = 30000;
    //     var balanceBefore = await bsh_coinV2.balanceOf(holder.address, coin.id);
    //     await truffleAssert.reverts( 
    //         bsh_coinV2.mint(holder.address, coin.id, _value),
    //         'Unauthorized'    
    //     );
    //     var balanceAfter = await bsh_coinV2.balanceOf(holder.address, coin.id);
    //     assert(
    //         web3.utils.BN(balanceAfter).toNumber() == web3.utils.BN(balanceBefore).toNumber()
    //     );
    // });

    // it('BSHCoin receives Mint Token Request - New Owner - Success', async () => { 
    //     var _value = 30000;
    //     var balanceBefore = await bsh_coinV2.balanceOf(holder.address, coin.id);
    //     await bsh_coinV2.mint(holder.address, coin.id, _value, {from: accounts[1]}); 
    //     var balanceAfter = await bsh_coinV2.balanceOf(holder.address, coin.id);
    //     assert(
    //         web3.utils.BN(balanceAfter).toNumber() == web3.utils.BN(balanceBefore).toNumber() + 30000
    //     );
    // });
});


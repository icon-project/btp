const MockBSHPeriphery = artifacts.require("MockBSHPeriphery");
const BSHPeriphery = artifacts.require("BSHPeriphery");
const BSHCore = artifacts.require("BSHCore");
const BMC = artifacts.require("MockBMC");
const Holder = artifacts.require("Holder");
const NotPayable = artifacts.require("NotPayable");
const NonRefundable = artifacts.require("NonRefundable");
const Refundable = artifacts.require("Refundable");
const { assert } = require('chai');
const truffleAssert = require('truffle-assertions');

contract('PRA BSHCore Query and Management', (accounts) => {
    let bsh_core, bsh_perif;          var _uri = 'https://github.com/icon-project/btp'
    var _native = 'PARA';                       var _fee = 10; 

    before(async () => {
        bmc = await BMC.new('1234.pra');
        bsh_core = await BSHCore.new();
        bsh_perif = await BSHPeriphery.new();
        await bsh_core.initialize(_uri, _native, _fee);
    });

    it(`Scenario 1: Should allow contract's owner to register a new coin`, async () => {
        var _name = "ICON";
        await bsh_core.register(_name);
        output = await bsh_core.coinNames();
        assert(
            output[0] === _native && output[1] === 'ICON'
        );
    });
    
    it('Scenario 2: Should not allow arbitrary client to register a new coin', async () => {   
        var _name = "TRON";
        await truffleAssert.reverts(
            bsh_core.register(_name, {from: accounts[1]}),
            "Ownable: caller is not the owner"
        );
    }); 

    it('Scenario 3: Should revert when contract owner registers an existed coin', async () => {
        var _name = "ICON";
        await truffleAssert.reverts(
            bsh_core.register(_name),
            "ExistToken"
        );
    }); 

    it('Scenario 4: Should allow contract owner to update BSHPeriphery contract', async () => {
        await bsh_core.updateBSHPeriphery(bsh_perif.address);
    });

    it('Scenario 5: Should revert when arbitrary client updates BSHPeriphery contract', async () => {
        await truffleAssert.reverts(
            bsh_core.updateBSHPeriphery(bsh_perif.address, {from: accounts[1]}),
            "Ownable: caller is not the owner"
        );
    });

    it('Scenario 6: Should allow contract owner to update a new URI', async () => {
        var new_uri = 'https://1234.iconee/'
        await bsh_core.updateUri(new_uri);
    });

    it('Scenario 7: Should revert when arbitrary client update a new URI', async () => {
        var new_uri = 'https://1234.iconee/'
        await truffleAssert.reverts(
            bsh_core.updateUri(new_uri, {from: accounts[1]}),
            "Ownable: caller is not the owner"
        );
    });

    it('Scenario 8: Should receive an id of a given coin name when querying a valid supporting coin', async () => {
        var _name1 = "wBTC";    var _name2 = "Ethereum";
        await bsh_core.register(_name1);
        await bsh_core.register(_name2);

        var _query = "ICON";
        var id = await bmc.hashCoinName(_query);
        var result = await bsh_core.coinId(_query);
        assert(
            web3.utils.BN(result).toString() === web3.utils.BN(id).toString()
        );
    }); 

    it('Scenario 9: Should receive an id = 0 when querying an invalid supporting coin', async () => {
        var _query = "EOS";
        var result = await bsh_core.coinId(_query);
        assert(
            web3.utils.BN(result).toNumber() === 0
        );
    }); 

    it('Scenario 10: Should allow to transfer an ownable role to a new address', async () => {
        var oldOwner = await bsh_core.owner();
        await bsh_core.transferOwnership(accounts[1]);
        var newOwner = await bsh_core.owner();
        assert(
            oldOwner === accounts[0] && newOwner === accounts[1]
        );
    }); 

    it('Scenario 11: Should not allow old owner to register a new coin', async () => {
        var _name3 = "TRON";
        await truffleAssert.reverts(
            bsh_core.register(_name3),
            "Ownable: caller is not the owner"
        );
        output = await bsh_core.coinNames();
        assert(
            output[0] === _native && output[1] === 'ICON' &&
            output[2] === 'wBTC' && output[3] === 'Ethereum'
        );
    });
    
    it('Scenario 12: Should allow new owner to register a new coin', async () => {   
        var _name3 = "TRON";
        await bsh_core.register(_name3, {from: accounts[1]});
        output = await bsh_core.coinNames();
        assert(
            output[0] === _native && output[1] === 'ICON' &&
            output[2] === 'wBTC' && output[3] === 'Ethereum' &&
            output[4] ===  'TRON'
        );
    }); 

    it('Scenario 13: Should allow new owner to update BSHPeriphery contract', async () => {
        await bsh_core.updateBSHPeriphery(bsh_perif.address, {from: accounts[1]});
    });

    it('Scenario 14: Should not allow old owner to update BSHPeriphery contract', async () => {
        await truffleAssert.reverts(
            bsh_core.updateBSHPeriphery(bsh_perif.address, {from: accounts[0]}),
            "Ownable: caller is not the owner"
        );
    });

    it('Scenario 15: Should allow new owner to update the new URI', async () => {
        var new_uri = 'https://1234.iconee/'
        await bsh_core.updateUri(new_uri, {from: accounts[1]});
    });

    it('Scenario 16: Should not allow old owner to update the new URI', async () => {
        var new_uri = 'https://1234.iconee/'
        await truffleAssert.reverts(
            bsh_core.updateUri(new_uri, {from: accounts[0]}),
            "Ownable: caller is not the owner"
        );
    });

});

contract('As a user, I want to send PRA to ICON blockchain', (accounts) => {
    let bsh_perif, bsh_core, bmc, nonrefundable, refundable;
    var service = 'Coin/WrappedCoin';               var _bmcICON = 'btp://1234.iconee/0x1234567812345678';              
    var _net = '1234.iconee';                       var _to = 'btp://1234.iconee/0x12345678';
    var RC_OK = 0;                                  var RC_ERR = 1;    
    var _amt = 5000;                                var deposit = 100000;
    var _native = 'PARA';                           var _fee = 10;                         
    var REPONSE_HANDLE_SERVICE = 2;                 var _uri = 'https://github.com/icon-project/btp';

    before(async () => {
        bsh_perif = await BSHPeriphery.new();
        bsh_core = await BSHCore.new();
        bmc = await BMC.new('1234.pra');
        await bsh_perif.initialize(bmc.address, bsh_core.address, service);
        await bsh_core.initialize(_uri, _native, _fee);
        await bsh_core.updateBSHPeriphery(bsh_perif.address);
        nonrefundable = await NonRefundable.new();
        refundable = await Refundable.new();
        await bmc.setBSH(bsh_perif.address);
        await bmc.approveService(service);
        await bmc.addVerifier(_net, accounts[1]);
        await bmc.addLink(_bmcICON);
    });

    it('Scenario 1: Should revert when transferring native coins to an invalid BTP Address format', async () => {
        var invalid_destination = '1234.iconee/0x12345678';
        await truffleAssert.reverts(
            bsh_core.transfer(invalid_destination, {from: accounts[0], value: 5000}),
            "revert"
        ); 
        bsh_coin_balance = await bsh_core.getBalanceOf(bsh_core.address, _native);
        account_balance = await bsh_core.getBalanceOf(accounts[0], _native);
        assert(
            web3.utils.BN(bsh_coin_balance._usableBalance).toNumber() === 0 &&
            web3.utils.BN(account_balance._lockedBalance).toNumber() === 0
        );
    });

    it('Scenario 2: Should revert when transferring zero coin' , async () => {
        await truffleAssert.reverts(
            bsh_core.transfer(_to, {from: accounts[0], value: 0}),
            "InvalidAmount"
        ); 
    });

    it('Scenario 3: Should revert when charging fee is zero' , async () => {
        await truffleAssert.reverts(
            bsh_core.transfer(_to, {from: accounts[0], value: 10}),
            "InvalidAmount"
        ); 
    });

    it('Scenario 4: Should revert when transferring to an invalid network/not supported network' , async () => {
        var invalid_destination = 'btp://1234.eos/0x12345678';
        await truffleAssert.reverts(
            bsh_core.transfer(invalid_destination, {from: accounts[1], value: 5000}),
            "BMCRevertNotExistsBMV"
        ); 
    });

    it('Scenario 5: Should succeed when Account client transferring a valid native coin to a side chain', async () => {
        var account_balanceBefore = await bsh_core.getBalanceOf(accounts[0], _native);
        var output = await bsh_core.transfer(_to, {from: accounts[0], value: _amt});
        var account_balanceAfter = await bsh_core.getBalanceOf(accounts[0], _native);
        var bsh_coin_balance = await bsh_core.getBalanceOf(bsh_core.address, _native);
        var chargedFee = Math.floor(_amt/ 1000);
        //  TODO: 
        //  - catch emit event Message throwing from BMC contract
        //  - catch emit event TransferStart throwing from BSHPeriphery contract

        // truffleAssert.eventEmitted(output, 'TransferStart', (ev) => {
        //     return ev._from === accounts[0] && ev._to === _to && ev._sn === 0 &&
        //         ev._assetDetails.length === 1 &&
        //         ev._assetDetails[0].coinName === 'PARA' && 
        //         ev._assetDetails[0].value === _amt - chargedFee &&
        //         ev._assetDetails[0].fee === chargedFee
        // });
        assert(
            web3.utils.BN(bsh_coin_balance._usableBalance).toNumber() === _amt &&
            web3.utils.BN(account_balanceBefore._lockedBalance).toNumber() === 0 && 
            web3.utils.BN(account_balanceAfter._lockedBalance).toNumber() === _amt
        );
    });

    it('Scenario 6: Should update locked balance when BSHPeriphery receives a successful response of a recent request', async () => {
        var account_balanceBefore = await bsh_core.getBalanceOf(accounts[0], _native);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net, service, 0, RC_OK, "");
        var account_balanceAfter = await bsh_core.getBalanceOf(accounts[0], _native);
        var fees = await bsh_core.getAccumulatedFees();
        // TODO: catch emit event TransferEnd throwing from BSHService contract
 
        assert(
            fees[0].coinName === _native && 
            Number(fees[0].value) === Math.floor(_amt/ 1000) &&
            web3.utils.BN(account_balanceBefore._lockedBalance).toNumber() === _amt &&
            web3.utils.BN(account_balanceAfter._lockedBalance).toNumber() === 0
        );
    });

    it('Scenario 5: Should succeed when Account client transferring a valid native coin to a side chain', async () => {
        var account_balanceBefore = await bsh_core.getBalanceOf(accounts[0], _native);
        var output = await bsh_core.transfer(_to, {from: accounts[0], value: _amt});
        var account_balanceAfter = await bsh_core.getBalanceOf(accounts[0], _native);
        var bsh_coin_balance = await bsh_core.getBalanceOf(bsh_core.address, _native);
        var chargedFee = Math.floor(_amt / 1000);
        //  TODO: 
        //  - catch emit event Message throwing from BMC contract
        //  - catch emit event TransferStart throwing from BSHPeriphery contract

        // truffleAssert.eventEmitted(output, 'TransferStart', (ev) => {
        //     return ev._from === accounts[0] && ev._to === _to && ev._sn === 1 &&
        //         ev._assetDetails.length === 1 &&
        //         ev._assetDetails[0].coinName === 'PARA' && 
        //         ev._assetDetails[0].value === _amt - chargedFee &&
        //         ev._assetDetails[0].fee === chargedFee
        // });

        assert(
            web3.utils.BN(bsh_coin_balance._usableBalance).toNumber() === 2 * _amt &&
            web3.utils.BN(account_balanceBefore._lockedBalance).toNumber() === 0 && 
            web3.utils.BN(account_balanceAfter._lockedBalance).toNumber() === _amt
        );
    });

    it('Scenario 7: Should succeed to refund when BSHPeriphery receives an error response of a recent request', async () => {
        var account_balanceBefore = await bsh_core.getBalanceOf(accounts[0], _native);
        var bsh_coin_balance_before = await bsh_core.getBalanceOf(bsh_core.address, _native);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net, service, 1, RC_ERR, "");
        var account_balanceAfter = await bsh_core.getBalanceOf(accounts[0], _native);
        var bsh_coin_balance_after = await bsh_core.getBalanceOf(bsh_core.address, _native);
        // TODO: catch emit event TransferEnd throwing from BSHPeriphery contract

        assert(
            web3.utils.BN(account_balanceBefore._lockedBalance).toNumber() === _amt && 
            web3.utils.BN(account_balanceAfter._lockedBalance).toNumber() === 0 &&
            web3.utils.BN(account_balanceAfter._refundableBalance).toNumber() === 0 && 
            web3.utils.BN(bsh_coin_balance_before._usableBalance).toNumber() === 2 * _amt &&
            web3.utils.BN(bsh_coin_balance_after._usableBalance).toNumber() === _amt
        );
    });

    it('Scenario 8: Should succeed when Non-refundable contract transferring a valid native coin to a side chain', async () => {
        await nonrefundable.deposit({from: accounts[2], value: deposit});
        var contract_balanceBefore = await bsh_core.getBalanceOf(nonrefundable.address, _native);
        var bsh_coin_balance_before = await bsh_core.getBalanceOf(bsh_core.address, _native);
        await nonrefundable.transfer(bsh_core.address, _to, _amt);
        var contract_balanceAfter = await bsh_core.getBalanceOf(nonrefundable.address, _native);
        var bsh_coin_balance_after = await bsh_core.getBalanceOf(bsh_core.address, _native);
        //  TODO: 
        //  - emit event Message throwing from BMC contract
        //  - emit event TransferStart throwing from BSHPeriphery contract
        
        assert(
            web3.utils.BN(contract_balanceBefore._usableBalance).toNumber() === 
                web3.utils.BN(contract_balanceAfter._usableBalance).toNumber() + _amt &&
            web3.utils.BN(contract_balanceBefore._lockedBalance).toNumber() === 0 && 
            web3.utils.BN(contract_balanceAfter._lockedBalance).toNumber() === _amt &&
            web3.utils.BN(bsh_coin_balance_before._usableBalance).toNumber() === _amt &&
            web3.utils.BN(bsh_coin_balance_after._usableBalance).toNumber() === 2 * _amt
        );
    });

    it(`Scenario 9: Should issue refundable balance when BSHPeriphery receives an error response of a recent request and fails to refund coins back to Non-refundable contract`, async () => {
        var contract_balanceBefore = await bsh_core.getBalanceOf(nonrefundable.address, _native);
        var bsh_coin_balance_before = await bsh_core.getBalanceOf(bsh_core.address, _native);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net, service, 2, RC_ERR, "");
        var contract_balanceAfter = await bsh_core.getBalanceOf(nonrefundable.address, _native);
        var bsh_coin_balance_after = await bsh_core.getBalanceOf(bsh_core.address, _native);
        // TODO: catch emit event TransferEnd throwing from BSHService contract    
        assert(
            web3.utils.BN(contract_balanceBefore._lockedBalance).toNumber() === _amt && 
            web3.utils.BN(contract_balanceAfter._lockedBalance).toNumber() === 0 &&
            web3.utils.BN(contract_balanceBefore._usableBalance).toNumber() === 
                web3.utils.BN(contract_balanceAfter._usableBalance).toNumber() &&
            web3.utils.BN(contract_balanceAfter._refundableBalance).toNumber() === _amt &&
            web3.utils.BN(bsh_coin_balance_before._usableBalance).toNumber() === 
                web3.utils.BN(bsh_coin_balance_after._usableBalance).toNumber()
        );
    });

    it('Scenario 10: Should succeed when Refundable contract transferring a valid native coin to a side chain', async () => {
        await refundable.deposit({from: accounts[2], value: deposit});
        var contract_balanceBefore = await bsh_core.getBalanceOf(refundable.address, _native);
        var bsh_coin_balance_before = await bsh_core.getBalanceOf(bsh_core.address, _native);
        await refundable.transfer(bsh_core.address, _to, _amt);
        var contract_balanceAfter = await bsh_core.getBalanceOf(refundable.address, _native);
        var bsh_coin_balance_after = await bsh_core.getBalanceOf(bsh_core.address, _native);
        //  TODO: 
        //  - emit event Message throwing from BMC contract
        //  - emit event TransferStart throwing from BSHService contract

        assert(
            web3.utils.BN(contract_balanceBefore._usableBalance).toNumber() === 
                web3.utils.BN(contract_balanceAfter._usableBalance).toNumber() + _amt &&
            web3.utils.BN(contract_balanceBefore._lockedBalance).toNumber() === 0 && 
            web3.utils.BN(contract_balanceAfter._lockedBalance).toNumber() === _amt &&
            web3.utils.BN(bsh_coin_balance_after._usableBalance).toNumber() ===
                web3.utils.BN(bsh_coin_balance_before._usableBalance).toNumber() + _amt
        );
    });

    it('Scenario 11: Should succeed to refund when BSHPeriphery receives an error response of a recent request', async () => {
        var contract_balanceBefore = await bsh_core.getBalanceOf(refundable.address, _native);
        var bsh_coin_balance_before = await bsh_core.getBalanceOf(bsh_core.address, _native);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net, service, 3, RC_ERR, "");
        var contract_balanceAfter = await bsh_core.getBalanceOf(refundable.address, _native);
        var bsh_coin_balance_after = await bsh_core.getBalanceOf(bsh_core.address, _native);
        // TODO: catch emit event TransferEnd throwing from BSHService contract

        assert(
            web3.utils.BN(contract_balanceBefore._lockedBalance).toNumber() === _amt && 
            web3.utils.BN(contract_balanceAfter._lockedBalance).toNumber() === 0 &&
            web3.utils.BN(contract_balanceAfter._usableBalance).toNumber() === 
                web3.utils.BN(contract_balanceBefore._usableBalance).toNumber() + _amt &&
            web3.utils.BN(contract_balanceAfter._refundableBalance).toNumber() === 0 &&
            web3.utils.BN(bsh_coin_balance_before._usableBalance).toNumber() ===
                web3.utils.BN(bsh_coin_balance_after._usableBalance).toNumber() + _amt
        );
    });
});

contract('As a user, I want to send ERC1155_ICX to ICON blockchain', (accounts) => {
    let bsh_perif, bsh_core, bmc, holder;
    var service = 'Coin/WrappedCoin';           var _uri = 'https://github.com/icon-project/btp';
    var _native = 'PARA';                       var _fee = 10;     
    var _name = 'ICON';                         var _bmcICON = 'btp://1234.iconee/0x1234567812345678';
    var _net = '1234.iconee';                   var _from = '0x12345678';   var _value = 999999999999999;                       
    var REPONSE_HANDLE_SERVICE = 2;             var RC_OK = 0;              var RC_ERR = 1;
    var id;     

    before(async () => {    
        bsh_perif = await BSHPeriphery.new();
        bsh_core = await BSHCore.new();
        bmc = await BMC.new('1234.pra');
        await bsh_perif.initialize(bmc.address, bsh_core.address, service);
        await bsh_core.initialize(_uri, _native, _fee);
        await bsh_core.updateBSHPeriphery(bsh_perif.address);
        holder = await Holder.new();
        await bmc.setBSH(bsh_perif.address);
        await bmc.approveService(service);
        await bmc.addVerifier(_net, accounts[1]);
        await bmc.addLink(_bmcICON);
        await holder.addBSHContract(bsh_perif.address, bsh_core.address);
        await bsh_core.register(_name);
        await bmc.transferRequestWithAddress(
            _net, service, _from, holder.address, _name, _value
        );
        id = await bsh_core.coinId(_name);
    });

    it('Scenario 1: Should revert when User has not yet set approval for token being transferred out by Operator', async () => {
        var _to = 'btp://1234.iconee/0x12345678';
        var _value = 5000;
        var balanceBefore = await bsh_core.getBalanceOf(holder.address, _name);
        await truffleAssert.reverts(
            holder.callTransfer(_name, _value, _to),
            "ERC1155: caller is not owner nor approved"
        ); 
        var balanceAfter = await bsh_core.getBalanceOf(holder.address, _name);
        assert(
            web3.utils.BN(balanceAfter._usableBalance).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalance).toNumber() &&
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() === 0 &&    
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() === 0
        );
    });

    it(`Scenario 2: Should revert when User has set approval, but user's balance has insufficient amount`, async () => {
        var _to = 'btp://1234.iconee/0x12345678';
        var _value = 9999999999999999n;
        var balanceBefore = await bsh_core.getBalanceOf(holder.address, _name);
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransfer(_name, _value, _to),
            "ERC1155: insufficient balance for transfer"
        ); 
        var balanceAfter = await bsh_core.getBalanceOf(holder.address, _name);
        assert(
            web3.utils.BN(balanceAfter._usableBalance).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalance).toNumber() &&
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() === 0 &&    
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() === 0
        );
    });

    it('Scenario 3: Should revert when User requests to transfer an invalid Token', async () => {
        var _to = 'btp://1234.iconee/0x12345678';
        var _value = 9999999999999999n;
        var _token = 'EOS';
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransfer(_token, _value, _to),
            "unregistered_coin"
        ); 
    });

    it('Scenario 4: Should revert when User transfers Tokens to an invalid BTP Address format', async () => {
        var _to = '1234.iconee/0x12345678';
        var contract_balanceBefore = await bsh_core.getBalanceOf(holder.address, _name);
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransfer(_name, 5000, _to),
            "VM Exception while processing transaction: revert"
        ); 
        var contract_balanceAfter = await bsh_core.getBalanceOf(holder.address, _name);
        var bsh_core_balance = await bsh_core.getBalanceOf(bsh_core.address, _name);
        assert(
            web3.utils.BN(contract_balanceBefore._lockedBalance).toNumber() === 0 &&
            web3.utils.BN(contract_balanceAfter._lockedBalance).toNumber() === 0 &&
            web3.utils.BN(contract_balanceAfter._usableBalance).toNumber() === 
                web3.utils.BN(contract_balanceBefore._usableBalance).toNumber() &&
            web3.utils.BN(bsh_core_balance._usableBalance).toNumber() === 0
        );
    });

    it('Scenario 5: Should revert when User requests to transfer zero Token', async () => {
        var _to = '1234.iconee/0x12345678';
        var balanceBefore = await bsh_core.getBalanceOf(holder.address, _name);
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransfer(_name, 0, _to),
            "InvalidAmount"
        ); 
        var balanceAfter = await bsh_core.getBalanceOf(holder.address, _name);
        assert(
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._usableBalance).toNumber() === 
                web3.utils.BN(balanceAfter._usableBalance).toNumber()
        );
    });

    it('Scenario 6: Should revert when charging fee is zero', async () => {
        var _to = '1234.iconee/0x12345678';
        var _name = 'ICON';
        var balanceBefore = await bsh_core.getBalanceOf(holder.address, _name);
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransfer(_name, 10, _to),
            "InvalidAmount"
        ); 
        var balanceAfter = await bsh_core.getBalanceOf(holder.address, _name);
        assert(
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._usableBalance).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalance).toNumber()
        );
    });

    it('Scenario 7: Should revert when User requests to transfer to an invalid network/Not Supported Network', async () => {
        var _to = 'btp://1234.eos/0x12345678';
        var _name = 'ICON';
        var balanceBefore = await bsh_core.getBalanceOf(holder.address, _name);
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransfer(_name, 1000, _to),
            "BMCRevertNotExistsBMV"
        ); 
        var balanceAfter = await bsh_core.getBalanceOf(holder.address, _name);
        var bsh_core_balance = await bsh_core.getBalanceOf(bsh_core.address, _name);
        assert(
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._usableBalance).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalance).toNumber() &&
            web3.utils.BN(bsh_core_balance._usableBalance).toNumber() === 0
        );
    });

    it('Scenario 8: Should succeed when User sends a valid transferring request', async () => {
        var _to = 'btp://1234.iconee/0x12345678';
        var _value = 1000;
        var balanceBefore = await bsh_core.getBalanceOf(holder.address, _name);
        await holder.setApprove(bsh_core.address);
        await holder.callTransfer(_name, _value, _to);
        var balanceAfter = await bsh_core.getBalanceOf(holder.address, _name);
        var bsh_core_balance = await bsh_core.getBalanceOf(bsh_core.address, _name);
        //  TODO: 
        //  - emit event Message throwing from BMC contract
        //  - emit event TransferStart throwing from BSHService contract

        assert(
            web3.utils.BN(balanceAfter._usableBalance).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalance).toNumber() - _value &&
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() === 0 &&    
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() === _value &&
            web3.utils.BN(bsh_core_balance._usableBalance).toNumber() === _value
        );
    });

    it('Scenario 9: Should update locked balance when BSHPeriphery receives a successful response of a recent request', async () => {
        var _value = 1000;
        var contract_balanceBefore = await bsh_core.getBalanceOf(holder.address, _name);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net, service, 0, RC_OK, "");
        var contract_balanceAfter = await bsh_core.getBalanceOf(holder.address, _name);
        var fees = await bsh_core.getAccumulatedFees();
        var bsh_core_balance = await bsh_core.getBalanceOf(bsh_core.address, _name);
        // TODO: catch emit event TransferEnd throwing from BSHService contract
        assert(
            web3.utils.BN(contract_balanceBefore._lockedBalance).toNumber() === _value && 
            web3.utils.BN(contract_balanceAfter._lockedBalance).toNumber() === 0 &&
            web3.utils.BN(contract_balanceBefore._usableBalance).toNumber() === 
                web3.utils.BN(contract_balanceAfter._usableBalance).toNumber() &&
            fees[1].coinName === _name && 
            Number(fees[1].value) === Math.floor(_value / 1000) &&
            web3.utils.BN(bsh_core_balance._usableBalance).toNumber() === Math.floor(_value / 1000)
        );
    });

    it('Scenario 8: Should succeed when User sends a valid transferring request', async () => {
        var _to = 'btp://1234.iconee/0x12345678';
        var _value = 100000000000000;
        var balanceBefore = await bsh_core.getBalanceOf(holder.address, _name);
        var bsh_core_balance_before = await bsh_core.getBalanceOf(bsh_core.address, _name);
        await holder.setApprove(bsh_core.address);
        await holder.callTransfer(_name, _value, _to);
        var balanceAfter = await bsh_core.getBalanceOf(holder.address, _name);
        var bsh_core_balance_after = await bsh_core.getBalanceOf(bsh_core.address, _name);
        //  TODO: 
        //  - emit event Message throwing from BMC contract
        //  - emit event TransferStart throwing from BSHService contract
        assert(
            web3.utils.BN(balanceAfter._usableBalance).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalance).toNumber() - _value &&
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() === 0 &&    
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() === _value &&
            web3.utils.BN(bsh_core_balance_after._usableBalance).toNumber() === 
                web3.utils.BN(bsh_core_balance_before._usableBalance).toNumber() + _value
        );
    });

    it('Scenario 10: Should issue a refund when BSHPeriphery receives an error response of a recent request', async () => {
        var _value = 100000000000000;
        var balanceBefore = await bsh_core.getBalanceOf(holder.address, _name);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net, service, 1, RC_ERR, "");
        var balanceAfter = await bsh_core.getBalanceOf(holder.address, _name);
        // TODO: catch emit event TransferEnd throwing from BSHService contract
        assert(
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() === _value && 
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._usableBalance).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalance).toNumber() + _value &&
            web3.utils.BN(balanceAfter._refundableBalance).toNumber() === 0
        );
    });
});

contract('As a user, I want to receive PRA from ICON blockchain', (accounts) => {
    let bmc, bsh_perif, bsh_core, notpayable, refundable;
    var service = 'Coin/WrappedCoin';       var _bmcICON = 'btp://1234.iconee/0x1234567812345678';
    var _net = '1234.iconee';               var _to = 'btp://1234.iconee/0x12345678';
    var _native = 'PARA';                   var _fee = 10;   
    var RC_ERR = 1;                         var RC_OK = 0;
    var _uri = 'https://github.com/icon-project/btp';

    before(async () => {
        bsh_perif = await BSHPeriphery.new();
        bsh_core = await BSHCore.new();
        bmc = await BMC.new('1234.pra');
        await bsh_perif.initialize(bmc.address, bsh_core.address, service);
        await bsh_core.initialize(_uri, _native, _fee);
        await bsh_core.updateBSHPeriphery(bsh_perif.address);
        notpayable = await NotPayable.new();
        refundable = await Refundable.new();
        await bmc.setBSH(bsh_perif.address);
        await bmc.approveService(service);
        await bmc.addVerifier(_net, accounts[1]);
        await bmc.addLink(_bmcICON);
        await bsh_core.transfer(_to, {from: accounts[0], value: 100000000});
        btpAddr = await bmc.bmcAddress();
    });

    it('Scenario 1: Should emit an error message when receiving address is invalid', async () => {
        var _from = '0x12345678';
        var _value = 1000;
        var _address = '0x1234567890123456789';
        var _msg = await bmc.encodeBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'invalid_address');
        var output = await bmc.transferRequestStringAddress(
            _bmcICON, '',service, 10, _from, _address, _native, _value
        );
        assert(
            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _msg
        );
    });

    it('Scenario 2: Should emit an error message when BSHCore has insufficient funds to transfer', async () => { 
        var _from = '0x12345678';
        var _value = 1000000000;
        var balanceBefore = await bmc.getBalance(accounts[1]);
        var _msg = await bmc.encodeBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'transfer_failed');
        var output = await bmc.transferRequestAddress(
            _bmcICON, '',service, 10, _from, accounts[1], _native, _value
        );
        var balanceAfter = await bmc.getBalance(accounts[1]);
        assert(
            web3.utils.BN(balanceAfter).toString() === web3.utils.BN(balanceBefore).toString() &&
            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _msg
        );
    });

    it(`Scenario 3: Should emit an error message when BSHCore tries to transfer PARA coins to a non-payable contract, but it fails`, async () => {
        var _from = '0x12345678';
        var _value = 1000;
        var balanceBefore = await bmc.getBalance(notpayable.address);
        var _msg = await bmc.encodeBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'transfer_failed');
        var output = await bmc.transferRequestAddress(
            _bmcICON, '', service, 10, _from, notpayable.address, _native, _value
        );
        var balanceAfter = await bmc.getBalance(notpayable.address);
        assert(
            web3.utils.BN(balanceAfter).toNumber() === web3.utils.BN(balanceBefore).toNumber() &&
            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _msg
        );
    });

    it('Scenario 4: Should be able to transfer coins to an account when BSHPeriphery receives a request of transferring coins', async () => { 
        var _from = '0x12345678';
        var _value = 12345;
        var balanceBefore = await bmc.getBalance(accounts[1]);
        var _msg = await bmc.encodeBMCMessage(btpAddr, _bmcICON, service, 10, RC_OK, '');
        var output = await bmc.transferRequestAddress(
            _bmcICON, '', service, 10, _from, accounts[1], _native, _value
        );
        var balanceAfter = await bmc.getBalance(accounts[1]);

        assert(
            web3.utils.BN(balanceAfter).toString() === 
                web3.utils.BN(balanceBefore).add(new web3.utils.BN(_value)).toString() &&
            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _msg    
        );
    });

    it(`Scenario 5: Should be able to transfer coins to a payable contract receiver when BSHPeriphery receives a request of transferring coins`, async () => { 
        var _from = '0x12345678';
        var _value = 23456;
        var balanceBefore = await bmc.getBalance(refundable.address);
        var _msg = await bmc.encodeBMCMessage(btpAddr, _bmcICON, service, 10, RC_OK, '');
        var output = await bmc.transferRequestAddress(
            _bmcICON, '',service, 10, _from, refundable.address, _native, _value
        );
        var balanceAfter = await bmc.getBalance(refundable.address);
        assert(
            web3.utils.BN(balanceAfter).toNumber() === 
                web3.utils.BN(balanceBefore).toNumber() + _value &&
            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _msg    
        );
    });
});

contract('As a user, I want to receive ERC1155_ICX from ICON blockchain', (accounts) => {
    let bmc, bsh_perif, bsh_core, holder, notpayable;
    var service = 'Coin/WrappedCoin';                   var _uri = 'https://github.com/icon-project/btp';
    var _native = 'PARA';                               var _fee = 10;
    var _name = 'ICON';                                 var _bmcICON = 'btp://1234.iconee/0x1234567812345678';
    var _net = '1234.iconee';                           var _from = '0x12345678';           
    var RC_ERR = 1;                                    var RC_OK = 0;          

    before(async () => {
        bsh_perif = await BSHPeriphery.new();
        bsh_core = await BSHCore.new();
        bmc = await BMC.new('1234.pra');
        await bsh_perif.initialize(bmc.address, bsh_core.address, service);
        await bsh_core.initialize(_uri, _native, _fee);
        await bsh_core.updateBSHPeriphery(bsh_perif.address);
        holder = await Holder.new();
        notpayable = await NotPayable.new();
        await bmc.setBSH(bsh_perif.address);
        await bmc.approveService(service);
        await bmc.addVerifier(_net, accounts[1]);
        await bmc.addLink(_bmcICON);
        await holder.addBSHContract(bsh_perif.address, bsh_core.address);
        await bsh_core.register(_name);
        id = await bsh_core.coinId(_name);
        btpAddr = await bmc.bmcAddress();
    });

    it('Scenario 1: Should emit an error message when receiving address is invalid', async () => {
        var _value = 1000;
        var _address = '0x1234567890123456789';
        var _msg = await bmc.encodeBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'invalid_address');
        var output = await bmc.transferRequestStringAddress(
            _bmcICON, '', service, 10, _from, _address, _name, _value
        );
        assert(
            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _msg
        );
    });

    it(`Scenario 2: Should emit an error message when receiving contract does not implement ERC1155Holder/Receiver`, async () => {
        var _value = 1000;
        var balanceBefore = await bsh_core.balanceOf(notpayable.address, id);
        var _msg = await bmc.encodeBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'transfer_failed');
        var output = await bmc.transferRequestAddress(
            _bmcICON, '', service, 10, _from, notpayable.address, _name, _value
        );
        var balanceAfter = await bsh_core.balanceOf(notpayable.address, id);
        assert(
            web3.utils.BN(balanceAfter).toNumber() === web3.utils.BN(balanceBefore).toNumber() &&
            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _msg
        );
    });

    it('Scenario 3: Should emit an error message when BSHPerphery receives a request of invalid token', async () => {
        var _value = 3000;
        var _tokenName = 'Ethereum';
        var invalid_coin_id = await bsh_core.coinId(_tokenName);
        var balanceBefore = await bsh_core.balanceOf(holder.address, invalid_coin_id);
        var _msg = await bmc.encodeBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'unregistered_coin');
        var output = await bmc.transferRequestAddress(
            _bmcICON, '', service, 10, _from, holder.address, _tokenName, _value
        );
        var balanceAfter = await bsh_core.balanceOf(holder.address, invalid_coin_id);
        assert(
            web3.utils.BN(balanceAfter).toNumber() === web3.utils.BN(balanceBefore).toNumber() &&
            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _msg
        );
    });

    it('Scenario 4: Should mint tokens successfully when a receiver is a ERC1155Holder contract', async () => { 
        var _value = 2500;
        var balanceBefore = await bsh_core.balanceOf(holder.address, id);
        var _msg = await bmc.encodeBMCMessage(btpAddr, _bmcICON, service, 10, RC_OK, '');
        var output = await bmc.transferRequestAddress(
            _bmcICON, '', service, 10, _from, holder.address, _name, _value
        );
        var balanceAfter = await bsh_core.balanceOf(holder.address, id);
        assert(
            web3.utils.BN(balanceAfter).toNumber() === 
                web3.utils.BN(balanceBefore).toNumber() + _value &&
            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _msg    
        );
    });

    it('Scenario 5: Should mint tokens successfully when a receiver is an account client', async () => { 
        var _value = 5500;
        var balanceBefore = await bsh_core.balanceOf(accounts[1], id);
        var _msg = await bmc.encodeBMCMessage(btpAddr, _bmcICON, service, 10, RC_OK, '');
        var output = await bmc.transferRequestAddress(
            _bmcICON, '', service, 10, _from, accounts[1], _name, _value
        );
        var balanceAfter = await bsh_core.balanceOf(accounts[1], id);
        assert(
            web3.utils.BN(balanceAfter).toNumber() === 
                web3.utils.BN(balanceBefore).toNumber() + _value &&
            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _msg 
        );
    });
});

contract('BSHs handle Gather Fee Service Requests', (accounts) => {
    let bsh_perif, bsh_core, bmc, holder;
    var service = 'Coin/WrappedCoin';                   var _uri = 'https://github.com/icon-project/btp';
    var _native = 'PARA';                               var _fee = 10;
    var _name1 = 'ICON';    var _name2 = 'BINANCE';     var _name3 = 'ETHEREUM';        var _name4 = 'TRON';                                             
    var _net1 = '1234.iconee';                          var _net2 = '1234.binance';                               
    var _from1 = '0x12345678';                          var _from2 = '0x12345678';
    var _value1 = 999999999999999;                      var _value2 = 999999999999999;
    var _to1 = 'btp://1234.iconee/0x12345678';          var _to2 = 'btp://1234.binance/0x12345678';
    var _txAmt = 10000;                                 var _txAmt1 = 1000000;          var _txAmt2 = 5000000;
    var RC_OK = 0;                                      var RC_ERR = 1;                                                         
    var REPONSE_HANDLE_SERVICE = 2;                     var _bmcICON = 'btp://1234.iconee/0x1234567812345678'; 
    var _sn0 = 0;           var _sn1 = 1;               var _sn2 = 2; 

    before(async () => {
        bsh_perif = await MockBSHPeriphery.new();
        bsh_core = await BSHCore.new();
        bmc = await BMC.new('1234.pra');
        await bsh_perif.initialize(bmc.address, bsh_core.address, service);
        await bsh_core.initialize(_uri, _native, _fee);
        await bsh_core.updateBSHPeriphery(bsh_perif.address);
        holder = await Holder.new();
        btpAddr = await bmc.bmcAddress();
        await bmc.setBSH(bsh_perif.address);
        await bmc.approveService(service);
        await bmc.addVerifier(_net1, accounts[1]);
        await bmc.addVerifier(_net2, accounts[2]);
        await bmc.addLink(_bmcICON);
        await holder.addBSHContract(bsh_perif.address, bsh_core.address);
        await bsh_core.register(_name1);
        await bsh_core.register(_name2);
        await bsh_core.register(_name3);
        await bsh_core.register(_name4);
        await bmc.transferRequestWithAddress(
            _net1, service, _from1, holder.address, _name1, _value1
        );
        await bmc.transferRequestWithAddress(
            _net2, service, _from2, holder.address, _name2, _value2
        );
        await bsh_core.transfer(_to1, {from: accounts[0], value: _txAmt});
        await bmc.response(REPONSE_HANDLE_SERVICE, _net1, service, _sn0, RC_OK, "");
        await holder.setApprove(bsh_core.address);
        await holder.callTransfer(_name1, _txAmt1, _to1);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net1, service, _sn1, RC_OK, "");
        await holder.callTransfer(_name2, _txAmt2, _to2);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net2, service, _sn2, RC_OK, "");
    });

    it('Scenario 1: Should be able to query Aggregation Fee', async () => {
        var aggregationFee = await bsh_core.getAccumulatedFees();
        assert(
            aggregationFee.length === 5 &&
            aggregationFee[0].coinName === 'PARA' && Number(aggregationFee[0].value) === 10 &&
            aggregationFee[1].coinName === 'ICON' && Number(aggregationFee[1].value) === 1000 &&
            aggregationFee[2].coinName === 'BINANCE' && Number(aggregationFee[2].value) === 5000 &&
            aggregationFee[3].coinName === 'ETHEREUM' && Number(aggregationFee[3].value) === 0 &&
            aggregationFee[4].coinName === 'TRON' && Number(aggregationFee[4].value) === 0
        );
    });

    it('Scenario 2: Should revert when receiving a FeeGathering request not from BMCService', async () => {
        var _sn3 = 3
        var FA1Before = await bsh_perif.getAggregationFeeOf(_native);     //  state Aggregation Fee of each type of Coins
        var FA2Before = await bsh_perif.getAggregationFeeOf(_name1);
        var FA3Before = await bsh_perif.getAggregationFeeOf(_name2);
        await truffleAssert.reverts( 
            bsh_perif.handleFeeGathering(_to1, service, {from: accounts[1]}),
            'Unauthorized'    
        );
        var FA1After = await bsh_perif.getAggregationFeeOf(_native);
        var FA2After = await bsh_perif.getAggregationFeeOf(_name1);
        var FA3After = await bsh_perif.getAggregationFeeOf(_name2);
        var fees = await bsh_perif.getFees(_sn3);     //  get pending Aggregation Fee list
        assert(
            web3.utils.BN(FA1Before).toNumber() === web3.utils.BN(FA1After).toNumber() && 
            web3.utils.BN(FA2Before).toNumber() === web3.utils.BN(FA2After).toNumber() &&
            web3.utils.BN(FA3Before).toNumber() === web3.utils.BN(FA3After).toNumber() &&
            fees.length === 0
        );
    });

    //  Before: 
    //      + state Aggregation Fee of each type of Coins are set
    //      + pendingAggregation Fee list is empty
    //  After: 
    //      + all states of Aggregation Fee are push into pendingAggregation Fee list
    //      + state Aggregation Fee of each type of Coins are reset
    it('Scenario 3: Should handle GatherFee request from BMCService contract', async () => {
        var _sn3 = 3;
        var FA1Before = await bsh_perif.getAggregationFeeOf(_native);     //  state Aggregation Fee of each type of Coins
        var FA2Before = await bsh_perif.getAggregationFeeOf(_name1);
        var FA3Before = await bsh_perif.getAggregationFeeOf(_name2);
        var output = await bmc.gatherFee(_to1, service);
        var FA1After = await bsh_perif.getAggregationFeeOf(_native);
        var FA2After = await bsh_perif.getAggregationFeeOf(_name1);
        var FA3After = await bsh_perif.getAggregationFeeOf(_name2);
        var fees = await bsh_perif.getFees(_sn3);     //  get pending Aggregation Fee list
        var _msg = await bmc.encodeTransferCoin(
            btpAddr, _bmcICON, _to1, service, _sn3, bsh_core.address, fees
        );
        //  TODO: catch event TransferStart throwing from BSHService contract

        assert(
            web3.utils.BN(FA1Before).toNumber() === Math.floor(_txAmt / 1000) && 
            web3.utils.BN(FA2Before).toNumber() === Math.floor(_txAmt1 / 1000) &&
            web3.utils.BN(FA3Before).toNumber() === Math.floor(_txAmt2 / 1000) &&
            web3.utils.BN(FA1After).toNumber() === 0 && 
            web3.utils.BN(FA2After).toNumber() === 0 && 
            web3.utils.BN(FA3After).toNumber() === 0 && 
            fees[0].coinName === _native && Number(fees[0].value) === Math.floor(_txAmt / 1000) &&
            fees[1].coinName === _name1 && Number(fees[1].value) === Math.floor(_txAmt1 / 1000) &&
            fees[2].coinName === _name2 && Number(fees[2].value) === Math.floor(_txAmt2 / 1000) &&
            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _msg
        );
    });

    it('Scenario 4: Should reset a pending state when receiving a successful response', async () => {
        var _sn3 = 3;
        var feesBefore = await bsh_perif.getFees(_sn3);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net1, service, _sn3, RC_OK, "");
        var feesAfter = await bsh_perif.getFees(_sn3);
        //  TODO: catch event TransferEnd throwing from BSHService contract
        assert(
            feesBefore.length === 3 &&
            feesBefore[0].coinName === _native && Number(feesBefore[0].value) === Math.floor(_txAmt / 1000) &&
            feesBefore[1].coinName === _name1 && Number(feesBefore[1].value) === Math.floor(_txAmt1 / 1000) &&
            feesBefore[2].coinName === _name2 && Number(feesBefore[2].value) === Math.floor(_txAmt2 / 1000) &&
            feesAfter.length === 0
        );
    });

    it('Scenario 5: Should restore aggregationFA state when receiving an error response', async () => {
        var _sn4 = 4;   var _sn5 = 5;   var _sn6 = 6;
        var _amt1 = 2000000;                    var _amt2 = 6000000;
        await holder.callTransfer(_name1, _amt1, _to1);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net1, service, _sn4, RC_OK, "");
        await holder.callTransfer(_name2, _amt2, _to2);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net2, service, _sn5, RC_OK, "");
        await bmc.gatherFee(_to1, service);

        var FA1Before = await bsh_perif.getAggregationFeeOf(_name1);
        var FA2Before = await bsh_perif.getAggregationFeeOf(_name2);
        var feesBefore = await bsh_perif.getFees(_sn6);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net1, service, _sn6, RC_ERR, "");
        var FA1After = await bsh_perif.getAggregationFeeOf(_name1);
        var FA2After = await bsh_perif.getAggregationFeeOf(_name2);
        var feesAfter = await bsh_perif.getFees(_sn6);
        // TODO: catch event TransferEnd throwing from BSHService contract
        assert(
            feesBefore.length === 2 &&
            feesBefore[0].coinName === _name1 && Number(feesBefore[0].value) === Math.floor(_amt1 / 1000) &&
            feesBefore[1].coinName === _name2 && Number(feesBefore[1].value) === Math.floor(_amt2 / 1000) &&
            web3.utils.BN(FA1Before).toNumber() === 0 && 
            web3.utils.BN(FA2Before).toNumber() === 0 &&
            feesAfter.length === 0 &&
            web3.utils.BN(FA1After).toNumber() === Math.floor(_amt1 / 1000) && 
            web3.utils.BN(FA2After).toNumber() === Math.floor(_amt2 / 1000)
        );
    });
});

contract('As a user, I want to receive multiple Coins/Tokens from ICON blockchain', (accounts) => {
    let bsh_perif, bsh_core, bmc, holder, refundable;
    var service = 'Coin/WrappedCoin';                   var _uri = 'https://github.com/icon-project/btp';
    var _native = 'PARA';                               var _fee = 10;
    var _name1 = 'ICON';    var _name2 = 'BINANCE';     var _name3 = 'ETHEREUM';        var _name4 = 'TRON';                                             
    var _net1 = '1234.iconee';                          var _bmcICON = 'btp://1234.iconee/0x1234567812345678';                                                     
    var RC_OK = 0;                                      var RC_ERR = 1;                 
    var _from1 = '0x12345678';                          var _to = 'btp://1234.iconee/0x12345678';                                                         

    before(async () => {
        bsh_perif = await BSHPeriphery.new();
        bsh_core = await BSHCore.new();
        bmc = await BMC.new('1234.pra');
        await bsh_perif.initialize(bmc.address, bsh_core.address, service);
        await bsh_core.initialize(_uri, _native, _fee);
        await bsh_core.updateBSHPeriphery(bsh_perif.address);
        holder = await Holder.new();
        refundable = await Refundable.new();
        btpAddr = await bmc.bmcAddress();
        await bmc.setBSH(bsh_perif.address);
        await bmc.approveService(service);
        await bmc.addVerifier(_net1, accounts[1]);
        await bmc.addLink(_bmcICON);
        await holder.addBSHContract(bsh_perif.address, bsh_core.address);
        await bsh_core.register(_name1);
        await bsh_core.register(_name2);
        await bsh_core.register(_name3);
        await bsh_core.register(_name4);
        await bsh_core.transfer(_to, {from: accounts[0], value: 10000000});
    });

    it('Scenario 1: Should emit an error message when receiving address is invalid', async () => {
        var _value1 = 1000;     var _value2 = 10000;    var _value3 = 40000;
        var _address = '0x1234567890123456789';
        var _msg = await bmc.encodeBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'invalid_address');
        var output = await bmc.transferBatchStringAddress(
            _bmcICON, '', service, 10, _from1, _address, [[_native, _value1], [_name1, _value2], [_name2, _value3]]
        );

        assert(
            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _msg
        )
    });

    it('Scenario 2: Should emit an error message when BSHPerphery receives a request of invalid token', async () => {
        var _value1 = 1000;     var _value2 = 10000;    var _value3 = 40000;
        var _invalid_token = 'EOS';
        var balance1Before = await bsh_core.getBalanceOf(holder.address, _name1);
        var balance2Before = await bsh_core.getBalanceOf(holder.address, _name2);
        var balance3Before = await bsh_core.getBalanceOf(holder.address, _invalid_token);
        var _msg = await bmc.encodeBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'unregistered_coin');
        var output = await bmc.transferBatchAddress(
            _bmcICON, '', service, 10, _from1, holder.address, [[_name1, _value1], [_name2, _value2], [_invalid_token, _value3]]
        );
        var balance1After = await bsh_core.getBalanceOf(holder.address, _name1);
        var balance2After = await bsh_core.getBalanceOf(holder.address, _name2);
        var balance3After = await bsh_core.getBalanceOf(holder.address, _invalid_token);

        assert(
            web3.utils.BN(balance1Before._usableBalance).toNumber() === 
                web3.utils.BN(balance1After._usableBalance).toNumber() &&
            web3.utils.BN(balance2Before._usableBalance).toNumber() === 
                web3.utils.BN(balance2After._usableBalance).toNumber() &&  
            web3.utils.BN(balance3Before._usableBalance).toNumber() === 
                web3.utils.BN(balance3After._usableBalance).toNumber() &&      
            web3.utils.BN(balance1After._usableBalance).toNumber() === 0 &&
            web3.utils.BN(balance2After._usableBalance).toNumber() === 0 &&
            web3.utils.BN(balance3After._usableBalance).toNumber() === 0 &&

            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _msg
        )
    });

    it('Scenario 3: Should emit an error response when one of requests is failed in TransferBatch', async () => {
        var _value1 = 1000;     var _value2 = 10000;    var _value3 = 20000000;
        var balance1Before = await bsh_core.getBalanceOf(accounts[1], _name1);
        var balance2Before = await bsh_core.getBalanceOf(accounts[1], _name2);
        var balance3Before = await bsh_core.getBalanceOf(accounts[1], _native);
        var _msg = await bmc.encodeBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'transfer_failed');
        var output = await bmc.transferBatchAddress(
            _bmcICON, '', service, 10, _from1, accounts[1], [[_name1, _value1], [_name2, _value2], [_native, _value3]]
        );
        var balance1After = await bsh_core.getBalanceOf(accounts[1], _name1);
        var balance2After = await bsh_core.getBalanceOf(accounts[1], _name2);
        var balance3After = await bsh_core.getBalanceOf(accounts[1], _native);

        assert(
            web3.utils.BN(balance1Before._usableBalance).toNumber() === 
                web3.utils.BN(balance1After._usableBalance).toNumber() &&
            web3.utils.BN(balance2Before._usableBalance).toNumber() === 
                web3.utils.BN(balance2After._usableBalance).toNumber() &&  
            web3.utils.BN(balance3Before._usableBalance).toString() === 
                web3.utils.BN(balance3After._usableBalance).toString() &&      
            web3.utils.BN(balance1After._usableBalance).toNumber() === 0 &&
            web3.utils.BN(balance2After._usableBalance).toNumber() === 0 &&

            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _msg
        )
    });

    it('Scenario 4: Should emit an error response when one of requests is failed in TransferBatch', async () => {
        var _value1 = 1000;     var _value2 = 10000;    var _value3 = 40000;
        var balance1Before = await bsh_core.getBalanceOf(refundable.address, _native);
        var balance2Before = await bsh_core.getBalanceOf(refundable.address, _name1);
        var balance3Before = await bsh_core.getBalanceOf(refundable.address, _name2);
        var _msg = await bmc.encodeBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'transfer_failed');
        var output = await bmc.transferBatchAddress(
            _bmcICON, '', service, 10, _from1, refundable.address, [[_native, _value1], [_name1, _value2], [_name2, _value3]]
        );
        var balance1After = await bsh_core.getBalanceOf(refundable.address, _native);
        var balance2After = await bsh_core.getBalanceOf(refundable.address, _name1);
        var balance3After = await bsh_core.getBalanceOf(refundable.address, _name2);

        assert(
            web3.utils.BN(balance1Before._usableBalance).toNumber() === 
                web3.utils.BN(balance1After._usableBalance).toNumber() &&
            web3.utils.BN(balance2Before._usableBalance).toNumber() === 
                web3.utils.BN(balance2After._usableBalance).toNumber() &&  
            web3.utils.BN(balance3Before._usableBalance).toNumber() === 
                web3.utils.BN(balance3After._usableBalance).toNumber() &&      
            web3.utils.BN(balance1After._usableBalance).toNumber() === 0 &&
            web3.utils.BN(balance2After._usableBalance).toNumber() === 0 &&
            web3.utils.BN(balance3After._usableBalance).toNumber() === 0 &&

            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _msg
        )
    });

    it('Scenario 5: Should emit an error response when one of requests is failed in TransferBatch', async () => {
        var _value1 = 1000;     var _value2 = 10000;    var _value3 = 40000;
        var balance1Before = await bsh_core.getBalanceOf(holder.address, _name1);
        var balance2Before = await bsh_core.getBalanceOf(holder.address, _name2);
        var balance3Before = await bsh_core.getBalanceOf(holder.address, _native);
        var _msg = await bmc.encodeBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'transfer_failed');
        var output = await bmc.transferBatchAddress(
            _bmcICON, '', service, 10, _from1, holder.address, [[_name1, _value1], [_name2, _value2], [_native, _value3]]
        );
        var balance1After = await bsh_core.getBalanceOf(holder.address, _name1);
        var balance2After = await bsh_core.getBalanceOf(holder.address, _name2);
        var balance3After = await bsh_core.getBalanceOf(holder.address, _native);

        assert(
            web3.utils.BN(balance1Before._usableBalance).toNumber() === 
                web3.utils.BN(balance1After._usableBalance).toNumber() &&
            web3.utils.BN(balance2Before._usableBalance).toNumber() === 
                web3.utils.BN(balance2After._usableBalance).toNumber() &&  
            web3.utils.BN(balance3Before._usableBalance).toNumber() === 
                web3.utils.BN(balance3After._usableBalance).toNumber() &&      
            web3.utils.BN(balance1After._usableBalance).toNumber() === 0 &&
            web3.utils.BN(balance2After._usableBalance).toNumber() === 0 &&
            web3.utils.BN(balance3After._usableBalance).toNumber() === 0 &&

            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _msg
        )
    });

    it('Scenario 6: Should succeed in TransferBatch', async () => {
        var _value1 = 1000;     var _value2 = 10000;    var _value3 = 40000;
        var balance1Before = await bsh_core.getBalanceOf(holder.address, _name1);
        var balance2Before = await bsh_core.getBalanceOf(holder.address, _name2);
        var balance3Before = await bsh_core.getBalanceOf(holder.address, _name3);
        var _msg = await bmc.encodeBMCMessage(btpAddr, _bmcICON, service, 10, RC_OK, '');
        var output = await bmc.transferBatchAddress(
            _bmcICON, '', service, 10, _from1, holder.address, [[_name1, _value1], [_name2, _value2], [_name3, _value3]]
        );
        var balance1After = await bsh_core.getBalanceOf(holder.address, _name1);
        var balance2After = await bsh_core.getBalanceOf(holder.address, _name2);
        var balance3After = await bsh_core.getBalanceOf(holder.address, _name3);

        assert(
            web3.utils.BN(balance1Before._usableBalance).toNumber() === 0 &&
            web3.utils.BN(balance2Before._usableBalance).toNumber() === 0 &&
            web3.utils.BN(balance3Before._usableBalance).toNumber() === 0 &&    
            web3.utils.BN(balance1After._usableBalance).toNumber() === _value1 &&
            web3.utils.BN(balance2After._usableBalance).toNumber() === _value2 &&
            web3.utils.BN(balance3After._usableBalance).toNumber() === _value3 &&

            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _msg
        )
    });

    it('Scenario 7: Should succeed in TransferBatch', async () => {
        var _value1 = 1000;     var _value2 = 10000;    var _value3 = 40000;
        var balance1Before = await bsh_core.getBalanceOf(accounts[1], _native);
        var balance2Before = await bsh_core.getBalanceOf(accounts[1], _name2);
        var balance3Before = await bsh_core.getBalanceOf(accounts[1], _name3);
        var _msg = await bmc.encodeBMCMessage(btpAddr, _bmcICON, service, 10, RC_OK, '');
        var output = await bmc.transferBatchAddress(
            _bmcICON, '', service, 10, _from1, accounts[1], [[_native, _value1], [_name2, _value2], [_name3, _value3]]
        );
        var balance1After = await bsh_core.getBalanceOf(accounts[1], _native);
        var balance2After = await bsh_core.getBalanceOf(accounts[1], _name2);
        var balance3After = await bsh_core.getBalanceOf(accounts[1], _name3);

        assert(   
            web3.utils.BN(balance1After._usableBalance).toString() === 
                web3.utils.BN(balance1Before._usableBalance).add(new web3.utils.BN(_value1)).toString() &&
            web3.utils.BN(balance2After._usableBalance).toNumber() === 
                web3.utils.BN(balance2Before._usableBalance).toNumber() + _value2 &&
            web3.utils.BN(balance3After._usableBalance).toNumber() === 
                web3.utils.BN(balance3Before._usableBalance).toNumber() + _value3 &&

            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _msg
        )
    });
});    

contract('As a user, I want to send multiple coins/tokens to ICON blockchain', (accounts) => {
    let bsh_perif, bsh_core, bmc, holder;
    var service = 'Coin/WrappedCoin';           var _uri = 'https://github.com/icon-project/btp';
    var _native = 'PARA';                       var _fee = 10;      
    var _net = '1234.iconee';                   var _from = '0x12345678';   var _value = 999999999999999;                       
    var REPONSE_HANDLE_SERVICE = 2;             var RC_OK = 0;              var RC_ERR = 1;
    var _bmcICON = 'btp://1234.iconee/0x1234567812345678';         
    var _coin1 = 'ICON';    var _coin2 = 'TRON';    var _coin3 = 'BINANCE';
    var initAmt = 100000000;

    before(async () => {    
        bsh_perif = await BSHPeriphery.new();
        bsh_core = await BSHCore.new();
        bmc = await BMC.new('1234.pra');
        await bsh_perif.initialize(bmc.address, bsh_core.address, service);
        await bsh_core.initialize(_uri, _native, _fee);
        await bsh_core.updateBSHPeriphery(bsh_perif.address);
        holder = await Holder.new();
        await bmc.setBSH(bsh_perif.address);
        await bmc.approveService(service);
        await bmc.addVerifier(_net, accounts[1]);
        await bmc.addLink(_bmcICON);
        await holder.addBSHContract(bsh_perif.address, bsh_core.address);
        await bsh_core.register(_coin1);
        await bsh_core.register(_coin2);
        await bsh_core.register(_coin3);
        await bsh_core.transfer('btp://1234.iconee/0x12345678', {from: accounts[0], value: initAmt});
        await holder.deposit({from: accounts[1], value: 100000000});
        await bmc.transferRequestWithAddress(
            _net, service, _from, holder.address, _coin1, _value
        );
        await bmc.transferRequestWithAddress(
            _net, service, _from, holder.address, _coin2, _value
        );
        await bmc.transferRequestWithAddress(
            _net, service, _from, holder.address, _coin3, _value
        );

        await bmc.transferRequestWithAddress(
            _net, service, _from, accounts[1], _coin1, _value
        );
        await bmc.transferRequestWithAddress(
            _net, service, _from, accounts[1], _coin2, _value
        );
        await bmc.transferRequestWithAddress(
            _net, service, _from, accounts[1], _coin3, _value
        );
    });

    it('Scenario 1: Should revert when User has not yet set approval for token being transferred out by Operator', async () => {
        var _to = 'btp://1234.iconee/0x12345678';
        var _coins = [_native, _coin1, _coin2];
        var _values = [1000, 2000, 3000];
        var balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        await truffleAssert.reverts(
            holder.callTransferBatch(bsh_core.address, _coins, _values, _to),
            "ERC1155: caller is not owner nor approved"
        ); 
        var balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        var bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);

        assert(
            web3.utils.BN(balanceAfter._usableBalances[0]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[0]).toNumber() &&
            web3.utils.BN(balanceAfter._usableBalances[1]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[1]).toNumber() &&
            web3.utils.BN(balanceAfter._usableBalances[2]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[2]).toNumber() &&  

            web3.utils.BN(balanceBefore._lockedBalances[0]).toNumber() === 0 &&  
            web3.utils.BN(balanceBefore._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceBefore._lockedBalances[2]).toNumber() === 0 &&  

            web3.utils.BN(balanceAfter._lockedBalances[0]).toNumber() === 0 && 
            web3.utils.BN(balanceAfter._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._lockedBalances[2]).toNumber() === 0 &&

            web3.utils.BN(bsh_core_balance._usableBalances[0]).toNumber() === initAmt &&
            web3.utils.BN(bsh_core_balance._usableBalances[1]).toNumber() === 0 &&
            web3.utils.BN(bsh_core_balance._usableBalances[2]).toNumber() === 0
        );
    });

    it(`Scenario 2: Should revert when User has set approval, but user's balance has insufficient amount`, async () => {
        var _to = 'btp://1234.iconee/0x12345678';
        var _coins = [_native, _coin1, _coin2];
        var _values = [1000, 2000, 9999999999999999n];
        var balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransferBatch(bsh_core.address, _coins, _values, _to),
            "ERC1155: insufficient balance for transfer"
        ); 
        var balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        var bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);

        assert(
            web3.utils.BN(balanceAfter._usableBalances[0]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[0]).toNumber() &&
            web3.utils.BN(balanceAfter._usableBalances[1]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[1]).toNumber() &&
            web3.utils.BN(balanceAfter._usableBalances[2]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[2]).toNumber() &&   

            web3.utils.BN(balanceBefore._lockedBalances[0]).toNumber() === 0 &&  
            web3.utils.BN(balanceBefore._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceBefore._lockedBalances[2]).toNumber() === 0 &&  

            web3.utils.BN(balanceAfter._lockedBalances[0]).toNumber() === 0 && 
            web3.utils.BN(balanceAfter._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._lockedBalances[2]).toNumber() === 0 &&

            web3.utils.BN(bsh_core_balance._usableBalances[0]).toNumber() === initAmt &&
            web3.utils.BN(bsh_core_balance._usableBalances[1]).toNumber() === 0 &&
            web3.utils.BN(bsh_core_balance._usableBalances[2]).toNumber() === 0
        );
    });

    it('Scenario 3: Should revert when User requests to transfer an invalid Token', async () => {
        var _to = 'btp://1234.iconee/0x12345678';
        var invalid_token = 'EOS';
        var _coins = [_native, _coin1, invalid_token];
        var _values = [1000, 2000, 3000];
        var balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransferBatch(bsh_core.address, _coins, _values, _to),
            "unregistered_coin"
        ); 
        var balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        var bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);

        assert(
            web3.utils.BN(balanceAfter._usableBalances[0]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[0]).toNumber() &&
            web3.utils.BN(balanceAfter._usableBalances[1]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[1]).toNumber() &&
            web3.utils.BN(balanceAfter._usableBalances[2]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[2]).toNumber() &&    
            web3.utils.BN(balanceBefore._usableBalances[2]).toNumber() === 0 &

            web3.utils.BN(balanceBefore._lockedBalances[0]).toNumber() === 0 &&  
            web3.utils.BN(balanceBefore._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceBefore._lockedBalances[2]).toNumber() === 0 &&  

            web3.utils.BN(balanceAfter._lockedBalances[0]).toNumber() === 0 && 
            web3.utils.BN(balanceAfter._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._lockedBalances[2]).toNumber() === 0 && 

            web3.utils.BN(bsh_core_balance._usableBalances[0]).toNumber() === initAmt &&
            web3.utils.BN(bsh_core_balance._usableBalances[1]).toNumber() === 0 &&
            web3.utils.BN(bsh_core_balance._usableBalances[2]).toNumber() === 0
        );
    });

    it('Scenario 4: Should revert when User transfers Tokens to an invalid BTP Address format', async () => {
        var _to = '1234.iconee/0x12345678';
        var _coins = [_native, _coin1, _coin2];
        var _values = [1000, 2000, 3000];
        var balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransferBatch(bsh_core.address, _coins, _values, _to),
            "VM Exception while processing transaction: revert"
        ); 
        var balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        var bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);

        assert(
            web3.utils.BN(balanceAfter._usableBalances[0]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[0]).toNumber() &&
            web3.utils.BN(balanceAfter._usableBalances[1]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[1]).toNumber() &&
            web3.utils.BN(balanceAfter._usableBalances[2]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[2]).toNumber() && 

            web3.utils.BN(balanceBefore._lockedBalances[0]).toNumber() === 0 &&  
            web3.utils.BN(balanceBefore._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceBefore._lockedBalances[2]).toNumber() === 0 &&  

            web3.utils.BN(balanceAfter._lockedBalances[0]).toNumber() === 0 && 
            web3.utils.BN(balanceAfter._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._lockedBalances[2]).toNumber() === 0 && 

            web3.utils.BN(bsh_core_balance._usableBalances[0]).toNumber() === initAmt &&
            web3.utils.BN(bsh_core_balance._usableBalances[1]).toNumber() === 0 &&
            web3.utils.BN(bsh_core_balance._usableBalances[2]).toNumber() === 0
        );
    });

    it('Scenario 5: Should revert when User requests to transfer zero Token', async () => {
        var _to = '1234.iconee/0x12345678';
        var _coins = [_native, _coin1, _coin2];
        var _values = [1000, 2000, 0];
        var balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransferBatch(bsh_core.address, _coins, _values, _to),
            "InvalidAmount"
        ); 
        var balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        var bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);

        assert(
            web3.utils.BN(balanceAfter._usableBalances[0]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[0]).toNumber() &&
            web3.utils.BN(balanceAfter._usableBalances[1]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[1]).toNumber() &&
            web3.utils.BN(balanceAfter._usableBalances[2]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[2]).toNumber() &&    

            web3.utils.BN(balanceBefore._lockedBalances[0]).toNumber() === 0 &&  
            web3.utils.BN(balanceBefore._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceBefore._lockedBalances[2]).toNumber() === 0 &&  

            web3.utils.BN(balanceAfter._lockedBalances[0]).toNumber() === 0 && 
            web3.utils.BN(balanceAfter._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._lockedBalances[2]).toNumber() === 0 && 

            web3.utils.BN(bsh_core_balance._usableBalances[0]).toNumber() === initAmt &&
            web3.utils.BN(bsh_core_balance._usableBalances[1]).toNumber() === 0 &&
            web3.utils.BN(bsh_core_balance._usableBalances[2]).toNumber() === 0
        );
    });

    it('Scenario 6: Should revert when charging fee is zero', async () => {
        var _to = '1234.iconee/0x12345678';
        var _coins = [_native, _coin1, _coin2];
        var _values = [1000, 2000, 100];
        var balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransferBatch(bsh_core.address, _coins, _values, _to),
            "InvalidAmount"
        ); 
        var balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        var bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);

        assert(
            web3.utils.BN(balanceAfter._usableBalances[0]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[0]).toNumber() &&
            web3.utils.BN(balanceAfter._usableBalances[1]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[1]).toNumber() &&
            web3.utils.BN(balanceAfter._usableBalances[2]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[2]).toNumber() &&    

            web3.utils.BN(balanceBefore._lockedBalances[0]).toNumber() === 0 &&  
            web3.utils.BN(balanceBefore._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceBefore._lockedBalances[2]).toNumber() === 0 &&  

            web3.utils.BN(balanceAfter._lockedBalances[0]).toNumber() === 0 && 
            web3.utils.BN(balanceAfter._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._lockedBalances[2]).toNumber() === 0 && 

            web3.utils.BN(bsh_core_balance._usableBalances[0]).toNumber() === initAmt &&
            web3.utils.BN(bsh_core_balance._usableBalances[1]).toNumber() === 0 &&
            web3.utils.BN(bsh_core_balance._usableBalances[2]).toNumber() === 0
        );
    });

    it('Scenario 7: Should revert when User requests to transfer to an invalid network/Not Supported Network', async () => {
        var _to = 'btp://1234.eos/0x12345678';
        var _coins = [_native, _coin1, _coin2];
        var _values = [1000, 2000, 3000];
        var balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransferBatch(bsh_core.address, _coins, _values, _to),
            "BMCRevertNotExistsBMV"
        ); 
        var balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        var bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);
        
        assert(
            web3.utils.BN(balanceAfter._usableBalances[0]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[0]).toNumber() &&
            web3.utils.BN(balanceAfter._usableBalances[1]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[1]).toNumber() &&
            web3.utils.BN(balanceAfter._usableBalances[2]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[2]).toNumber() &&    

            web3.utils.BN(balanceBefore._lockedBalances[0]).toNumber() === 0 &&  
            web3.utils.BN(balanceBefore._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceBefore._lockedBalances[2]).toNumber() === 0 &&  

            web3.utils.BN(balanceAfter._lockedBalances[0]).toNumber() === 0 && 
            web3.utils.BN(balanceAfter._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._lockedBalances[2]).toNumber() === 0 && 

            web3.utils.BN(bsh_core_balance._usableBalances[0]).toNumber() === initAmt &&
            web3.utils.BN(bsh_core_balance._usableBalances[1]).toNumber() === 0 &&
            web3.utils.BN(bsh_core_balance._usableBalances[2]).toNumber() === 0
        );
    });

    it('Scenario 8: Should revert when an account client sends an invalid request of transferBatch', async () => {
        var _to = 'btp://1234.eos/0x12345678';
        var _coins = [_native, _native, _native];
        var _values = [1000, 2000, 3000];
        var balanceBefore = await bsh_core.getBalanceOfBatch(accounts[2], _coins);
        await truffleAssert.reverts(
            bsh_core.transferBatch(_coins, _values, _to, {from: accounts[2], value: 6000}),
            "InvalidAmount"
        ); 
        var balanceAfter = await bsh_core.getBalanceOfBatch(accounts[2], _coins);
        var bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);
        
        assert(
            web3.utils.BN(balanceBefore._lockedBalances[0]).toNumber() === 0 &&  
            web3.utils.BN(balanceBefore._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceBefore._lockedBalances[2]).toNumber() === 0 &&  

            web3.utils.BN(balanceAfter._lockedBalances[0]).toNumber() === 0 && 
            web3.utils.BN(balanceAfter._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._lockedBalances[2]).toNumber() === 0 && 

            web3.utils.BN(bsh_core_balance._usableBalances[0]).toNumber() === initAmt &&
            web3.utils.BN(bsh_core_balance._usableBalances[1]).toNumber() === initAmt &&
            web3.utils.BN(bsh_core_balance._usableBalances[2]).toNumber() === initAmt
        );
    });

    it('Scenario 9: Should revert when a contract client sends an invalid request of transferBatch', async () => {
        var _to = 'btp://1234.eos/0x12345678';
        var _coins = [_native, _coin1, _coin2];
        var _values = [1000, 2000];
        var balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransferBatch(bsh_core.address, _coins, _values, _to),
            "InvalidRequest"
        ); 
        var balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        var bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);
        
        assert(
            web3.utils.BN(balanceAfter._usableBalances[0]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[0]).toNumber() &&
            web3.utils.BN(balanceAfter._usableBalances[1]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[1]).toNumber() &&
            web3.utils.BN(balanceAfter._usableBalances[2]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[2]).toNumber() &&    

            web3.utils.BN(balanceBefore._lockedBalances[0]).toNumber() === 0 &&  
            web3.utils.BN(balanceBefore._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceBefore._lockedBalances[2]).toNumber() === 0 &&  

            web3.utils.BN(balanceAfter._lockedBalances[0]).toNumber() === 0 && 
            web3.utils.BN(balanceAfter._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._lockedBalances[2]).toNumber() === 0 && 

            web3.utils.BN(bsh_core_balance._usableBalances[0]).toNumber() === initAmt &&
            web3.utils.BN(bsh_core_balance._usableBalances[1]).toNumber() === 0 &&
            web3.utils.BN(bsh_core_balance._usableBalances[2]).toNumber() === 0
        );
    });

    it('Scenario 10: Should succeed when a contract client sends a valid transferBatch request', async () => {
        var _to = 'btp://1234.iconee/0x12345678';
        var _coins = [_native, _coin1, _coin2];
        var _value1 = 1000;     var _value2 = 2000;     var _value3 = 1000;
        var _values = [_value1, _value2, _value3];
        var balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        await holder.setApprove(bsh_core.address);
        await holder.callTransferBatch(bsh_core.address, _coins, _values, _to);
        var balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        var bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);

        assert(
            web3.utils.BN(balanceAfter._usableBalances[0]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[0]).toNumber() - _value1 &&
            web3.utils.BN(balanceAfter._usableBalances[1]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[1]).toNumber() - _value2 &&
            web3.utils.BN(balanceAfter._usableBalances[2]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[2]).toNumber() - _value3 &&    

            web3.utils.BN(balanceBefore._lockedBalances[0]).toNumber() === 0 &&  
            web3.utils.BN(balanceBefore._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceBefore._lockedBalances[2]).toNumber() === 0 &&  

            web3.utils.BN(balanceAfter._lockedBalances[0]).toNumber() === _value1 && 
            web3.utils.BN(balanceAfter._lockedBalances[1]).toNumber() === _value2 &&
            web3.utils.BN(balanceAfter._lockedBalances[2]).toNumber() === _value3 &&

            web3.utils.BN(bsh_core_balance._usableBalances[0]).toNumber() === initAmt + _value1 &&
            web3.utils.BN(bsh_core_balance._usableBalances[1]).toNumber() === _value2 &&
            web3.utils.BN(bsh_core_balance._usableBalances[2]).toNumber() === _value3
        );
    });

    it('Scenario 11: Should update locked balance when BSHPeriphery receives a successful response of a recent request', async () => {
        var _value1 = 1000;     var _value2 = 2000;     var _value3 = 1000;
        var _coins = [_native, _coin1, _coin2];
        var balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net, service, 1, RC_OK, "");
        var balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        var fees = await bsh_core.getAccumulatedFees();
        var bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);
        // TODO: catch emit event TransferEnd throwing from BSHPeriphery contract
 
        assert(
            web3.utils.BN(balanceBefore._lockedBalances[0]).toNumber() === _value1 && 
            web3.utils.BN(balanceBefore._lockedBalances[1]).toNumber() === _value2 &&
            web3.utils.BN(balanceBefore._lockedBalances[2]).toNumber() === _value3 &&

            web3.utils.BN(balanceAfter._lockedBalances[0]).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._lockedBalances[2]).toNumber() === 0 &&

            web3.utils.BN(balanceBefore._usableBalances[0]).toNumber() === 
                web3.utils.BN(balanceAfter._usableBalances[0]).toNumber() &&
            web3.utils.BN(balanceBefore._usableBalances[1]).toNumber() === 
                web3.utils.BN(balanceAfter._usableBalances[1]).toNumber() &&
            web3.utils.BN(balanceBefore._usableBalances[2]).toNumber() === 
                web3.utils.BN(balanceAfter._usableBalances[2]).toNumber() &&

            fees[0].coinName === _native && Number(fees[0].value) === Math.floor(_value1 / 1000) &&
            fees[1].coinName === _coin1 && Number(fees[1].value) === Math.floor(_value2 / 1000) &&
            fees[2].coinName === _coin2 && Number(fees[2].value) === Math.floor(_value3 / 1000) &&

            web3.utils.BN(bsh_core_balance._usableBalances[0]).toNumber() === initAmt + _value1 &&
            web3.utils.BN(bsh_core_balance._usableBalances[1]).toNumber() === Math.floor(_value2 / 1000) &&
            web3.utils.BN(bsh_core_balance._usableBalances[2]).toNumber() === Math.floor(_value3 / 1000)
        );
    });

    it('Scenario 12: Should succeed when an account client sends a valid transferBatch request', async () => {
        var _to = 'btp://1234.iconee/0x12345678';
        var _coins = [_coin3, _coin1, _coin2];
        var _value1 = 1000;     var _value2 = 2000;     var _value3 = 1000;
        var _values = [_value1, _value2, _value3];
        var balanceBefore = await bsh_core.getBalanceOfBatch(accounts[1], _coins);
        await bsh_core.setApprovalForAll(bsh_core.address, true, {from: accounts[1]});
        await bsh_core.transferBatch(_coins, _values, _to, {from: accounts[1]});
        var balanceAfter = await bsh_core.getBalanceOfBatch(accounts[1], _coins);
        var bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);
        assert(
            web3.utils.BN(balanceAfter._usableBalances[0]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[0]).toNumber() - _value1 &&
            web3.utils.BN(balanceAfter._usableBalances[1]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[1]).toNumber() - _value2 &&
            web3.utils.BN(balanceAfter._usableBalances[2]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[2]).toNumber() - _value3 &&    

            web3.utils.BN(balanceBefore._lockedBalances[0]).toNumber() === 0 &&  
            web3.utils.BN(balanceBefore._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceBefore._lockedBalances[2]).toNumber() === 0 &&  

            web3.utils.BN(balanceAfter._lockedBalances[0]).toNumber() === _value1 && 
            web3.utils.BN(balanceAfter._lockedBalances[1]).toNumber() === _value2 &&
            web3.utils.BN(balanceAfter._lockedBalances[2]).toNumber() === _value3 &&

            web3.utils.BN(bsh_core_balance._usableBalances[0]).toNumber() === _value1 &&
            web3.utils.BN(bsh_core_balance._usableBalances[1]).toNumber() === _value2 + Math.floor(_value2 / 1000) &&
            web3.utils.BN(bsh_core_balance._usableBalances[2]).toNumber() === _value3 + Math.floor(_value3 / 1000)
        );
    });

    it('Scenario 13: Should refund tokens back to account client when BSHPeriphery receives an error response of a recent request', async () => {
        var _value1 = 1000;     var _value2 = 2000;     var _value3 = 1000;
        var _coins = [_coin3, _coin1, _coin2];
        var balanceBefore = await bsh_core.getBalanceOfBatch(accounts[1], _coins);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net, service, 2, RC_ERR, "");
        var balanceAfter = await bsh_core.getBalanceOfBatch(accounts[1], _coins);
        var bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);
        // TODO: catch emit event TransferEnd throwing from BSHPeriphery contract
 
        assert(
            web3.utils.BN(balanceBefore._lockedBalances[0]).toNumber() === _value1 && 
            web3.utils.BN(balanceBefore._lockedBalances[1]).toNumber() === _value2 &&
            web3.utils.BN(balanceBefore._lockedBalances[2]).toNumber() === _value3 &&

            web3.utils.BN(balanceAfter._lockedBalances[0]).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._lockedBalances[2]).toNumber() === 0 &&

            web3.utils.BN(balanceBefore._usableBalances[0]).toNumber() === 
                web3.utils.BN(balanceAfter._usableBalances[0]).toNumber() - _value1 &&
            web3.utils.BN(balanceBefore._usableBalances[1]).toNumber() === 
                web3.utils.BN(balanceAfter._usableBalances[1]).toNumber() - _value2 &&
            web3.utils.BN(balanceBefore._usableBalances[2]).toNumber() === 
                web3.utils.BN(balanceAfter._usableBalances[2]).toNumber() - _value3 &&

            web3.utils.BN(bsh_core_balance._usableBalances[0]).toNumber() === 0 &&
            web3.utils.BN(bsh_core_balance._usableBalances[1]).toNumber() === Math.floor(_value2 / 1000) &&
            web3.utils.BN(bsh_core_balance._usableBalances[2]).toNumber() === Math.floor(_value3 / 1000)
        );
    });

    it('Scenario 14: Should succeed when a contract client sends a valid transferBatch request', async () => {
        var _to = 'btp://1234.iconee/0x12345678';
        var _coins = [_coin3, _coin1, _coin2];
        var _value1 = 1000;     var _value2 = 2000;     var _value3 = 1000;
        var _values = [_value1, _value2, _value3];
        var balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        await holder.setApprove(bsh_core.address);
        await holder.callTransferBatch(bsh_core.address, _coins, _values, _to);
        var balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        var bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);

        assert(
            web3.utils.BN(balanceAfter._usableBalances[0]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[0]).toNumber() - _value1 &&
            web3.utils.BN(balanceAfter._usableBalances[1]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[1]).toNumber() - _value2 &&
            web3.utils.BN(balanceAfter._usableBalances[2]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[2]).toNumber() - _value3 &&    

            web3.utils.BN(balanceBefore._lockedBalances[0]).toNumber() === 0 &&  
            web3.utils.BN(balanceBefore._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceBefore._lockedBalances[2]).toNumber() === 0 &&  

            web3.utils.BN(balanceAfter._lockedBalances[0]).toNumber() === _value1 && 
            web3.utils.BN(balanceAfter._lockedBalances[1]).toNumber() === _value2 &&
            web3.utils.BN(balanceAfter._lockedBalances[2]).toNumber() === _value3 &&

            web3.utils.BN(bsh_core_balance._usableBalances[0]).toNumber() === _value1 &&
            web3.utils.BN(bsh_core_balance._usableBalances[1]).toNumber() === _value2 + Math.floor(_value2 / 1000) &&
            web3.utils.BN(bsh_core_balance._usableBalances[2]).toNumber() === _value3 + Math.floor(_value3 / 1000)
        );
    });

    it('Scenario 15: Should refund tokens back to contract when BSHPeriphery receives an error response of a recent request', async () => {
        var _value1 = 1000;     var _value2 = 2000;     var _value3 = 1000;
        var _coins = [_coin3, _coin1, _coin2];
        var balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net, service, 3, RC_ERR, "");
        var balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        var bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);
        // TODO: catch emit event TransferEnd throwing from BSHPeriphery contract
 
        assert(
            web3.utils.BN(balanceBefore._lockedBalances[0]).toNumber() === _value1 && 
            web3.utils.BN(balanceBefore._lockedBalances[1]).toNumber() === _value2 &&
            web3.utils.BN(balanceBefore._lockedBalances[2]).toNumber() === _value3 &&

            web3.utils.BN(balanceAfter._lockedBalances[0]).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._lockedBalances[2]).toNumber() === 0 &&

            web3.utils.BN(balanceBefore._usableBalances[0]).toNumber() === 
                web3.utils.BN(balanceAfter._usableBalances[0]).toNumber() - _value1 &&
            web3.utils.BN(balanceBefore._usableBalances[1]).toNumber() === 
                web3.utils.BN(balanceAfter._usableBalances[1]).toNumber() - _value2 &&
            web3.utils.BN(balanceBefore._usableBalances[2]).toNumber() === 
                web3.utils.BN(balanceAfter._usableBalances[2]).toNumber() - _value3 &&

            web3.utils.BN(bsh_core_balance._usableBalances[0]).toNumber() === 0 &&
            web3.utils.BN(bsh_core_balance._usableBalances[1]).toNumber() === Math.floor(_value2 / 1000) &&
            web3.utils.BN(bsh_core_balance._usableBalances[2]).toNumber() === Math.floor(_value3 / 1000)
        );
    });
    //  This test is replicated from Scenario 8
    it('Scenario 16: Should succeed when a contract client sends a valid transferBatch request', async () => {
        var _to = 'btp://1234.iconee/0x12345678';
        var _coins = [_native, _coin1, _coin2];
        var _value1 = 1000;     var _value2 = 2000;     var _value3 = 1000;
        var _values = [_value1, _value2, _value3];
        var balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        await holder.setApprove(bsh_core.address);
        await holder.callTransferBatch(bsh_core.address, _coins, _values, _to);
        var balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        var bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);

        assert(
            web3.utils.BN(balanceAfter._usableBalances[0]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[0]).toNumber() - _value1 &&
            web3.utils.BN(balanceAfter._usableBalances[1]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[1]).toNumber() - _value2 &&
            web3.utils.BN(balanceAfter._usableBalances[2]).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalances[2]).toNumber() - _value3 &&    

            web3.utils.BN(balanceBefore._lockedBalances[0]).toNumber() === 0 &&  
            web3.utils.BN(balanceBefore._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceBefore._lockedBalances[2]).toNumber() === 0 &&  

            web3.utils.BN(balanceAfter._lockedBalances[0]).toNumber() === _value1 && 
            web3.utils.BN(balanceAfter._lockedBalances[1]).toNumber() === _value2 &&
            web3.utils.BN(balanceAfter._lockedBalances[2]).toNumber() === _value3 &&

            web3.utils.BN(bsh_core_balance._usableBalances[0]).toNumber() === initAmt + 2 * _value1 &&
            web3.utils.BN(bsh_core_balance._usableBalances[1]).toNumber() === _value2 + Math.floor(_value2 / 1000) &&
            web3.utils.BN(bsh_core_balance._usableBalances[2]).toNumber() === _value3 + Math.floor(_value3 / 1000)
        );
    });

    it('Scenario 17: Should issue one refundable balance to contract when BSHPeriphery receives an error response of a recent request', async () => {
        var _value1 = 1000;     var _value2 = 2000;     var _value3 = 1000;
        var _coins = [_native, _coin1, _coin2];
        var balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net, service, 4, RC_ERR, "");
        var balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        var bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);
        // TODO: catch emit event TransferEnd throwing from BSHPeriphery contract
 
        assert(
            web3.utils.BN(balanceBefore._lockedBalances[0]).toNumber() === _value1 && 
            web3.utils.BN(balanceBefore._lockedBalances[1]).toNumber() === _value2 &&
            web3.utils.BN(balanceBefore._lockedBalances[2]).toNumber() === _value3 &&

            web3.utils.BN(balanceAfter._lockedBalances[0]).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._lockedBalances[2]).toNumber() === 0 &&

            web3.utils.BN(balanceBefore._usableBalances[0]).toNumber() === 
                web3.utils.BN(balanceAfter._usableBalances[0]).toNumber() && 
            web3.utils.BN(balanceBefore._usableBalances[1]).toNumber() === 
                web3.utils.BN(balanceAfter._usableBalances[1]).toNumber() - _value2 &&
            web3.utils.BN(balanceBefore._usableBalances[2]).toNumber() === 
                web3.utils.BN(balanceAfter._usableBalances[2]).toNumber() - _value3 &&

            web3.utils.BN(balanceBefore._refundableBalances[0]).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._refundableBalances[0]).toNumber() === _value1 &&       

            web3.utils.BN(bsh_core_balance._usableBalances[0]).toNumber() === initAmt + 2 * _value1 &&
            web3.utils.BN(bsh_core_balance._usableBalances[1]).toNumber() === Math.floor(_value2 / 1000) &&
            web3.utils.BN(bsh_core_balance._usableBalances[2]).toNumber() === Math.floor(_value3 / 1000)
        );
    });
});    
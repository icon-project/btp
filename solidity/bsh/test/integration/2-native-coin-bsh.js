const MockBSHPeriphery = artifacts.require("MockBSHPeriphery");
const BSHPeriphery = artifacts.require("BSHPeriphery");
const BSHCore = artifacts.require("BSHCore");
const BMC = artifacts.require("MockBMC");
const Holder = artifacts.require("Holder");
const NotPayable = artifacts.require("NotPayable");
const NonRefundable = artifacts.require("NonRefundable");
const Refundable = artifacts.require("Refundable");
const EncodeMsg = artifacts.require("EncodeMessage");
const { assert } = require('chai');
const truffleAssert = require('truffle-assertions');
const rlp = require('rlp');

let toHex = (buf) => { 
    buf = buf.toString('hex');
    if (buf.substring(0, 2) == '0x')
        return buf;
    return '0x' + buf.toString('hex');
};


contract('PRA BSHCore Query and Management', (accounts) => {
    let bsh_core, bsh_perif;                    let _uri = 'https://github.com/icon-project/btp'
    let _native = 'PARA';                       let _fee = 10; 
    let _net = '1234.iconee';                   let _bmcICON = 'btp://1234.iconee/0x1234567812345678'; 
    let service = 'Coin/WrappedCoin';           let REPONSE_HANDLE_SERVICE = 2;         let RC_OK = 0;
    before(async () => {
        bmc = await BMC.new('1234.pra');
        bsh_core = await BSHCore.new();
        bsh_perif = await BSHPeriphery.new();
        encode_msg = await EncodeMsg.new();
        await bsh_core.initialize(_uri, _native, _fee);
        await bsh_perif.initialize(bmc.address, bsh_core.address, service);
        await bmc.addService(service, bsh_perif.address);
        await bmc.addVerifier(_net, accounts[1]);
        await bmc.addLink(_bmcICON);
    });

    it(`Scenario 1: Should allow contract's owner to register a new coin`, async () => {
        let _name = "ICON";
        await bsh_core.register(_name);
        output = await bsh_core.coinNames();
        assert(
            output[0] === _native && output[1] === 'ICON'
        );
    });
    
    it('Scenario 2: Should revert when an arbitrary client tries to register a new coin', async () => {   
        let _name = "TRON";
        await truffleAssert.reverts(
            bsh_core.register.call(_name, {from: accounts[1]}),
            "Unauthorized"
        );
    }); 

    it('Scenario 3: Should revert when contract owner registers an existed coin', async () => {
        let _name = "ICON";
        await truffleAssert.reverts(
            bsh_core.register.call(_name),
            "ExistToken"
        );
    }); 

    it('Scenario 4: Should allow contract owner to update BSHPeriphery contract', async () => {
        await bsh_core.updateBSHPeriphery(bsh_perif.address);
    });

    it('Scenario 5: Should revert when arbitrary client updates BSHPeriphery contract', async () => {
        await truffleAssert.reverts(
            bsh_core.updateBSHPeriphery.call(accounts[2], {from: accounts[1]}),
            "Unauthorized"
        );
    });

    it('Scenario 6: Should revert when contract owner updates BSHPeriphery while this contract has pending requests', async () => {
        let _to = 'btp://1234.iconee/0x12345678';
        await bsh_core.transfer(_to, {from: accounts[0], value: 100000000});
        await truffleAssert.reverts(
            bsh_core.updateBSHPeriphery.call(accounts[2]),
            "HasPendingRequest"
        );
        //  Clear pending request
        let _msg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_OK, "");
        await bmc.receiveResponse(_net, service, 0, _msg);
    });

    it('Scenario 7: Should allow contract owner to update a new URI', async () => {
        let new_uri = 'https://1234.iconee/'
        await bsh_core.updateUri(new_uri);
    });

    it('Scenario 8: Should revert when arbitrary client update a new URI', async () => {
        let new_uri = 'https://1234.iconee/'
        await truffleAssert.reverts(
            bsh_core.updateUri.call(new_uri, {from: accounts[1]}),
            "Unauthorized"
        );
    });

    it('Scenario 9: Should allow contract owner to update fee ratio', async () => {
        let new_fee = 20;
        await bsh_core.setFeeRatio(new_fee);
    });

    it('Scenario 10: Should revert when arbitrary client updates fee ratio', async () => {
        let new_fee = 20;
        await truffleAssert.reverts(
            bsh_core.setFeeRatio.call(new_fee, {from: accounts[1]}),
            "Unauthorized"
        );
    });

    it('Scenario 11: Should revert when Fee Numerator is higher than Fee Denominator', async () => {
        let new_fee = 20000;
        await truffleAssert.reverts(
            bsh_core.setFeeRatio.call(new_fee),
            "InvalidSetting"
        );
    });

    it('Scenario 12: Should receive an id of a given coin name when querying a valid supporting coin', async () => {
        let _name1 = "wBTC";    let _name2 = "Ethereum";
        await bsh_core.register(_name1);
        await bsh_core.register(_name2);

        let _query = "ICON";
        let id = web3.utils.keccak256(_query);
        let result = await bsh_core.coinId(_query);
        assert(
            web3.utils.BN(result).toString() === web3.utils.toBN(id).toString()
        );
    }); 

    it('Scenario 13: Should receive an id = 0 when querying an invalid supporting coin', async () => {
        let _query = "EOS";
        let result = await bsh_core.coinId(_query);
        assert(
            web3.utils.BN(result).toNumber() === 0
        );
    }); 

    it('Scenario 14: Should revert when a non-Owner tries to add a new Owner', async () => {
        let oldList = await bsh_core.getOwners();
        try {
            await bsh_core.addOwner(accounts[1], {from: accounts[2]});
        }catch (err) {
            assert(err, "exited with an error (status 0)");
        }
        let newList = await bsh_core.getOwners();
        assert(
            oldList.length === 1 && oldList[0] === accounts[0] &&
            newList.length === 1 && newList[0] === accounts[0]
        );
    }); 

    it('Scenario 15: Should allow a current Owner to add a new Owner', async () => {
        let oldList = await bsh_core.getOwners();
        await bsh_core.addOwner(accounts[1]);
        let newList = await bsh_core.getOwners();
        assert(
            oldList.length === 1 && oldList[0] === accounts[0] &&
            newList.length === 2 && newList[0] === accounts[0] && newList[1] === accounts[1]
        );
    }); 
    
    it('Scenario 16: Should allow old owner to register a new coin - After adding new Owner', async () => {
        let _name3 = "TRON";
        await bsh_core.register(_name3);
        output = await bsh_core.coinNames();
        assert(
            output[0] === _native && output[1] === 'ICON' &&
            output[2] === 'wBTC' && output[3] === 'Ethereum' &&
            output[4] ===  'TRON'
        );
    });

    it('Scenario 17: Should allow new owner to register a new coin', async () => {   
        let _name3 = "BINANCE";
        await bsh_core.register(_name3, {from: accounts[1]});
        output = await bsh_core.coinNames();
        assert(
            output[0] === _native && output[1] === 'ICON' &&
            output[2] === 'wBTC' && output[3] === 'Ethereum' &&
            output[4] ===  'TRON' && output[5] === 'BINANCE'
        );
    }); 

    it('Scenario 18: Should allow new owner to update BSHPeriphery contract', async () => {
        let newBSHPerif = await BSHPeriphery.new();
        await bsh_core.updateBSHPeriphery(newBSHPerif.address, {from: accounts[1]});
    });

    it('Scenario 19: Should also allow old owner to update BSHPeriphery contract - After adding new Owner', async () => {
        let newBSHPerif = await BSHPeriphery.new();
        await bsh_core.updateBSHPeriphery(newBSHPerif.address, {from: accounts[0]});
    });

    it('Scenario 20: Should allow new owner to update the new URI', async () => {
        let new_uri = 'https://1234.iconee/'
        await bsh_core.updateUri(new_uri, {from: accounts[1]});
    });

    it('Scenario 21: Should also allow old owner to update the new URI - After adding new Owner', async () => {
        let new_uri = 'https://1234.iconee/'
        await bsh_core.updateUri(new_uri, {from: accounts[0]});
    });

    it('Scenario 22: Should allow new owner to update new fee ratio', async () => {
        let new_fee = 30;
        await bsh_core.setFeeRatio(new_fee, {from: accounts[1]});
    });

    it('Scenario 23: Should also allow old owner to update new fee ratio - After adding new Owner', async () => {
        let new_fee = 30;
        await bsh_core.setFeeRatio(new_fee, {from: accounts[0]});
    });

    it('Scenario 24: Should revert when non-Owner tries to remove an Owner', async () => {
        let oldList = await bsh_core.getOwners();
        await truffleAssert.reverts(
            bsh_core.removeOwner.call(accounts[0], {from: accounts[2]}),
            "Unauthorized"
        );
        let newList = await bsh_core.getOwners();
        assert(
            oldList.length === 2 && oldList[0] === accounts[0] && oldList[1] === accounts[1] &&
            newList.length === 2 && newList[0] === accounts[0] && newList[1] === accounts[1]
        );
    });

    it('Scenario 25: Should allow one current Owner to remove another Owner', async () => {
        let oldList = await bsh_core.getOwners();
        await bsh_core.removeOwner(accounts[0], {from: accounts[1]});
        let newList = await bsh_core.getOwners();
        assert(
            oldList.length === 2 && oldList[0] === accounts[0] && oldList[1] === accounts[1] &&
            newList.length === 1 && newList[0] === accounts[1]
        );
    });

    it('Scenario 26: Should revert when the last Owner removes him/herself', async () => {
        let oldList = await bsh_core.getOwners();
        await truffleAssert.reverts(
            bsh_core.removeOwner.call(accounts[1], {from: accounts[1]}),
            "Unable to remove last Owner"
        );
        let newList = await bsh_core.getOwners();
        assert(
            oldList.length === 1 && oldList[0] === accounts[1] &&
            newList.length === 1 && newList[0] === accounts[1]
        );
    });

    it('Scenario 27: Should revert when removed Owner tries to register a new coin', async () => {
        let _name3 = "KYBER";
        await truffleAssert.reverts(
            bsh_core.register.call(_name3),
            'Unauthorized'
        );
        output = await bsh_core.coinNames();
        assert(
            output[0] === _native && output[1] === 'ICON' &&
            output[2] === 'wBTC' && output[3] === 'Ethereum' &&
            output[4] ===  'TRON' && output[5] === 'BINANCE'
        );
    });

    it('Scenario 28: Should revert when removed Owner tries to update BSHPeriphery contract', async () => {
        await truffleAssert.reverts(
            bsh_core.updateBSHPeriphery.call(accounts[3], {from: accounts[0]}),
            'Unauthorized'
        );
    });

    it('Scenario 29: Should revert when removed Owner tries to update the new URI', async () => {
        let new_uri = 'https://1234.iconee/'
        await truffleAssert.reverts(
            bsh_core.updateUri.call(new_uri, {from: accounts[0]}),
            'Unauthorized'
        );
    });

    it('Scenario 30: Should revert when removed Owner tries to update new fee ratio', async () => {
        let new_fee = 30;
        await truffleAssert.reverts(
            bsh_core.setFeeRatio.call(new_fee, {from: accounts[0]}),
            'Unauthorized'
        );
    });
});

contract('As a user, I want to send PRA to ICON blockchain', (accounts) => {
    let bsh_perif, bsh_core, bmc, nonrefundable, refundable;
    let service = 'Coin/WrappedCoin';               let _bmcICON = 'btp://1234.iconee/0x1234567812345678';              
    let _net = '1234.iconee';                       let _to = 'btp://1234.iconee/0x12345678';
    let RC_OK = 0;                                  let RC_ERR = 1;    
    let _amt = 5000;                                let deposit = 100000;
    let _native = 'PARA';                           let _fee = 10;                         
    let REPONSE_HANDLE_SERVICE = 2;                 let _uri = 'https://github.com/icon-project/btp';

    before(async () => {
        bsh_perif = await BSHPeriphery.new();
        bsh_core = await BSHCore.new();
        bmc = await BMC.new('1234.pra');
        encode_msg = await EncodeMsg.new();
        await bsh_perif.initialize(bmc.address, bsh_core.address, service);
        await bsh_core.initialize(_uri, _native, _fee);
        await bsh_core.updateBSHPeriphery(bsh_perif.address);
        nonrefundable = await NonRefundable.new();
        refundable = await Refundable.new();
        await bmc.addService(service, bsh_perif.address);
        await bmc.addVerifier(_net, accounts[1]);
        await bmc.addLink(_bmcICON);
    });

    it('Scenario 1: Should revert when transferring native coins to an invalid BTP Address format', async () => {
        let invalid_destination = '1234.iconee/0x12345678';
        await truffleAssert.reverts(
            bsh_core.transfer.call(invalid_destination, {from: accounts[0], value: 5000}),
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
            bsh_core.transfer.call(_to, {from: accounts[0], value: 0}),
            "InvalidAmount"
        ); 
    });

    it('Scenario 3: Should revert when charging fee is zero' , async () => {
        await truffleAssert.reverts(
            bsh_core.transfer.call(_to, {from: accounts[0], value: 10}),
            "InvalidAmount"
        ); 
    });

    it('Scenario 4: Should revert when transferring to an invalid network/not supported network' , async () => {
        let invalid_destination = 'btp://1234.eos/0x12345678';
        await truffleAssert.reverts(
            bsh_core.transfer.call(invalid_destination, {from: accounts[1], value: 5000}),
            "BMCRevertNotExistsBMV"
        ); 
    });

    it('Scenario 5: Should succeed when Account client transferring a valid native coin to a side chain', async () => {
        let account_balanceBefore = await bsh_core.getBalanceOf(accounts[0], _native);
        let tx = await bsh_core.transfer(_to, {from: accounts[0], value: _amt});
        let account_balanceAfter = await bsh_core.getBalanceOf(accounts[0], _native);
        let bsh_coin_balance = await bsh_core.getBalanceOf(bsh_core.address, _native);
        let chargedFee = Math.floor(_amt/ 1000);

        const transferEvents = await bsh_perif.getPastEvents('TransferStart', { fromBlock: tx.receipt.blockNumber, toBlock: 'latest' });
        let event = transferEvents[0].returnValues;
        assert.equal(event._from, accounts[0]);
        assert.equal(event._to, _to);
        assert.equal(event._sn, 0);
        assert.equal(event._assetDetails.length, 1);
        assert.equal(event._assetDetails[0].coinName, 'PARA'); 
        assert.equal(event._assetDetails[0].value, _amt - chargedFee);
        assert.equal(event._assetDetails[0].fee, chargedFee);

        const linkStatus = await bmc.getStatus(_bmcICON);
        const bmcBtpAddress = await bmc.getBmcBtpAddress();

        const messageEvents = await bmc.getPastEvents('Message', { fromBlock: tx.receipt.blockNumber, toBlock: 'latest' });
        event = messageEvents[0].returnValues;
        assert.equal(event._next, _bmcICON);
        assert.equal(event._seq, linkStatus.txSeq);
        
        const bmcMsg = rlp.decode(event._msg);

        assert.equal(web3.utils.hexToUtf8(toHex(bmcMsg[0])), bmcBtpAddress);
        assert.equal(web3.utils.hexToUtf8(toHex(bmcMsg[1])), _bmcICON);
        assert.equal(web3.utils.hexToUtf8(toHex(bmcMsg[2])), service);
        assert.equal(web3.utils.hexToNumber(toHex(bmcMsg[3])), 0);

        const ServiceMsg = rlp.decode(bmcMsg[4]);
        assert.equal(web3.utils.hexToUtf8(toHex(ServiceMsg[0])), 0);

        const coinTransferMsg = rlp.decode(ServiceMsg[1]);
        assert.equal(web3.utils.hexToUtf8(toHex(coinTransferMsg[0])), accounts[0]);
        assert.equal(web3.utils.hexToUtf8(toHex(coinTransferMsg[1])), _to.split('/').slice(-1)[0]);
        assert.equal(web3.utils.hexToUtf8(toHex(coinTransferMsg[2][0][0])), _native);
        assert.equal(web3.utils.hexToNumber(toHex(coinTransferMsg[2][0][1])), _amt - chargedFee);

        assert(
            web3.utils.BN(bsh_coin_balance._usableBalance).toNumber() === _amt &&
            web3.utils.BN(account_balanceBefore._lockedBalance).toNumber() === 0 && 
            web3.utils.BN(account_balanceAfter._lockedBalance).toNumber() === _amt
        );
    });

    it('Scenario 6: Should update locked balance when BSHPeriphery receives a successful response of a recent request', async () => {
        let account_balanceBefore = await bsh_core.getBalanceOf(accounts[0], _native);
        let _msg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_OK, "");
        await bmc.receiveResponse(_net, service, 0, _msg);
        let account_balanceAfter = await bsh_core.getBalanceOf(accounts[0], _native);
        let fees = await bsh_core.getAccumulatedFees();
        // TODO: catch emit event TransferEnd throwing from BSHService contract
 
        assert(
            fees[0].coinName === _native && 
            Number(fees[0].value) === Math.floor(_amt/ 1000) &&
            web3.utils.BN(account_balanceBefore._lockedBalance).toNumber() === _amt &&
            web3.utils.BN(account_balanceAfter._lockedBalance).toNumber() === 0
        );
    });

    it('Scenario 5: Should succeed when Account client transferring a valid native coin to a side chain', async () => {
        let account_balanceBefore = await bsh_core.getBalanceOf(accounts[0], _native);
        let output = await bsh_core.transfer(_to, {from: accounts[0], value: _amt});
        let account_balanceAfter = await bsh_core.getBalanceOf(accounts[0], _native);
        let bsh_coin_balance = await bsh_core.getBalanceOf(bsh_core.address, _native);
        let chargedFee = Math.floor(_amt / 1000);
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
        let account_balanceBefore = await bsh_core.getBalanceOf(accounts[0], _native);
        let bsh_coin_balance_before = await bsh_core.getBalanceOf(bsh_core.address, _native);
        let _msg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_ERR, "");
        await bmc.receiveResponse(_net, service, 1, _msg);
        let account_balanceAfter = await bsh_core.getBalanceOf(accounts[0], _native);
        let bsh_coin_balance_after = await bsh_core.getBalanceOf(bsh_core.address, _native);
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
        let contract_balanceBefore = await bsh_core.getBalanceOf(nonrefundable.address, _native);
        let bsh_coin_balance_before = await bsh_core.getBalanceOf(bsh_core.address, _native);
        await nonrefundable.transfer(bsh_core.address, _to, _amt);
        let contract_balanceAfter = await bsh_core.getBalanceOf(nonrefundable.address, _native);
        let bsh_coin_balance_after = await bsh_core.getBalanceOf(bsh_core.address, _native);
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
        let contract_balanceBefore = await bsh_core.getBalanceOf(nonrefundable.address, _native);
        let bsh_coin_balance_before = await bsh_core.getBalanceOf(bsh_core.address, _native);
        let _msg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_ERR, "");
        await bmc.receiveResponse(_net, service, 2, _msg);
        let contract_balanceAfter = await bsh_core.getBalanceOf(nonrefundable.address, _native);
        let bsh_coin_balance_after = await bsh_core.getBalanceOf(bsh_core.address, _native);
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
        let contract_balanceBefore = await bsh_core.getBalanceOf(refundable.address, _native);
        let bsh_coin_balance_before = await bsh_core.getBalanceOf(bsh_core.address, _native);
        await refundable.transfer(bsh_core.address, _to, _amt);
        let contract_balanceAfter = await bsh_core.getBalanceOf(refundable.address, _native);
        let bsh_coin_balance_after = await bsh_core.getBalanceOf(bsh_core.address, _native);
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
        let contract_balanceBefore = await bsh_core.getBalanceOf(refundable.address, _native);
        let bsh_coin_balance_before = await bsh_core.getBalanceOf(bsh_core.address, _native);
        let _msg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_ERR, "");
        await bmc.receiveResponse(_net, service, 3, _msg);
        let contract_balanceAfter = await bsh_core.getBalanceOf(refundable.address, _native);
        let bsh_coin_balance_after = await bsh_core.getBalanceOf(bsh_core.address, _native);
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
    let service = 'Coin/WrappedCoin';           let _uri = 'https://github.com/icon-project/btp';
    let _native = 'PARA';                       let _fee = 10;     
    let _name = 'ICON';                         let _bmcICON = 'btp://1234.iconee/0x1234567812345678';
    let _net = '1234.iconee';                   let _from = '0x12345678';   let _value = 999999999999999;                       
    let REPONSE_HANDLE_SERVICE = 2;             let RC_OK = 0;              let RC_ERR = 1;
    let id;     

    before(async () => {    
        bsh_perif = await BSHPeriphery.new();
        bsh_core = await BSHCore.new();
        bmc = await BMC.new('1234.pra');
        encode_msg = await EncodeMsg.new();
        await bsh_perif.initialize(bmc.address, bsh_core.address, service);
        await bsh_core.initialize(_uri, _native, _fee);
        await bsh_core.updateBSHPeriphery(bsh_perif.address);
        holder = await Holder.new();
        await bmc.addService(service, bsh_perif.address);
        await bmc.addVerifier(_net, accounts[1]);
        await bmc.addLink(_bmcICON);
        await holder.addBSHContract(bsh_perif.address, bsh_core.address);
        await bsh_core.register(_name);
        let _msg = await encode_msg.encodeTransferMsgWithAddress(_from, holder.address, _name, _value);
        await bmc.receiveRequest(_bmcICON, "", service, 0, _msg);
        id = await bsh_core.coinId(_name);
    });

    it('Scenario 1: Should revert when User has not yet set approval for token being transferred out by Operator', async () => {
        let _to = 'btp://1234.iconee/0x12345678';
        let _value = 5000;
        let balanceBefore = await bsh_core.getBalanceOf(holder.address, _name);
        await truffleAssert.reverts(
            holder.callTransfer.call(_name, _value, _to),
            "ERC1155: caller is not owner nor approved"
        ); 
        let balanceAfter = await bsh_core.getBalanceOf(holder.address, _name);
        assert(
            web3.utils.BN(balanceAfter._usableBalance).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalance).toNumber() &&
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() === 0 &&    
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() === 0
        );
    });

    it(`Scenario 2: Should revert when User has set approval, but user's balance has insufficient amount`, async () => {
        let _to = 'btp://1234.iconee/0x12345678';
        let _value = 9999999999999999n;
        let balanceBefore = await bsh_core.getBalanceOf(holder.address, _name);
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransfer.call(_name, _value, _to),
            "ERC1155: insufficient balance for transfer"
        ); 
        let balanceAfter = await bsh_core.getBalanceOf(holder.address, _name);
        assert(
            web3.utils.BN(balanceAfter._usableBalance).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalance).toNumber() &&
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() === 0 &&    
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() === 0
        );
    });

    it('Scenario 3: Should revert when User requests to transfer an invalid Token', async () => {
        let _to = 'btp://1234.iconee/0x12345678';
        let _value = 9999999999999999n;
        let _token = 'EOS';
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransfer.call(_token, _value, _to),
            "UnregisterCoin"
        ); 
    });

    it('Scenario 4: Should revert when User transfers Tokens to an invalid BTP Address format', async () => {
        let _to = '1234.iconee/0x12345678';
        let contract_balanceBefore = await bsh_core.getBalanceOf(holder.address, _name);
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransfer.call(_name, 5000, _to),
            "VM Exception while processing transaction: revert"
        ); 
        let contract_balanceAfter = await bsh_core.getBalanceOf(holder.address, _name);
        let bsh_core_balance = await bsh_core.getBalanceOf(bsh_core.address, _name);
        assert(
            web3.utils.BN(contract_balanceBefore._lockedBalance).toNumber() === 0 &&
            web3.utils.BN(contract_balanceAfter._lockedBalance).toNumber() === 0 &&
            web3.utils.BN(contract_balanceAfter._usableBalance).toNumber() === 
                web3.utils.BN(contract_balanceBefore._usableBalance).toNumber() &&
            web3.utils.BN(bsh_core_balance._usableBalance).toNumber() === 0
        );
    });

    it('Scenario 5: Should revert when User requests to transfer zero Token', async () => {
        let _to = '1234.iconee/0x12345678';
        let balanceBefore = await bsh_core.getBalanceOf(holder.address, _name);
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransfer.call(_name, 0, _to),
            "InvalidAmount"
        ); 
        let balanceAfter = await bsh_core.getBalanceOf(holder.address, _name);
        assert(
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._usableBalance).toNumber() === 
                web3.utils.BN(balanceAfter._usableBalance).toNumber()
        );
    });

    it('Scenario 6: Should revert when charging fee is zero', async () => {
        let _to = '1234.iconee/0x12345678';
        let _name = 'ICON';
        let balanceBefore = await bsh_core.getBalanceOf(holder.address, _name);
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransfer.call(_name, 10, _to),
            "InvalidAmount"
        ); 
        let balanceAfter = await bsh_core.getBalanceOf(holder.address, _name);
        assert(
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._usableBalance).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalance).toNumber()
        );
    });

    it('Scenario 7: Should revert when User requests to transfer to an invalid network/Not Supported Network', async () => {
        let _to = 'btp://1234.eos/0x12345678';
        let _name = 'ICON';
        let balanceBefore = await bsh_core.getBalanceOf(holder.address, _name);
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransfer.call(_name, 1000, _to),
            "BMCRevertNotExistsBMV"
        ); 
        let balanceAfter = await bsh_core.getBalanceOf(holder.address, _name);
        let bsh_core_balance = await bsh_core.getBalanceOf(bsh_core.address, _name);
        assert(
            web3.utils.BN(balanceBefore._lockedBalance).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._lockedBalance).toNumber() === 0 &&
            web3.utils.BN(balanceAfter._usableBalance).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalance).toNumber() &&
            web3.utils.BN(bsh_core_balance._usableBalance).toNumber() === 0
        );
    });

    it('Scenario 8: Should succeed when User sends a valid transferring request', async () => {
        let _to = 'btp://1234.iconee/0x12345678';
        let _value = 1000;
        let balanceBefore = await bsh_core.getBalanceOf(holder.address, _name);
        await holder.setApprove(bsh_core.address);
        await holder.callTransfer(_name, _value, _to);
        let balanceAfter = await bsh_core.getBalanceOf(holder.address, _name);
        let bsh_core_balance = await bsh_core.getBalanceOf(bsh_core.address, _name);
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
        let _value = 1000;
        let contract_balanceBefore = await bsh_core.getBalanceOf(holder.address, _name);
        let _msg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_OK, "");
        await bmc.receiveResponse(_net, service, 0, _msg);
        let contract_balanceAfter = await bsh_core.getBalanceOf(holder.address, _name);
        let fees = await bsh_core.getAccumulatedFees();
        let bsh_core_balance = await bsh_core.getBalanceOf(bsh_core.address, _name);
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
        let _to = 'btp://1234.iconee/0x12345678';
        let _value = 100000000000000;
        let balanceBefore = await bsh_core.getBalanceOf(holder.address, _name);
        let bsh_core_balance_before = await bsh_core.getBalanceOf(bsh_core.address, _name);
        await holder.setApprove(bsh_core.address);
        await holder.callTransfer(_name, _value, _to);
        let balanceAfter = await bsh_core.getBalanceOf(holder.address, _name);
        let bsh_core_balance_after = await bsh_core.getBalanceOf(bsh_core.address, _name);
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
        let _value = 100000000000000;
        let balanceBefore = await bsh_core.getBalanceOf(holder.address, _name);
        let _msg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_ERR, "");
        await bmc.receiveResponse(_net, service, 1, _msg);
        let balanceAfter = await bsh_core.getBalanceOf(holder.address, _name);
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
    let service = 'Coin/WrappedCoin';       let _bmcICON = 'btp://1234.iconee/0x1234567812345678';
    let _net = '1234.iconee';               let _to = 'btp://1234.iconee/0x12345678';
    let _native = 'PARA';                   let _fee = 10;   
    let RC_ERR = 1;                         let RC_OK = 0;
    let _uri = 'https://github.com/icon-project/btp';

    before(async () => {
        bsh_perif = await BSHPeriphery.new();
        bsh_core = await BSHCore.new();
        bmc = await BMC.new('1234.pra');
        encode_msg = await EncodeMsg.new();
        await bsh_perif.initialize(bmc.address, bsh_core.address, service);
        await bsh_core.initialize(_uri, _native, _fee);
        await bsh_core.updateBSHPeriphery(bsh_perif.address);
        notpayable = await NotPayable.new();
        refundable = await Refundable.new();
        await bmc.addService(service, bsh_perif.address);
        await bmc.addVerifier(_net, accounts[1]);
        await bmc.addLink(_bmcICON);
        await bsh_core.transfer(_to, {from: accounts[0], value: 100000000});
        btpAddr = await bmc.bmcAddress();
    });

    it('Scenario 1: Should emit an error message when receiving address is invalid', async () => {
        let _from = '0x12345678';
        let _value = 1000;
        let _address = '0x1234567890123456789';
        let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'InvalidAddress');
        let _msg = await encode_msg.encodeTransferMsgWithStringAddress(_from, _address, _native, _value);
        let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
        assert(
            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _eventMsg
        );
    });

    it('Scenario 2: Should emit an error message when BSHCore has insufficient funds to transfer', async () => { 
        let _from = '0x12345678';
        let _value = 1000000000;
        let balanceBefore = await bmc.getBalance(accounts[1]);
        let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'TransferFailed');
        let _msg = await encode_msg.encodeTransferMsgWithAddress(_from, accounts[1], _native, _value);
        let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
        let balanceAfter = await bmc.getBalance(accounts[1]);
        assert(
            web3.utils.BN(balanceAfter).toString() === web3.utils.BN(balanceBefore).toString() &&
            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _eventMsg
        );
    });

    it(`Scenario 3: Should emit an error message when BSHCore tries to transfer PARA coins to a non-payable contract, but it fails`, async () => {
        let _from = '0x12345678';
        let _value = 1000;
        let balanceBefore = await bmc.getBalance(notpayable.address);
        let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'TransferFailed');
        let _msg = await encode_msg.encodeTransferMsgWithAddress(_from, notpayable.address, _native, _value);
        let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
        let balanceAfter = await bmc.getBalance(notpayable.address);
        assert(
            web3.utils.BN(balanceAfter).toNumber() === web3.utils.BN(balanceBefore).toNumber() &&
            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _eventMsg
        );
    });

    it('Scenario 4: Should be able to transfer coins to an account when BSHPeriphery receives a request of transferring coins', async () => { 
        let _from = '0x12345678';
        let _value = 12345;
        let balanceBefore = await bmc.getBalance(accounts[1]);
        let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_OK, '');
        let _msg = await encode_msg.encodeTransferMsgWithAddress(_from, accounts[1], _native, _value);
        let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
        let balanceAfter = await bmc.getBalance(accounts[1]);

        assert(
            web3.utils.BN(balanceAfter).toString() === 
                web3.utils.BN(balanceBefore).add(new web3.utils.BN(_value)).toString() &&
            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _eventMsg    
        );
    });

    it(`Scenario 5: Should be able to transfer coins to a payable contract receiver when BSHPeriphery receives a request of transferring coins`, async () => { 
        let _from = '0x12345678';
        let _value = 23456;
        let balanceBefore = await bmc.getBalance(refundable.address);
        let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_OK, '');
        let _msg = await encode_msg.encodeTransferMsgWithStringAddress(_from, refundable.address, _native, _value);
        let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
        let balanceAfter = await bmc.getBalance(refundable.address);
        assert(
            web3.utils.BN(balanceAfter).toNumber() === 
                web3.utils.BN(balanceBefore).toNumber() + _value &&
            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _eventMsg    
        );
    });
});

contract('As a user, I want to receive ERC1155_ICX from ICON blockchain', (accounts) => {
    let bmc, bsh_perif, bsh_core, holder, notpayable;
    let service = 'Coin/WrappedCoin';                   let _uri = 'https://github.com/icon-project/btp';
    let _native = 'PARA';                               let _fee = 10;
    let _name = 'ICON';                                 let _bmcICON = 'btp://1234.iconee/0x1234567812345678';
    let _net = '1234.iconee';                           let _from = '0x12345678';           
    let RC_ERR = 1;                                     let RC_OK = 0;          

    before(async () => {
        bsh_perif = await BSHPeriphery.new();
        bsh_core = await BSHCore.new();
        bmc = await BMC.new('1234.pra');
        encode_msg = await EncodeMsg.new();
        await bsh_perif.initialize(bmc.address, bsh_core.address, service);
        await bsh_core.initialize(_uri, _native, _fee);
        await bsh_core.updateBSHPeriphery(bsh_perif.address);
        holder = await Holder.new();
        notpayable = await NotPayable.new();
        await bmc.addService(service, bsh_perif.address);
        await bmc.addVerifier(_net, accounts[1]);
        await bmc.addLink(_bmcICON);
        await holder.addBSHContract(bsh_perif.address, bsh_core.address);
        await bsh_core.register(_name);
        id = await bsh_core.coinId(_name);
        btpAddr = await bmc.bmcAddress();
    });

    it('Scenario 1: Should emit an error message when receiving address is invalid', async () => {
        let _value = 1000;
        let _address = '0x1234567890123456789';
        let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'InvalidAddress');
        let _msg = await encode_msg.encodeTransferMsgWithStringAddress(_from, _address, _name, _value);
        let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
        assert(
            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _eventMsg
        );
    });

    it(`Scenario 2: Should emit an error message when receiving contract does not implement ERC1155Holder/Receiver`, async () => {
        let _value = 1000;
        let balanceBefore = await bsh_core.balanceOf(notpayable.address, id);
        let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'TransferFailed');
        let _msg = await encode_msg.encodeTransferMsgWithAddress(_from, notpayable.address, _name, _value);
        let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
        let balanceAfter = await bsh_core.balanceOf(notpayable.address, id);
        assert(
            web3.utils.BN(balanceAfter).toNumber() === web3.utils.BN(balanceBefore).toNumber() &&
            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _eventMsg
        );
    });

    it('Scenario 3: Should emit an error message when BSHPerphery receives a request of invalid token', async () => {
        let _value = 3000;
        let _tokenName = 'Ethereum';
        let invalid_coin_id = await bsh_core.coinId(_tokenName);
        let balanceBefore = await bsh_core.balanceOf(holder.address, invalid_coin_id);
        let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'UnregisteredCoin');
        let _msg = await encode_msg.encodeTransferMsgWithAddress(_from, holder.address, _tokenName, _value);
        let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
        let balanceAfter = await bsh_core.balanceOf(holder.address, invalid_coin_id);
        assert(
            web3.utils.BN(balanceAfter).toNumber() === web3.utils.BN(balanceBefore).toNumber() &&
            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _eventMsg
        );
    });

    it('Scenario 4: Should mint tokens successfully when a receiver is a ERC1155Holder contract', async () => { 
        let _value = 2500;
        let balanceBefore = await bsh_core.balanceOf(holder.address, id);
        let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_OK, '');
        let _msg = await encode_msg.encodeTransferMsgWithAddress(_from, holder.address, _name, _value);
        let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
        let balanceAfter = await bsh_core.balanceOf(holder.address, id);
        assert(
            web3.utils.BN(balanceAfter).toNumber() === 
                web3.utils.BN(balanceBefore).toNumber() + _value &&
            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _eventMsg    
        );
    });

    it('Scenario 5: Should mint tokens successfully when a receiver is an account client', async () => { 
        let _value = 5500;
        let balanceBefore = await bsh_core.balanceOf(accounts[1], id);
        let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_OK, '');
        let _msg = await encode_msg.encodeTransferMsgWithAddress(_from, accounts[1], _name, _value);
        let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
        let balanceAfter = await bsh_core.balanceOf(accounts[1], id);
        assert(
            web3.utils.BN(balanceAfter).toNumber() === 
                web3.utils.BN(balanceBefore).toNumber() + _value &&
            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _eventMsg 
        );
    });
});

contract('BSHs handle Gather Fee Service Requests', (accounts) => {
    let bsh_perif, bsh_core, bmc, holder;
    let service = 'Coin/WrappedCoin';                   let _uri = 'https://github.com/icon-project/btp';
    let _native = 'PARA';                               let _fee = 10;
    let _name1 = 'ICON';    let _name2 = 'BINANCE';     let _name3 = 'ETHEREUM';        let _name4 = 'TRON';                                             
    let _net1 = '1234.iconee';                          let _net2 = '1234.binance';                               
    let _from1 = '0x12345678';                          let _from2 = '0x12345678';
    let _value1 = 999999999999999;                      let _value2 = 999999999999999;
    let _to1 = 'btp://1234.iconee/0x12345678';          let _to2 = 'btp://1234.binance/0x12345678';
    let _txAmt = 10000;                                 let _txAmt1 = 1000000;          let _txAmt2 = 5000000;
    let RC_OK = 0;                                      let RC_ERR = 1;                                                         
    let REPONSE_HANDLE_SERVICE = 2;                     let _bmcICON = 'btp://1234.iconee/0x1234567812345678'; 
    let _sn0 = 0;           let _sn1 = 1;               let _sn2 = 2; 

    before(async () => {
        bsh_perif = await MockBSHPeriphery.new();
        bsh_core = await BSHCore.new();
        bmc = await BMC.new('1234.pra');
        encode_msg = await EncodeMsg.new();
        await bsh_perif.initialize(bmc.address, bsh_core.address, service);
        await bsh_core.initialize(_uri, _native, _fee);
        await bsh_core.updateBSHPeriphery(bsh_perif.address);
        holder = await Holder.new();
        btpAddr = await bmc.bmcAddress();
        await bmc.addService(service, bsh_perif.address);
        await bmc.addVerifier(_net1, accounts[1]);
        await bmc.addVerifier(_net2, accounts[2]);
        await bmc.addLink(_bmcICON);
        await holder.addBSHContract(bsh_perif.address, bsh_core.address);
        await bsh_core.register(_name1);
        await bsh_core.register(_name2);
        await bsh_core.register(_name3);
        await bsh_core.register(_name4);
        let _msg1 = await encode_msg.encodeTransferMsgWithAddress(_from1, holder.address, _name1, _value1);
        await bmc.receiveRequest(_bmcICON, "", service, _sn0, _msg1);
        let _msg2 = await encode_msg.encodeTransferMsgWithAddress(_from2, holder.address, _name2, _value2);
        await bmc.receiveRequest(_bmcICON, "", service, _sn1, _msg2);
        await bsh_core.transfer(_to1, {from: accounts[0], value: _txAmt});
        let _responseMsg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_OK, "");
        await bmc.receiveResponse(_net1, service, _sn0, _responseMsg);
        await holder.setApprove(bsh_core.address);
        await holder.callTransfer(_name1, _txAmt1, _to1);
        await bmc.receiveResponse(_net1, service, _sn1, _responseMsg);
        await holder.callTransfer(_name2, _txAmt2, _to2);
        await bmc.receiveResponse(_net1, service, _sn2, _responseMsg);
    });

    it('Scenario 1: Should be able to query Aggregation Fee', async () => {
        let aggregationFee = await bsh_core.getAccumulatedFees();
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
        let _sn3 = 3
        let FA1Before = await bsh_perif.getAggregationFeeOf(_native);     //  state Aggregation Fee of each type of Coins
        let FA2Before = await bsh_perif.getAggregationFeeOf(_name1);
        let FA3Before = await bsh_perif.getAggregationFeeOf(_name2);
        await truffleAssert.reverts( 
            bsh_perif.handleFeeGathering.call(_to1, service, {from: accounts[1]}),
            'Unauthorized'    
        );
        let FA1After = await bsh_perif.getAggregationFeeOf(_native);
        let FA2After = await bsh_perif.getAggregationFeeOf(_name1);
        let FA3After = await bsh_perif.getAggregationFeeOf(_name2);
        let fees = await bsh_perif.getFees(_sn3);     //  get pending Aggregation Fee list
        assert(
            web3.utils.BN(FA1Before).toNumber() === web3.utils.BN(FA1After).toNumber() && 
            web3.utils.BN(FA2Before).toNumber() === web3.utils.BN(FA2After).toNumber() &&
            web3.utils.BN(FA3Before).toNumber() === web3.utils.BN(FA3After).toNumber() &&
            fees.amounts.length === 0
        );
    });

    //  Before: 
    //      + state Aggregation Fee of each type of Coins are set
    //      + pendingAggregation Fee list is empty
    //  After: 
    //      + all states of Aggregation Fee are push into pendingAggregation Fee list
    //      + state Aggregation Fee of each type of Coins are reset
    it('Scenario 3: Should handle GatherFee request from BMCService contract', async () => {
        let _sn3 = 3;
        let FA1Before = await bsh_perif.getAggregationFeeOf(_native);     //  state Aggregation Fee of each type of Coins
        let FA2Before = await bsh_perif.getAggregationFeeOf(_name1);
        let FA3Before = await bsh_perif.getAggregationFeeOf(_name2);
        let _bmcService = await encode_msg.encodeBMCService(_to1, [service]);
        let output = await bmc.receiveRequest(_bmcICON, '', 'bmc', 100, _bmcService);
        let FA1After = await bsh_perif.getAggregationFeeOf(_native);
        let FA2After = await bsh_perif.getAggregationFeeOf(_name1);
        let FA3After = await bsh_perif.getAggregationFeeOf(_name2);
        let fees = await bsh_perif.getFees(_sn3);     //  get pending Aggregation Fee list
        let list = [];
        for (let i = 0; i < fees.amounts.length; i++) {
            list[i] = [fees.coinNames[i], fees.amounts[i]];
        }
        let _eventMsg = await encode_msg.encodeTransferFeesBMCMessage(
            btpAddr, _bmcICON, _to1, service, _sn3, bsh_core.address, list
        );
        //  TODO: catch event TransferStart throwing from BSHService contract

        assert(
            web3.utils.BN(FA1Before).toNumber() === Math.floor(_txAmt / 1000) && 
            web3.utils.BN(FA2Before).toNumber() === Math.floor(_txAmt1 / 1000) &&
            web3.utils.BN(FA3Before).toNumber() === Math.floor(_txAmt2 / 1000) &&
            web3.utils.BN(FA1After).toNumber() === 0 && 
            web3.utils.BN(FA2After).toNumber() === 0 && 
            web3.utils.BN(FA3After).toNumber() === 0 && 
            fees.coinNames[0] === _native && Number(fees.amounts[0]) === Math.floor(_txAmt / 1000) &&
            fees.coinNames[1] === _name1 && Number(fees.amounts[1]) === Math.floor(_txAmt1 / 1000) &&
            fees.coinNames[2] === _name2 && Number(fees.amounts[2]) === Math.floor(_txAmt2 / 1000) &&
            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _eventMsg
        );
    });

    it('Scenario 4: Should reset a pending state when receiving a successful response', async () => {
        let _sn3 = 3;
        let feesBefore = await bsh_perif.getFees(_sn3);
        let _responseMsg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_OK, "");
        await bmc.receiveResponse(_net1, service, _sn3, _responseMsg);
        let feesAfter = await bsh_perif.getFees(_sn3);
        //  TODO: catch event TransferEnd throwing from BSHService contract
        assert(
            feesBefore.amounts.length === 3 &&
            feesBefore.coinNames[0] === _native && Number(feesBefore.amounts[0]) === Math.floor(_txAmt / 1000) &&
            feesBefore.coinNames[1] === _name1 && Number(feesBefore.amounts[1]) === Math.floor(_txAmt1 / 1000) &&
            feesBefore.coinNames[2] === _name2 && Number(feesBefore.amounts[2]) === Math.floor(_txAmt2 / 1000) &&
            feesAfter.amounts.length === 0
        );
    });

    it('Scenario 5: Should restore aggregationFA state when receiving an error response', async () => {
        let _sn4 = 4;   let _sn5 = 5;   let _sn6 = 6;
        let _amt1 = 2000000;                    let _amt2 = 6000000;
        await holder.callTransfer(_name1, _amt1, _to1);
        let _responseMsg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_OK, "");
        await bmc.receiveResponse(_net1, service, _sn4, _responseMsg);
        await holder.callTransfer(_name2, _amt2, _to2);
        await bmc.receiveResponse(_net2, service, _sn5, _responseMsg);
        let _bmcService = await encode_msg.encodeBMCService(_to1, [service]);
        await bmc.receiveRequest(_bmcICON, '', 'bmc', 100, _bmcService);

        let FA1Before = await bsh_perif.getAggregationFeeOf(_name1);
        let FA2Before = await bsh_perif.getAggregationFeeOf(_name2);
        let feesBefore = await bsh_perif.getFees(_sn6);
        let _errMsg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_ERR, "");
        await bmc.receiveResponse(_net1, service, _sn6, _errMsg);
        let FA1After = await bsh_perif.getAggregationFeeOf(_name1);
        let FA2After = await bsh_perif.getAggregationFeeOf(_name2);
        let feesAfter = await bsh_perif.getFees(_sn6);
        // TODO: catch event TransferEnd throwing from BSHService contract
        assert(
            feesBefore.amounts.length === 2 &&
            feesBefore.coinNames[0] === _name1 && Number(feesBefore.amounts[0]) === Math.floor(_amt1 / 1000) &&
            feesBefore.coinNames[1] === _name2 && Number(feesBefore.amounts[1]) === Math.floor(_amt2 / 1000) &&
            web3.utils.BN(FA1Before).toNumber() === 0 && 
            web3.utils.BN(FA2Before).toNumber() === 0 &&
            feesAfter.amounts.length === 0 &&
            web3.utils.BN(FA1After).toNumber() === Math.floor(_amt1 / 1000) && 
            web3.utils.BN(FA2After).toNumber() === Math.floor(_amt2 / 1000)
        );
    });
});

contract('As a user, I want to receive multiple Coins/Tokens from ICON blockchain', (accounts) => {
    let bsh_perif, bsh_core, bmc, holder, refundable;
    let service = 'Coin/WrappedCoin';                   let _uri = 'https://github.com/icon-project/btp';
    let _native = 'PARA';                               let _fee = 10;
    let _name1 = 'ICON';    let _name2 = 'BINANCE';     let _name3 = 'ETHEREUM';        let _name4 = 'TRON';                                             
    let _net1 = '1234.iconee';                          let _bmcICON = 'btp://1234.iconee/0x1234567812345678';                                                     
    let RC_OK = 0;                                      let RC_ERR = 1;                 
    let _from1 = '0x12345678';                          let _to = 'btp://1234.iconee/0x12345678';                                                         

    before(async () => {
        bsh_perif = await BSHPeriphery.new();
        bsh_core = await BSHCore.new();
        bmc = await BMC.new('1234.pra');
        encode_msg = await EncodeMsg.new();
        await bsh_perif.initialize(bmc.address, bsh_core.address, service);
        await bsh_core.initialize(_uri, _native, _fee);
        await bsh_core.updateBSHPeriphery(bsh_perif.address);
        holder = await Holder.new();
        refundable = await Refundable.new();
        btpAddr = await bmc.bmcAddress();
        await bmc.addService(service, bsh_perif.address);
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
        let _value1 = 1000;     let _value2 = 10000;    let _value3 = 40000;
        let _address = '0x1234567890123456789';
        let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'InvalidAddress');
        let _msg = await encode_msg.encodeBatchTransferMsgWithStringAddress(
            _from1, _address, [[_native, _value1], [_name1, _value2], [_name2, _value3]]
        );
        let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
        assert(
            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _eventMsg
        )
    });

    it('Scenario 2: Should emit an error message when BSHPerphery receives a request of invalid token', async () => {
        let _value1 = 1000;     let _value2 = 10000;    let _value3 = 40000;
        let _invalid_token = 'EOS';
        let balance1Before = await bsh_core.getBalanceOf(holder.address, _name1);
        let balance2Before = await bsh_core.getBalanceOf(holder.address, _name2);
        let balance3Before = await bsh_core.getBalanceOf(holder.address, _invalid_token);
        let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'UnregisteredCoin');
        let _msg = await encode_msg.encodeBatchTransferMsgWithAddress(
            _from1, holder.address, [[_name1, _value1], [_name2, _value2], [_invalid_token, _value3]]
        );
        let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
        let balance1After = await bsh_core.getBalanceOf(holder.address, _name1);
        let balance2After = await bsh_core.getBalanceOf(holder.address, _name2);
        let balance3After = await bsh_core.getBalanceOf(holder.address, _invalid_token);

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

            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _eventMsg
        )
    });

    it('Scenario 3: Should emit an error response when one of requests is failed in TransferBatch', async () => {
        let _value1 = 1000;     let _value2 = 10000;    let _value3 = 20000000;
        let balance1Before = await bsh_core.getBalanceOf(accounts[1], _name1);
        let balance2Before = await bsh_core.getBalanceOf(accounts[1], _name2);
        let balance3Before = await bsh_core.getBalanceOf(accounts[1], _native);
        let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'TransferFailed');
        let _msg = await encode_msg.encodeBatchTransferMsgWithAddress(
            _from1, accounts[1], [[_name1, _value1], [_name2, _value2], [_native, _value3]]
        );
        let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
        let balance1After = await bsh_core.getBalanceOf(accounts[1], _name1);
        let balance2After = await bsh_core.getBalanceOf(accounts[1], _name2);
        let balance3After = await bsh_core.getBalanceOf(accounts[1], _native);

        assert(
            web3.utils.BN(balance1Before._usableBalance).toNumber() === 
                web3.utils.BN(balance1After._usableBalance).toNumber() &&
            web3.utils.BN(balance2Before._usableBalance).toNumber() === 
                web3.utils.BN(balance2After._usableBalance).toNumber() &&  
            web3.utils.BN(balance3Before._usableBalance).toString() === 
                web3.utils.BN(balance3After._usableBalance).toString() &&      
            web3.utils.BN(balance1After._usableBalance).toNumber() === 0 &&
            web3.utils.BN(balance2After._usableBalance).toNumber() === 0 &&

            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _eventMsg
        )
    });

    it('Scenario 4: Should emit an error response when one of requests is failed in TransferBatch', async () => {
        let _value1 = 1000;     let _value2 = 10000;    let _value3 = 40000;
        let balance1Before = await bsh_core.getBalanceOf(refundable.address, _native);
        let balance2Before = await bsh_core.getBalanceOf(refundable.address, _name1);
        let balance3Before = await bsh_core.getBalanceOf(refundable.address, _name2);
        let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'TransferFailed');
        let _msg = await encode_msg.encodeBatchTransferMsgWithAddress(
            _from1, refundable.address, [[_native, _value1], [_name1, _value2], [_name2, _value3]]
        );
        let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
        let balance1After = await bsh_core.getBalanceOf(refundable.address, _native);
        let balance2After = await bsh_core.getBalanceOf(refundable.address, _name1);
        let balance3After = await bsh_core.getBalanceOf(refundable.address, _name2);

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

            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _eventMsg
        )
    });

    it('Scenario 5: Should emit an error response when one of requests is failed in TransferBatch', async () => {
        let _value1 = 1000;     let _value2 = 10000;    let _value3 = 40000;
        let balance1Before = await bsh_core.getBalanceOf(holder.address, _name1);
        let balance2Before = await bsh_core.getBalanceOf(holder.address, _name2);
        let balance3Before = await bsh_core.getBalanceOf(holder.address, _native);
        let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'TransferFailed');
        let _msg = await encode_msg.encodeBatchTransferMsgWithAddress(
            _from1, holder.address, [[_name1, _value1], [_name2, _value2], [_native, _value3]]
        );
        let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
        let balance1After = await bsh_core.getBalanceOf(holder.address, _name1);
        let balance2After = await bsh_core.getBalanceOf(holder.address, _name2);
        let balance3After = await bsh_core.getBalanceOf(holder.address, _native);

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

            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _eventMsg
        )
    });

    it('Scenario 6: Should succeed in TransferBatch', async () => {
        let _value1 = 1000;     let _value2 = 10000;    let _value3 = 40000;
        let balance1Before = await bsh_core.getBalanceOf(holder.address, _name1);
        let balance2Before = await bsh_core.getBalanceOf(holder.address, _name2);
        let balance3Before = await bsh_core.getBalanceOf(holder.address, _name3);
        let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_OK, '');
        let _msg = await encode_msg.encodeBatchTransferMsgWithAddress(
            _from1, holder.address, [[_name1, _value1], [_name2, _value2], [_name3, _value3]]
        );
        let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
        let balance1After = await bsh_core.getBalanceOf(holder.address, _name1);
        let balance2After = await bsh_core.getBalanceOf(holder.address, _name2);
        let balance3After = await bsh_core.getBalanceOf(holder.address, _name3);

        assert(
            web3.utils.BN(balance1Before._usableBalance).toNumber() === 0 &&
            web3.utils.BN(balance2Before._usableBalance).toNumber() === 0 &&
            web3.utils.BN(balance3Before._usableBalance).toNumber() === 0 &&    
            web3.utils.BN(balance1After._usableBalance).toNumber() === _value1 &&
            web3.utils.BN(balance2After._usableBalance).toNumber() === _value2 &&
            web3.utils.BN(balance3After._usableBalance).toNumber() === _value3 &&

            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _eventMsg
        )
    });

    it('Scenario 7: Should succeed in TransferBatch', async () => {
        let _value1 = 1000;     let _value2 = 10000;    let _value3 = 40000;
        let balance1Before = await bsh_core.getBalanceOf(accounts[1], _native);
        let balance2Before = await bsh_core.getBalanceOf(accounts[1], _name2);
        let balance3Before = await bsh_core.getBalanceOf(accounts[1], _name3);
        let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_OK, '');
        let _msg = await encode_msg.encodeBatchTransferMsgWithAddress(
            _from1, accounts[1], [[_native, _value1], [_name2, _value2], [_name3, _value3]]
        );
        let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
        let balance1After = await bsh_core.getBalanceOf(accounts[1], _native);
        let balance2After = await bsh_core.getBalanceOf(accounts[1], _name2);
        let balance3After = await bsh_core.getBalanceOf(accounts[1], _name3);

        assert(   
            web3.utils.BN(balance1After._usableBalance).toString() === 
                web3.utils.BN(balance1Before._usableBalance).add(new web3.utils.BN(_value1)).toString() &&
            web3.utils.BN(balance2After._usableBalance).toNumber() === 
                web3.utils.BN(balance2Before._usableBalance).toNumber() + _value2 &&
            web3.utils.BN(balance3After._usableBalance).toNumber() === 
                web3.utils.BN(balance3Before._usableBalance).toNumber() + _value3 &&

            output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _eventMsg
        )
    });
});    

contract('As a user, I want to send multiple coins/tokens to ICON blockchain', (accounts) => {
    let bsh_perif, bsh_core, bmc, holder;
    let service = 'Coin/WrappedCoin';           let _uri = 'https://github.com/icon-project/btp';
    let _native = 'PARA';                       let _fee = 10;      
    let _net = '1234.iconee';                   let _from = '0x12345678';   let _value = 999999999999999;                       
    let REPONSE_HANDLE_SERVICE = 2;             let RC_OK = 0;              let RC_ERR = 1;
    let _bmcICON = 'btp://1234.iconee/0x1234567812345678';         
    let _coin1 = 'ICON';    let _coin2 = 'TRON';    let _coin3 = 'BINANCE';
    let initAmt = 100000000;

    before(async () => {    
        bsh_perif = await BSHPeriphery.new();
        bsh_core = await BSHCore.new();
        bmc = await BMC.new('1234.pra');
        encode_msg = await EncodeMsg.new();
        await bsh_perif.initialize(bmc.address, bsh_core.address, service);
        await bsh_core.initialize(_uri, _native, _fee);
        await bsh_core.updateBSHPeriphery(bsh_perif.address);
        holder = await Holder.new();
        await bmc.addService(service, bsh_perif.address);
        await bmc.addVerifier(_net, accounts[1]);
        await bmc.addLink(_bmcICON);
        await holder.addBSHContract(bsh_perif.address, bsh_core.address);
        await bsh_core.register(_coin1);
        await bsh_core.register(_coin2);
        await bsh_core.register(_coin3);
        await bsh_core.transfer('btp://1234.iconee/0x12345678', {from: accounts[0], value: initAmt});
        await holder.deposit({from: accounts[1], value: 100000000});
        let _msg1 = await encode_msg.encodeTransferMsgWithAddress(_from, holder.address, _coin1, _value);
        await bmc.receiveRequest(_bmcICON, "", service, 0, _msg1);
        let _msg2 = await encode_msg.encodeTransferMsgWithAddress(_from, holder.address, _coin2, _value);
        await bmc.receiveRequest(_bmcICON, "", service, 1, _msg2);
        let _msg3 = await encode_msg.encodeTransferMsgWithAddress(_from, holder.address, _coin3, _value);
        await bmc.receiveRequest(_bmcICON, "", service, 2, _msg3);

        _msg1 = await encode_msg.encodeTransferMsgWithAddress(_from, accounts[1], _coin1, _value);
        await bmc.receiveRequest(_bmcICON, "", service, 0, _msg1);
        _msg2 = await encode_msg.encodeTransferMsgWithAddress(_from, accounts[1], _coin2, _value);
        await bmc.receiveRequest(_bmcICON, "", service, 1, _msg2);
        _msg3 = await encode_msg.encodeTransferMsgWithAddress(_from, accounts[1], _coin3, _value);
        await bmc.receiveRequest(_bmcICON, "", service, 2, _msg3);
    });

    it('Scenario 1: Should revert when User has not yet set approval for token being transferred out by Operator', async () => {
        let _to = 'btp://1234.iconee/0x12345678';
        let _coins = [_native, _coin1, _coin2];
        let _values = [1000, 2000, 3000];
        let balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        await truffleAssert.reverts(
            holder.callTransferBatch.call(bsh_core.address, _coins, _values, _to),
            "VM Exception while processing transaction: revert"
        ); 
        let balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        let bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);

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
        let _to = 'btp://1234.iconee/0x12345678';
        let _coins = [_native, _coin1, _coin2];
        let _values = [1000, 2000, 9999999999999999n];
        let balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransferBatch.call(bsh_core.address, _coins, _values, _to),
            "VM Exception while processing transaction: revert"
        ); 
        let balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        let bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);

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
        let _to = 'btp://1234.iconee/0x12345678';
        let invalid_token = 'EOS';
        let _coins = [_native, _coin1, invalid_token];
        let _values = [1000, 2000, 3000];
        let balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransferBatch.call(bsh_core.address, _coins, _values, _to),
            "VM Exception while processing transaction: revert"
        ); 
        let balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        let bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);

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
        let _to = '1234.iconee/0x12345678';
        let _coins = [_native, _coin1, _coin2];
        let _values = [1000, 2000, 3000];
        let balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransferBatch.call(bsh_core.address, _coins, _values, _to),
            "VM Exception while processing transaction: revert"
        ); 
        let balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        let bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);

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
        let _to = '1234.iconee/0x12345678';
        let _coins = [_native, _coin1, _coin2];
        let _values = [1000, 2000, 0];
        let balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransferBatch.call(bsh_core.address, _coins, _values, _to),
            "VM Exception while processing transaction: revert"
        ); 
        let balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        let bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);

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
        let _to = '1234.iconee/0x12345678';
        let _coins = [_native, _coin1, _coin2];
        let _values = [1000, 2000, 100];
        let balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransferBatch.call(bsh_core.address, _coins, _values, _to),
            "VM Exception while processing transaction: revert"
        ); 
        let balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        let bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);

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
        let _to = 'btp://1234.eos/0x12345678';
        let _coins = [_native, _coin1, _coin2];
        let _values = [1000, 2000, 3000];
        let balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransferBatch.call(bsh_core.address, _coins, _values, _to),
            "VM Exception while processing transaction: revert"
        ); 
        let balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        let bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);
        
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
        let _to = 'btp://1234.eos/0x12345678';
        let _coins = [_native, _native, _native];
        let _values = [1000, 2000, 3000];
        let balanceBefore = await bsh_core.getBalanceOfBatch(accounts[2], _coins);
        await truffleAssert.reverts(
            bsh_core.transferBatch.call(_coins, _values, _to, {from: accounts[2], value: 6000}),
            "VM Exception while processing transaction: revert"
        ); 
        let balanceAfter = await bsh_core.getBalanceOfBatch(accounts[2], _coins);
        let bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);
        
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
        let _to = 'btp://1234.eos/0x12345678';
        let _coins = [_native, _coin1, _coin2];
        let _values = [1000, 2000];
        let balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        await holder.setApprove(bsh_core.address);
        await truffleAssert.reverts(
            holder.callTransferBatch.call(bsh_core.address, _coins, _values, _to),
            "VM Exception while processing transaction: revert"
        ); 
        let balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        let bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);
        
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
        let _to = 'btp://1234.iconee/0x12345678';
        let _coins = [_native, _coin1, _coin2];
        let _value1 = 1000;     let _value2 = 2000;     let _value3 = 1000;
        let _values = [_value1, _value2, _value3];
        let balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        await holder.setApprove(bsh_core.address);
        await holder.callTransferBatch(bsh_core.address, _coins, _values, _to);
        let balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        let bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);

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
        let _value1 = 1000;     let _value2 = 2000;     let _value3 = 1000;
        let _coins = [_native, _coin1, _coin2];
        let balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        let _responseMsg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_OK, "");
        await bmc.receiveResponse(_net, service, 1, _responseMsg);
        let balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        let fees = await bsh_core.getAccumulatedFees();
        let bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);
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
        let _to = 'btp://1234.iconee/0x12345678';
        let _coins = [_coin3, _coin1, _coin2];
        let _value1 = 1000;     let _value2 = 2000;     let _value3 = 1000;
        let _values = [_value1, _value2, _value3];
        let balanceBefore = await bsh_core.getBalanceOfBatch(accounts[1], _coins);
        await bsh_core.setApprovalForAll(bsh_core.address, true, {from: accounts[1]});
        await bsh_core.transferBatch(_coins, _values, _to, {from: accounts[1]});
        let balanceAfter = await bsh_core.getBalanceOfBatch(accounts[1], _coins);
        let bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);
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
        let _value1 = 1000;     let _value2 = 2000;     let _value3 = 1000;
        let _coins = [_coin3, _coin1, _coin2];
        let balanceBefore = await bsh_core.getBalanceOfBatch(accounts[1], _coins);
        let _responseMsg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_ERR, "");
        await bmc.receiveResponse(_net, service, 2, _responseMsg);
        let balanceAfter = await bsh_core.getBalanceOfBatch(accounts[1], _coins);
        let bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);
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
        let _to = 'btp://1234.iconee/0x12345678';
        let _coins = [_coin3, _coin1, _coin2];
        let _value1 = 1000;     let _value2 = 2000;     let _value3 = 1000;
        let _values = [_value1, _value2, _value3];
        let balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        await holder.setApprove(bsh_core.address);
        await holder.callTransferBatch(bsh_core.address, _coins, _values, _to);
        let balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        let bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);

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
        let _value1 = 1000;     let _value2 = 2000;     let _value3 = 1000;
        let _coins = [_coin3, _coin1, _coin2];
        let balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        let _responseMsg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_ERR, "");
        await bmc.receiveResponse(_net, service, 3, _responseMsg);
        let balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        let bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);
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
        let _to = 'btp://1234.iconee/0x12345678';
        let _coins = [_native, _coin1, _coin2];
        let _value1 = 1000;     let _value2 = 2000;     let _value3 = 1000;
        let _values = [_value1, _value2, _value3];
        let balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        await holder.setApprove(bsh_core.address);
        await holder.callTransferBatch(bsh_core.address, _coins, _values, _to);
        let balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        let bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);

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
        let _value1 = 1000;     let _value2 = 2000;     let _value3 = 1000;
        let _coins = [_native, _coin1, _coin2];
        let balanceBefore = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        let _responseMsg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_ERR, "");
        await bmc.receiveResponse(_net, service, 4, _responseMsg);
        let balanceAfter = await bsh_core.getBalanceOfBatch(holder.address, _coins);
        let bsh_core_balance = await bsh_core.getBalanceOfBatch(bsh_core.address, _coins);
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
const BSHCoreV1 = artifacts.require("BSHCoreV1");
const BSHCoreV2 = artifacts.require("BSHCoreV2");
const { assert } = require('chai');
const truffleAssert = require('truffle-assertions');
const { deployProxy, upgradeProxy } = require('@openzeppelin/truffle-upgrades');

contract('BSHCore Unit Tests - After Upgrading Contract', (accounts) => {
    let bsh_coreV1, bsh_coreV2;
    let _native = 'PARA';       let _uri = 'https://github.com/icon-project/btp'                   
    let _fee = 10;      let _fixed_fee = 500000;
    before(async () => {
        bsh_coreV1 = await deployProxy(BSHCoreV1, [_uri, _native, _fee, _fixed_fee]);
        bsh_coreV2 = await upgradeProxy(bsh_coreV1.address, BSHCoreV2);
    });

    it(`Scenario 1: Should allow contract's owner to register a new coin`, async () => {
        let _name = "ICON";
        await bsh_coreV2.register(_name);
        output = await bsh_coreV2.coinNames();
        assert(
            output[0] === _native && output[1] === 'ICON'
        );
    });
    
    it('Scenario 2: Should revert when an arbitrary client tries to register a new coin', async () => {   
        let _name = "TRON";
        await truffleAssert.reverts(
            bsh_coreV2.register.call(_name, {from: accounts[1]}),
            "Unauthorized"
        );
    }); 

    it('Scenario 3: Should revert when contract owner registers an existed coin', async () => {
        let _name = "ICON";
        await truffleAssert.reverts(
            bsh_coreV2.register.call(_name),
            "ExistToken"
        );
    }); 

    it('Scenario 4: Should allow contract owner to update BSHPeriphery contract', async () => {
        await bsh_coreV2.updateBSHPeriphery(accounts[2]);
    });

    it('Scenario 5: Should revert when arbitrary client updates BSHPeriphery contract', async () => {
        await truffleAssert.reverts(
            bsh_coreV2.updateBSHPeriphery.call(accounts[2], {from: accounts[1]}),
            "Unauthorized"
        );
    });

    it('Scenario 6: Should allow contract owner to update a new URI', async () => {
        let new_uri = 'https://1234.iconee/'
        await bsh_coreV2.updateUri(new_uri);
    });

    it('Scenario 7: Should revert when arbitrary client update a new URI', async () => {
        let new_uri = 'https://1234.iconee/'
        await truffleAssert.reverts(
            bsh_coreV2.updateUri.call(new_uri, {from: accounts[1]}),
            "Unauthorized"
        );
    });

    it('Scenario 8: Should allow contract owner to update fee ratio', async () => {
        let new_fee = 20;
        await bsh_coreV2.setFeeRatio(new_fee);

        assert(
            web3.utils.BN(await bsh_coreV2.feeNumerator()).toNumber() === new_fee
        );
    });

    it('Scenario 9: Should revert when arbitrary client updates fee ratio', async () => {
        let old_fee = 20;
        let new_fee = 50;
        await truffleAssert.reverts(
            bsh_coreV2.setFeeRatio.call(new_fee, {from: accounts[1]}),
            "Unauthorized"
        );

        assert(
            web3.utils.BN(await bsh_coreV2.feeNumerator()).toNumber() === old_fee
        );
    });

    it('Scenario 10: Should revert when Fee Numerator is higher than Fee Denominator', async () => {
        let old_fee = 20;
        let new_fee = 20000;
        await truffleAssert.reverts(
            bsh_coreV2.setFeeRatio.call(new_fee),
            "InvalidSetting"
        );

        assert(
            web3.utils.BN(await bsh_coreV2.feeNumerator()).toNumber() === old_fee
        );
    });

    it('Scenario 11: Should allow contract owner to update fixed fee', async () => {
        let new_fixed_fee = 1000000;
        await bsh_coreV2.setFixedFee(new_fixed_fee);

        assert(
            web3.utils.BN(await bsh_coreV2.fixedFee()).toNumber() === new_fixed_fee
        );
    });

    it('Scenario 12: Should revert when arbitrary client updates fixed fee', async () => {
        let old_fixed_fee = 1000000;
        let new_fixed_fee = 2000000;
        await truffleAssert.reverts(
            bsh_coreV2.setFixedFee.call(new_fixed_fee, {from: accounts[1]}),
            "Unauthorized"
        );

        assert(
            web3.utils.BN(await bsh_coreV2.fixedFee()).toNumber() === old_fixed_fee
        );
    });

    it('Scenario 13: Should revert when Owner set fixed fee is zero', async () => {
        let old_fixed_fee = 1000000;
        let new_fixed_fee = 0;
        await truffleAssert.reverts(
            bsh_coreV2.setFixedFee.call(new_fixed_fee),
            "InvalidSetting"
        );

        assert(
            web3.utils.BN(await bsh_coreV2.fixedFee()).toNumber() === old_fixed_fee
        );
    });

    it('Scenario 14: Should receive an id of a given coin name when querying a valid supporting coin', async () => {
        let _name1 = "wBTC";    let _name2 = "Ethereum";
        await bsh_coreV2.register(_name1);
        await bsh_coreV2.register(_name2);

        let _query = "ICON";
        let id = web3.utils.keccak256(_query);
        let result = await bsh_coreV2.coinId(_query);
        assert(
            web3.utils.BN(result).toString() === web3.utils.toBN(id).toString()
        );
    }); 

    it('Scenario 15: Should receive an id = 0 when querying an invalid supporting coin', async () => {
        let _query = "EOS";
        let result = await bsh_coreV2.coinId(_query);
        assert(
            web3.utils.BN(result).toNumber() === 0
        );
    }); 

    it('Scenario 16: Should revert when a non-Owner tries to add a new Owner', async () => {
        let oldList = await bsh_coreV2.getOwners();
        await truffleAssert.reverts(
            bsh_coreV2.addOwner.call(accounts[1], {from: accounts[2]}),
            "Unauthorized"
        );
        let newList = await bsh_coreV2.getOwners();
        assert(
            oldList.length === 1 && oldList[0] === accounts[0] &&
            newList.length === 1 && newList[0] === accounts[0]
        );
    }); 

    it('Scenario 17: Should allow a current Owner to add a new Owner', async () => {
        let oldList = await bsh_coreV2.getOwners();
        await bsh_coreV2.addOwner(accounts[1]);
        let newList = await bsh_coreV2.getOwners();
        assert(
            oldList.length === 1 && oldList[0] === accounts[0] &&
            newList.length === 2 && newList[0] === accounts[0] && newList[1] === accounts[1]
        );
    }); 
    
    it('Scenario 18: Should allow old owner to register a new coin - After adding new Owner', async () => {
        let _name3 = "TRON";
        await bsh_coreV2.register(_name3);
        output = await bsh_coreV2.coinNames();
        assert(
            output[0] === _native && output[1] === 'ICON' &&
            output[2] === 'wBTC' && output[3] === 'Ethereum' &&
            output[4] ===  'TRON'
        );
    });

    it('Scenario 19: Should allow new owner to register a new coin', async () => {   
        let _name3 = "BINANCE";
        await bsh_coreV2.register(_name3, {from: accounts[1]});
        output = await bsh_coreV2.coinNames();
        assert(
            output[0] === _native && output[1] === 'ICON' &&
            output[2] === 'wBTC' && output[3] === 'Ethereum' &&
            output[4] ===  'TRON' && output[5] === 'BINANCE'
        );
    }); 

    it('Scenario 20: Should allow new owner to update BSHPeriphery contract', async () => {
        //  Must clear BSHPerif setting since BSHPeriphery has been set
        //  The requirement: if (BSHPerif.address != address(0)) must check whether BSHPerif has any pending requests
        //  Instead of creating a MockBSHPerif, just clear BSHPerif setting 
        await bsh_coreV2.clearBSHPerifSetting();
        await bsh_coreV2.updateBSHPeriphery(accounts[3], {from: accounts[1]});
    });

    it('Scenario 21: Should also allow old owner to update BSHPeriphery contract - After adding new Owner', async () => {
        //  Must clear BSHPerif setting since BSHPeriphery has been set
        //  The requirement: if (BSHPerif.address != address(0)) must check whether BSHPerif has any pending requests
        //  Instead of creating a MockBSHPerif, just clear BSHPerif setting 
        await bsh_coreV2.clearBSHPerifSetting();
        await bsh_coreV2.updateBSHPeriphery(accounts[3], {from: accounts[0]});
    });

    it('Scenario 22: Should allow new owner to update the new URI', async () => {
        let new_uri = 'https://1234.iconee/'
        await bsh_coreV2.updateUri(new_uri, {from: accounts[1]});
    });

    it('Scenario 23: Should also allow old owner to update the new URI - After adding new Owner', async () => {
        let new_uri = 'https://1234.iconee/'
        await bsh_coreV2.updateUri(new_uri, {from: accounts[0]});
    });

    it('Scenario 24: Should allow new owner to update new fee ratio', async () => {
        let new_fee = 30;
        await bsh_coreV2.setFeeRatio(new_fee, {from: accounts[1]});

        assert(
            web3.utils.BN(await bsh_coreV2.feeNumerator()).toNumber() === new_fee
        );
    });

    it('Scenario 25: Should also allow old owner to update new fee ratio - After adding new Owner', async () => {
        let new_fee = 40;
        await bsh_coreV2.setFeeRatio(new_fee, {from: accounts[0]});

        assert(
            web3.utils.BN(await bsh_coreV2.feeNumerator()).toNumber() === new_fee
        );
    });

    it('Scenario 26: Should allow new owner to update new fixed fee', async () => {
        let new_fixed_fee = 3000000;
        await bsh_coreV2.setFixedFee(new_fixed_fee, {from: accounts[1]});

        assert(
            web3.utils.BN(await bsh_coreV2.fixedFee()).toNumber() === new_fixed_fee
        );
    });

    it('Scenario 27: Should also allow old owner to update new fixed fee - After adding new Owner', async () => {
        let new_fixed_fee = 4000000;
        await bsh_coreV2.setFixedFee(new_fixed_fee, {from: accounts[0]});

        assert(
            web3.utils.BN(await bsh_coreV2.fixedFee()).toNumber() === new_fixed_fee
        );
    });

    it('Scenario 28: Should revert when non-Owner tries to remove an Owner', async () => {
        let oldList = await bsh_coreV2.getOwners();
        await truffleAssert.reverts(
            bsh_coreV2.removeOwner.call(accounts[0], {from: accounts[2]}),
            "Unauthorized"
        );
        let newList = await bsh_coreV2.getOwners();
        assert(
            oldList.length === 2 && oldList[0] === accounts[0] && oldList[1] === accounts[1] &&
            newList.length === 2 && newList[0] === accounts[0] && newList[1] === accounts[1]
        );
    });

    it('Scenario 29: Should allow one current Owner to remove another Owner', async () => {
        let oldList = await bsh_coreV2.getOwners();
        await bsh_coreV2.removeOwner(accounts[0], {from: accounts[1]});
        let newList = await bsh_coreV2.getOwners();
        assert(
            oldList.length === 2 && oldList[0] === accounts[0] && oldList[1] === accounts[1] &&
            newList.length === 1 && newList[0] === accounts[1]
        );
    });

    it('Scenario 30: Should revert when the last Owner removes him/herself', async () => {
        let oldList = await bsh_coreV2.getOwners();
        await truffleAssert.reverts(
            bsh_coreV2.removeOwner.call(accounts[1], {from: accounts[1]}),
            "Unable to remove last Owner"
        );
        let newList = await bsh_coreV2.getOwners();
        assert(
            oldList.length === 1 && oldList[0] === accounts[1] &&
            newList.length === 1 && newList[0] === accounts[1]
        );
    });

    it('Scenario 31: Should revert when removed Owner tries to register a new coin', async () => {
        let _name3 = "KYBER";
        await truffleAssert.reverts(
            bsh_coreV2.register.call(_name3),
            'Unauthorized'
        );
        output = await bsh_coreV2.coinNames();
        assert(
            output[0] === _native && output[1] === 'ICON' &&
            output[2] === 'wBTC' && output[3] === 'Ethereum' &&
            output[4] ===  'TRON' && output[5] === 'BINANCE'
        );
    });

    it('Scenario 32: Should revert when removed Owner tries to update BSHPeriphery contract', async () => {
        //  Must clear BSHPerif setting since BSHPeriphery has been set
        //  The requirement: if (BSHPerif.address != address(0)) must check whether BSHPerif has any pending requests
        //  Instead of creating a MockBSHPerif, just clear BSHPerif setting 
        await bsh_coreV2.clearBSHPerifSetting();
        await truffleAssert.reverts(
            bsh_coreV2.updateBSHPeriphery.call(accounts[3], {from: accounts[0]}),
            'Unauthorized'
        );
    });

    it('Scenario 33: Should revert when removed Owner tries to update the new URI', async () => {
        let new_uri = 'https://1234.iconee/'
        await truffleAssert.reverts(
            bsh_coreV2.updateUri.call(new_uri, {from: accounts[0]}),
            'Unauthorized'
        );
    });

    it('Scenario 34: Should revert when removed Owner tries to update new fee ratio', async () => {
        let new_fee = 30;
        await truffleAssert.reverts(
            bsh_coreV2.setFeeRatio.call(new_fee, {from: accounts[0]}),
            'Unauthorized'
        );
    });

    it('Scenario 35: Should allow arbitrary client to query balance of an account', async () => {
        let _coin = 'ICON';
        let _id = await bsh_coreV2.coinId(_coin);
        let _value = 2000;
        await bsh_coreV2.mintMock(accounts[2], _id, _value);
        let balance = await bsh_coreV2.getBalanceOf(accounts[2], _coin, {from: accounts[2]});
        assert(
            web3.utils.BN(balance._usableBalance).toNumber() === _value &&
            web3.utils.BN(balance._lockedBalance).toNumber() === 0 &&
            web3.utils.BN(balance._refundableBalance).toNumber() === 0
        );
    });

    it('Scenario 36: Should allow arbitrary client to query a batch of balances of an account', async () => {
        let _coin1 = 'ICON';        let _coin2 = 'TRON';             
        let _id = await bsh_coreV2.coinId(_coin2);
        let _value = 10000;         let another_value = 2000;
        await bsh_coreV2.mintMock(accounts[2], _id, _value);
        let balance = await bsh_coreV2.getBalanceOfBatch(accounts[2], [_coin1,_coin2], {from: accounts[2]});
        assert(
            web3.utils.BN(balance._usableBalances[0]).toNumber() === another_value &&
            web3.utils.BN(balance._lockedBalances[0]).toNumber() === 0 &&
            web3.utils.BN(balance._refundableBalances[0]).toNumber() === 0 &&

            web3.utils.BN(balance._usableBalances[1]).toNumber() === _value &&
            web3.utils.BN(balance._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balance._refundableBalances[1]).toNumber() === 0
        );
    });

    it('Scenario 37: Should allow arbitrary client to query an Accumulated Fees', async () => {
        let _coin1 = 'ICON';    let _coin2 = 'TRON';
        let _value1 = 40000;    let _value2 = 10000;    let _native_value = 2000;
        await bsh_coreV2.setAggregationFee(_coin1, _value1);
        await bsh_coreV2.setAggregationFee(_coin2, _value2);
        await bsh_coreV2.setAggregationFee(_native, _native_value);
        let fees = await bsh_coreV2.getAccumulatedFees({from: accounts[3]});
        assert(
            fees[0].coinName === _native && Number(fees[0].value) === _native_value &&
            fees[1].coinName === _coin1 && Number(fees[1].value) === _value1 &&
            fees[2].coinName === 'wBTC' && Number(fees[2].value) === 0 &&
            fees[3].coinName === 'Ethereum' && Number(fees[3].value) === 0 &&
            fees[4].coinName === _coin2 && Number(fees[4].value) === _value2
        );
    });

    it('Scenario 38: Should revert when a client reclaims an exceeding amount', async () => {
        let _coin = 'ICON';     let _value = 10000;     let _exceedAmt = 20000;
        await bsh_coreV2.setRefundableBalance(accounts[2], _coin, _value);
        await truffleAssert.reverts(
            bsh_coreV2.reclaim.call(_coin, _exceedAmt, {from: accounts[2]}),
            "Imbalance"
        );
    });

    it('Scenario 39: Should revert when a client, which does not own a refundable, tries to reclaim', async () => {
        let _coin = 'ICON';     let _value = 10000;
        await truffleAssert.reverts(
            bsh_coreV2.reclaim.call(_coin, _value, {from: accounts[3]}),
            "Imbalance"
        );
    });

    it('Scenario 40: Should succeed when a client, which owns a refundable, tries to reclaim', async () => {
        let _coin = 'ICON';     let _value = 10000;
        let _id = await bsh_coreV2.coinId(_coin);
        await bsh_coreV2.mintMock(bsh_coreV2.address, _id, _value);
        let balanceBefore = await bsh_coreV2.getBalanceOf(accounts[2], _coin);
        await bsh_coreV2.reclaim(_coin, _value, {from: accounts[2]});
        let balanceAfter = await bsh_coreV2.getBalanceOf(accounts[2], _coin);
        assert(
            web3.utils.BN(balanceAfter._usableBalance).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalance).toNumber() + _value
        );
    });

    it('Scenario 41: Should not allow any clients (even a contract owner) to call a refund()', async () => {
        //  This function should be called only by itself (BSHCore contract)
        let _coin = 'ICON';     let _value = 10000;
        await truffleAssert.reverts(
            bsh_coreV2.refund.call(accounts[0], _coin, _value),
            "Unauthorized"
        );
    });

    it('Scenario 42: Should not allow any clients (even a contract owner) to call a mint()', async () => {
        //  This function should be called only by BSHPeriphery contract
        let _coin = 'ICON';     let _value = 10000;
        await truffleAssert.reverts(
            bsh_coreV2.mint.call(accounts[0], _coin, _value),
            "Unauthorized"
        );
    });

    it('Scenario 43: Should not allow any clients (even a contract owner) to call a handleResponseService()', async () => {
        //  This function should be called only by BSHPeriphery contract
        let RC_OK = 0;
        let _coin = 'ICON';     let _value = 10000;     let _fee = 1;   let _rspCode = RC_OK;
        await truffleAssert.reverts(
            bsh_coreV2.handleResponseService.call(accounts[0], _coin, _value, _fee, _rspCode),
            "Unauthorized"
        );
    });

    it('Scenario 44: Should not allow any clients (even a contract owner) to call a transferFees()', async () => {
        //  This function should be called only by BSHPeriphery contract
        let _fa = 'btp://1234.iconee/0x1234567812345678';
        await truffleAssert.reverts(
            bsh_coreV2.transferFees.call(_fa),
            "Unauthorized"
        );
    });
});
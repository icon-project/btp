const BSHPerif = artifacts.require("BSHPeriphery");
const MockBSHCore = artifacts.require("MockBSHCore");
const CheckParseAddress = artifacts.require("CheckParseAddress");
const { assert } = require('chai');
const truffleAssert = require('truffle-assertions');

contract('BSHCore Unit Tests', (accounts) => {
    let bsh_core;
    let _native = 'PARA';                   let _fee = 10;
    let _uri = 'https://github.com/icon-project/btp'

    before(async () => {
        bsh_core = await MockBSHCore.new();
        await bsh_core.initialize(_uri, _native, _fee);
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
        await bsh_core.updateBSHPeriphery(accounts[2]);
    });

    it('Scenario 5: Should revert when arbitrary client updates BSHPeriphery contract', async () => {
        await truffleAssert.reverts(
            bsh_core.updateBSHPeriphery.call(accounts[2], {from: accounts[1]}),
            "Unauthorized"
        );
    });

    it('Scenario 6: Should allow contract owner to update a new URI', async () => {
        let new_uri = 'https://1234.iconee/'
        await bsh_core.updateUri(new_uri);
    });

    it('Scenario 7: Should revert when arbitrary client update a new URI', async () => {
        let new_uri = 'https://1234.iconee/'
        await truffleAssert.reverts(
            bsh_core.updateUri.call(new_uri, {from: accounts[1]}),
            "Unauthorized"
        );
    });

    it('Scenario 8: Should allow contract owner to update fee ratio', async () => {
        let new_fee = 20;
        await bsh_core.setFeeRatio(new_fee);
    });

    it('Scenario 9: Should revert when arbitrary client updates fee ratio', async () => {
        let new_fee = 20;
        await truffleAssert.reverts(
            bsh_core.setFeeRatio.call(new_fee, {from: accounts[1]}),
            "Unauthorized"
        );
    });

    it('Scenario 10: Should revert when Fee Numerator is higher than Fee Denominator', async () => {
        let new_fee = 20000;
        await truffleAssert.reverts(
            bsh_core.setFeeRatio.call(new_fee),
            "InvalidSetting"
        );
    });

    it('Scenario 11: Should receive an id of a given coin name when querying a valid supporting coin', async () => {
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

    it('Scenario 12: Should receive an id = 0 when querying an invalid supporting coin', async () => {
        let _query = "EOS";
        let result = await bsh_core.coinId(_query);
        assert(
            web3.utils.BN(result).toNumber() === 0
        );
    }); 

    it('Scenario 13: Should revert when a non-Owner tries to add a new Owner', async () => {
        let oldList = await bsh_core.getOwners();
        await truffleAssert.reverts(
            bsh_core.addOwner.call(accounts[1], {from: accounts[2]}),
            "Unauthorized"
        );
        let newList = await bsh_core.getOwners();
        assert(
            oldList.length === 1 && oldList[0] === accounts[0] &&
            newList.length === 1 && newList[0] === accounts[0]
        );
    }); 

    it('Scenario 14: Should allow a current Owner to add a new Owner', async () => {
        let oldList = await bsh_core.getOwners();
        await bsh_core.addOwner(accounts[1]);
        let newList = await bsh_core.getOwners();
        assert(
            oldList.length === 1 && oldList[0] === accounts[0] &&
            newList.length === 2 && newList[0] === accounts[0] && newList[1] === accounts[1]
        );
    }); 
    
    it('Scenario 15: Should allow old owner to register a new coin - After adding new Owner', async () => {
        let _name3 = "TRON";
        await bsh_core.register(_name3);
        output = await bsh_core.coinNames();
        assert(
            output[0] === _native && output[1] === 'ICON' &&
            output[2] === 'wBTC' && output[3] === 'Ethereum' &&
            output[4] ===  'TRON'
        );
    });

    it('Scenario 16: Should allow new owner to register a new coin', async () => {   
        let _name3 = "BINANCE";
        await bsh_core.register(_name3, {from: accounts[1]});
        output = await bsh_core.coinNames();
        assert(
            output[0] === _native && output[1] === 'ICON' &&
            output[2] === 'wBTC' && output[3] === 'Ethereum' &&
            output[4] ===  'TRON' && output[5] === 'BINANCE'
        );
    }); 

    it('Scenario 17: Should allow new owner to update BSHPeriphery contract', async () => {
        //  Must clear BSHPerif setting since BSHPeriphery has been set
        //  The requirement: if (BSHPerif.address != address(0)) must check whether BSHPerif has any pending requests
        //  Instead of creating a MockBSHPerif, just clear BSHPerif setting 
        await bsh_core.clearBSHPerifSetting();
        await bsh_core.updateBSHPeriphery(accounts[3], {from: accounts[1]});
    });

    it('Scenario 18: Should also allow old owner to update BSHPeriphery contract - After adding new Owner', async () => {
        //  Must clear BSHPerif setting since BSHPeriphery has been set
        //  The requirement: if (BSHPerif.address != address(0)) must check whether BSHPerif has any pending requests
        //  Instead of creating a MockBSHPerif, just clear BSHPerif setting 
        await bsh_core.clearBSHPerifSetting();
        await bsh_core.updateBSHPeriphery(accounts[3], {from: accounts[0]});
    });

    it('Scenario 19: Should allow new owner to update the new URI', async () => {
        let new_uri = 'https://1234.iconee/'
        await bsh_core.updateUri(new_uri, {from: accounts[1]});
    });

    it('Scenario 20: Should also allow old owner to update the new URI - After adding new Owner', async () => {
        let new_uri = 'https://1234.iconee/'
        await bsh_core.updateUri(new_uri, {from: accounts[0]});
    });

    it('Scenario 21: Should allow new owner to update new fee ratio', async () => {
        let new_fee = 30;
        await bsh_core.setFeeRatio(new_fee, {from: accounts[1]});
    });

    it('Scenario 22: Should also allow old owner to update new fee ratio - After adding new Owner', async () => {
        let new_fee = 30;
        await bsh_core.setFeeRatio(new_fee, {from: accounts[0]});
    });

    it('Scenario 23: Should revert when non-Owner tries to remove an Owner', async () => {
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

    it('Scenario 24: Should allow one current Owner to remove another Owner', async () => {
        let oldList = await bsh_core.getOwners();
        await bsh_core.removeOwner(accounts[0], {from: accounts[1]});
        let newList = await bsh_core.getOwners();
        assert(
            oldList.length === 2 && oldList[0] === accounts[0] && oldList[1] === accounts[1] &&
            newList.length === 1 && newList[0] === accounts[1]
        );
    });

    it('Scenario 25: Should revert when the last Owner removes him/herself', async () => {
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

    it('Scenario 26: Should revert when removed Owner tries to register a new coin', async () => {
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

    it('Scenario 27: Should revert when removed Owner tries to update BSHPeriphery contract', async () => {
        //  Must clear BSHPerif setting since BSHPeriphery has been set
        //  The requirement: if (BSHPerif.address != address(0)) must check whether BSHPerif has any pending requests
        //  Instead of creating a MockBSHPerif, just clear BSHPerif setting 
        await bsh_core.clearBSHPerifSetting();
        await truffleAssert.reverts(
            bsh_core.updateBSHPeriphery.call(accounts[3], {from: accounts[0]}),
            'Unauthorized'
        );
    });

    it('Scenario 28: Should revert when removed Owner tries to update the new URI', async () => {
        let new_uri = 'https://1234.iconee/'
        await truffleAssert.reverts(
            bsh_core.updateUri.call(new_uri, {from: accounts[0]}),
            'Unauthorized'
        );
    });

    it('Scenario 29: Should revert when removed Owner tries to update new fee ratio', async () => {
        let new_fee = 30;
        await truffleAssert.reverts(
            bsh_core.setFeeRatio.call(new_fee, {from: accounts[0]}),
            'Unauthorized'
        );
    });

    it('Scenario 30: Should allow arbitrary client to query balance of an account', async () => {
        let _coin = 'ICON';
        let _id = await bsh_core.coinId(_coin);
        let _value = 2000;
        await bsh_core.mintMock(accounts[2], _id, _value);
        let balance = await bsh_core.getBalanceOf(accounts[2], _coin, {from: accounts[2]});
        assert(
            web3.utils.BN(balance._usableBalance).toNumber() === _value &&
            web3.utils.BN(balance._lockedBalance).toNumber() === 0 &&
            web3.utils.BN(balance._refundableBalance).toNumber() === 0
        );
    });

    it('Scenario 31: Should allow arbitrary client to query a batch of balances of an account', async () => {
        let _coin1 = 'ICON';        let _coin2 = 'TRON';             
        let _id = await bsh_core.coinId(_coin2);
        let _value = 10000;         let another_value = 2000;
        await bsh_core.mintMock(accounts[2], _id, _value);
        let balance = await bsh_core.getBalanceOfBatch(accounts[2], [_coin1,_coin2], {from: accounts[2]});
        assert(
            web3.utils.BN(balance._usableBalances[0]).toNumber() === another_value &&
            web3.utils.BN(balance._lockedBalances[0]).toNumber() === 0 &&
            web3.utils.BN(balance._refundableBalances[0]).toNumber() === 0 &&

            web3.utils.BN(balance._usableBalances[1]).toNumber() === _value &&
            web3.utils.BN(balance._lockedBalances[1]).toNumber() === 0 &&
            web3.utils.BN(balance._refundableBalances[1]).toNumber() === 0
        );
    });

    it('Scenario 32: Should allow arbitrary client to query an Accumulated Fees', async () => {
        let _coin1 = 'ICON';    let _coin2 = 'TRON';
        let _value1 = 40000;    let _value2 = 10000;    let _native_value = 2000;
        await bsh_core.setAggregationFee(_coin1, _value1);
        await bsh_core.setAggregationFee(_coin2, _value2);
        await bsh_core.setAggregationFee(_native, _native_value);
        let fees = await bsh_core.getAccumulatedFees({from: accounts[3]});
        assert(
            fees[0].coinName === _native && Number(fees[0].value) === _native_value &&
            fees[1].coinName === _coin1 && Number(fees[1].value) === _value1 &&
            fees[2].coinName === 'wBTC' && Number(fees[2].value) === 0 &&
            fees[3].coinName === 'Ethereum' && Number(fees[3].value) === 0 &&
            fees[4].coinName === _coin2 && Number(fees[4].value) === _value2
        );
    });

    it('Scenario 33: Should revert when a client reclaims an exceeding amount', async () => {
        let _coin = 'ICON';     let _value = 10000;     let _exceedAmt = 20000;
        await bsh_core.setRefundableBalance(accounts[2], _coin, _value);
        await truffleAssert.reverts(
            bsh_core.reclaim.call(_coin, _exceedAmt, {from: accounts[2]}),
            "Imbalance"
        );
    });

    it('Scenario 34: Should revert when a client, which does not own a refundable, tries to reclaim', async () => {
        let _coin = 'ICON';     let _value = 10000;
        await truffleAssert.reverts(
            bsh_core.reclaim.call(_coin, _value, {from: accounts[3]}),
            "Imbalance"
        );
    });

    it('Scenario 35: Should succeed when a client, which owns a refundable, tries to reclaim', async () => {
        let _coin = 'ICON';     let _value = 10000;
        let _id = await bsh_core.coinId(_coin);
        await bsh_core.mintMock(bsh_core.address, _id, _value);
        let balanceBefore = await bsh_core.getBalanceOf(accounts[2], _coin);
        await bsh_core.reclaim(_coin, _value, {from: accounts[2]});
        let balanceAfter = await bsh_core.getBalanceOf(accounts[2], _coin);
        assert(
            web3.utils.BN(balanceAfter._usableBalance).toNumber() === 
                web3.utils.BN(balanceBefore._usableBalance).toNumber() + _value
        );
    });

    it('Scenario 36: Should not allow any clients (even a contract owner) to call a refund()', async () => {
        //  This function should be called only by itself (BSHCore contract)
        let _coin = 'ICON';     let _value = 10000;
        await truffleAssert.reverts(
            bsh_core.refund.call(accounts[0], _coin, _value),
            "Unauthorized"
        );
    });

    it('Scenario 37: Should not allow any clients (even a contract owner) to call a mint()', async () => {
        //  This function should be called only by BSHPeriphery contract
        let _coin = 'ICON';     let _value = 10000;
        await truffleAssert.reverts(
            bsh_core.mint.call(accounts[0], _coin, _value),
            "Unauthorized"
        );
    });

    it('Scenario 38: Should not allow any clients (even a contract owner) to call a handleResponseService()', async () => {
        //  This function should be called only by BSHPeriphery contract
        let RC_OK = 0;
        let _coin = 'ICON';     let _value = 10000;     let _fee = 1;   let _rspCode = RC_OK;
        await truffleAssert.reverts(
            bsh_core.handleResponseService.call(accounts[0], _coin, _value, _fee, _rspCode),
            "Unauthorized"
        );
    });

    it('Scenario 39: Should not allow any clients (even a contract owner) to call a transferFees()', async () => {
        //  This function should be called only by BSHPeriphery contract
        let _fa = 'btp://1234.iconee/0x1234567812345678';
        await truffleAssert.reverts(
            bsh_core.transferFees.call(_fa),
            "Unauthorized"
        );
    });
});

//  BSHPeriphery is being used for communications among BSHCore and BMCPeriphery contract
//  Thus, all tests relating to BSHPeriphery will be moved to Integration Test
//  This part just covers some basic feature which is checking an authorization
contract('BSHPeriphery Unit Tests', (accounts) => {
    let bsh_perif;

    before(async () => {
        bsh_perif = await BSHPerif.new();
    });

    it('Scenario 1: Should revert when a client, which is not a BMCPeriphery contract, calls handleBTPMessage()', async () => {
        let _from = '1234.iconee';      let _svc = 'Coin/WrappedCoin';      let _sn = 10;
        await truffleAssert.reverts(
            bsh_perif.handleBTPMessage.call(_from, _svc, _sn, '0x'),
            "Unauthorized"
        );
    });

    it('Scenario 2: Should revert when a client, which is not a BMCPeriphery contract, calls handleBTPError()', async () => {
        let _from = '1234.iconee';      let _svc = 'Coin/WrappedCoin';      let _sn = 10;       let RC_OK = 0;
        await truffleAssert.reverts(
            bsh_perif.handleBTPError.call(_from, _svc, _sn, RC_OK, ''),
            "Unauthorized"
        );
    });

    it('Scenario 3: Should revert when any clients try to call handleRequestService()', async () => {
        //  This function should only be called internally even though it was set external
        let _to = '1234.iconee';      let _svc = 'Coin/WrappedCoin';      let _sn = 10;       let RC_OK = 0;
        let _assets = [ ['PARA', 1000], ['ICON', 1000], ['TRON', 1000], ['Ethereum', 1000] ];
        await truffleAssert.reverts(
            bsh_perif.handleRequestService.call(_to, _assets),
            "Unauthorized"
        );
    });

    it('Scenario 4: Should revert when a client, which is not a BMCPeriphery contract, calls handleFeeGathering()', async () => {
        let _fa = 'btp://1234.iconee/0x12345678012345678';      let _svc = 'Coin/WrappedCoin';
        await truffleAssert.reverts(
            bsh_perif.handleFeeGathering.call(_fa, _svc),
            "Unauthorized"
        );
    });
});

contract('ParseAddress Library Unit Test', (accounts) => {
    let cpa;

    before(async () => {
        cpa = await CheckParseAddress.new();
    });

    describe('Convert String to Adress', () => {
        it('Should revert when string address has invalid length', async() => {
            const strAddr = accounts[0] + '1';
            await truffleAssert.reverts(
                cpa.convertStringToAddress(strAddr),
                "Invalid address format"
            );
        });

        it('Should revert when string address has invalid prefix', async() => {
            const strAddr = accounts[0];
            const new1 = '1' + strAddr.slice(1, strAddr.length - 1);
            await truffleAssert.reverts(
                cpa.convertStringToAddress(new1),
                "Invalid address format"
            );

            const new2 = 'o' + strAddr.slice(1, strAddr.length - 1);
            await truffleAssert.reverts(
                cpa.convertStringToAddress(new2),
                "Invalid address format"
            );

            const new3 = 'h' + strAddr.slice(1, strAddr.length - 1);
            await truffleAssert.reverts(
                cpa.convertStringToAddress(new3),
                "Invalid address format"
            );

            const new4 = 'c' + strAddr.slice(1, strAddr.length - 1);
            await truffleAssert.reverts(
                cpa.convertStringToAddress(new4),
                "Invalid address format"
            );

            const new5 = '0a' + strAddr.slice(2, strAddr.length - 1);
            await truffleAssert.reverts(
                cpa.convertStringToAddress(new5),
                "Invalid address format"
            );
        });

        it('Should revert when string address has no prefix', async() => {
            const strAddr = accounts[0];
            const new1 = strAddr.slice(2, strAddr.length - 1);
            await truffleAssert.reverts(
                cpa.convertStringToAddress(new1),
                "Invalid address format"
            );
        });

        //  This unit test verifies the case that a string address
        //  has length of 42 and has a prefix '0x'
        //  but the address is generated randomly without checksum
        it('Should revert when string address is an arbitrary string that looks like a valid address', async() => {
            const strAddr = '0xa0ff1ad998affec330ffca12389edab4312a2b81'
            await truffleAssert.reverts(
                cpa.convertStringToAddress(strAddr),
                "Invalid checksum"
            );
        });

        it('Should revert when string address is an arbitrary string', async() => {
            const strAddr = '0xddawjoh2j3h1kjhffklashhsdklj1h2i3h121h23';
            await truffleAssert.reverts(
                cpa.convertStringToAddress(strAddr),
                "Invalid address"
            );
        });

        it('Should return an address when string address is valid', async() => {
            let strAddr = '0xf24FF3a9CF04c71Dbc94D0b566f7A27B94566cac';
            let res = await cpa.convertStringToAddress(strAddr);
            assert(
                web3.utils.isAddress(res)
            );

            strAddr = web3.utils.toChecksumAddress('0xc1912fee45d61c87cc5ea59dae31190fffff232d');
            res = await cpa.convertStringToAddress(strAddr);
            assert(
                web3.utils.isAddress(res)
            );
        })
    });

    describe('Convert Address to String', () => {
        it('Should convert address to string with a valid checksum', async () => {
            const account = web3.eth.accounts.create();
            const res = await cpa.convertAddressToString(account.address.toLowerCase());

            assert.equal(res, account.address);
            assert.isTrue(web3.utils.checkAddressChecksum(res));
        });
    });
})
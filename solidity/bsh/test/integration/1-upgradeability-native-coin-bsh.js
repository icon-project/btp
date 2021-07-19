const BSHPerifV1 = artifacts.require("BSHPeripheryV1");
const BSHPerifV2 = artifacts.require("BSHPeripheryV2");
const BSHCoreV1 = artifacts.require("BSHCoreV1");
const BSHCoreV2 = artifacts.require("BSHCoreV2");
const BMC = artifacts.require("MockBMC");
const Holder = artifacts.require("AnotherHolder");
const NotPayable = artifacts.require("NotPayable");
const NonRefundable = artifacts.require("NonRefundable");
const Refundable = artifacts.require("Refundable");
const EncodeMsg = artifacts.require("EncodeMessage");
const { assert } = require('chai');
const truffleAssert = require('truffle-assertions');
const { deployProxy, upgradeProxy } = require('@openzeppelin/truffle-upgrades');

//  All of these unit tests below check upgradability of smart contract using Openzeppelin Library and SDK
//  BSHService and BSHCoin contracts have two versions
//  These two versions cover many upgradeable features:
//  - Adding additional state variables
//  - Adding additional functions
contract('NativeCoinBSH contracts - After Upgrading Contract', (accounts) => {
    describe('PRA BSHCore Query and Management - After Upgrading Contract', () => {
        let bsh_perifV1, bsh_perifV2, bsh_coreV1, bsh_coreV2;
        let _native = 'PARA';                   let _fee = 10;
        let service = 'Coin/WrappedCoin';       let _uri = 'https://github.com/icon-project/btp'
        let _net = '1234.iconee';               let _bmcICON = 'btp://1234.iconee/0x1234567812345678';
        let REPONSE_HANDLE_SERVICE = 2;         let RC_OK = 0;

        before(async () => {
            bmc = await BMC.new('1234.pra');
            bsh_coreV1 = await deployProxy(BSHCoreV1, [_uri, _native, _fee]);
            bsh_perifV1 = await deployProxy(BSHPerifV1, [bmc.address, bsh_coreV1.address, service]);
            encode_msg = await EncodeMsg.new();
            await bsh_coreV1.updateBSHPeriphery(bsh_perifV1.address);
            bsh_perifV2 = await upgradeProxy(bsh_perifV1.address, BSHPerifV2);
            bsh_coreV2 = await upgradeProxy(bsh_coreV1.address, BSHCoreV2);
            await bmc.addService(service, bsh_perifV2.address);
            await bmc.addVerifier(_net, accounts[1]);
            await bmc.addLink(_bmcICON);
        });

        it('Re-initialize BSHService Contract - Failure', async () => {
            await truffleAssert.reverts(
                bsh_perifV2.initialize.call(bmc.address, bsh_coreV2.address, service),
                "Initializable: contract is already initialized"
            );
        });

        it('Re-initialize BSHCoin Contract - Failure', async () => {
            await truffleAssert.reverts(
                bsh_coreV2.initialize.call(_uri, _native, _fee),
                "Initializable: contract is already initialized"
            );
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
            await bsh_coreV2.updateBSHPeriphery(bsh_perifV2.address);
        });

        it('Scenario 5: Should revert when arbitrary client updates BSHPeriphery contract', async () => {
            await truffleAssert.reverts(
                bsh_coreV2.updateBSHPeriphery.call(bsh_perifV2.address, {from: accounts[1]}),
                "Unauthorized"
            );
        });

        it('Scenario 6: Should revert when contract owner updates BSHPeriphery while this contract has pending requests', async () => {
            let _to = 'btp://1234.iconee/0x12345678';
            await bsh_coreV2.transfer(_to, {from: accounts[0], value: 100000000});
            await truffleAssert.reverts(
                bsh_coreV2.updateBSHPeriphery.call(accounts[2]),
                "HasPendingRequest"
            );
            //  Clear pending request
            let _msg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_OK, "");
            await bmc.receiveResponse(_net, service, 1, _msg);
        });


        it('Scenario 7: Should allow contract owner to update a new URI', async () => {
            let new_uri = 'https://1234.iconee/'
            await bsh_coreV2.updateUri(new_uri);
        });

        it('Scenario 8: Should revert when arbitrary client update a new URI', async () => {
            let new_uri = 'https://1234.iconee/'
            await truffleAssert.reverts(
                bsh_coreV2.updateUri.call(new_uri, {from: accounts[1]}),
                "Unauthorized"
            );
        });

        it('Scenario 9: Should allow contract owner to update fee ratio', async () => {
            let new_fee = 20;
            await bsh_coreV2.setFeeRatio(new_fee);
        });

        it('Scenario 10: Should revert when arbitrary client updates fee ratio', async () => {
            let new_fee = 20;
            await truffleAssert.reverts(
                bsh_coreV2.setFeeRatio.call(new_fee, {from: accounts[1]}),
                "Unauthorized"
            );
        });

        it('Scenario 11: Should revert when Fee Numerator is higher than Fee Denominator', async () => {
            let new_fee = 20000;
            await truffleAssert.reverts(
                bsh_coreV2.setFeeRatio.call(new_fee),
                "InvalidSetting"
            );
        });

        it('Scenario 12: Should receive an id of a given coin name when querying a valid supporting coin', async () => {
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

        it('Scenario 13: Should receive an id = 0 when querying an invalid supporting coin', async () => {
            let _query = "EOS";
            let result = await bsh_coreV2.coinId(_query);
            assert(
                web3.utils.BN(result).toNumber() === 0
            );
        }); 

        it('Scenario 14: Should revert when a non-Owner tries to add a new Owner', async () => {
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

        it('Scenario 15: Should allow a current Owner to add a new Owner', async () => {
            let oldList = await bsh_coreV2.getOwners();
            await bsh_coreV2.addOwner(accounts[1]);
            let newList = await bsh_coreV2.getOwners();
            assert(
                oldList.length === 1 && oldList[0] === accounts[0] &&
                newList.length === 2 && newList[0] === accounts[0] && newList[1] === accounts[1]
            );
        }); 
        
        it('Scenario 16: Should allow old owner to register a new coin - After adding new Owner', async () => {
            let _name3 = "TRON";
            await bsh_coreV2.register(_name3);
            output = await bsh_coreV2.coinNames();
            assert(
                output[0] === _native && output[1] === 'ICON' &&
                output[2] === 'wBTC' && output[3] === 'Ethereum' &&
                output[4] ===  'TRON'
            );
        });

        it('Scenario 17: Should allow new owner to register a new coin', async () => {   
            let _name3 = "BINANCE";
            await bsh_coreV2.register(_name3, {from: accounts[1]});
            output = await bsh_coreV2.coinNames();
            assert(
                output[0] === _native && output[1] === 'ICON' &&
                output[2] === 'wBTC' && output[3] === 'Ethereum' &&
                output[4] ===  'TRON' && output[5] === 'BINANCE'
            );
        }); 

        it('Scenario 18: Should allow new owner to update BSHPeriphery contract', async () => {
            let newBSHPerif = await BSHPerifV2.new();
            await bsh_coreV2.updateBSHPeriphery(newBSHPerif.address, {from: accounts[1]});
        });

        it('Scenario 19: Should also allow old owner to update BSHPeriphery contract - After adding new Owner', async () => {
            let newBSHPerif = await BSHPerifV2.new();
            await bsh_coreV2.updateBSHPeriphery(newBSHPerif.address, {from: accounts[0]});
        });

        it('Scenario 20: Should allow new owner to update the new URI', async () => {
            let new_uri = 'https://1234.iconee/'
            await bsh_coreV2.updateUri(new_uri, {from: accounts[1]});
        });

        it('Scenario 21: Should also allow old owner to update the new URI - After adding new Owner', async () => {
            let new_uri = 'https://1234.iconee/'
            await bsh_coreV2.updateUri(new_uri, {from: accounts[0]});
        });

        it('Scenario 22: Should allow new owner to update new fee ratio', async () => {
            let new_fee = 30;
            await bsh_coreV2.setFeeRatio(new_fee, {from: accounts[1]});
        });

        it('Scenario 23: Should also allow old owner to update new fee ratio - After adding new Owner', async () => {
            let new_fee = 30;
            await bsh_coreV2.setFeeRatio(new_fee, {from: accounts[0]});
        });

        it('Scenario 24: Should revert when non-Owner tries to remove an Owner', async () => {
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

        it('Scenario 25: Should allow one current Owner to remove another Owner', async () => {
            let oldList = await bsh_coreV2.getOwners();
            await bsh_coreV2.removeOwner(accounts[0], {from: accounts[1]});
            let newList = await bsh_coreV2.getOwners();
            assert(
                oldList.length === 2 && oldList[0] === accounts[0] && oldList[1] === accounts[1] &&
                newList.length === 1 && newList[0] === accounts[1]
            );
        });

        it('Scenario 26: Should revert when the last Owner removes him/herself', async () => {
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

        it('Scenario 27: Should revert when removed Owner tries to register a new coin', async () => {
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

        it('Scenario 28: Should revert when removed Owner tries to update BSHPeriphery contract', async () => {
            await truffleAssert.reverts(
                bsh_coreV2.updateBSHPeriphery.call(accounts[3], {from: accounts[0]}),
                'Unauthorized'
            );
        });

        it('Scenario 29: Should revert when removed Owner tries to update the new URI', async () => {
            let new_uri = 'https://1234.iconee/'
            await truffleAssert.reverts(
                bsh_coreV2.updateUri.call(new_uri, {from: accounts[0]}),
                'Unauthorized'
            );
        });

        it('Scenario 30: Should revert when removed Owner tries to update new fee ratio', async () => {
            let new_fee = 30;
            await truffleAssert.reverts(
                bsh_coreV2.setFeeRatio.call(new_fee, {from: accounts[0]}),
                'Unauthorized'
            );
        });
    });

    describe('As a user, I want to send PRA to ICON blockchain - After Upgrading Contract', () => {
        let bsh_perifV1, bsh_perifV2, bsh_coreV1, bsh_coreV2, bmc, nonrefundable, refundable;
        let service = 'Coin/WrappedCoin';               let _bmcICON = 'btp://1234.iconee/0x1234567812345678';
        let _net = '1234.iconee';                       let _to = 'btp://1234.iconee/0x12345678';
        let RC_OK = 0;                                  let RC_ERR = 1;    
        let _amt = 5000;                                let deposit = 100000;
        let _native = 'PARA';                           let _fee = 10;                         
        let REPONSE_HANDLE_SERVICE = 2;                 let _uri = 'https://github.com/icon-project/btp';

        before(async () => {
            bmc = await BMC.new('1234.pra');
            bsh_coreV1 = await deployProxy(BSHCoreV1, [_uri, _native, _fee]);
            bsh_perifV1 = await deployProxy(BSHPerifV1, [bmc.address, bsh_coreV1.address, service]);
            encode_msg = await EncodeMsg.new();
            await bsh_coreV1.updateBSHPeriphery(bsh_perifV1.address);
            await bmc.addService(service,bsh_perifV1.address);
            bsh_perifV2 = await upgradeProxy(bsh_perifV1.address, BSHPerifV2);
            bsh_coreV2 = await upgradeProxy(bsh_coreV1.address, BSHCoreV2);
            nonrefundable = await NonRefundable.new();
            refundable = await Refundable.new();
            await bmc.addVerifier(_net, accounts[1]);
            await bmc.addLink(_bmcICON);
        });

        it('Scenario 1: Should revert when transferring native coins to an invalid BTP Address format', async () => {
            let invalid_destination = '1234.iconee/0x12345678';
            await truffleAssert.reverts(
                bsh_coreV2.transfer.call(invalid_destination, {from: accounts[0], value: 5000}),
                "revert"
            ); 
            bsh_coin_balance = await bsh_coreV2.getBalanceOf(bsh_coreV2.address, _native);
            account_balance = await bsh_coreV2.getBalanceOf(accounts[0], _native);
            assert(
                web3.utils.BN(bsh_coin_balance._usableBalance).toNumber() === 0 &&
                web3.utils.BN(account_balance._lockedBalance).toNumber() === 0
            );
        });

        it('Scenario 2: Should revert when transferring zero coin' , async () => {
            await truffleAssert.reverts(
                bsh_coreV2.transfer.call(_to, {from: accounts[0], value: 0}),
                "InvalidAmount"
            ); 
        });

        it('Scenario 3: Should revert when charging fee is zero' , async () => {
            await truffleAssert.reverts(
                bsh_coreV2.transfer.call(_to, {from: accounts[0], value: 10}),
                "InvalidAmount"
            ); 
        });

        it('Scenario 4: Should revert when transferring to an invalid network/not supported network' , async () => {
            let invalid_destination = 'btp://1234.eos/0x12345678';
            await truffleAssert.reverts(
                bsh_coreV2.transfer.call(invalid_destination, {from: accounts[1], value: 5000}),
                "BMCRevertNotExistsBMV"
            ); 
        });

        it('Scenario 5: Should succeed when Account client transferring a valid native coin to a side chain', async () => {
            let account_balanceBefore = await bsh_coreV2.getBalanceOf(accounts[0], _native);
            let output = await bsh_coreV2.transfer(_to, {from: accounts[0], value: _amt});
            let account_balanceAfter = await bsh_coreV2.getBalanceOf(accounts[0], _native);
            let bsh_coin_balance = await bsh_coreV2.getBalanceOf(bsh_coreV2.address, _native);
            let chargedFee = Math.floor(_amt/ 1000);
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
            let account_balanceBefore = await bsh_coreV2.getBalanceOf(accounts[0], _native);
            let _msg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_OK, "");
            await bmc.receiveResponse(_net, service, 1, _msg);
            let account_balanceAfter = await bsh_coreV2.getBalanceOf(accounts[0], _native);
            let fees = await bsh_coreV2.getAccumulatedFees();
            // TODO: catch emit event TransferEnd throwing from BSHService contract
            assert(
                fees[0].coinName === _native && 
                Number(fees[0].value) === Math.floor(_amt/ 1000) &&
                web3.utils.BN(account_balanceBefore._lockedBalance).toNumber() === _amt &&
                web3.utils.BN(account_balanceAfter._lockedBalance).toNumber() === 0
            );
        });

        it('Scenario 5: Should succeed when Account client transferring a valid native coin to a side chain', async () => {
            let account_balanceBefore = await bsh_coreV2.getBalanceOf(accounts[0], _native);
            let output = await bsh_coreV2.transfer(_to, {from: accounts[0], value: _amt});
            let account_balanceAfter = await bsh_coreV2.getBalanceOf(accounts[0], _native);
            let bsh_coin_balance = await bsh_coreV2.getBalanceOf(bsh_coreV2.address, _native);
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
            let account_balanceBefore = await bsh_coreV2.getBalanceOf(accounts[0], _native);
            let bsh_coin_balance_before = await bsh_coreV2.getBalanceOf(bsh_coreV2.address, _native);
            let _msg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_ERR, "");
            await bmc.receiveResponse(_net, service, 2, _msg);
            let account_balanceAfter = await bsh_coreV2.getBalanceOf(accounts[0], _native);
            let bsh_coin_balance_after = await bsh_coreV2.getBalanceOf(bsh_coreV2.address, _native);
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
            let contract_balanceBefore = await bsh_coreV2.getBalanceOf(nonrefundable.address, _native);
            let bsh_coin_balance_before = await bsh_coreV2.getBalanceOf(bsh_coreV2.address, _native);
            await nonrefundable.transfer(bsh_coreV2.address, _to, _amt);
            let contract_balanceAfter = await bsh_coreV2.getBalanceOf(nonrefundable.address, _native);
            let bsh_coin_balance_after = await bsh_coreV2.getBalanceOf(bsh_coreV2.address, _native);
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
            let contract_balanceBefore = await bsh_coreV2.getBalanceOf(nonrefundable.address, _native);
            let bsh_coin_balance_before = await bsh_coreV2.getBalanceOf(bsh_coreV2.address, _native);
            let _msg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_ERR, "");
            await bmc.receiveResponse(_net, service, 3, _msg);
            let contract_balanceAfter = await bsh_coreV2.getBalanceOf(nonrefundable.address, _native);
            let bsh_coin_balance_after = await bsh_coreV2.getBalanceOf(bsh_coreV2.address, _native);
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
            let contract_balanceBefore = await bsh_coreV2.getBalanceOf(refundable.address, _native);
            let bsh_coin_balance_before = await bsh_coreV2.getBalanceOf(bsh_coreV2.address, _native);
            await refundable.transfer(bsh_coreV2.address, _to, _amt);
            let contract_balanceAfter = await bsh_coreV2.getBalanceOf(refundable.address, _native);
            let bsh_coin_balance_after = await bsh_coreV2.getBalanceOf(bsh_coreV2.address, _native);
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
            let contract_balanceBefore = await bsh_coreV2.getBalanceOf(refundable.address, _native);
            let bsh_coin_balance_before = await bsh_coreV2.getBalanceOf(bsh_coreV2.address, _native);
            let _msg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_ERR, "");
            await bmc.receiveResponse(_net, service, 4, _msg);
            let contract_balanceAfter = await bsh_coreV2.getBalanceOf(refundable.address, _native);
            let bsh_coin_balance_after = await bsh_coreV2.getBalanceOf(bsh_coreV2.address, _native);
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

    describe('As a user, I want to send ERC1155_ICX to ICON blockchain - After Upgrading Contract', () => {
        let bsh_perifV1, bsh_perifV2, bsh_coreV1, bsh_coreV2, bmc, holder;
        let service = 'Coin/WrappedCoin';           let _uri = 'https://github.com/icon-project/btp';
        let _native = 'PARA';                       let _fee = 10;     
        let _name = 'ICON';                         let _bmcICON = 'btp://1234.iconee/0x1234567812345678';
        let _net = '1234.iconee';                   let _from = '0x12345678';   let _value = 999999999999999;                       
        let REPONSE_HANDLE_SERVICE = 2;             let RC_OK = 0;              let RC_ERR = 1;

        before(async () => {
            bmc = await BMC.new('1234.pra');
            bsh_coreV1 = await deployProxy(BSHCoreV1, [_uri, _native, _fee]);
            bsh_perifV1 = await deployProxy(BSHPerifV1, [bmc.address, bsh_coreV1.address, service]);
            encode_msg = await EncodeMsg.new();
            await bsh_coreV1.updateBSHPeriphery(bsh_perifV1.address);
            await bmc.addService(service, bsh_perifV1.address);
            bsh_perifV2 = await upgradeProxy(bsh_perifV1.address, BSHPerifV2);
            bsh_coreV2 = await upgradeProxy(bsh_coreV1.address, BSHCoreV2);
            holder = await Holder.new();
            await bmc.addVerifier(_net, accounts[1]);
            await bmc.addLink(_bmcICON);
            await holder.addBSHContract(bsh_perifV2.address, bsh_coreV2.address);
            await bsh_coreV2.register(_name);
            let _msg = await encode_msg.encodeTransferMsgWithAddress(_from, holder.address, _name, _value);
            await bmc.receiveRequest(_bmcICON, "", service, 0, _msg);
            id = await bsh_coreV2.coinId(_name);
        });

        it('Scenario 1: Should revert when User has not yet set approval for token being transferred out by Operator', async () => {
            let _to = 'btp://1234.iconee/0x12345678';
            let _value = 5000;
            let balanceBefore = await bsh_coreV2.getBalanceOf(holder.address, _name);
            await truffleAssert.reverts(
                holder.callTransfer.call(_name, _value, _to),
                "ERC1155: caller is not owner nor approved"
            ); 
            let balanceAfter = await bsh_coreV2.getBalanceOf(holder.address, _name);
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
            let balanceBefore = await bsh_coreV2.getBalanceOf(holder.address, _name);
            await holder.setApprove(bsh_coreV2.address);
            await truffleAssert.reverts(
                holder.callTransfer.call(_name, _value, _to),
                "ERC1155: insufficient balance for transfer"
            ); 
            let balanceAfter = await bsh_coreV2.getBalanceOf(holder.address, _name);
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
            await holder.setApprove(bsh_coreV2.address);
            await truffleAssert.reverts(
                holder.callTransfer.call(_token, _value, _to),
                "UnregisterCoin"
            ); 
        });

        it('Scenario 4: Should revert when User transfers Tokens to an invalid BTP Address format', async () => {
            let _to = '1234.iconee/0x12345678';
            let contract_balanceBefore = await bsh_coreV2.getBalanceOf(holder.address, _name);
            await holder.setApprove(bsh_coreV2.address);
            await truffleAssert.reverts(
                holder.callTransfer.call(_name, 5000, _to),
                "VM Exception while processing transaction: revert"
            ); 
            let contract_balanceAfter = await bsh_coreV2.getBalanceOf(holder.address, _name);
            let bsh_core_balance = await bsh_coreV2.getBalanceOf(bsh_coreV2.address, _name);
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
            let balanceBefore = await bsh_coreV2.getBalanceOf(holder.address, _name);
            await holder.setApprove(bsh_coreV2.address);
            await truffleAssert.reverts(
                holder.callTransfer.call(_name, 0, _to),
                "InvalidAmount"
            ); 
            let balanceAfter = await bsh_coreV2.getBalanceOf(holder.address, _name);
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
            let balanceBefore = await bsh_coreV2.getBalanceOf(holder.address, _name);
            await holder.setApprove(bsh_coreV2.address);
            await truffleAssert.reverts(
                holder.callTransfer.call(_name, 10, _to),
                "InvalidAmount"
            ); 
            let balanceAfter = await bsh_coreV2.getBalanceOf(holder.address, _name);
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
            let balanceBefore = await bsh_coreV2.getBalanceOf(holder.address, _name);
            await holder.setApprove(bsh_coreV2.address);
            await truffleAssert.reverts(
                holder.callTransfer.call(_name, 1000, _to),
                "BMCRevertNotExistsBMV"
            ); 
            let balanceAfter = await bsh_coreV2.getBalanceOf(holder.address, _name);
            let bsh_core_balance = await bsh_coreV2.getBalanceOf(bsh_coreV2.address, _name);
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
            let balanceBefore = await bsh_coreV2.getBalanceOf(holder.address, _name);
            await holder.setApprove(bsh_coreV2.address);
            await holder.callTransfer(_name, _value, _to);
            let balanceAfter = await bsh_coreV2.getBalanceOf(holder.address, _name);
            let bsh_core_balance = await bsh_coreV2.getBalanceOf(bsh_coreV2.address, _name);
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
            let contract_balanceBefore = await bsh_coreV2.getBalanceOf(holder.address, _name);
            let _msg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_OK, "");
            await bmc.receiveResponse(_net, service, 1, _msg);
            let contract_balanceAfter = await bsh_coreV2.getBalanceOf(holder.address, _name);
            let fees = await bsh_coreV2.getAccumulatedFees();
            let bsh_core_balance = await bsh_coreV2.getBalanceOf(bsh_coreV2.address, _name);
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
            let balanceBefore = await bsh_coreV2.getBalanceOf(holder.address, _name);
            let bsh_core_balance_before = await bsh_coreV2.getBalanceOf(bsh_coreV2.address, _name);
            await holder.setApprove(bsh_coreV2.address);
            await holder.callTransfer(_name, _value, _to);
            let balanceAfter = await bsh_coreV2.getBalanceOf(holder.address, _name);
            let bsh_core_balance_after = await bsh_coreV2.getBalanceOf(bsh_coreV2.address, _name);
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
            let balanceBefore = await bsh_coreV2.getBalanceOf(holder.address, _name);
            let _msg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_ERR, "");
            await bmc.receiveResponse(_net, service, 2, _msg);
            let balanceAfter = await bsh_coreV2.getBalanceOf(holder.address, _name);
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
    
    describe('As a user, I want to receive PRA from ICON blockchain - After Upgrading Contract', () => {
        let bmc, bsh_perifV1, bsh_perifV2, bsh_coreV1, bsh_coreV2, notpayable, refundable;
        let service = 'Coin/WrappedCoin';       let _bmcICON = 'btp://1234.iconee/0x1234567812345678';
        let _net = '1234.iconee';               let _to = 'btp://1234.iconee/0x12345678';
        let _native = 'PARA';                   let _fee = 10;   
        let RC_ERR = 1;                       let RC_OK = 0;
        let _uri = 'https://github.com/icon-project/btp';
        
        before(async () => {
            bmc = await BMC.new('1234.pra');
            bsh_coreV1 = await deployProxy(BSHCoreV1, [_uri, _native, _fee]);
            bsh_perifV1 = await deployProxy(BSHPerifV1, [bmc.address, bsh_coreV1.address, service]);
            encode_msg = await EncodeMsg.new();
            await bsh_coreV1.updateBSHPeriphery(bsh_perifV1.address);
            await bmc.addService(service, bsh_perifV1.address);
            bsh_perifV2 = await upgradeProxy(bsh_perifV1.address, BSHPerifV2);
            bsh_coreV2 = await upgradeProxy(bsh_coreV1.address, BSHCoreV2);
            notpayable = await NotPayable.new();
            refundable = await Refundable.new();
            await bmc.addVerifier(_net, accounts[1]);
            await bmc.addLink(_bmcICON);
            await bsh_coreV2.transfer(_to, {from: accounts[0], value: 100000000});
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

    describe('As a user, I want to receive ERC1155_ICX from ICON blockchain - After Upgrading Contract', () => {
        let bmc, bsh_perifV1, bsh_perifV2, bsh_coreV1, bsh_coreV2, holder, notpayable;
        let service = 'Coin/WrappedCoin';                   let _uri = 'https://github.com/icon-project/btp';
        let _native = 'PARA';                               let _fee = 10;
        let _name = 'ICON';                                 let _bmcICON = 'btp://1234.iconee/0x1234567812345678';
        let _net = '1234.iconee';                           let _from = '0x12345678';           
        let RC_ERR = 1;                                     let RC_OK = 0;        

        before(async () => {
            bmc = await BMC.new('1234.pra');
            bsh_coreV1 = await deployProxy(BSHCoreV1, [_uri, _native, _fee]);
            bsh_perifV1 = await deployProxy(BSHPerifV1, [bmc.address, bsh_coreV1.address, service]);
            encode_msg = await EncodeMsg.new();
            await bsh_coreV1.updateBSHPeriphery(bsh_perifV1.address);
            await bmc.addService(service, bsh_perifV1.address);
            bsh_perifV2 = await upgradeProxy(bsh_perifV1.address, BSHPerifV2);
            bsh_coreV2 = await upgradeProxy(bsh_coreV1.address, BSHCoreV2);
            holder = await Holder.new();
            notpayable = await NotPayable.new();
            await bmc.addVerifier(_net, accounts[1]);
            await bmc.addLink(_bmcICON);
            await holder.addBSHContract(bsh_perifV2.address, bsh_coreV2.address);
            await bsh_coreV2.register(_name);
            id = await bsh_coreV2.coinId(_name);
            btpAddr = await bmc.bmcAddress();
        });

        it('Scenario 1: Should emit an error message when receiving address is an invalid address', async () => {
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
            let balanceBefore = await bsh_coreV2.balanceOf(notpayable.address, id);
            let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'TransferFailed');
            let _msg = await encode_msg.encodeTransferMsgWithAddress(_from, notpayable.address, _name, _value);
            let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
            let balanceAfter = await bsh_coreV2.balanceOf(notpayable.address, id);
            assert(
                web3.utils.BN(balanceAfter).toNumber() === web3.utils.BN(balanceBefore).toNumber() &&
                output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _eventMsg
            );
        });

        it('Scenario 3: Should emit an error message when BSHPerphery receives a request of invalid token', async () => {
            let _value = 3000;
            let _tokenName = 'Ethereum';
            let invalid_coin_id = await bsh_coreV2.coinId(_tokenName);
            let balanceBefore = await bsh_coreV2.balanceOf(holder.address, invalid_coin_id);
            let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'UnregisteredCoin');
            let _msg = await encode_msg.encodeTransferMsgWithAddress(_from, holder.address, _tokenName, _value);
            let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
            let balanceAfter = await bsh_coreV2.balanceOf(holder.address, invalid_coin_id);
            assert(
                web3.utils.BN(balanceAfter).toNumber() === web3.utils.BN(balanceBefore).toNumber() &&
                output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _eventMsg
            );
        });

        it('Scenario 4: Should mint tokens successfully when a receiver is a ERC1155Holder contract', async () => { 
            let _value = 2500;
            let balanceBefore = await bsh_coreV2.balanceOf(holder.address, id);
            let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_OK, '');
            let _msg = await encode_msg.encodeTransferMsgWithAddress(_from, holder.address, _name, _value);
            let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
            let balanceAfter = await bsh_coreV2.balanceOf(holder.address, id);
            assert(
                web3.utils.BN(balanceAfter).toNumber() === 
                    web3.utils.BN(balanceBefore).toNumber() + _value &&
                output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _eventMsg    
            );
        });

        it('Scenario 5: Should mint tokens successfully when a receiver is an account client', async () => { 
            let _value = 5500;
            let balanceBefore = await bsh_coreV2.balanceOf(accounts[1], id);
            let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_OK, '');
            let _msg = await encode_msg.encodeTransferMsgWithAddress(_from, accounts[1], _name, _value);
            let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
            let balanceAfter = await bsh_coreV2.balanceOf(accounts[1], id);
            assert(
                web3.utils.BN(balanceAfter).toNumber() === 
                    web3.utils.BN(balanceBefore).toNumber() + _value &&
                output.logs[0].args._next === _bmcICON && output.logs[0].args._msg === _eventMsg 
            );
        });
    });

    describe('BSHs Handle Fee Aggregation - After Upgrading Contract', () => {
        let bsh_perifV1, bsh_perifV2, bsh_coreV1, bsh_coreV2, bmc, holder;
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
        let _sn0 = 1;           let _sn1 = 2;               let _sn2 = 3;

        before(async () => {
            bmc = await BMC.new('1234.pra');
            bsh_coreV1 = await deployProxy(BSHCoreV1, [_uri, _native, _fee]);
            bsh_perifV1 = await deployProxy(BSHPerifV1, [bmc.address, bsh_coreV1.address, service]);
            encode_msg = await EncodeMsg.new();
            await bsh_coreV1.updateBSHPeriphery(bsh_perifV1.address);
            await bmc.addService(service, bsh_perifV1.address);
            bsh_perifV2 = await upgradeProxy(bsh_perifV1.address, BSHPerifV2);
            bsh_coreV2 = await upgradeProxy(bsh_coreV1.address, BSHCoreV2);
            holder = await Holder.new();
            btpAddr = await bmc.bmcAddress();
            await bmc.addVerifier(_net1, accounts[1]);
            await bmc.addVerifier(_net2, accounts[2]);
            await bmc.addLink(_bmcICON);
            await holder.addBSHContract(bsh_perifV2.address, bsh_coreV2.address);
            await bsh_coreV2.register(_name1);
            await bsh_coreV2.register(_name2);
            await bsh_coreV2.register(_name3);
            await bsh_coreV2.register(_name4);
            let _msg1 = await encode_msg.encodeTransferMsgWithAddress(_from1, holder.address, _name1, _value1);
            await bmc.receiveRequest(_bmcICON, "", service, _sn0, _msg1);
            let _msg2 = await encode_msg.encodeTransferMsgWithAddress(_from2, holder.address, _name2, _value2);
            await bmc.receiveRequest(_bmcICON, "", service, _sn1, _msg2);
            await bsh_coreV2.transfer(_to1, {from: accounts[0], value: _txAmt});
            let _responseMsg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_OK, "");
            await bmc.receiveResponse(_net1, service, _sn0, _responseMsg);
            await holder.setApprove(bsh_coreV2.address);
            await holder.callTransfer(_name1, _txAmt1, _to1);
            await bmc.receiveResponse(_net1, service, _sn1, _responseMsg);
            await holder.callTransfer(_name2, _txAmt2, _to2);
            await bmc.receiveResponse(_net1, service, _sn2, _responseMsg);
        });

        it('Scenario 1: Should be able to query Aggregation Fee', async () => {
            let aggregationFee = await bsh_coreV2.getAccumulatedFees();
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
            let FA1Before = await bsh_perifV2.getAggregationFeeOf(_native);     //  state Aggregation Fee of each type of Coins
            let FA2Before = await bsh_perifV2.getAggregationFeeOf(_name1);
            let FA3Before = await bsh_perifV2.getAggregationFeeOf(_name2);
            await truffleAssert.reverts( 
                bsh_perifV2.handleFeeGathering.call(_to1, service, {from: accounts[1]}),
                'Unauthorized'    
            );
            let FA1After = await bsh_perifV2.getAggregationFeeOf(_native);
            let FA2After = await bsh_perifV2.getAggregationFeeOf(_name1);
            let FA3After = await bsh_perifV2.getAggregationFeeOf(_name2);
            let fees = await bsh_perifV2.getPendingRequest(_sn3);     //  get pending Aggregation Fee list
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
            let _sn3 = 4;
            let FA1Before = await bsh_perifV2.getAggregationFeeOf(_native);     //  state Aggregation Fee of each type of Coins
            let FA2Before = await bsh_perifV2.getAggregationFeeOf(_name1);
            let FA3Before = await bsh_perifV2.getAggregationFeeOf(_name2);
            let _bmcService = await encode_msg.encodeBMCService(_to1, [service]);
            let output = await bmc.receiveRequest(_bmcICON, '', 'bmc', 100, _bmcService);
            let FA1After = await bsh_perifV2.getAggregationFeeOf(_native);
            let FA2After = await bsh_perifV2.getAggregationFeeOf(_name1);
            let FA3After = await bsh_perifV2.getAggregationFeeOf(_name2);
            let fees = await bsh_perifV2.getPendingRequest(_sn3);     //  get pending Aggregation Fee list
            let list = [];
            for (let i = 0; i < fees.amounts.length; i++) {
                list[i] = [fees.coinNames[i], fees.amounts[i]];
            }
            let _eventMsg = await encode_msg.encodeTransferFeesBMCMessage(
                btpAddr, _bmcICON, _to1, service, _sn3, bsh_coreV2.address, list
            );
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
            let _sn3 = 4;
            let feesBefore = await bsh_perifV2.getPendingRequest(_sn3);
            let _responseMsg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_OK, "");
            await bmc.receiveResponse(_net1, service, _sn3, _responseMsg);
            let feesAfter = await bsh_perifV2.getPendingRequest(_sn3);
            assert(
                feesBefore.amounts.length === 3 &&
                feesBefore.coinNames[0] === _native && Number(feesBefore.amounts[0]) === Math.floor(_txAmt / 1000) &&
                feesBefore.coinNames[1] === _name1 && Number(feesBefore.amounts[1]) === Math.floor(_txAmt1 / 1000) &&
                feesBefore.coinNames[2] === _name2 && Number(feesBefore.amounts[2]) === Math.floor(_txAmt2 / 1000) &&
                feesAfter.amounts.length === 0
            );
        });

        it('Scenario 5: Should restore aggregationFA state when receiving an error response', async () => {
            let _sn4 = 5;   let _sn5 = 6;   let _sn6 = 7;
            let _amt1 = 2000000;                    let _amt2 = 6000000;
            await holder.callTransfer(_name1, _amt1, _to1);
            let _responseMsg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_OK, "");
            await bmc.receiveResponse(_net1, service, _sn4, _responseMsg);
            await holder.callTransfer(_name2, _amt2, _to2);
            await bmc.receiveResponse(_net2, service, _sn5, _responseMsg);
            let _bmcService = await encode_msg.encodeBMCService(_to1, [service]);
            await bmc.receiveRequest(_bmcICON, '', 'bmc', 100, _bmcService);

            let FA1Before = await bsh_perifV2.getAggregationFeeOf(_name1);
            let FA2Before = await bsh_perifV2.getAggregationFeeOf(_name2);
            let feesBefore = await bsh_perifV2.getPendingRequest(_sn6);
            let _errMsg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_ERR, "");
            await bmc.receiveResponse(_net1, service, _sn6, _errMsg);
            let FA1After = await bsh_perifV2.getAggregationFeeOf(_name1);
            let FA2After = await bsh_perifV2.getAggregationFeeOf(_name2);
            let feesAfter = await bsh_perifV2.getPendingRequest(_sn6);
            assert(
                feesBefore.amounts.length === 2 &&
                // feesBefore.coinNames[0] === _name1 && Number(feesBefore.amounts[0]) === Math.floor(_amt1 / 1000) &&
                // feesBefore.coinNames[1] === _name2 && Number(feesBefore.amounts[1]) === Math.floor(_amt2 / 1000) &&
                web3.utils.BN(FA1Before).toNumber() === 0 && 
                web3.utils.BN(FA2Before).toNumber() === 0 &&
                feesAfter.amounts.length === 0 &&
                web3.utils.BN(FA1After).toNumber() === Math.floor(_amt1 / 1000) && 
                web3.utils.BN(FA2After).toNumber() === Math.floor(_amt2 / 1000)
            );
        });
    });

    describe('As a user, I want to receive multiple Coins/Tokens from ICON blockchain - After Upgrading Contract', () => {
        let bsh_perifV1, bsh_perifV2, bsh_coreV1, bsh_coreV2, bmc, holder, refundable;
        let service = 'Coin/WrappedCoin';                   let _uri = 'https://github.com/icon-project/btp';
        let _native = 'PARA';                               let _fee = 10;
        let _name1 = 'ICON';    let _name2 = 'BINANCE';     let _name3 = 'ETHEREUM';        let _name4 = 'TRON';                                             
        let _net1 = '1234.iconee';                          let _bmcICON = 'btp://1234.iconee/0x1234567812345678';                                                     
        let RC_OK = 0;                                      let RC_ERR = 1;                 
        let _from1 = '0x12345678';                          let _to = 'btp://1234.iconee/0x12345678';                                                         

        before(async () => {
            bmc = await BMC.new('1234.pra');
            bsh_coreV1 = await deployProxy(BSHCoreV1, [_uri, _native, _fee]);
            bsh_perifV1 = await deployProxy(BSHPerifV1, [bmc.address, bsh_coreV1.address, service]);
            encode_msg = await EncodeMsg.new();
            await bsh_coreV1.updateBSHPeriphery(bsh_perifV1.address);
            await bmc.addService(service, bsh_perifV1.address);
            bsh_perifV2 = await upgradeProxy(bsh_perifV1.address, BSHPerifV2);
            bsh_coreV2 = await upgradeProxy(bsh_coreV1.address, BSHCoreV2);
            holder = await Holder.new();
            refundable = await Refundable.new();
            btpAddr = await bmc.bmcAddress();
            await bmc.addVerifier(_net1, accounts[1]);
            await bmc.addLink(_bmcICON);
            await holder.addBSHContract(bsh_perifV2.address, bsh_coreV2.address);
            await bsh_coreV2.register(_name1);
            await bsh_coreV2.register(_name2);
            await bsh_coreV2.register(_name3);
            await bsh_coreV2.register(_name4);
            await bsh_coreV2.transfer(_to, {from: accounts[0], value: 10000000});
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
            let balance1Before = await bsh_coreV2.getBalanceOf(holder.address, _name1);
            let balance2Before = await bsh_coreV2.getBalanceOf(holder.address, _name2);
            let balance3Before = await bsh_coreV2.getBalanceOf(holder.address, _invalid_token);
            let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'UnregisteredCoin');
            let _msg = await encode_msg.encodeBatchTransferMsgWithAddress(
                _from1, holder.address, [[_name1, _value1], [_name2, _value2], [_invalid_token, _value3]]
            );
            let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
            let balance1After = await bsh_coreV2.getBalanceOf(holder.address, _name1);
            let balance2After = await bsh_coreV2.getBalanceOf(holder.address, _name2);
            let balance3After = await bsh_coreV2.getBalanceOf(holder.address, _invalid_token);

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
            let balance1Before = await bsh_coreV2.getBalanceOf(accounts[1], _name1);
            let balance2Before = await bsh_coreV2.getBalanceOf(accounts[1], _name2);
            let balance3Before = await bsh_coreV2.getBalanceOf(accounts[1], _native);
            let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'TransferFailed');
            let _msg = await encode_msg.encodeBatchTransferMsgWithAddress(
                _from1, accounts[1], [[_name1, _value1], [_name2, _value2], [_native, _value3]]
            );
            let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
            let balance1After = await bsh_coreV2.getBalanceOf(accounts[1], _name1);
            let balance2After = await bsh_coreV2.getBalanceOf(accounts[1], _name2);
            let balance3After = await bsh_coreV2.getBalanceOf(accounts[1], _native);

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
            let balance1Before = await bsh_coreV2.getBalanceOf(refundable.address, _native);
            let balance2Before = await bsh_coreV2.getBalanceOf(refundable.address, _name1);
            let balance3Before = await bsh_coreV2.getBalanceOf(refundable.address, _name2);
            let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'TransferFailed');
            let _msg = await encode_msg.encodeBatchTransferMsgWithAddress(
                _from1, refundable.address, [[_native, _value1], [_name1, _value2], [_name2, _value3]]
            );
            let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
            let balance1After = await bsh_coreV2.getBalanceOf(refundable.address, _native);
            let balance2After = await bsh_coreV2.getBalanceOf(refundable.address, _name1);
            let balance3After = await bsh_coreV2.getBalanceOf(refundable.address, _name2);

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
            let balance1Before = await bsh_coreV2.getBalanceOf(holder.address, _name1);
            let balance2Before = await bsh_coreV2.getBalanceOf(holder.address, _name2);
            let balance3Before = await bsh_coreV2.getBalanceOf(holder.address, _native);
            let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_ERR, 'TransferFailed');
            let _msg = await encode_msg.encodeBatchTransferMsgWithAddress(
                _from1, holder.address, [[_name1, _value1], [_name2, _value2], [_native, _value3]]
            );
            let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
            let balance1After = await bsh_coreV2.getBalanceOf(holder.address, _name1);
            let balance2After = await bsh_coreV2.getBalanceOf(holder.address, _name2);
            let balance3After = await bsh_coreV2.getBalanceOf(holder.address, _native);

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
            let balance1Before = await bsh_coreV2.getBalanceOf(holder.address, _name1);
            let balance2Before = await bsh_coreV2.getBalanceOf(holder.address, _name2);
            let balance3Before = await bsh_coreV2.getBalanceOf(holder.address, _name3);
            let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_OK, '');
            let _msg = await encode_msg.encodeBatchTransferMsgWithAddress(
                _from1, holder.address, [[_name1, _value1], [_name2, _value2], [_name3, _value3]]
            );
            let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
            let balance1After = await bsh_coreV2.getBalanceOf(holder.address, _name1);
            let balance2After = await bsh_coreV2.getBalanceOf(holder.address, _name2);
            let balance3After = await bsh_coreV2.getBalanceOf(holder.address, _name3);

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
            let balance1Before = await bsh_coreV2.getBalanceOf(accounts[1], _native);
            let balance2Before = await bsh_coreV2.getBalanceOf(accounts[1], _name2);
            let balance3Before = await bsh_coreV2.getBalanceOf(accounts[1], _name3);
            let _eventMsg = await encode_msg.encodeResponseBMCMessage(btpAddr, _bmcICON, service, 10, RC_OK, '');
            let _msg = await encode_msg.encodeBatchTransferMsgWithAddress(
                _from1, accounts[1], [[_native, _value1], [_name2, _value2], [_name3, _value3]]
            );
            let output = await bmc.receiveRequest(_bmcICON, '', service, 10, _msg);
            let balance1After = await bsh_coreV2.getBalanceOf(accounts[1], _native);
            let balance2After = await bsh_coreV2.getBalanceOf(accounts[1], _name2);
            let balance3After = await bsh_coreV2.getBalanceOf(accounts[1], _name3);

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

    describe('As a user, I want to send multiple coins/tokens to ICON blockchain - After Upgrading Contract', () => {
        let bsh_perifV1, bsh_perifV2, bsh_coreV1, bsh_coreV2, bmc, holder;
        let service = 'Coin/WrappedCoin';           let _uri = 'https://github.com/icon-project/btp';
        let _native = 'PARA';                       let _fee = 10;      
        let _net = '1234.iconee';                   let _from = '0x12345678';   let _value = 999999999999999;                       
        let REPONSE_HANDLE_SERVICE = 2;             let RC_OK = 0;              let RC_ERR = 1;
        let _bmcICON = 'btp://1234.iconee/0x1234567812345678';         
        let _coin1 = 'ICON';    let _coin2 = 'TRON';    let _coin3 = 'BINANCE';
        let initAmt = 100000000;

        before(async () => {    
            bmc = await BMC.new('1234.pra');
            bsh_coreV1 = await deployProxy(BSHCoreV1, [_uri, _native, _fee]);
            bsh_perifV1 = await deployProxy(BSHPerifV1, [bmc.address, bsh_coreV1.address, service]);
            encode_msg = await EncodeMsg.new();
            await bsh_coreV1.updateBSHPeriphery(bsh_perifV1.address);
            await bmc.addService(service, bsh_perifV1.address);
            bsh_perifV2 = await upgradeProxy(bsh_perifV1.address, BSHPerifV2);
            bsh_coreV2 = await upgradeProxy(bsh_coreV1.address, BSHCoreV2);
            holder = await Holder.new();
            await bmc.addVerifier(_net, accounts[1]);
            await bmc.addLink(_bmcICON);
            await holder.addBSHContract(bsh_perifV2.address, bsh_coreV2.address);
            await bsh_coreV2.register(_coin1);
            await bsh_coreV2.register(_coin2);
            await bsh_coreV2.register(_coin3);
            await bsh_coreV2.transfer('btp://1234.iconee/0x12345678', {from: accounts[0], value: initAmt});
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
            let balanceBefore = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            await truffleAssert.reverts(
                holder.callTransferBatch.call(bsh_coreV2.address, _coins, _values, _to),
                "VM Exception while processing transaction: revert"
            ); 
            let balanceAfter = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            let bsh_core_balance = await bsh_coreV2.getBalanceOfBatch(bsh_coreV2.address, _coins);

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
            let balanceBefore = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            await holder.setApprove(bsh_coreV2.address);
            await truffleAssert.reverts(
                holder.callTransferBatch.call(bsh_coreV2.address, _coins, _values, _to),
                "VM Exception while processing transaction: revert"
            ); 
            let balanceAfter = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            let bsh_core_balance = await bsh_coreV2.getBalanceOfBatch(bsh_coreV2.address, _coins);

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
            let balanceBefore = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            await holder.setApprove(bsh_coreV2.address);
            await truffleAssert.reverts(
                holder.callTransferBatch.call(bsh_coreV2.address, _coins, _values, _to),
                "VM Exception while processing transaction: revert"
            ); 
            let balanceAfter = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            let bsh_core_balance = await bsh_coreV2.getBalanceOfBatch(bsh_coreV2.address, _coins);

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
            let balanceBefore = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            await holder.setApprove(bsh_coreV2.address);
            await truffleAssert.reverts(
                holder.callTransferBatch.call(bsh_coreV2.address, _coins, _values, _to),
                "VM Exception while processing transaction: revert"
            ); 
            let balanceAfter = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            let bsh_core_balance = await bsh_coreV2.getBalanceOfBatch(bsh_coreV2.address, _coins);

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
            let balanceBefore = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            await holder.setApprove(bsh_coreV2.address);
            await truffleAssert.reverts(
                holder.callTransferBatch.call(bsh_coreV2.address, _coins, _values, _to),
                "VM Exception while processing transaction: revert"
            ); 
            let balanceAfter = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            let bsh_core_balance = await bsh_coreV2.getBalanceOfBatch(bsh_coreV2.address, _coins);

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
            let balanceBefore = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            await holder.setApprove(bsh_coreV2.address);
            await truffleAssert.reverts(
                holder.callTransferBatch.call(bsh_coreV2.address, _coins, _values, _to),
                "VM Exception while processing transaction: revert"
            ); 
            let balanceAfter = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            let bsh_core_balance = await bsh_coreV2.getBalanceOfBatch(bsh_coreV2.address, _coins);

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
            let balanceBefore = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            await holder.setApprove(bsh_coreV2.address);
            await truffleAssert.reverts(
                holder.callTransferBatch.call(bsh_coreV2.address, _coins, _values, _to),
                "VM Exception while processing transaction: revert"
            ); 
            let balanceAfter = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            let bsh_core_balance = await bsh_coreV2.getBalanceOfBatch(bsh_coreV2.address, _coins);
            
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
            let _to = 'btp://1234.iconee/0x12345678';
            let _coins = [_native, _native, _native];
            let _values = [1000, 1000, 1000];
            let balanceBefore = await bsh_coreV2.getBalanceOfBatch(accounts[2], _coins);
            await truffleAssert.reverts(
                bsh_coreV2.transferBatch.call(_coins, _values, _to, {from: accounts[2], value: 1000}),
                "VM Exception while processing transaction: revert"
            ); 
            let balanceAfter = await bsh_coreV2.getBalanceOfBatch(accounts[2], _coins);
            let bsh_core_balance = await bsh_coreV2.getBalanceOfBatch(bsh_coreV2.address, _coins);
            
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
            let balanceBefore = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            await holder.setApprove(bsh_coreV2.address);
            await truffleAssert.reverts(
                holder.callTransferBatch.call(bsh_coreV2.address, _coins, _values, _to),
                "VM Exception while processing transaction: revert"
            ); 
            let balanceAfter = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            let bsh_core_balance = await bsh_coreV2.getBalanceOfBatch(bsh_coreV2.address, _coins);
            
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
            let balanceBefore = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            await holder.setApprove(bsh_coreV2.address);
            await holder.callTransferBatch(bsh_coreV2.address, _coins, _values, _to);
            let balanceAfter = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            let bsh_core_balance = await bsh_coreV2.getBalanceOfBatch(bsh_coreV2.address, _coins);

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
            let balanceBefore = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            let _responseMsg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_OK, "");
            await bmc.receiveResponse(_net, service, 2, _responseMsg);
            let balanceAfter = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            let fees = await bsh_coreV2.getAccumulatedFees();
            let bsh_core_balance = await bsh_coreV2.getBalanceOfBatch(bsh_coreV2.address, _coins);
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
            let balanceBefore = await bsh_coreV2.getBalanceOfBatch(accounts[1], _coins);
            await bsh_coreV2.setApprovalForAll(bsh_coreV2.address, true, {from: accounts[1]});
            await bsh_coreV2.transferBatch(_coins, _values, _to, {from: accounts[1]});
            let balanceAfter = await bsh_coreV2.getBalanceOfBatch(accounts[1], _coins);
            let bsh_core_balance = await bsh_coreV2.getBalanceOfBatch(bsh_coreV2.address, _coins);
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
            let balanceBefore = await bsh_coreV2.getBalanceOfBatch(accounts[1], _coins);
            let _responseMsg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_ERR, "");
            await bmc.receiveResponse(_net, service, 3, _responseMsg);
            let balanceAfter = await bsh_coreV2.getBalanceOfBatch(accounts[1], _coins);
            let bsh_core_balance = await bsh_coreV2.getBalanceOfBatch(bsh_coreV2.address, _coins);
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
            let balanceBefore = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            await holder.setApprove(bsh_coreV2.address);
            await holder.callTransferBatch(bsh_coreV2.address, _coins, _values, _to);
            let balanceAfter = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            let bsh_core_balance = await bsh_coreV2.getBalanceOfBatch(bsh_coreV2.address, _coins);

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
            let balanceBefore = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            let _responseMsg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_ERR, "");
            await bmc.receiveResponse(_net, service, 4, _responseMsg);
            let balanceAfter = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            let bsh_core_balance = await bsh_coreV2.getBalanceOfBatch(bsh_coreV2.address, _coins);
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
            let balanceBefore = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            await holder.setApprove(bsh_coreV2.address);
            await holder.callTransferBatch(bsh_coreV2.address, _coins, _values, _to);
            let balanceAfter = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            let bsh_core_balance = await bsh_coreV2.getBalanceOfBatch(bsh_coreV2.address, _coins);

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
            let balanceBefore = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            let _responseMsg = await encode_msg.encodeResponseMsg(REPONSE_HANDLE_SERVICE, RC_ERR, "");
            await bmc.receiveResponse(_net, service, 5, _responseMsg);
            let balanceAfter = await bsh_coreV2.getBalanceOfBatch(holder.address, _coins);
            let bsh_core_balance = await bsh_coreV2.getBalanceOfBatch(bsh_coreV2.address, _coins);
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
}); 
const rlp = require('rlp');
const assert = require('chai').assert;
const { 
    base64Msg,
    iconNet,
    prevBtpAddr,
    validatorsList,
    btpMsgs,
    encodedValidators,
    initOffset,
    initRootSize,
    initCacheSize,
    lastBlockHash,
    praNet
} = require('./data');
const { deployProxy, upgradeProxy } = require('@openzeppelin/truffle-upgrades');

const MockBMC = artifacts.require('MockBMC');
const BMV = artifacts.require('BMV');
const DataValidator = artifacts.require('DataValidator');

const BMVV2 = artifacts.require('BMVV2');
const DataValidatorV2 = artifacts.require('DataValidatorV2');

contract('TestBMV', async () => {
    let bmv, dataValidator, bmc;

    beforeEach(async () => {
        bmc = await MockBMC.new(praNet);
        dataValidator = await deployProxy(DataValidator);
        bmv = await deployProxy(
            BMV,
            [
                bmc.address,
                dataValidator.address,
                iconNet,
                encodedValidators,
                initOffset,
                initRootSize,
                initCacheSize,
                lastBlockHash
            ]
        );
    });

    it('check contract code size', async () => {
        console.log('- BMV1 : ', BMV.deployedBytecode.length / 2 - 1);
        assert.isBelow(BMV.deployedBytecode.length / 2 - 1, 24576, 'contract size is restricted to 24KB');
        console.log('- BMV2 : ', DataValidator.deployedBytecode.length / 2 - 1);
        assert.isBelow(DataValidator.deployedBytecode.length / 2 - 1, 24576, 'contract size is restricted to 24KB');
    });

    it('should get connected BMC\'s address', async () => {
        const addr = await bmv.getConnectedBMC();
        assert.equal(addr, bmc.address, 'incorrect bmc address');
    });

    it('should get network address', async () => {
        const btpAddr = await bmv.getNetAddress();
        assert.equal(btpAddr, iconNet, 'incorrect btp network address');
    });

    it('should get list of validtors and its serialized data in RLP', async () => {
        const res = await bmv.getValidators();
        const hash = web3.utils.soliditySha3(rlp.encode(validatorsList));
        assert.equal(res[0], hash, 'incorrect validators\' hash');
        for (let i = 0; i < res[1].length; i++) {
            assert.deepEqual(Buffer.from(res[1][i].substring(2), 'hex'), validatorsList[i], 'incorrect validator address');
        }
    });

    it('should get BMV status', async () => {
        await bmc.testHandleRelayMessage(bmv.address, prevBtpAddr, 0, base64Msg);
        const status = await bmv.getStatus();
        assert.isNotEmpty(status, 'invalid status');
        assert.equal(status[0], initOffset + 1, 'incorrect current MTA height');
        assert.equal(status[1], initOffset, 'incorrect offset');
        assert.equal(status[2], initOffset + 1, 'incorrect last block height');
    });

    it('should verify relay message', async () => {
        const res = await bmc.testHandleRelayMessage.call(bmv.address, prevBtpAddr, 0, base64Msg);
        for (let i = 0; i < btpMsgs.length; i++)
            assert.equal(btpMsgs[i], res[i], 'incorrect service messages');
    });

    it('should upgrade BMV', async () => {
        const upgradeDataValidator = await upgradeProxy(dataValidator.address, DataValidatorV2);
        const upgradeBMV = await upgradeProxy(bmv.address, BMVV2);

        let msgs = await upgradeDataValidator.validateReceipt.call(
            'param1',
            'param2',
            100,
            web3.utils.randomHex(20),
            web3.utils.randomHex(32)
        );

        assert.equal(web3.utils.hexToAscii(msgs[0]), 'Succeed to upgrade Data Validator contract');

        msgs = await upgradeBMV.handleRelayMessage.call(
            'param1',
            'param2',
            200,
            'param3'
        );

        assert.equal(web3.utils.hexToAscii(msgs[0]), 'Succeed to upgrade Data Validator contract');
        assert.equal(web3.utils.hexToAscii(msgs[1]), 'Succeed to upgrade BMV contract');
    });
});

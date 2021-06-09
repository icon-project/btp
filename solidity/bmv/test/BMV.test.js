const rlp = require('rlp');
const assert = require('chai').assert;
const { 
    base64Msg,
    iconNet,
    prevBtpAddr,
    validatorsList,
    serviceMsgs,
    encodedValidators,
    initOffset,
    lastBlockHash
} = require('./data');
const { deployProxy, upgradeProxy } = require('@openzeppelin/truffle-upgrades');

let testBMV = artifacts.require('BMV');
let mockBMC = artifacts.require('MockBMC');
let testDataValidator = artifacts.require('DataValidator');

let bmvV2 = artifacts.require('BMVV2');
let dataValidatorV2 = artifacts.require('DataValidatorV2');

contract('TestBMV', async (accounts) => {
    beforeEach(async () => {
        bmv = await testBMV.deployed();
        bmc = await mockBMC.deployed();
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
        assert.equal(status[0], 12, 'incorrect current MTA height');
        assert.equal(status[1], 8, 'incorrect offset');
        assert.equal(status[2], 12, 'incorrect last block height');
    });

    it('should verify relay message', async () => {
        const res = await bmc.testHandleRelayMessage.call(bmv.address, prevBtpAddr, 0, base64Msg);
        for (let i = 0; i < serviceMsgs.length; i++)
            assert.equal(serviceMsgs[i], res[i], 'incorrect service messages');
    });

    it('check contract code size', async () => {
        // console.log((testBMV.deployedBytecode.length / 2) - 1);
        assert.isBelow((testBMV.deployedBytecode.length / 2) - 1, 24576, 'contract size is restricted to 24KB');
        // console.log((testDataValidator.deployedBytecode.length / 2) - 1);
        assert.isBelow((testDataValidator.deployedBytecode.length / 2) - 1, 24576, 'contract size is restricted to 24KB');
    });

    it('should upgrade BMV', async () => {
        const dataValidator = await deployProxy(testDataValidator);
        const BMV = await deployProxy(
            testBMV,
            [
                bmc.address,
                dataValidator.address,
                iconNet,
                encodedValidators,
                initOffset,
                lastBlockHash
            ]
        );
        const upgradeDataValidator = await upgradeProxy(dataValidator.address, dataValidatorV2);
        const upgradeBMV = await upgradeProxy(BMV.address, bmvV2);

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

const rlp = require("rlp");
const assert = require("chai").assert;
const { 
    base64Msg,
    iconNet,
    prevBtpAddr,
    validatorsList,
    serviceMsgs
} = require("./data");

let testBMV = artifacts.require("BMV");
let mockBMC = artifacts.require("MockBMC");

contract("TestBMV", async (accounts) => {
    beforeEach(async () => {
        bmv = await testBMV.deployed();
        bmc = await mockBMC.deployed();
    });

    it("should get connected BMC's address", async () => {
        const addr = await bmv.getConnectedBMC();
        assert.equal(addr, bmc.address, "incorrect bmc address");
    });

    it("should get network address", async () => {
        const btpAddr = await bmv.getNetAddress();
        assert.equal(btpAddr, iconNet, "incorrect btp network address");
    });

    it("should get list of validtors and its serialized data in RLP", async () => {
        const res = await bmv.getValidators();
        const hash = web3.utils.soliditySha3(rlp.encode(validatorsList));
        assert.equal(res[0], hash, "incorrect validators' hash");
        for (let i = 0; i < res[1].length; i++) {
            assert.deepEqual(Buffer.from(res[1][i].substring(2), "hex"), validatorsList[i], "incorrect validator address");
        }
    });

    it("should get BMV status", async () => {
        await bmc.testHandleRelayMessage(bmv.address, prevBtpAddr, 0, base64Msg);
        const status = await bmv.getStatus();
        assert.isNotEmpty(status, "invalid status");
        assert.equal(status[0], 12, "incorrect current MTA height");
        assert.equal(status[1], 8, "incorrect offset");
        assert.equal(status[2], 12, "incorrect last block height");
    });

    it("should verify relay message", async () => {
        const res = await bmc.testHandleRelayMessage.call(bmv.address, prevBtpAddr, 0, base64Msg);
        for (let i = 0; i < serviceMsgs.length; i++)
            assert.equal(serviceMsgs[i], res[i], "incorrect service messages");
    });

    it("check contract code size", async () => {
        // console.log((testBMV.bytecode.length / 2) - 1); // 37490 bytes
        // console.log((testBMV.deployedBytecode.length / 2) - 1); // 31095 bytes
    });
});

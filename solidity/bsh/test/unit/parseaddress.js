
const CheckParseAddress = artifacts.require("CheckParseAddress");
const { assert } = require('chai');
const truffleAssert = require('truffle-assertions');

contract('ParseAddress Library Unit Test', (accounts) => {
    let cpa;

    before(async () => {
        cpa = await CheckParseAddress.new();
    });

    describe('Convert String to Adress', () => {
       
        it('Should return an address when string address is valid', async() => {
            let strAddr = '0x70e789d2f5d469ea30e0525dbfdd5515d6ead30d';
            let res = await cpa.convertStringToAddress(strAddr);
            assert(
                web3.utils.isAddress(res)
            );

            strAddr = web3.utils.toChecksumAddress('0x70e789d2f5d469ea30e0525dbfdd5515d6ead30d');
            res = await cpa.convertStringToAddress(strAddr);
            assert(
                web3.utils.isAddress(res)
            );
        })
    });

    describe('Convert Address to String', () => {
        it('Should convert address to string with a valid checksum', async () => {
            const account = "0x70e789d2f5d469ea30e0525dbfdd5515d6ead30d";
            const res = await cpa.convertAddressToString(account.toLowerCase());

            assert.equal(res, account);
            assert.isTrue(web3.utils.checkAddressChecksum(res));
        });
    });
})
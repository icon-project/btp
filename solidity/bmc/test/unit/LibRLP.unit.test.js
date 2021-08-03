const { assert } = require('chai');
const testLibRLP = artifacts.require('TestLibRLP');

contract('RLP library unit tests', () => {
    let libRLP;

    before(async () => {
        libRLP = await testLibRLP.new();
    });

    describe('RLP encode unit tests', () => {
        it('should encode bytes', async () => {
            const hexBytes = '0x4e454b49dc8a2e2a229e0ce911e9fd4d2aa647de4cf6e0df40cf71bff7283330';
            const res = await libRLP.encodeBytes(hexBytes);
            assert.equal(res, '0xa04e454b49dc8a2e2a229e0ce911e9fd4d2aa647de4cf6e0df40cf71bff7283330');
        });

        it('should encode string', async () => {
            const input = "correctly computes keccak256 hash of the item payload (nested list)";
            const res = await libRLP.encodeString(input);
            assert.equal(res, '0xb843636f72726563746c7920636f6d7075746573206b656363616b3235362068617368206f6620746865206974656d207061796c6f616420286e6573746564206c69737429');
        });

        it('should encode address', async () => {
            const address = '0xF2CD2AA0c7926743B1D4310b2BC984a0a453c3d4';
            const res = await libRLP.encodeAddress(address);
            assert.equal(res, '0x94f2cd2aa0c7926743b1d4310b2bc984a0a453c3d4');
        });

        it('should encode unsigned integer', async () => {
            let res = await libRLP.encodeUint(54994);
            assert.equal(res, '0x8300d6d2');

            res = await libRLP.encodeUint(549943);
            assert.equal(res, '0x83086437');

            res = await libRLP.encodeUint(549945499454999);
            assert.equal(res, '0x8701f42c2a240e17');
        });

        it('should encode signed integer', async () => {
            let res = await libRLP.encodeInt(3278);
            assert.equal(res, '0x820cce');

            res = await libRLP.encodeInt(-3278);
            assert.equal(res, '0x82f332');

            res = await libRLP.encodeInt(888889);
            assert.equal(res, '0x830d9039');

            res = await libRLP.encodeInt(-888889);
            assert.equal(res, '0x83f26fc7');
        });

        it('should encode boolean', async () => {
            let res = await libRLP.encodeBool(true);
            assert.equal(res, '0x01');

            res = await libRLP.encodeBool(false);
            assert.equal(res, '0x00');
        });

        it('should encode list of rlp bytes', async () => {
            const encodedBytes = '0xa04e454b49dc8a2e2a229e0ce911e9fd4d2aa647de4cf6e0df40cf71bff7283330';
            const encodedString = '0xb843636f72726563746c7920636f6d7075746573206b656363616b3235362068617368206f6620746865206974656d207061796c6f616420286e6573746564206c69737429';
            const encodedAddress = '0x94f2cd2aa0c7926743b1d4310b2bc984a0a453c3d4';
            const encodedUint = '0x8701f42c2a240e17';
            const encodedInt = '0x83f26fc7';
            const encodeBoolean = '0x00';
            const list = [
                encodedBytes,
                encodedString,
                encodedAddress,
                encodedUint,
                encodedInt,
                encodeBoolean
            ]

            const res = await libRLP.encodeList(list);
            assert.equal(res, '0xf888a04e454b49dc8a2e2a229e0ce911e9fd4d2aa647de4cf6e0df40cf71bff7283330b843636f72726563746c7920636f6d7075746573206b656363616b3235362068617368206f6620746865206974656d207061796c6f616420286e6573746564206c6973742994f2cd2aa0c7926743b1d4310b2bc984a0a453c3d48701f42c2a240e1783f26fc700');
        });
    });

    describe('RLP decode unit tests', () => {
        it('should decode rlp bytes to bytes', async () => {
            const res = await libRLP.decodeBytes('0xa04e454b49dc8a2e2a229e0ce911e9fd4d2aa647de4cf6e0df40cf71bff7283330');
            assert.equal(res, '0x4e454b49dc8a2e2a229e0ce911e9fd4d2aa647de4cf6e0df40cf71bff7283330');
        });

        it('should decode rlp bytes to string', async () => {
            const res = await libRLP.decodeString('0xb843636f72726563746c7920636f6d7075746573206b656363616b3235362068617368206f6620746865206974656d207061796c6f616420286e6573746564206c69737429');
            assert.equal(res, 'correctly computes keccak256 hash of the item payload (nested list)');
        });

        it('should decode rlp bytes to address', async () => {
            const res = await libRLP.decodeAddress('0x94f2cd2aa0c7926743b1d4310b2bc984a0a453c3d4');
            assert.equal(res, '0xF2CD2AA0c7926743B1D4310b2BC984a0a453c3d4');
        });

        it('should decode rlp bytes to unsiged integer', async () => {
            let res = await libRLP.decodeUint('0x8300d6d2');
            assert.equal(res, 54994);

            res = await libRLP.decodeUint('0x83086437');
            assert.equal(res, 549943);

            res = await libRLP.decodeUint('0x8701f42c2a240e17');
            assert.equal(res, 549945499454999);
        });

        it('should decode rlp bytes to signed integer', async () => {
            let res = await libRLP.decodeInt('0x820cce');
            assert.equal(res, 3278);

            res = await libRLP.decodeInt('0x82f332');
            assert.equal(res, -3278);

            res = await libRLP.decodeInt('0x830d9039');
            assert.equal(res, 888889);

            res = await libRLP.decodeInt('0x83f26fc7');
            assert.equal(res, -888889);
        });

        it('should decode rlp bytes to boolean', async () => {
            let res = await libRLP.decodeBool('0x01');
            assert.isTrue(res);

            res = await libRLP.decodeBool('0x00');
            assert.isFalse(res);
        });

        it('should decode rlp bytes to list of items', async () => {
            const encodedBytes = '0xa04e454b49dc8a2e2a229e0ce911e9fd4d2aa647de4cf6e0df40cf71bff7283330';
            const encodedString = '0xb843636f72726563746c7920636f6d7075746573206b656363616b3235362068617368206f6620746865206974656d207061796c6f616420286e6573746564206c69737429';
            const encodedAddress = '0x94f2cd2aa0c7926743b1d4310b2bc984a0a453c3d4';
            const encodedUint = '0x8701f42c2a240e17';
            const encodedInt = '0x83f26fc7';
            const encodeBoolean = '0x00';
            const list = [
                encodedBytes,
                encodedString,
                encodedAddress,
                encodedUint,
                encodedInt,
                encodeBoolean
            ]

            const res = await libRLP.decodeList('0xf888a04e454b49dc8a2e2a229e0ce911e9fd4d2aa647de4cf6e0df40cf71bff7283330b843636f72726563746c7920636f6d7075746573206b656363616b3235362068617368206f6620746865206974656d207061796c6f616420286e6573746564206c6973742994f2cd2aa0c7926743b1d4310b2bc984a0a453c3d48701f42c2a240e1783f26fc700');
            assert.deepEqual(res, list);
        });
    });
});
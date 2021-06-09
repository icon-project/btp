const assert = require('chai').assert;
const URLSafeBase64 = require('urlsafe-base64');
const rlp = require('rlp');
const truffleAssert = require('truffle-assertions');

let testMpt = artifacts.require('testLibMPT');

contract('TestLibMPT', async () => {
    let testLibMpt;

    beforeEach(async () => {
        testLibMpt = await testMpt.new();
    });

    it('should convert bytes to nibbles', async () => {
        let nibbles = await testLibMpt.bytesToNibbles('0x00', '0x');
        assert.equal(nibbles, '0x0000');

        nibbles = await testLibMpt.bytesToNibbles('0x10', '0x00');
        assert.equal(nibbles, '0x000100');

        nibbles = await testLibMpt.bytesToNibbles('0x112345', '0x00');
        assert.equal(nibbles, '0x00010102030405');

        nibbles = await testLibMpt.bytesToNibbles('0x00012345', '0x');
        assert.equal(nibbles, '0x0000000102030405');

        nibbles = await testLibMpt.bytesToNibbles('0x200f1cb8', '0x01');
        assert.equal(nibbles, '0x010200000f010c0b08');

        nibbles = await testLibMpt.bytesToNibbles('0x3f1cb8', '0x0102');
        assert.equal(nibbles, '0x0102030f010c0b08');
    });

    it('should get shared nibbles length from 2 bytes', async () => {
        let sharedLength = await testLibMpt.matchNibbles(Buffer.from(''), Buffer.from('a'));
        assert.equal(sharedLength, 0);

        sharedLength = await testLibMpt.matchNibbles(Buffer.from('a'), Buffer.from(''));
        assert.equal(sharedLength, 0);

        sharedLength = await testLibMpt.matchNibbles(Buffer.from('a'), Buffer.from('a'));
        assert.equal(sharedLength, 1);

        sharedLength = await testLibMpt.matchNibbles(Buffer.from('aaac'), Buffer.from('aaab'));
        assert.equal(sharedLength, 3);

        sharedLength = await testLibMpt.matchNibbles(Buffer.from('abcd'), Buffer.from('ab'));
        assert.equal(sharedLength, 2);

        sharedLength = await testLibMpt.matchNibbles(Buffer.from('abcd'), Buffer.from('abcdef'));
        assert.equal(sharedLength, 4);

        sharedLength = await testLibMpt.matchNibbles(Buffer.from('fedcba'), Buffer.from('abcdef'));
        assert.equal(sharedLength, 0);
    });

    it('should validate MPT proof and return the value of leaf node', async () => {
        let rootHash = '0x2d98966d69f4ada0703e8c02a1dbe5386762cc3b90fb9e94d7fe4cf81da744ea';
        let key = '0x00';
        let proofs = [
            '0xf90195822000b9018ff9018c9501f41a446a295d02e1a9d6ea341de9efb4e39910cdf858964d657373616765287374722c696e742c627974657329b83e6274703a2f2f30783563643235662e69636f6e2f63786331663639336331333639666161616335666532663465303435303234633066303330333564366601f90119b90116f90113b83e6274703a2f2f30783465353931382e69636f6e2f637866343161343436613239356430326531613964366561333431646539656662346533393931306364b83e6274703a2f2f30783563643235662e69636f6e2f637863316636393363313336396661616163356665326634653034353032346330663033303335643666865f6576656e7400b889f887844c696e6bf880b83e6274703a2f2f30783465353931382e69636f6e2f637866343161343436613239356430326531613964366561333431646539656662346533393931306364b83e6274703a2f2f30783563643235662e69636f6e2f637863316636393363313336396661616163356665326634653034353032346330663033303335643666'
        ];
        let res = await testLibMpt.prove.call(rootHash, key, proofs);
        assert.equal(res, '0xf9018c9501f41a446a295d02e1a9d6ea341de9efb4e39910cdf858964d657373616765287374722c696e742c627974657329b83e6274703a2f2f30783563643235662e69636f6e2f63786331663639336331333639666161616335666532663465303435303234633066303330333564366601f90119b90116f90113b83e6274703a2f2f30783465353931382e69636f6e2f637866343161343436613239356430326531613964366561333431646539656662346533393931306364b83e6274703a2f2f30783563643235662e69636f6e2f637863316636393363313336396661616163356665326634653034353032346330663033303335643666865f6576656e7400b889f887844c696e6bf880b83e6274703a2f2f30783465353931382e69636f6e2f637866343161343436613239356430326531613964366561333431646539656662346533393931306364b83e6274703a2f2f30783563643235662e69636f6e2f637863316636393363313336396661616163356665326634653034353032346330663033303335643666');

        rootHash = '0x1d854f17d88a1be09b146325fe67fb6baaf5ab192f40fe07ecfa0f84d73179f7';
        key = '0x02';
        proofs = [
            '0xe210a0649ceaf94e739f89bbbd83ee6fdae6b979c401a6a6f13c3c598f72f6cdae799a',
            '0xf871a03820879dad43db74eb87a07da4ab0a5141c1379f79d98b29cb80f5a297ba60bfa01d13281e65471f09e46ac284af3e5f67715db5c73341c3391eb3a4bb6cc888ffa0baff8756de1ba52f32ca341fe264b3081a148b2629436252e713139c36568d928080808080808080808080808080',
            '0xef20adec0095002f071e05f858af2332c7c4ac7244319d14c233b2830493e0830186a08502e90edd0080f800f800f800'
        ]
        res = await testLibMpt.prove.call(rootHash, key, proofs);
        assert.equal(res, '0xec0095002f071e05f858af2332c7c4ac7244319d14c233b2830493e0830186a08502e90edd0080f800f800f800')
    });

    it('should revert if given serialized proofs length is invalid', async () => {
        let rootHash = '0x1d854f17d88a1be09b146325fe67fb6baaf5ab192f40fe07ecfa0f84d73179f7';
        let key = '0x02';
        let proofs = [
            '0xe210a0649ceaf94e739f89bbbd83ee6fdae6b979c401a6a6f13c3c598f72f6cdae799a11',
            '0xf871a03820879dad43db74eb87a07da4ab0a5141c1379f79d98b29cb80f5a297ba60bfa01d13281e65471f09e46ac284af3e5f67715db5c73341c3391eb3a4bb6cc888ffa0baff8756de1ba52f32ca341fe264b3081a148b2629436252e713139c36568d928080808080808080808080808080',
            '0xef20adec0095002f071e05f858af2332c7c4ac7244319d14c233b2830493e0830186a08502e90edd0080f800f800f800'
        ]
        await truffleAssert.reverts(testLibMpt.prove.call(rootHash, key, proofs), 'MPTException: Invalid list length');
    });

    // TODO: pending for SHA3-256
    it.skip('should revert if root hash in proof is invalid', async () => {
        let rootHash = '0x2d854f17d88a1be09b146325fe67fb6baaf5ab192f40fe07ecfa0f84d73179f7';
        let key = '0x02';
        let proofs = [
            '0xe210a0649ceaf94e739f89bbbd83ee6fdae6b979c401a6a6f13c3c598f72f6cdae799a',
            '0xf871a03820879dad43db74eb87a07da4ab0a5141c1379f79d98b29cb80f5a297ba60bfa01d13281e65471f09e46ac284af3e5f67715db5c73341c3391eb3a4bb6cc888ffa0baff8756de1ba52f32ca341fe264b3081a148b2629436252e713139c36568d928080808080808080808080808080',
            '0xef20adec0095002f071e05f858af2332c7c4ac7244319d14c233b2830493e0830186a08502e90edd0080f800f800f800'
        ]
        await truffleAssert.reverts(testLibMpt.prove.call(rootHash, key, proofs), 'MPTException: Mismatch hash');
    });

    it('should revert if nibbles on extension are invalid', async () => {
        let rootHash = '0x1d854f17d88a1be09b146325fe67fb6baaf5ab192f40fe07ecfa0f84d73179f7';
        let key = '0x14';
        let proofs = [
            '0xe210a0649ceaf94e739f89bbbd83ee6fdae6b979c401a6a6f13c3c598f72f6cdae799a',
            '0xf871a03820879dad43db74eb87a07da4ab0a5141c1379f79d98b29cb80f5a297ba60bfa01d13281e65471f09e46ac284af3e5f67715db5c73341c3391eb3a4bb6cc888ffa0baff8756de1ba52f32ca341fe264b3081a148b2629436252e713139c36568d928080808080808080808080808080',
            '0xef20adec0095002f071e05f858af2332c7c4ac7244319d14c233b2830493e0830186a08502e90edd0080f800f800f800'
        ]
        await truffleAssert.reverts(testLibMpt.prove.call(rootHash, key, proofs), 'MPTException: Mismatch nibbles on extension');
    });

    it('should revert if nibbles on leaf are invalid', async () => {
        let rootHash = '0x1d854f17d88a1be09b146325fe67fb6baaf5ab192f40fe07ecfa0f84d73179f7';
        let key = '0x023';
        let proofs = [
            '0xe210a0649ceaf94e739f89bbbd83ee6fdae6b979c401a6a6f13c3c598f72f6cdae799a',
            '0xf871a03820879dad43db74eb87a07da4ab0a5141c1379f79d98b29cb80f5a297ba60bfa01d13281e65471f09e46ac284af3e5f67715db5c73341c3391eb3a4bb6cc888ffa0baff8756de1ba52f32ca341fe264b3081a148b2629436252e713139c36568d928080808080808080808080808080',
            '0xef20adec0095002f071e05f858af2332c7c4ac7244319d14c233b2830493e0830186a08502e90edd0080f800f800f800'
        ]
        await truffleAssert.reverts(testLibMpt.prove.call(rootHash, key, proofs), 'MPTException: Mismatch nibbles on leaf');
    });
});
const rlp = require('rlp');
const utils = require('./utils');
const data = require('./data');

let bmv = artifacts.require('BtpMessageVerifier');
let validators;

contract('handle relay message', (accounts) => {
    before(async () => {
        bmv = await bmv.deployed();
        validators = data.validators.keys.map((v) => utils.keyToAccount(v));
    });

    it('verify network type section decision proof', async () => {
        const NTSDH = '0x' + utils.sha3(Buffer.from('NetworkTypeSectionDecision'));
        const signatures = validators.map((validator, index) => {
            return utils.toHexstr(validator.sign(NTSDH));
        });

        let result = await bmv.verifySignature.call(NTSDH, signatures);
    });

    it('calculate merkle root', async () => {
        let values = [
            'alice',
            'bob',
            'crystal',
            'dante',
            'elise'
        ].map(v => Buffer.from(v));

        const branch = (l, r) => {
            return utils.sha3(Buffer.concat([
                Buffer.from(l, 'hex'),
                Buffer.from(r, 'hex')
            ]));
        }

        //                   h12345(root)
        //          h1234
        //    h12           h34
        // h1     h2     h3     h4     h5
        // v1     v2     v3     v4     v5
        let leaves = values.map((v) => utils.sha3(v));
        let h12 = branch(leaves[0], leaves[1]);
        let h34 = branch(leaves[2], leaves[3]);
        let h1234 = branch(h12, h34);
        let root = branch(h1234, leaves[4]);

        let result = await bmv.calculateMerkleRoot.call(
            '0x' + values[1].toString('hex'),
            '0x' + Buffer.from(rlp.encode([
                [0, Buffer.from(leaves[0], 'hex')],
                [1, Buffer.from(h34, 'hex')],
                [1, Buffer.from(leaves[4], 'hex')]
            ])).toString('hex')
        );

        assert('0x' + root == result, "fail to calculate merkle root");
    });

    it('decode proof context', async () => {
        let proofContext = rlp.encode([
            validators.map((validator) => utils.toBuffer(validator.address))
        ]);

        let result = await bmv.decodeProofContext.call(proofContext);

        assert.deepEqual(result, validators.map((v) => v.address));
    });

    it('make network section root', async () => {
        const network_id = 0x1;
        const messages_root_number = 0x20;
        const prev_ns_hash = '0x' + utils.sha3('prev_ns_hash');
        const message_count = 0x3;
        const messages_root = '0x' + utils.sha3('message_root');

        let result = await bmv.makeNS.call(network_id, messages_root_number,
            prev_ns_hash, message_count, messages_root);

        assert(result == '0x' + utils.sha3(rlp.encode([
            network_id,
            messages_root_number,
            prev_ns_hash,
            message_count,
            messages_root
        ])), "failed to encode network section");
    });
});


const MessageProofMock = artifacts.require('MessageProofMock');

const encodeToString = (hex) => {
    return Buffer.from(hex.slice(2, hex.length), 'hex').toString();
};

const data = [
    [
        // [
        //     [],
        //     ['bird', 'monkey', 'lion'],
        //     []
        // ]
        ['bird', 'monkey', 'lion'],
        Buffer.from('d6f800d18462697264866d6f6e6b6579846c696f6ef800', 'hex'),
        '0x07d74e34c2e775d482485d1ffe1a2b14138829cea4744ea97407a8f90518de69'
    ], [
        // [
        //     [],
        //     [bird, monkey, lion],
        //     [
        //         [1, 38f4921087750bcc254ecc791170ebb01a0297d49c630a8844c18f6104a5da07],
        //         [1, 41791102999c339c844880b23950704cc43aa840f3739e365323cda4dfa89e7a]
        //     ]
        // ]
        ['bird', 'monkey', 'lion'],
        Buffer.from('f85bc0d18462697264866d6f6e6b6579846c696f6ef846e201a038f4921087750bcc254ecc791170ebb01a0297d49c630a8844c18f6104a5da07e201a041791102999c339c844880b23950704cc43aa840f3739e365323cda4dfa89e7a', 'hex'),
        '0x6e84e2af80a9151351b9451ac75674612c7ea97519fd90bff1deefff80f16240'
    ], [
        // [
        //     [
        //         [2, d3f9be5d7f0324950c636665ce754816c87f5bd9cd64aee0ce1c0bd0fa8a2242],
        //         [1, 1c8120673ee3599a351d16679db297925336d9c524aa267ded697da9f3547a27]
        //     ],
        //     [tiger, dog],
        //     []
        // ]
        ['tiger', 'dog'],
        Buffer.from('f854f846e202a0d3f9be5d7f0324950c636665ce754816c87f5bd9cd64aee0ce1c0bd0fa8a2242e201a01c8120673ee3599a351d16679db297925336d9c524aa267ded697da9f3547a27ca85746967657283646f67c0', 'hex'),
        '0x6e84e2af80a9151351b9451ac75674612c7ea97519fd90bff1deefff80f16240'
    ], [
        // [
        //     [],
        //     [bird, monkey, lion],
        //     [
        //         [1, 38f4921087750bcc254ecc791170ebb01a0297d49c630a8844c18f6104a5da07],
        //         [3, ca44bc4808c039b42c6d8791bb6cfa462cb6001e76683837666b1e2dbe56868b]
        //     ]
        // ]
        ['bird', 'monkey', 'lion'],
        Buffer.from('f85bc0d18462697264866d6f6e6b6579846c696f6ef846e201a038f4921087750bcc254ecc791170ebb01a0297d49c630a8844c18f6104a5da07e203a0ca44bc4808c039b42c6d8791bb6cfa462cb6001e76683837666b1e2dbe56868b', 'hex'),
        '0xb531073940c619e7db862952ba2c7f52d18639e1525706e14997621c76b5316a'
    ], [
        // [
        //     [
        //         [2, d3f9be5d7f0324950c636665ce754816c87f5bd9cd64aee0ce1c0bd0fa8a2242],
        //         [1, 1c8120673ee3599a351d16679db297925336d9c524aa267ded697da9f3547a27]
        //     ],
        //     [tiger, dog],
        //     [
        //         [1, 52763589e772702fa7977a28b3cfb6ca534f0208a2b2d55f7558af664eac478a],
        //         [1, 468412432735e704136dcef80532ffc5db671fd0361b59f77d1462bcb83995e9]
        //     ]
        // ]
        ['tiger', 'dog'],
        Buffer.from('f89bf846e202a0d3f9be5d7f0324950c636665ce754816c87f5bd9cd64aee0ce1c0bd0fa8a2242e201a01c8120673ee3599a351d16679db297925336d9c524aa267ded697da9f3547a27ca85746967657283646f67f846e201a052763589e772702fa7977a28b3cfb6ca534f0208a2b2d55f7558af664eac478ae201a0468412432735e704136dcef80532ffc5db671fd0361b59f77d1462bcb83995e9', 'hex'),
        '0xb531073940c619e7db862952ba2c7f52d18639e1525706e14997621c76b5316a'
    ], [
        // [
        //     [
        //         [4, 849f910824cda0c31e6495aa8b8264d8f0ad4450b78127cbb59ea87e1e6f6c89],
        //         [1, 41791102999c339c844880b23950704cc43aa840f3739e365323cda4dfa89e7a]
        //     ],
        //     [cat, elephant],
        //     []
        // ]
        ['cat', 'elephant'],
        Buffer.from('f857f846e204a0849f910824cda0c31e6495aa8b8264d8f0ad4450b78127cbb59ea87e1e6f6c89e201a041791102999c339c844880b23950704cc43aa840f3739e365323cda4dfa89e7acd8363617488656c657068616e74c0', 'hex'),
        '0xb531073940c619e7db862952ba2c7f52d18639e1525706e14997621c76b5316a'
    ]
].map(d => {
    return { msg: d[0], rlp: d[1], root: d[2] };
});

contract('MessageProof', (accounts) => {
    beforeEach(async () => {
        this.instance = await MessageProofMock.new();
    });

    it('decode valid message', async () => {
        for (let nth in data) {
            let v = await this.instance.decode.call(data[nth].rlp);
            assert.deepEqual(v[1].map(m => encodeToString(m)), data[nth].msg);
        }
    });

    it('calculate root of messages', async () => {
        for (let nth in data) {
            let r = await this.instance.calculate.call(data[nth].rlp);
            assert.equal(r, data[nth].root, `root mismatch (nth=${nth})`);
        }
    });

});


const BtpMessageVerifier = artifacts.require('BtpMessageVerifier');
const BN = require('bn.js');
const chai = require('chai');
const { expect } = chai;

const ZB32 = '0x0000000000000000000000000000000000000000000000000000000000000000';
const SNID = '0x6274703a2f2f3078312e69636f6e';
const NTID = new BN('0');
const NID = new BN('0');

chai.use(require('chai-bn')(BN));

contract('BtpMessageVerifier', (account) => {
    const RLP_FIRST_BLOCK_UPDATE = '0xf8870101a0177b09e6b788d2a5f9d7572265f8a54176a83783e6d1d99d1865bc24e04fb493c00001f80000f800f800b858f856f85494524122f6386c9cd3342ecc377dbc9dcb036dd2e0948103611a29623db06a937b24f52d70cf44c1c41494910d15f35a3e685968d8596512efa56d840bb3c59451226eee21a3d3758727886df161c108f5857f3f'

    beforeEach(async () => {
        this.instance = await BtpMessageVerifier.new();
        await this.instance.initialize(SNID, NTID, NID, RLP_FIRST_BLOCK_UPDATE);
    });

    shouldHaveThisState.call(this, {
        height: new BN(1),
        messageRoot: ZB32,
        messageSn: new BN(0),
        messageCount: new BN(0),
        remainMessageCount: new BN(0),
        networkSectionHash: '0x73d99f781d2472277c1a1c2f931b58193b4673ba52a0ee2678f5db2e3d776b57',
        validators: [
            '0x524122f6386c9cd3342ecc377dbc9dcb036dd2e0',
            '0x8103611a29623db06a937b24f52d70cf44c1c414',
            '0x910d15f35a3e685968d8596512efa56d840bb3c5',
            '0x51226eee21a3d3758727886df161c108f5857f3f'
        ]
    });

    describe('send relay message containing block update that changed validators', () => {
        const RLP_RM_CHANGED_VALIDATORS = '0xf901c6f901c3f901c001b901bcf901b90600a0c117ffff03c64deb54d6492e796d4b86734c70dcca222ba2cf660808d43540e1c00001a073d99f781d2472277c1a1c2f931b58193b4673ba52a0ee2678f5db2e3d776b5700f800b90112f9010ff9010cb8417574c6c819a4ca8c2a5418fe59bb0884f94162c7c6425d096e87a7fd492a8873466b268c1a026c9a1346d1f69a5ef927d837842f3067aff938b73cf9e8a693e800b8410b378997849540e084c7ace6bfbee2045c501c2e841c21ba7accd874afabfb994facd2da6f39a76b01af51259895ebb4d2332508095cc03a9c010f9e97bd094f00b841a8b859d7dc72d1918a64054ef41bbbe92dbbca5086986290d4bede7c0f1e174d264c3b46a53e2562d9c5ce8f0a41f3327f17f802b63c95bb065e38c49992572500b841985639b38d3bd281a0fae6f397cd9fed0b8f0a982c117c696e4290cc5a3ba07724392cfc5e6ce0b0e3060d311f8ce885adf55ba5ad32c44c0e1ffe0f0b1d923b01b858f856f8549491a6eb0a05527a5b816e89f3f18ed666486976f49462f08482840bcbe93aba6dae370ba7c99c6229aa94d3cf82a1654f30fe558a87b8470c8bcf13127a329458722d1bf46b7a617f86a319e5f255f19af0d6ca'

        beforeEach(async () => {
            await this.instance.handleRelayMessage('', '', 0, RLP_RM_CHANGED_VALIDATORS);
        });

        shouldHaveThisState.call(this, {
            height: new BN(6),
            messageRoot: ZB32,
            messageSn: new BN(0),
            messageCount: new BN(0),
            remainMessageCount: new BN(0),
            networkSectionHash: '0xeaff3b1ce7b1b22c9de0d8c0876fe73f6d179b92ec2a79b2d8fec07b84bcc5a9',
            validators: [
                '0x91a6eb0a05527a5b816e89f3f18ed666486976f4',
                '0x62f08482840bcbe93aba6dae370ba7c99c6229aa',
                '0xd3cf82a1654f30fe558a87b8470c8bcf13127a32',
                '0x58722d1bf46b7a617f86a319e5f255f19af0d6ca'
            ]
        });
    });

    describe('send relay message containing single message proof', () => {
        const RLP_RM_WITH_SINGLE_MESSAGE_PROOF = '0xf901a7f901a4f9018701b90183f901800b05a0177b09e6b788d2a5f9d7572265f8a54176a83783e6d1d99d1865bc24e04fb493c00000a073d99f781d2472277c1a1c2f931b58193b4673ba52a0ee2678f5db2e3d776b5703a007d74e34c2e775d482485d1ffe1a2b14138829cea4744ea97407a8f90518de69b90112f9010ff9010cb84179929f6f4652217ba91ec3558e9ae387777e06a6347b3420eb4640114aca6b88486510d3ff65b3f27e532f290cafbe911ebf3d8851f7084ac6e83d5e38465a6a01b8415f8883c5171bb875adcb107d3521f6f6de45843b4556bd5fdda90b9e1cbb86a94fe9ccd37ba3c524b92f663169a28af2879cf094dc0f0a80f0f5b8095be131f301b841c8934adaedf0d7e503e26736020f42949e745ce7ab7ac919832d222303d0fe0b698cbe4a143f3f24daf05cfd642dbaebb74495b813427e71b1f0570810ed824600b841d4ee9a0767423c9d6bd9b9bd9522f352628f54e48dc90be58111aa2a8c26735c61c831ccfd873477fdf357bba6523cfb4fc7414956dfb46206cc8cedb9049edb00f800d90297d6f800d18462697264866d6f6e6b6579846c696f6ef800';

        describe('returns messages', () => {
            it('returns messages', async () => {
                let messages = await this.instance.handleRelayMessage
                    .call('', '', 0, RLP_RM_WITH_SINGLE_MESSAGE_PROOF)

                expect(messages.map(m => Buffer.from(m.slice(2, m.length), 'hex').toString()))
                    .to.deep.equal(['bird', 'monkey', 'lion']);
            });
        });

        describe('changes state', () => {
            beforeEach(async () => {
                await this.instance.handleRelayMessage('', '', 0, RLP_RM_WITH_SINGLE_MESSAGE_PROOF);
            });

            shouldHaveThisState.call(this, {
                height: new BN(11),
                messageRoot: '0x07d74e34c2e775d482485d1ffe1a2b14138829cea4744ea97407a8f90518de69',
                messageSn: new BN(3),
                messageCount: new BN(3),
                remainMessageCount: new BN(0),
                networkSectionHash: '0xbe657769403cd39ada848f7a87c2636c9c5100585986c7599e73efe87dd87ead',
                validators: [
                    '0x524122f6386c9cd3342ecc377dbc9dcb036dd2e0',
                    '0x8103611a29623db06a937b24f52d70cf44c1c414',
                    '0x910d15f35a3e685968d8596512efa56d840bb3c5',
                    '0x51226eee21a3d3758727886df161c108f5857f3f'
                ]
            });
        });
    });
});

function shouldHaveThisState(props) {
    it('has source network id', async () => {
        expect(await this.instance.srcNetworkId())
            .to.equal(SNID);
    });

    it('has network type id', async () => {
        expect(await this.instance.networkTypeId())
            .to.be.bignumber.equal(NTID);
    });

    it('has network id', async () => {
        expect(await this.instance.networkId())
            .to.be.bignumber.equal(NID);
    });

    it('has block height', async () => {
        expect(await this.instance.height())
            .to.be.bignumber.equal(props.height);
    });

    it('has network section hash', async () => {
        expect(await this.instance.networkSectionHash())
            .to.equal(props.networkSectionHash);
    });

    it('has message root', async () => {
        expect(await this.instance.messageRoot())
            .to.equal(props.messageRoot);
    });

    it('has message count', async () => {
        expect(await this.instance.messageCount())
            .to.be.bignumber.equal(props.messageCount);
    });

    it('has remain message count', async () => {
        expect(await this.instance.remainMessageCount())
            .to.be.bignumber.equal(props.remainMessageCount);
    });


    it('has next message sequence number', async () => {
        expect(await this.instance.nextMessageSn())
            .to.be.bignumber.equal(props.messageSn);
    });

    it('has validators', async () => {
        expect(await this.instance.validatorsCount())
            .to.be.bignumber.equal(new BN(props.validators.length));

        for (nth in props.validators) {
            expect((await this.instance.validators(nth)).toLowerCase())
                .to.equal(props.validators[nth]);
        }
    });
}

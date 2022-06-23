const { BN, constants, expectEvent, expectRevert } = require('@openzeppelin/test-helpers');
const { expect } = require('chai');
const { deployProxy, upgradeProxy } = require('@openzeppelin/truffle-upgrades')
const { ZB32, toBytesString, toStr } = require('./utils');

const BtpMessageVerifier = artifacts.require('BtpMessageVerifier');
const BtpMessageVerifierV2 = artifacts.require('BtpMessageVerifierV2');

contract('BtpMessageVerifierV2', (accounts) => {
    const SRC_NETWORK_ID = 'btp://0x1.icon'
    const BMC = accounts[0];
    const NETWORK_TYPE_ID = new BN(1);
    const NETWORK_ID = new BN(1);
    const FIRST_BLOCK_UPDATE = '0xf8870a01a0177b09e6b788d2a5f9d7572265f8a54176a83783e6d1d99d1865bc24e04fb493c00101f80000f800f800b858f856f85494524122f6386c9cd3342ecc377dbc9dcb036dd2e0948103611a29623db06a937b24f52d70cf44c1c41494910d15f35a3e685968d8596512efa56d840bb3c59451226eee21a3d3758727886df161c108f5857f3f';
    const validators = [
        '0x524122f6386c9cd3342ecc377dbc9dcb036dd2e0',
        '0x8103611a29623db06a937b24f52d70cf44c1c414',
        '0x910d15f35a3e685968d8596512efa56d840bb3c5',
        '0x51226eee21a3d3758727886df161c108f5857f3f'
    ];

    describe('when initial version has been installed', () => {
        beforeEach(async () => {
            this.v1 = await deployProxy(BtpMessageVerifier, [
                BMC,
                toBytesString(SRC_NETWORK_ID),
                NETWORK_TYPE_ID,
                NETWORK_ID,
                FIRST_BLOCK_UPDATE
            ]);
        });

        describe('when initial version was upgraded to second version', () => {
            beforeEach(async () => {
                this.instance = await upgradeProxy(this.v1.address, BtpMessageVerifierV2);
            });

            shouldHaveThisState.call(this, {
                height: new BN(10),
                messageRoot: ZB32,
                messageSn: new BN(0),
                messageCount: new BN(0),
                remainMessageCount: new BN(0),
                networkSectionHash: '0xb791b4b069c561ca31093f825f083f6cc3c8e5ad5135625becd2ff77a8ccfa1e',
                validators
            });
        });

        describe('when there are messages to be handled in initial version', () => {
            const RELAY_MESSAGES = [
                // [BlockUpdate(msg count = 1)]
                '0xf9018df9018af9018701b90183f901801401a0177b09e6b788d2a5f9d7572265f8a54176a83783e6d1d99d1865bc24e04fb493c00100a0b791b4b069c561ca31093f825f083f6cc3c8e5ad5135625becd2ff77a8ccfa1e01a09c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501b90112f9010ff9010cb841f83b0c75b8839eb55a84713279adf8d460c457a5e0f2256c426b73be6c182364041c1343c32e2ae22b191969bbcdae67e78a80d01176b58648cafa13496bcc2501b84142fdf2906da9b70163b5263207a82b48f53eae9d073c89baf569c49e49aec44b58796cbcb73e9b9641065b264b5d1523128a849f852b8b76dfa7cd18bc4b818300b841e53b821c5d4650b6f9bf2d03e32b0834509aabb3fcb6037ff8f4e9d8c78893c23812ba54baa2e21cb09b3ff83d9eba118280c456891bee670493c00a3945b26a00b8411aa9686c0d83543a38fec3a2b8a59fecea80f19cf46dc66d513cfbe6447f66633b0d449740602bd75cce89fceff16879d103618a420d7f861288cd29533c4def01f800',
                // [MessageProof]
                '0xd0cfce028ccbf800c685616c696365f800'
            ];
            beforeEach(async () => {
                await this.v1.handleRelayMessage('', SRC_NETWORK_ID, 0, RELAY_MESSAGES[0]);
            });

            describe('when initial version was upgraded to second version', () => {
                beforeEach(async () => {
                    this.instance = await upgradeProxy(this.v1.address, BtpMessageVerifierV2);
                });

                shouldHaveThisState.call(this, {
                    height: new BN(20),
                    messageRoot: '0x9c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501',
                    messageSn: new BN(0),
                    messageCount: new BN(1),
                    remainMessageCount: new BN(1),
                    networkSectionHash: '0x064be11ce64b5e8c007b34bebbf39dd0df0f77afcaf50cf31cc379af17924f4a',
                    validators
                });

                it.skip('second version returns messages', async () => {
                    let msgs = await this.instance.handleRelayMessage.call('', SRC_NETWORK_ID, 0, RELAY_MESSAGES[1]);
                    expect(msgs.map(v => toStr(v))).to.deep.equal(['alice']);
                });
            });
        });
    });
});

function shouldHaveThisState(props) {
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

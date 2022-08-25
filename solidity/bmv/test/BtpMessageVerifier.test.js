const { BN, constants, expectEvent, expectRevert } = require('@openzeppelin/test-helpers');
const url = require('url');
const { expect } = require('chai');
const BtpMessageVerifier = artifacts.require('BtpMessageVerifier');
const { ZB32, toStr } = require('./utils');
const Errors = {
    ERR_UNKNOWN: "bmv: Unknown|25",
    ERR_NOT_VERIFIABLE: "bmv: NotVerifiable|26",
    ERR_ALREADY_VERIFIED: "bmv: AlreadyVerified|27",

    ERR_UNAUTHORIZED: "bmv: Unauthorized",
}

contract('BtpMessageVerifier', (accounts) => {
    const BMC = accounts[0];
    const SEQUENCE_OFFSET = new BN('0');

    describe('common test scenario', () => {
        const RELAY_MESSAGES = [
            '0xcecdcc028ac9f800c483646f67f800',
            '0xf90227f90224f9020601b90202f901ffb8e8f8e61400a02bf59cc47e3a486a79538905537cb05de356ab87c4b3c6fc2f9eef60a7f9e2f1e3e201a0a4997d283af68023f69666832df08cafb4b91789b10438f13b48bdfbaa03e4ac0203a0b01a7e90a687b64b58e2410a31e1b2e8e131672563c6c52db84eeadd15b6956403a04eaeed1d1e8444f108a0f79abbc5150dd768bbda89279c2e4a301fe8c4e5dd26b858f856f854948b23df84e212853bdda59507788bf55171a6f3909468d5c7be0d4423e196b67068c4cfc45223f1a528944752531087ea4697dc2f128e01c6f466ee254e769498cdda8989b8ecc0d30b617e1dc242e4ccd296c4b90112f9010ff9010cb84108cb45aaa3b46719f3e8894c97b470544f1a5539f78f0fffd6380d375e5b1bd4079aaa08257400abda1309c3fe0cbd280f3aeae4b669c0172e542933d1ae397501b84125cda1cc0255f0ee02064e8c79a6406dba33d181fb71c3d0a45b2bdafe20d1f25f64412c51190d0b042c905ae90c9bfffe12a71889dd2e25c44f98aee3f2e0b200b84167c8f3d0646f58610853a51e62e167bdeaffcc50e54c4a0c038b8cfcfff12f181695f3ef9df5ebe31dff9b27c382145978c0089f8e00bcbf083ee70c2bbca05801b841d164cc3920f625346a9de5d041dcd21e55a2707d84869e2b15f89d1eeeb84d5659dcd49f8dca65e1c01f6f6b99a1d218af383f517d11a7a603d2b254a018ba6b00da0298d7f800d28363617488656c657068616e748462697264f800',
            '0xf901b4f901b1f901ae01b901aaf901a7b890f88e1e00a02bf59cc47e3a486a79538905537cb05de356ab87c4b3c6fc2f9eef60a7f9e2f1e3e201a0dd7d7cd1229649a054dbfd03969abe0ce4e439afc09f36e07165b95f866f565c0208a0335fc784176e18f4d3efefc662503f0fd8fe120751ed4251a66aaa938640015203a086c05cf325d1a9fc932360037a87b8871c838f274eab4d8010ae9c81b3de24a9f800b90112f9010ff9010cb841920b6c311d5d4be72c8ddb9599b94842c029a8e2630608d892d87dd2b911c3894029a749ff42647ee7b849404591a5227fb932a2cb7cdc017dd78a04383b770b01b841d5d46fd23e06882bf5325554cf63090ddea2c15a09ad740707b9a50715d4ca6c5e14d836c01edebedc09d414e9315a11bad0c25c3f5304dadde5567ff953e79401b8418e30acb1f4e3879289c969584966b080cee33e4f42dc2d14d8ed74f8ef2e69ec1551cfe0ae54d0e14f5ede7cb3597d3e8823a2ba2147c5d9b81ee144b2046c2f01b84166d5b56bcf5a7110a9680ed0cdc4999b4a520991b2fa91c741068d57e42390f57962f2e432a17762208f8c766f83f1fb6e7fee83af30026d4e5a83e22441963e00',
            '0xf868f866f502b3f2c0cc866d6f6e6b6579846c696f6ee3e201a038f4921087750bcc254ecc791170ebb01a0297d49c630a8844c18f6104a5da07ef02adece3e202a09dc633d90e96b8744b27aaa5bb6bb2cca28f187c196cf7af1c82d8d1e8cd5f6fc6857469676572c0',
             '0xf903d7f903d4f901e701b901e3f901e0b8c9f8c72800a00a6b1f7f6a4399ebb1ecbc91f6aaf1909ac24eae0f9272fd509d258678a78108e3e201a08c46a84b0bad8d2ee375d1fb666256ef0abb7bfcf208b0e6e9a83718e2dda4c8020fa049da67cde1ed94d33df761d65abe0f8b17bedd41a133df50de63aeb80aa9a7e700f800b858f856f854943a7628e882019c2de9eb598a17e6b2f9bff8df0c946b9669b054c5305ec5ca0b17601ab3c5f2afc3f694991ebc1d9508d0c0d5857dbd1d0db643f6d87143946e5cba0c7a327bf777645a7bccf74781544c4b2db90112f9010ff9010cb8417bc3f31ee790c25efbadba0c305b0e2c9dee1ec46e1b860f30b5f55f0b0afe5c36986bc38e59303e7b66855d7cd8ecfb435f902763fd92a1a29d3a34f1bb96b100b841b50311b27807ca71522905a2568320431d716beef7f51d32c0419417a8e2b20f451a917eff4db316a12a1ce507dee1d9337c103b580e02a1d53ed4967f6da46800b8415014f0c97ba43928f264d26e7e02458a6ef250fa2c3ed509c1188c001163afe95c5b2631b687088e5e6dbe3252c6ee142238eb5cdb699951f744c57e2df0b9fb01b841e9cc9d47aeae55b7953e6588ff29e4aa160a8c285948e543157dfc9c9de8f8283fe7625f3cddf9ee24b710dc541164676f637b8af0685e4be76bf69f77e7bd8f00f901e701b901e3f901e0b8c9f8c73200a0bf658c599a716a705fe05b89552d363f0bbd6788619d005d1fdc5ae857b5f5fee3e201a0d1cdfff9f24d50ba88a34f19cbdf5c214169df63629e299d9baffb242a1bb428020fa0efe5785c35214f54e0ee595ebbe96842cd158d9329e3f3f6ed7e30f8cfa835f900f800b858f856f85494829296427556b23451dbf1cbfc9e436b5011813394958f3dcecaec9f52738de296a413316953dd5da6942a1e8e9fa2f3bb0c481cc9682edc4136652de0e3942d90a4cb77b86ed5ebdd49fb959a38c98b32ec4eb90112f9010ff9010cb841f95751b14d944b428c157847a70a4ad6d135ed0042b840a27d67018548d04d182b1f0a270e915c55f6f2e01fac5dae9f4e133a2456575291904a48603dbad3b901b84168d72d61d1469baca873017a2071cd7f656034c3b033b51a8334a3c95030341731cc0811c359e735ad6df1dc7939358c8998920b774ba258c51bd2cbbab51da700b8417ae75cd923c74e6383d5153149e01636a34d80526619aee149eb579c0013a8fb74b8d86ada8ffc16da4605b41b89451c6c730cdf5cbccd8b81e6f07b8365dd3c00b841058c1ddd5710315931db918133161278a5a2472fad846b4a61343f426838eaff3516ee70037941cb2892d0698e1ddc5d8c1acba12dd635f39091c89eede006a501'
        ];

        describe('when bmv has been initialized', () => {
            const SRC_NETWORK_ID = '0x1.icon'
            const SRC_BMC_BTP_ADDR = 'btp://' + SRC_NETWORK_ID + '/' + 'srcBmcCA';
            const NETWORK_TYPE_ID = new BN(2);
            const FIRST_BLOCK_UPDATE = '0xf8a40a00a07335dc4b60092f2d5aa4b419bf4ea1fe7e76b6a74f7177a26b20733a20d75081c00201f80001a041791102999c339c844880b23950704cc43aa840f3739e365323cda4dfa89e7ab858f856f8549435343874344652abe559b226f00abec23a98a7a594cfb89e639a3b69704631cadae3517165fbf06e1e944fe6e85b23709cbf74e98f81d5869e2b46b9721f94ccefbb67c172b02e70b699884b76e39806eefe00';

            beforeEach(async () => {
                this.instance = await BtpMessageVerifier.new();
                await this.instance.initialize(BMC, SRC_NETWORK_ID, NETWORK_TYPE_ID, FIRST_BLOCK_UPDATE, SEQUENCE_OFFSET);
            });

            describe('sends RelayMessage=[MessageProof]', () => {
                it('returns messages', async () => {
                    let msgs = await this.instance.handleRelayMessage.call('', SRC_BMC_BTP_ADDR, 0, RELAY_MESSAGES[0]);
                    expect(msgs.map(v => toStr(v))).to.deep.equal(['dog']);
                });

                describe('when state has been updated, height(10)', () => {
                    beforeEach(async () => {
                        await this.instance.handleRelayMessage('', SRC_BMC_BTP_ADDR, 0, RELAY_MESSAGES[0]);
                    });

                    describe('sends RelayMessage=[BlockUpdate, MessageProof]', () => {
                        it('returns messages', async () => {
                            let msgs = await this.instance.handleRelayMessage.call('', SRC_BMC_BTP_ADDR, 1, RELAY_MESSAGES[1]);
                            expect(msgs.map(v => toStr(v))).to.deep.equal(['cat', 'elephant', 'bird']);
                        });

                        describe('when state has been updated, height(20)', () => {
                            beforeEach(async () => {
                                await this.instance.handleRelayMessage('', SRC_BMC_BTP_ADDR, 1, RELAY_MESSAGES[1]);
                            });

                            describe('sends RelayMessage=[BlockUpdate(changing validators)]', () => {
                                it('returns empty list', async () => {
                                    let msgs = await this.instance.handleRelayMessage.call('', SRC_BMC_BTP_ADDR, 4, RELAY_MESSAGES[2]);
                                    expect(msgs.map(v => toStr(v))).to.be.empty;
                                });

                                describe('when state has been updated, height(30)', () => {
                                    beforeEach(async () => {
                                        await this.instance.handleRelayMessage('', SRC_BMC_BTP_ADDR, 4, RELAY_MESSAGES[2]);
                                    });

                                    describe('sends RelayMessage=[MessageProof, MessageProof]', () => {
                                        it('returns messages', async () => {
                                            let msgs = await this.instance.handleRelayMessage.call('', SRC_BMC_BTP_ADDR, 4, RELAY_MESSAGES[3]);
                                            expect(msgs.map(v => toStr(v))).to.deep.equal(['monkey', 'lion', 'tiger']);
                                        });

                                        describe('when state has been updated, height(40)', () => {
                                            beforeEach(async () => {
                                                await this.instance.handleRelayMessage('', SRC_BMC_BTP_ADDR, 4, RELAY_MESSAGES[3]);
                                            });

                                            describe('sends RelayMessage=[BlockUpdate(changing validators), BlockUpdate(chainging validators)]', () => {
                                                it('returns empty list', async () => {
                                                    let msgs = await this.instance.handleRelayMessage.call('', SRC_BMC_BTP_ADDR, 7, RELAY_MESSAGES[4]);
                                                    expect(msgs.map(v => toStr(v))).to.be.empty;
                                                });

                                                describe('when state has been updated, height(50)', () => {
                                                    beforeEach(async () => {
                                                        await this.instance.handleRelayMessage('', SRC_BMC_BTP_ADDR, 7, RELAY_MESSAGES[4]);
                                                    });

                                                    shouldHaveThisState.call(this, {
                                                        height: new BN(50),
                                                        messageRoot: '0x86c05cf325d1a9fc932360037a87b8871c838f274eab4d8010ae9c81b3de24a9',
                                                        messageSn: new BN(7),
                                                        messageCount: new BN(3),
                                                        remainMessageCount: new BN(0),
                                                        networkSectionHash: '0xd33456b7b455a6381b4e523716b33f9593d3dfe00c7219a3656c983ed99ab8a9',
                                                        validators: [
                                                            '0x829296427556b23451dbf1cbfc9e436b50118133',
                                                            '0x958f3dcecaec9f52738de296a413316953dd5da6',
                                                            '0x2a1e8e9fa2f3bb0c481cc9682edc4136652de0e3',
                                                            '0x2d90a4cb77b86ed5ebdd49fb959a38c98b32ec4e'
                                                        ]
                                                    });
                                                });
                                            });
                                        });
                                    });
                                });
                            });
                        });
                    });
                });
            });
        });
    });

    describe('when has installed: BlockUpdate', () => {
        const SRC_NETWORK_ID = '0x1.icon'
        const SRC_BMC_BTP_ADDR = 'btp://' + SRC_NETWORK_ID + '/' + 'srcBmcCA';
        const NETWORK_TYPE_ID = new BN(1);
        const VALIDATORS = [
            '0x35343874344652abe559b226f00abec23a98a7a5',
            '0xcfb89e639a3b69704631cadae3517165fbf06e1e',
            '0x4fe6e85b23709cbf74e98f81d5869e2b46b9721f',
            '0xccefbb67c172b02e70b699884b76e39806eefe00'
        ];

        beforeEach(async () => {
            const FIRST_BLOCK_UPDATE = '0xf8850a01a07335dc4b60092f2d5aa4b419bf4ea1fe7e76b6a74f7177a26b20733a20d75081c00101f80000f800b858f856f8549435343874344652abe559b226f00abec23a98a7a594cfb89e639a3b69704631cadae3517165fbf06e1e944fe6e85b23709cbf74e98f81d5869e2b46b9721f94ccefbb67c172b02e70b699884b76e39806eefe00';
            this.instance = await BtpMessageVerifier.new();
            await this.instance.initialize(BMC, SRC_NETWORK_ID, 1, FIRST_BLOCK_UPDATE, SEQUENCE_OFFSET);
        });

        describe('when send RELAY_MESSAGE = [BlockUpdate, MessageProof]', () => {
            const RELAY_MESSAGE = '0xf901a0f9019df9018b01b90187f90184b86df86b1401a07335dc4b60092f2d5aa4b419bf4ea1fe7e76b6a74f7177a26b20733a20d75081c00100a0b791b4b069c561ca31093f825f083f6cc3c8e5ad5135625becd2ff77a8ccfa1e01a09c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501f800b90112f9010ff9010cb8414793e4cc621a0d89e3558124b9c5a8d286f9b79a914afa6947260210ac14785366a3b4b4deda123d7b4b0bd30fe4acebc3000eb493001019012a70cb5004184c01b841b7516d10759de1c1b4eabff1b2209220316e566df4b3108162d8467d8dfca52602fb0dabbf5bd2e639ded058df2a39649a156dcfdb2088176ca9fad43f2f110f00b841f04e0d99fe053db692144e422113e92d398c1a07dac7a22f889f04188f778a697a65fcebd77c2d58735efc1c3735450388ddcc4a83d0156855c0772b5a75280301b841873b1feefa67d31894e864094261d0e877241a4b2e2c0bf428c8179ffae84f54004d6436c52d58a816ee095ceae0e18cef280ea0be64a916e88fcc34b5332fc200ce028ccbf800c685616c696365f800';

            it('returns message', async () => {
                let msgs = await this.instance.handleRelayMessage.call('', SRC_BMC_BTP_ADDR, 0, RELAY_MESSAGE);
                expect(msgs.map(v => toStr(v))).to.deep.equal(['alice']);
            });

            describe('after changed state', () => {
                beforeEach(async () => {
                    await this.instance.handleRelayMessage('', SRC_BMC_BTP_ADDR, 0, RELAY_MESSAGE);
                });

                shouldHaveThisState.call(this, {
                    height: new BN(20),
                    messageRoot: '0x9c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501',
                    messageSn: new BN(1),
                    messageCount: new BN(1),
                    remainMessageCount: new BN(0),
                    networkSectionHash: '0x064be11ce64b5e8c007b34bebbf39dd0df0f77afcaf50cf31cc379af17924f4a',
                    validators: VALIDATORS
                });
            });
        });

        describe('when send RELAY_MESSAGES = [[BlockUpdate, MessageProof], [MessageProof]]', () => {
            const RELAY_MESSAGE = '0xf901c1f901bef9018b01b90187f90184b86df86b1401a07335dc4b60092f2d5aa4b419bf4ea1fe7e76b6a74f7177a26b20733a20d75081c00100a0b791b4b069c561ca31093f825f083f6cc3c8e5ad5135625becd2ff77a8ccfa1e02a02bdf15e9913a52d9f36bb7e62634a6079cc32fc2fe975aadfcbc67b7e3a8a61bf800b90112f9010ff9010cb841b23d299670ce1181965ae060700e7fcd7596c319990cdcfa49304afb164556055a18022739dfff287b97faebbf720c5459992b3743f3b6a66ae147aeaee2a26a01b8419938698e6f61ac1fd4a0e3b528e9c578a4f8b6cdae30757a1b9f12da1eccc6f723f15628280117ffd9e5fc15b7e1e87876fee4ee1574fc41cf0cab9cd70cb78f01b841884446814048f31fe57b03d76a50905b55748e197e767d0aa15ac2bbe528327e603886ac006e99d1faf798f462b09641f8c36357afac3d58f51dd5b350519fb901b8415097fd3a067782f1bb6054ac1a1706b5276beb5fdd115cab07ebc3ba160d05cd2f9779a77a46d06ac530c007e4553a236899c529b41134d5be0ad502485d007f01ef02adecc0c685616c696365e3e201a038e47a7b719dce63662aeaf43440326f551b8a7ee198cee35cb5d517f2d296a2';
            it('returns messages: [alice]', async () => {
                let msgs = await this.instance.handleRelayMessage.call('', SRC_BMC_BTP_ADDR, 0, RELAY_MESSAGE);
                expect(msgs.map(v => toStr(v))).to.deep.equal(['alice']);
            });

            describe('after changed state: RELAY_MESSAGES[0]', () => {
                beforeEach(async () => {
                    await this.instance.handleRelayMessage('', SRC_BMC_BTP_ADDR, 0, RELAY_MESSAGE);
                });

                describe('when send RELAY_MESSAGES[1]', () => {
                    const RELAY_MESSAGE = '0xefeeed02abeae3e201a09c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501c483626f62c0'
                    it('returns messages: RELAY_MESSAGES[1]', async () => {
                        let msgs = await this.instance.handleRelayMessage.call('', SRC_BMC_BTP_ADDR, 1, RELAY_MESSAGE);
                        expect(msgs.map(v => toStr(v))).to.deep.equal(['bob']);
                    });

                    describe('after changed state: RELAY_MESSAGES[1]', () => {
                        beforeEach(async () => {
                            await this.instance.handleRelayMessage('', SRC_BMC_BTP_ADDR, 1, RELAY_MESSAGE);
                        });

                        shouldHaveThisState.call(this, {
                            height: new BN(20),
                            messageRoot: '0x2bdf15e9913a52d9f36bb7e62634a6079cc32fc2fe975aadfcbc67b7e3a8a61b',
                            messageSn: new BN(2),
                            messageCount: new BN(2),
                            remainMessageCount: new BN(0),
                            networkSectionHash: '0x4f8b2d17e51d233e0b1a89413a490633b1fee96a17265e7f697190118975daff',
                            validators: VALIDATORS
                        });
                    });
                });
            });
        });

        describe('when send RELAY_MESSAGES = [[BlockUpdate, MessageProof], [MessageProof], [MessageProof]]', () => {
            const RELAY_MESSAGES = [
                '0xf901e8f901e5f9018b01b90187f90184b86df86b1401a07335dc4b60092f2d5aa4b419bf4ea1fe7e76b6a74f7177a26b20733a20d75081c00100a0b791b4b069c561ca31093f825f083f6cc3c8e5ad5135625becd2ff77a8ccfa1e03a04a466308d2644bc8e169b3202117709fb424f2ec2df2e6c8280291744d2cbaa7f800b90112f9010ff9010cb84144c09aec56c9e31b9fc25654f90d5a6159938d8fe08eb5057e6f66fa8a9d30f57f9f207087f7f8d5bba56e5fa9aa745a9d556609fef86a3eb35679061ad679fb01b841aebb2695e701c09d9517fc2823ab11ccee293d320140432ab5b7bcf3c61154aa25b168d3bb21f4057fffa0c283b67dd53a2abfccc2d86036d5dcdc0e5fb7e74e01b841d2d5adfd3f2282be213ee510d54280421df43653112d369b1b352e044932cb7d10f24518947cc6f9a85d9a6a358481ef7668c73d23fb817458845cfe8b104ec300b8416d978688a0e654ef48280cb4013c9b27e12f90aa059d124f3353ac70113f0a430bb4dc59a433d551703edb694252ca77e378efbc926dd0ded55d0ad22162fac201f85502b852f850c0c685616c696365f846e201a038e47a7b719dce63662aeaf43440326f551b8a7ee198cee35cb5d517f2d296a2e201a0a2c791857d936d97cc584df15995fb9e6a3aff25630796d718e2f8ba105b0488',
                '0xf856f854f85202b84ff84de3e201a09c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501c483626f62e3e201a0a2c791857d936d97cc584df15995fb9e6a3aff25630796d718e2f8ba105b0488',
                '0xf3f2f102afeee3e202a02bdf15e9913a52d9f36bb7e62634a6079cc32fc2fe975aadfcbc67b7e3a8a61bc8876372797374616cc0'
            ];

            it('returns messages', async () => {
                let msgs = await this.instance.handleRelayMessage.call('', SRC_BMC_BTP_ADDR, 0, RELAY_MESSAGES[0]);
                expect(msgs.map(v => toStr(v))).to.deep.equal(['alice']);
            });

            describe('after changed state', () => {
                beforeEach(async () => {
                    await this.instance.handleRelayMessage('', SRC_BMC_BTP_ADDR, 0, RELAY_MESSAGES[0]);
                });

                describe('when send RELAY_MESSAGES[1]', () => {
                    it('returns messages', async () => {
                        let msgs = await this.instance.handleRelayMessage.call('', SRC_BMC_BTP_ADDR, 1, RELAY_MESSAGES[1]);
                        expect(msgs.map(v => toStr(v))).to.deep.equal(['bob']);
                    });

                    describe('after changed state', () => {
                        beforeEach(async () => {
                            await this.instance.handleRelayMessage('', SRC_BMC_BTP_ADDR, 1, RELAY_MESSAGES[1]);
                        });

                        describe('when send RELAY_MESSAGE[2]', () => {
                            it('returns messages', async () => {
                                let msgs = await this.instance.handleRelayMessage.call('', SRC_BMC_BTP_ADDR, 2, RELAY_MESSAGES[2]);
                                expect(msgs.map(v => toStr(v))).to.deep.equal(['crystal']);
                            });

                            describe('after changed state', () => {
                                beforeEach(async () => {
                                    await this.instance.handleRelayMessage('', SRC_BMC_BTP_ADDR, 2, RELAY_MESSAGES[2]);
                                });

                                shouldHaveThisState.call(this, {
                                    height: new BN(20),
                                    messageRoot: '0x4a466308d2644bc8e169b3202117709fb424f2ec2df2e6c8280291744d2cbaa7',
                                    messageSn: new BN(3),
                                    messageCount: new BN(3),
                                    remainMessageCount: new BN(0),
                                    networkSectionHash: '0xccb41e470a2b1d74baacdda37355c199e8a1dba368bfaefd4516eb5668e36e60',
                                    validators: VALIDATORS
                                });
                            });
                        });
                    });
                });
            });
        });

        describe('when send RELAY_MESSAGE = [BlockUpdate, MessageProof, MessageProof, MessageProof]', () => {
            const RELAY_MESSAGE = '0xf9026ef9026bf9018b01b90187f90184b86df86b1401a07335dc4b60092f2d5aa4b419bf4ea1fe7e76b6a74f7177a26b20733a20d75081c00100a0b791b4b069c561ca31093f825f083f6cc3c8e5ad5135625becd2ff77a8ccfa1e03a04a466308d2644bc8e169b3202117709fb424f2ec2df2e6c8280291744d2cbaa7f800b90112f9010ff9010cb841365be6bf81ffc8ae8416e485cad76b202d4ceeb74e39a93e2173c4aa89654176578642b2a8e372db3a0729dbd9bd1eb8ce3691d7145e3447d8a70c3fcdc0c67f00b8417c9257412f93e401eea301d5a73b0f90fcf909d5711a04a40831f4b0ff70a7875f288b1d776cbca3d3b2eeab095a6032b1f9454c861aeb2d8279f8b7f77d5dc301b84136aa7b1eabef684b9de4df6d1c388450bb287606261737a7ca92a5773c38489863bfa4c9b509c24940aa4d2d20e22fa968d0927aed634bf7b36463fded02297b01b841a6d98e94ff29cde35bfed374217e03584174c9953f1fe110701aee13c790f63017514aa5e7f72497776595f21d6232dd5acb5763b992b05311bf16f9babcab1f00f85502b852f850c0c685616c696365f846e201a038e47a7b719dce63662aeaf43440326f551b8a7ee198cee35cb5d517f2d296a2e201a0a2c791857d936d97cc584df15995fb9e6a3aff25630796d718e2f8ba105b0488f85202b84ff84de3e201a09c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501c483626f62e3e201a0a2c791857d936d97cc584df15995fb9e6a3aff25630796d718e2f8ba105b0488f102afeee3e202a02bdf15e9913a52d9f36bb7e62634a6079cc32fc2fe975aadfcbc67b7e3a8a61bc8876372797374616cc0';

            it('returns messages', async () => {
                let msgs = await this.instance.handleRelayMessage.call('', SRC_BMC_BTP_ADDR, 0, RELAY_MESSAGE);
                expect(msgs.map(v => toStr(v))).to.deep.equal(['alice', 'bob', 'crystal']);
            });

            describe('after changed state', () => {
                beforeEach(async () => {
                    await this.instance.handleRelayMessage('', SRC_BMC_BTP_ADDR, 0, RELAY_MESSAGE);
                });

                shouldHaveThisState.call(this, {
                    height: new BN(20),
                    messageRoot: '0x4a466308d2644bc8e169b3202117709fb424f2ec2df2e6c8280291744d2cbaa7',
                    messageSn: new BN(3),
                    messageCount: new BN(3),
                    remainMessageCount: new BN(0),
                    networkSectionHash: '0xccb41e470a2b1d74baacdda37355c199e8a1dba368bfaefd4516eb5668e36e60',
                    validators: VALIDATORS
                });
            });
        });

        describe('when send RELAY_MESSAGES = [[BlockUpdate], [MessageProof, MessageProof, MessageProof]]', () => {
            const RELAY_MESSAGE = '0xf90191f9018ef9018b01b90187f90184b86df86b1401a07335dc4b60092f2d5aa4b419bf4ea1fe7e76b6a74f7177a26b20733a20d75081c00100a0b791b4b069c561ca31093f825f083f6cc3c8e5ad5135625becd2ff77a8ccfa1e03a04a466308d2644bc8e169b3202117709fb424f2ec2df2e6c8280291744d2cbaa7f800b90112f9010ff9010cb841af7fac4977966e607c1ab78a4e2bd26bc32fc7cdffefaf0cc79e079decee66ae10311cd30102117d74711d6fc614070fe04f30f3524c2c29935c0cbd9fca319401b8416e97d848d1c9770bb00901ccb0f9b41780a8c431bd9ff87b90069476a6225f1a0dfda69f377de01e36981bd4784d3b95c880c2d72c7da6a43ff2644bafd985a200b84199bf76a3459768a97107e5e663f0beea08e774d1a620fa95438bed6a520b72a047d2799293739d8532f7506f2aa56f4f2cfe5517c221da8e470bf6865986aa9d01b84151ebd23b7daa77ee835b541e783f5fcf92b891b8a592ff44035c03862d1a2c9b368f49a99d02536041c96cae31cabe5e63c62e6bf7ac336f83c84633c0bda62d00';
            it('returns empty message list, [BlockUpdate]', async () => {
                let msgs = await this.instance.handleRelayMessage.call('', SRC_BMC_BTP_ADDR, 0, RELAY_MESSAGE);
                expect(msgs.map(v => toStr(v))).to.be.empty;
            });

            describe('after changed state, [BlockUpdate]', () => {
                beforeEach(async () => {
                    await this.instance.handleRelayMessage('', SRC_BMC_BTP_ADDR, 0, RELAY_MESSAGE);
                });

                describe('when send RELAY_MESSAGE[1]', () => {
                    const RELAY_MESSAGE = '0xf8dff8ddf85502b852f850c0c685616c696365f846e201a038e47a7b719dce63662aeaf43440326f551b8a7ee198cee35cb5d517f2d296a2e201a0a2c791857d936d97cc584df15995fb9e6a3aff25630796d718e2f8ba105b0488f85202b84ff84de3e201a09c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501c483626f62e3e201a0a2c791857d936d97cc584df15995fb9e6a3aff25630796d718e2f8ba105b0488f102afeee3e202a02bdf15e9913a52d9f36bb7e62634a6079cc32fc2fe975aadfcbc67b7e3a8a61bc8876372797374616cc0';
                    it('returns messages', async () => {
                        let msgs = await this.instance.handleRelayMessage.call('', SRC_BMC_BTP_ADDR, 0, RELAY_MESSAGE);
                        expect(msgs.map(v => toStr(v))).to.deep.equal(['alice', 'bob', 'crystal']);
                    });

                    describe('after changed state: RELAY_MESSAGE[1]', () => {
                        beforeEach(async () => {
                            await this.instance.handleRelayMessage('', SRC_BMC_BTP_ADDR, 0, RELAY_MESSAGE);
                        });

                        shouldHaveThisState.call(this, {
                            height: new BN(20),
                            messageRoot: '0x4a466308d2644bc8e169b3202117709fb424f2ec2df2e6c8280291744d2cbaa7',
                            messageSn: new BN(3),
                            messageCount: new BN(3),
                            remainMessageCount: new BN(0),
                            networkSectionHash: '0xccb41e470a2b1d74baacdda37355c199e8a1dba368bfaefd4516eb5668e36e60',
                            validators: VALIDATORS
                        });
                    });
                });
            });
        });

        describe('when send RELAY_MESSAGE for another network', () => {
            const RELAY_MESSAGE = '0xf901a0f9019df9018b01b90187f90184b86df86b1e01a07335dc4b60092f2d5aa4b419bf4ea1fe7e76b6a74f7177a26b20733a20d75081c00200a0273e85733c50a59785a41c066ee49e642480c73882d9ca9e67b19dd69c79417801a09c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501f800b90112f9010ff9010cb8413d9785ac576e0587d4c1e6a7f4581eb876f8f01a759c15ee1deff20ebe12927f36a3a5725c9b9fc5914f18bf1ebd42efeee46f13e10758916f3b75540908641c00b84164c794f1121e2d40a558523a8001d6a58461f84bcd119f3145d5c518c134b42b3a4b84165a38863b8b555f3c977793f67a64e8cf76ac1ef92503cfdc3e870f2b00b841511f7b80e962d1e8c0bbf3d23516fa08f2772ba73771136498f1fa9a842bfcae4bf34397004b609650c3406e3f7d26b40bf0a6f73ebf7d31479f6aeda456ab0b01b841ae82418a2a900973be91e44d8493303a0bda790f937b127e43c953484041eedb25cad687df3bfc43347d7e6eac6f5849786421eeb5fc0d9d2bb375abe1126af601ce028ccbf800c685616c696365f800';

            it('revert', async () => {
                await expectRevert(this.instance.handleRelayMessage.call(
                    '', SRC_BMC_BTP_ADDR, 0, RELAY_MESSAGE), Errors.ERR_UNKNOWN,
                );
            });
        });

        describe('when send RELAY_MESSAGE with invalid signatures', () => {
            describe('has enough the quorum', () => {
                const RELAY_MESSAGE = '0xf901f8f901f5f901e301b901dff901dcb8c5f8c31401a0ec7919d99cfa1c0750f9aeeaec2ddd7b6b23b682f30022156a9e8615f66c88e4c00101a0b791b4b069c561ca31093f825f083f6cc3c8e5ad5135625becd2ff77a8ccfa1e01a09c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501b858f856f8549435343874344652abe559b226f00abec23a98a7a594cfb89e639a3b69704631cadae3517165fbf06e1e944fe6e85b23709cbf74e98f81d5869e2b46b9721f9482878eadbf424a7ea546ae23e20e1ce0d12fb347b90112f9010ff9010cb841471ed82abea405da27de0c377e2df70ed47b8fa62e6cb85d3b6fa69eca98053e5bb2a338838f413670c228c7988dd99f702633cbc5263995cc2ca377432fb7fc00b8418ceadbbb69f2338f9ce20a97a87539c8c47afdb29176a957aa8e77ef68dd385f252bc66b4166d79dd978c607b6f326a8ce9b9fa24295a93a552de8716432eb3100b841bd330b366d2052994685800744703180765d47eb0a1d3ecc7d4cae366e823c5f3e194397b527606f641d6a781c4bdc40c83c3a2cf8d5ab1ccd3d567cbc476f9c00b841ec891a27ea8b67e95b1e63e99b67eb8cf5f52e941718a16d878435c957f29a3f3040516e91792722ebf2f9d07a4582a9ca0b5f6a53c61fccbccb17dc55ccb12e01ce028ccbf800c685616c696365f800';

                it('returns messages', async () => {
                    let msgs = await this.instance.handleRelayMessage.call('', SRC_BMC_BTP_ADDR, 0, RELAY_MESSAGE);
                    expect(msgs.map(v => toStr(v))).to.deep.equal(['alice']);
                });
            });

            describe('has lack of quorum', () => {
                const RELAY_MESSAGE = '0xf901f8f901f5f901e301b901dff901dcb8c5f8c31401a06f83a1850e8b5f7965c707b466e912a885722a7520c281a07cfe9e51f50f15d8c00101a0b791b4b069c561ca31093f825f083f6cc3c8e5ad5135625becd2ff77a8ccfa1e01a09c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501b858f856f8549435343874344652abe559b226f00abec23a98a7a594cfb89e639a3b69704631cadae3517165fbf06e1e948ba4ab599a7e56cf5be3869083a72ec93b6b61029482878eadbf424a7ea546ae23e20e1ce0d12fb347b90112f9010ff9010cb84105ab183c57b4bb3d91638c7415410772feb52eb8953d9e8b55ecf4d68a5f33bd1954ba570cb3db034b75435d0bc48bbd43d5a0a51b953c83526bef52db25489f01b8416dedea340f2f5d6cf097d82de0d75f89174b7155ffe860e55877cda86d64e03f69446be9c9a88f069957d976e034089448b2aab993c8aaf73988c78723a538e201b8417d4ba124dc49eb58f6f2cc9dece0f205eb337d27f91dcb795f21aec9c3aa78ec03b3e0e3a0e27233f9c56a550f18f741e575fd9bbc990d843e87d52b392470d700b841b53fb696b1328ae51f0f4746bd4721325dd7c90b288f5bc4f5baa31d43cccfaa6474f0686f6a2de047a99a156515c766ffff99114f74f7dce788ce5d4d68107000ce028ccbf800c685616c696365f800';

                it('reverts', async () => {
                    await expectRevert(
                        this.instance.handleRelayMessage.call('', SRC_BMC_BTP_ADDR, 0, RELAY_MESSAGE),
                        Errors.ERR_UNKNOWN
                    );
                });
            });
        });

        describe('when send RELAY_MESSAGE with changing validators', () => {
            const RELAY_MESSAGE = '0xf901caf901c7f901c401b901c0f901bdb8a6f8a41401a04dde9282f6974d1bd10b33438eb7f744ea9d67e889fd0bf3e2b4daf3df433e07c00101a0b791b4b069c561ca31093f825f083f6cc3c8e5ad5135625becd2ff77a8ccfa1e00f800b858f856f854940a9e59d8bede1d139b40b275d0c7fcab6820d6a9940b10209d62329898f219b898f461a4df659bf545948d379c4e61b7b1d97210fbb90c4288d37a3a810994a9c64b9a1326aa0f0258a721859422efe35b0365b90112f9010ff9010cb84163aa67676dd8dde45d1c68d1eeac735e995b59784c5343997dc9177584ef835e78822987b1153ab66edd9ec41d909498f6ba403ee118904ce97cf4aa0307dceb00b841d8afea151e53d1784e0e06eab06121ad9040774ede50b79c776cd55f9022d3b361be4075fbe8e5c2f6540f4a3c9ad3bd27ca12f5cc33656cdd4f6fc31214acd400b841a486a972f3bce39809f89b7ea9c313a1f3bcfba806ab1bcb1a301352cc57215138a80f1636b696b5bbf2d2c5690654398b7bf7a446c636b8688b85de34534dc201b841ccdc9dd69c5c2005a785186dac0487835fae3a1693e457931afc63ae8182370f3081bf4701bd832c98b3ef3d85c5bc58876f79cd05413b9b939cf6ea0ceed89500';

            describe('after changed state', () => {
                beforeEach(async () => {
                    await this.instance.handleRelayMessage('', SRC_BMC_BTP_ADDR, 0, RELAY_MESSAGE);
                });

                shouldHaveThisState.call(this, {
                    height: new BN(20),
                    messageRoot:ZB32,
                    messageSn: new BN(0),
                    messageCount: new BN(0),
                    remainMessageCount: new BN(0),
                    networkSectionHash: '0x39ea97ede1d7b3019f4f1b4ce6b463d4189dc86f620e5ced3611fded5ae41995',
                    validators: [
                        '0x0a9e59d8bede1d139b40b275d0c7fcab6820d6a9',
                        '0x0b10209d62329898f219b898f461a4df659bf545',
                        '0x8d379c4e61b7b1d97210fbb90c4288d37a3a8109',
                        '0xa9c64b9a1326aa0f0258a721859422efe35b0365'

                    ]
                });

                describe('send RELAY_MESSAGE signed by changed validators', () => {
                    const RELAY_MESSAGE = '0xf901a0f9019df9018b01b90187f90184b86df86b1e01a04dde9282f6974d1bd10b33438eb7f744ea9d67e889fd0bf3e2b4daf3df433e07c00100a039ea97ede1d7b3019f4f1b4ce6b463d4189dc86f620e5ced3611fded5ae4199501a09c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501f800b90112f9010ff9010cb841f3c316bbb2d40b2f73bfb17ef41108aa1dc9b82061fa0052ead3d336d55001406cef71e2e116eec7853485b603a0632f59ab8c2e2997a8ff6e71793874b360f300b841957629a681a5691d5482fd63fca39e2fff9d2fa970b9d71de321b3f683b41a31272a12f9fafb18ef0bd4e6c04802b75f8ec4093ba3ff5add98e26713d11ec58f01b841bf5faa1fd57bf67cc425d5d3ed8eb8b775bd023015d418db3783ecb464dbbc8a59bd168dd3491b028b802ac27343b5d26f36459bafa739474807e490c81ac16700b841b4ad75fbdc8ad125c5baf5fc55533be35bc0b6ce4b018c86fcc80f6304481abd066b927ec7470d6b149c6e6a511cf3fd4fa6b3a765bec57a585795ac8cafe41d01ce028ccbf800c685616c696365f800';

                    it('returns messages', async () => {
                        let msgs = await this.instance.handleRelayMessage.call('', SRC_BMC_BTP_ADDR, 0, RELAY_MESSAGE);
                        expect(msgs.map(v => toStr(v))).to.deep.equal(['alice']);
                    });
                });
            });
        });
    });

    describe('when has installed with block update which has generated with message', () => {
        const SRC_NETWORK_ID = '0x1.icon'
        const SRC_BMC_BTP_ADDR = 'btp://' + SRC_NETWORK_ID + '/' + 'srcBmcCA';
        const NETWORK_TYPE_ID = new BN(1);
        const FIRST_BLOCK_UPDATE = '0xf8a40a00a0177b09e6b788d2a5f9d7572265f8a54176a83783e6d1d99d1865bc24e04fb493c00101f80001a09c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501b858f856f85494524122f6386c9cd3342ecc377dbc9dcb036dd2e0948103611a29623db06a937b24f52d70cf44c1c41494910d15f35a3e685968d8596512efa56d840bb3c59451226eee21a3d3758727886df161c108f5857f3f';
        const VALIDATORS = [
            '0x524122f6386c9cd3342ecc377dbc9dcb036dd2e0',
            '0x8103611a29623db06a937b24f52d70cf44c1c414',
            '0x910d15f35a3e685968d8596512efa56d840bb3c5',
            '0x51226eee21a3d3758727886df161c108f5857f3f'
        ];

        beforeEach(async () => {
            this.instance = await BtpMessageVerifier.new();
            await this.instance.initialize(BMC, SRC_NETWORK_ID, NETWORK_TYPE_ID, FIRST_BLOCK_UPDATE, SEQUENCE_OFFSET);
        });

        describe('when miss RELAY_MESSAGE = [MessageProof], and send RELAY_MESSAGE = [BlockUpdate]', () => {
            const INVALID_RELAY_MESSAGE = '0xf90192f9018ff9018c01b90188f90185f86b1400a0177b09e6b788d2a5f9d7572265f8a54176a83783e6d1d99d1865bc24e04fb493c00102a07239515338d40afd6e908375975f969a503d75b40821bfa8412736980ff2f2b901a09c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501f800f90115b90112f9010ff9010cb841f7dfe7d5ca9aede7e77c317f7d32d500244e16c4587f22bccf686c9f014f720e573e2358254c6ee7386b22997014488803d9ec5ce2bb7470893eb55d61eb0e9801b841a908985d0bf055dcf0c9ff4c3b8ab2868e26b3ae9025437b539fc26308debf3c422991acbbf78b1fe2df3363c8a1c6d9307b5140df515103f7c20a3fe740fddb00b84170d4b3ada1ecca9d8aa333d5648daeec41aa102c75601aab04f907198a751abc3487874e30f2400bae0506a2601cc297474b66cc336a3d7e80c6500e4afe5d9d00b84160bdb50163ceb69f42fb236d8535d37bb3786b76c9c0ff7ea0ecd5716a50a69f6759fb193614cb92c1942e0c8ea7863d6c77c7e69a32340fa7760407f96f292600';

            it('revert', async () => {
                await expectRevert(
                    this.instance.handleRelayMessage.call('', SRC_BMC_BTP_ADDR, 0, INVALID_RELAY_MESSAGE),
                    Errors.ERR_UNKNOWN
                );
            });
        });

        describe('when invalid source network sends RELAY_MESSAGE = [MessageProof]', () => {
            const RELAY_MESSAGE = '0xd0cfce028ccbf800c685616c696365f800';

            it('revert', async () => {
                await expectRevert(this.instance.handleRelayMessage.call('', 'btp://0x2.eth/invalidSmcAddress', 0, RELAY_MESSAGE),
                    Errors.ERR_UNAUTHORIZED
                );
            });
        });

        describe('when invalid sender send RELAY_MESSAGE = [ MessageProof]', () => {
            const RELAY_MESSAGE = '0xd0cfce028ccbf800c685616c696365f800';
            it('revert', async () => {
                await expectRevert(this.instance.handleRelayMessage.call(
                    '', SRC_BMC_BTP_ADDR, 0, RELAY_MESSAGE, { from: accounts[1] }),
                    "bmv: Unauthorized"
                );
            });
        });

        describe('when send RELAY_MESSAGE = [MessageProof]', () => {
            const RELAY_MESSAGE = '0xd0cfce028ccbf800c685616c696365f800';

            it('returns messages', async () => {
                let msgs = await this.instance.handleRelayMessage.call('', SRC_BMC_BTP_ADDR, 0, RELAY_MESSAGE);
                expect(msgs.map(v => toStr(v))).to.deep.equal(['alice']);
            });

            describe('after changed state', () => {
                beforeEach(async () => {
                    await this.instance.handleRelayMessage('', SRC_BMC_BTP_ADDR, 0, RELAY_MESSAGE);
                });

                shouldHaveThisState.call(this, {
                    height: new BN(10),
                    messageRoot:'0x9c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501',
                    messageSn: new BN(1),
                    messageCount: new BN(1),
                    remainMessageCount: new BN(0),
                    networkSectionHash: '0x7239515338d40afd6e908375975f969a503d75b40821bfa8412736980ff2f2b9',
                    validators: VALIDATORS
                });
            });
        });
    });
});

function shouldHaveImmutableState(props) {
    it('has source network id', async () => {
        expect(await this.instance.getSrcNetworkId())
            .to.equal(props.srcNetworkId);
    });

    it('has network type id', async () => {
        expect(await this.instance.getNetworkTypeId())
            .to.be.bignumber.equal(props.networkTypeId);
    });
}

function shouldHaveThisState(props) {
    it('has block height', async () => {
        expect(await this.instance.getHeight())
            .to.be.bignumber.equal(props.height);
    });

    it('has network section hash', async () => {
        expect(await this.instance.getNetworkSectionHash())
            .to.equal(props.networkSectionHash);
    });

    it('has message root', async () => {
        expect(await this.instance.getMessageRoot())
            .to.equal(props.messageRoot);
    });

    it('has message count', async () => {
        expect(await this.instance.getMessageCount())
            .to.be.bignumber.equal(props.messageCount);
    });

    it('has remain message count', async () => {
        expect(await this.instance.getRemainMessageCount())
            .to.be.bignumber.equal(props.remainMessageCount);
    });


    it('has next message sequence number', async () => {
        expect(await this.instance.getNextMessageSn())
            .to.be.bignumber.equal(props.messageSn);
    });

    it('has validators', async () => {
        expect(await this.instance.getValidatorsCount())
            .to.be.bignumber.equal(new BN(props.validators.length));

        for (nth in props.validators) {
            expect((await this.instance.getValidators(nth)).toLowerCase())
                .to.equal(props.validators[nth]);
        }
    });
}

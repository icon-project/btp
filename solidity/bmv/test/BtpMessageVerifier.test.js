const { BN, constants, expectEvent, expectRevert } = require('@openzeppelin/test-helpers');
const { expect } = require('chai');
const BtpMessageVerifier = artifacts.require('BtpMessageVerifier');
const { ZB32, toBytesString, toStr } = require('./utils');

const SRC_NETWORK_ID = 'btp://0x1.icon'
const NTID = new BN('2');
const NID = new BN('2');
const SEQUENCE_OFFSET = new BN('0');

contract('BtpMessageVerifier', (accounts) => {
    const BMC = accounts[0];

    describe('common test scenario', () => {
        const RELAY_MESSAGES = [
            '0xcecdcc028ac9f800c483646f67f800',
            '0xf90228f90225f9020701b90203f90200f8e61400a0d643eeba45acdab7b4fd65ecdb8622e67243cb264251917f845ba014c57c15cfe3e201a0a4997d283af68023f69666832df08cafb4b91789b10438f13b48bdfbaa03e4ac0203a0b01a7e90a687b64b58e2410a31e1b2e8e131672563c6c52db84eeadd15b6956403a04eaeed1d1e8444f108a0f79abbc5150dd768bbda89279c2e4a301fe8c4e5dd26b858f856f85494911ac74dd9ff8f4cdd91e747afcfdc9410a926e99497e36fb88560a3023c509704801eb1149acecf4394a9b0a74b2b63ab9cd20c6e38c88195c8175beb4694432b6448f3471aef190819b3c4f549a3a689d83af90115b90112f9010ff9010cb8411c8e1c4e89ea6f2a22c06d49984d8088f40b5d8bc1ff547d790df8374454f9ff73f4324168cb68a1c78d74ee05a2cccb1a471c26c3dcd05af4c21241d31a8fd301b8411f915ae7f047db8fee3fec42882a1747aa898c5149e8751e479235f33779fd414f52c3a63d0dd24953f3e8ed77eb06965410c2c365f669abc74d0239dbc7052a01b841d21638f8aee5194920df53652adc906f66944044e1d9176e45b9d3d80d010ded7967ff4dd944b6f9214794f0c967529663eeda3d3b51c2b7bcd56ff8f6a41d3800b8416be3ab56807f762f262a9feb819aa1aac1e83330e1568764bb9e766519135fb64f72adef323187c700e6b30b3a44e766ecb8885f1290074c43f7bd3bb06f56dc01da0298d7f800d28363617488656c657068616e748462697264f800',
            '0xf901b5f901b2f901af01b901abf901a8f88e1e00a0d643eeba45acdab7b4fd65ecdb8622e67243cb264251917f845ba014c57c15cfe3e201a0dd7d7cd1229649a054dbfd03969abe0ce4e439afc09f36e07165b95f866f565c0208a0335fc784176e18f4d3efefc662503f0fd8fe120751ed4251a66aaa938640015203a086c05cf325d1a9fc932360037a87b8871c838f274eab4d8010ae9c81b3de24a9f800f90115b90112f9010ff9010cb841128dd79e62e79f27067e51e192322a76d889e2b6e2a28897c6ab2649a0352e9a7991e5891e841a2c82d80817ebe763dd797f0d1c09c535d2ac6aa427edabf16500b8418e7f3249ec892000e44ee25576bbf63d00258adf9cb74033f6b04a8c35fb47ec202f27dadc243d7ba6768231152aa3de0db21c779a243e6abcdd037fd1723e0b00b841a7a888d0cc7acc6a0d69e52fd955ae350ec3ec0fc0b9587b3f342cf5dc63889d7a5496eeb7e81f0a94ea43b1b5f5aa198fa6ea52f60bef6bc195e8a1f516bdbb01b84116d124bdb4b67e63dad5b911f26c338d9501221778d16058cf787381b234561a4987039f35ba1435db164c34dff4b9a7b01a49ab3319132c9ce547c7878f05e700',
            '0xf868f866f502b3f2c0cc866d6f6e6b6579846c696f6ee3e201a038f4921087750bcc254ecc791170ebb01a0297d49c630a8844c18f6104a5da07ef02adece3e202a09dc633d90e96b8744b27aaa5bb6bb2cca28f187c196cf7af1c82d8d1e8cd5f6fc6857469676572c0',
            '0xf903d9f903d6f901e801b901e4f901e1f8c72800a0d24c4f3d98a310f94047f0ea18b553f9f570195605189e4cb7b33ca88a29008ae3e201a08c46a84b0bad8d2ee375d1fb666256ef0abb7bfcf208b0e6e9a83718e2dda4c8020fa049da67cde1ed94d33df761d65abe0f8b17bedd41a133df50de63aeb80aa9a7e700f800b858f856f85494dccf35820a337512baa66780f112fef74593375d940536e6d35901f056ed48f19c8d2b8e76c6292399942182851a75a5b1657d7435e3178975ba97372083940f951e1fa3f96a2a3b92f05f382c6a87126945cbf90115b90112f9010ff9010cb841e841970cd4ea08870f949bfaef59e6efab1a7132a2f7156e9ed21427eb7631eb6ed1e46b1befab6f080399f0d2bef6492081d8aa3e716de819cc8a7e99c25d9400b8414a0688b6ebf8ac47b423267fa86cff8bdef6095934fa151dbf07b7a861fd5e4400557247b0278df25b4c865b3c8a067f9b40693ef75f31e680030a2eaf25cd0a01b841b1158d5d63fe8f080ff55d136e3eb07b7adf3f1fac409d541599a1eb1111c4011b62649875f724509de431ff61cd80e700cc5cfa317b9e33df67e65aa6d5cecf00b84198fc2f6d8695775685b80c68562ffb2e487389fc5b839d5c95c365426b58bf812c6f1eb33baa775291ad284e02ddb11dd5e9476fdf2ba1f6bdd4ea722ef6dcb200f901e801b901e4f901e1f8c73200a0f15b61d91392ddcb9fb60beb138a8a1fff821be0371d3c20f9caa063b9df8e3ae3e201a0d1cdfff9f24d50ba88a34f19cbdf5c214169df63629e299d9baffb242a1bb428020fa0efe5785c35214f54e0ee595ebbe96842cd158d9329e3f3f6ed7e30f8cfa835f900f800b858f856f85494f28a5305dbe556fd0103596aca9129d730a916b89442aa62a65b7d03ed68a06b95ab3445d6af294be99433a9779f75b36f372a4a6600579b9a9959e4568794c9ae50b4423509c64dd74aee98ee2aa0e0fed323f90115b90112f9010ff9010cb841e6d82ebc01e10cce3d6867c6caa2337f8eb487efec4fb00f81aa1c79158f218455945df9705d5fbccd0c14990d3d36db83085b13c81a0d5d63c1710bc51f965e01b84119cfcaa2ae283d6eefc52e9fec010ef53a8e4b4aa853a9ed6a026f58c930a937745cf1b21202c50865cc85a2bc505a307df7f6e3a107993d35eed249377d1d2a01b841c807ddaf5cef5ddf7688c32f6eed930fbf9f79893a0bcc355472e4f904103c0e0c6643c6e860249a068f8754d75a8f90d8dccd20a0cbb0899882d2bd3edd03b200b841ed78341a0f2ad318a3fff4f2e118c25e7a70cd6579de184a3c2817120ef32cc228f610348252c047b6192f86fd79fba9a39afe4e51ad9d205867918c6a45feca00'
        ];

        describe('when bmv has been initialized', () => {
            const SRC_NETWORK_ID = 'btp://0x1.icon';
            const NETWORK_TYPE_ID = new BN(2);
            const NETWORK_ID = new BN(2);
            const FIRST_BLOCK_UPDATE = '0xf8a40a00a08d647190ed786d4f8a522f32351ff2269732f6e3771b908271ca29aa73c2c03cc00201f80001a041791102999c339c844880b23950704cc43aa840f3739e365323cda4dfa89e7ab858f856f85494e3aee81071e40c44dde872e568d8d09429c5970e94b3d988f28443446c9d7ac62f673085e2736ca4e094f7646979981a5d7dcc05ea261ae26ecd2c2c81889477fec3da1ba8b192f4b278057395a5124392c88b';
            const VALIDATORS = [
                '0xe3aee81071e40c44dde872e568d8d09429c5970e',
                '0xb3d988f28443446c9d7ac62f673085e2736ca4e0',
                '0xf7646979981a5d7dcc05ea261ae26ecd2c2c8188',
                '0x77fec3da1ba8b192f4b278057395a5124392c88b'
            ];

            beforeEach(async () => {
                this.instance = await BtpMessageVerifier.new();
                await this.instance.initialize(BMC, toBytesString(SRC_NETWORK_ID), NETWORK_TYPE_ID, NETWORK_ID, FIRST_BLOCK_UPDATE, SEQUENCE_OFFSET);
            });

            shouldHaveImmutableState.call(this, {
                srcNetworkId: toBytesString(SRC_NETWORK_ID),
                networkTypeId: NETWORK_TYPE_ID,
                networkId: NETWORK_ID
            });

            shouldHaveThisState.call(this, {
                height: new BN(10),
                messageRoot: '0x41791102999c339c844880b23950704cc43aa840f3739e365323cda4dfa89e7a',
                messageSn: new BN(0),
                messageCount: new BN(1),
                remainMessageCount: new BN(1),
                networkSectionHash: '0xb01a7e90a687b64b58e2410a31e1b2e8e131672563c6c52db84eeadd15b69564',
                validators: VALIDATORS
            });

            describe('sends RelayMessage=[MessageProof]', () => {
                it('returns messages', async () => {
                    let msgs = await this.instance.handleRelayMessage.call('', SRC_NETWORK_ID, 0, RELAY_MESSAGES[0]);
                    expect(msgs.map(v => toStr(v))).to.deep.equal(['dog']);
                });

                describe('when state has been updated, height(10)', () => {
                    beforeEach(async () => {
                        await this.instance.handleRelayMessage('', SRC_NETWORK_ID, 0, RELAY_MESSAGES[0]);
                    });

                    shouldHaveThisState.call(this, {
                        height: new BN(10),
                        messageRoot: '0x41791102999c339c844880b23950704cc43aa840f3739e365323cda4dfa89e7a',
                        messageSn: new BN(1),
                        messageCount: new BN(1),
                        remainMessageCount: new BN(0),
                        networkSectionHash: '0xb01a7e90a687b64b58e2410a31e1b2e8e131672563c6c52db84eeadd15b69564',
                        validators: VALIDATORS
                    });

                    describe('sends RelayMessage=[BlockUpdate, MessageProof]', () => {
                        it('returns messages', async () => {
                            let msgs = await this.instance.handleRelayMessage.call('', SRC_NETWORK_ID, 1, RELAY_MESSAGES[1]);
                            expect(msgs.map(v => toStr(v))).to.deep.equal(['cat', 'elephant', 'bird']);
                        });

                        describe('when state has been updated, height(20)', () => {
                            beforeEach(async () => {
                                await this.instance.handleRelayMessage('', SRC_NETWORK_ID, 1, RELAY_MESSAGES[1]);
                            });

                            shouldHaveThisState.call(this, {
                                height: new BN(20),
                                messageRoot: '0x4eaeed1d1e8444f108a0f79abbc5150dd768bbda89279c2e4a301fe8c4e5dd26',
                                messageSn: new BN(4),
                                messageCount: new BN(3),
                                remainMessageCount: new BN(0),
                                networkSectionHash: '0x335fc784176e18f4d3efefc662503f0fd8fe120751ed4251a66aaa9386400152',
                                validators: [
                                    '0x911ac74dd9ff8f4cdd91e747afcfdc9410a926e9',
                                    '0x97e36fb88560a3023c509704801eb1149acecf43',
                                    '0xa9b0a74b2b63ab9cd20c6e38c88195c8175beb46',
                                    '0x432b6448f3471aef190819b3c4f549a3a689d83a'
                                ]
                            });

                            describe('sends RelayMessage=[BlockUpdate(changing validators)]', () => {
                                it('returns empty list', async () => {
                                    let msgs = await this.instance.handleRelayMessage.call('', SRC_NETWORK_ID, 4, RELAY_MESSAGES[2]);
                                    expect(msgs.map(v => toStr(v))).to.be.empty;
                                });

                                describe('when state has been updated, height(30)', () => {
                                    beforeEach(async () => {
                                        await this.instance.handleRelayMessage('', SRC_NETWORK_ID, 4, RELAY_MESSAGES[2]);
                                    });

                                    let validators = [
                                        '0x911ac74dd9ff8f4cdd91e747afcfdc9410a926e9',
                                        '0x97e36fb88560a3023c509704801eb1149acecf43',
                                        '0xa9b0a74b2b63ab9cd20c6e38c88195c8175beb46',
                                        '0x432b6448f3471aef190819b3c4f549a3a689d83a'
                                    ];

                                    shouldHaveThisState.call(this, {
                                        height: new BN(30),
                                        messageRoot: '0x86c05cf325d1a9fc932360037a87b8871c838f274eab4d8010ae9c81b3de24a9',
                                        messageSn: new BN(4),
                                        messageCount: new BN(3),
                                        remainMessageCount: new BN(3),
                                        networkSectionHash: '0x49da67cde1ed94d33df761d65abe0f8b17bedd41a133df50de63aeb80aa9a7e7',
                                        validators
                                    });

                                    describe('sends RelayMessage=[MessageProof, MessageProof]', () => {
                                        it('returns messages', async () => {
                                            let msgs = await this.instance.handleRelayMessage.call('', SRC_NETWORK_ID, 4, RELAY_MESSAGES[3]);
                                            expect(msgs.map(v => toStr(v))).to.deep.equal(['monkey', 'lion', 'tiger']);
                                        });

                                        describe('when state has been updated, height(40)', () => {
                                            beforeEach(async () => {
                                                await this.instance.handleRelayMessage('', SRC_NETWORK_ID, 4, RELAY_MESSAGES[3]);
                                            });

                                            shouldHaveThisState.call(this, {
                                                height: new BN(30),
                                                messageRoot: '0x86c05cf325d1a9fc932360037a87b8871c838f274eab4d8010ae9c81b3de24a9',
                                                messageSn: new BN(7),
                                                messageCount: new BN(3),
                                                remainMessageCount: new BN(0),
                                                networkSectionHash: '0x49da67cde1ed94d33df761d65abe0f8b17bedd41a133df50de63aeb80aa9a7e7',
                                                validators
                                            });
                                            describe('sends RelayMessage=[BlockUpdate(changing validators), BlockUpdate(chainging validators)]', () => {
                                                it('returns empty list', async () => {
                                                    let msgs = await this.instance.handleRelayMessage.call('', SRC_NETWORK_ID, 7, RELAY_MESSAGES[4]);
                                                    expect(msgs.map(v => toStr(v))).to.be.empty;
                                                });

                                                describe('when state has been updated, height(50)', () => {
                                                    beforeEach(async () => {
                                                        await this.instance.handleRelayMessage('', SRC_NETWORK_ID, 7, RELAY_MESSAGES[4]);
                                                    });

                                                    shouldHaveThisState.call(this, {
                                                        height: new BN(50),
                                                        messageRoot: '0x86c05cf325d1a9fc932360037a87b8871c838f274eab4d8010ae9c81b3de24a9',
                                                        messageSn: new BN(7),
                                                        messageCount: new BN(3),
                                                        remainMessageCount: new BN(0),
                                                        networkSectionHash: '0xd33456b7b455a6381b4e523716b33f9593d3dfe00c7219a3656c983ed99ab8a9',
                                                        validators: [
                                                            '0xf28a5305dbe556fd0103596aca9129d730a916b8',
                                                            '0x42aa62a65b7d03ed68a06b95ab3445d6af294be9',
                                                            '0x33a9779f75b36f372a4a6600579b9a9959e45687',
                                                            '0xc9ae50b4423509c64dd74aee98ee2aa0e0fed323'
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
        const NETWORK_TYPE_ID = new BN(1);
        const NETWORK_ID = new BN(1);
        const FIRST_BLOCK_UPDATE = '0xf8850a01a0177b09e6b788d2a5f9d7572265f8a54176a83783e6d1d99d1865bc24e04fb493c00101f80000f800b858f856f85494524122f6386c9cd3342ecc377dbc9dcb036dd2e0948103611a29623db06a937b24f52d70cf44c1c41494910d15f35a3e685968d8596512efa56d840bb3c59451226eee21a3d3758727886df161c108f5857f3f';
        const validators = [
            '0x524122f6386c9cd3342ecc377dbc9dcb036dd2e0',
            '0x8103611a29623db06a937b24f52d70cf44c1c414',
            '0x910d15f35a3e685968d8596512efa56d840bb3c5',
            '0x51226eee21a3d3758727886df161c108f5857f3f'
        ];

        beforeEach(async () => {
            this.instance = await BtpMessageVerifier.new();
            await this.instance.initialize(BMC, toBytesString(SRC_NETWORK_ID), NETWORK_TYPE_ID, NETWORK_ID, FIRST_BLOCK_UPDATE, SEQUENCE_OFFSET);
        });

        shouldHaveImmutableState.call(this, {
            srcNetworkId: toBytesString(SRC_NETWORK_ID),
            networkTypeId: NETWORK_TYPE_ID,
            networkId: NETWORK_ID
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

        describe('when send RELAY_MESSAGE = [BlockUpdate, MessageProof]', () => {
            const RELAY_MESSAGE = '0xf901a1f9019ef9018c01b90188f90185f86b1401a0177b09e6b788d2a5f9d7572265f8a54176a83783e6d1d99d1865bc24e04fb493c00100a0b791b4b069c561ca31093f825f083f6cc3c8e5ad5135625becd2ff77a8ccfa1e01a09c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501f800f90115b90112f9010ff9010cb841081455a1d67834537febe7796c17f74366bc0566f29d505abe98a5e297d549cc5aa96c24f7bb92c3298974abf3c5610353f96f2d10b8d9234d8c6081d1de256601b841155f92300d2d788f25bb04ee9a8faa2b93265dc2b1bf45ef4e7c9e166c379243339066c6e13b706d9d631d6f3fb726164be9e65345803d2cd6802abed37374e000b8411670b589335b85d2e3d2a77e6e9210b09277d47ff7b2185148c30183454a530d66271a147a14a5b8c24896422ff57d6da8e242edbd4c9e110b018ff07df00c7300b841474a1f2b4889c8f84c5211648b9099348c3488b2932e020ddece72bc6490d3ae7d96f61766a7be599f414c31e0780382ae2b3ad3e2aa14304faf22b028abfd1800ce028ccbf800c685616c696365f800';

            it('returns message', async () => {
                let msgs = await this.instance.handleRelayMessage.call('', SRC_NETWORK_ID, 0, RELAY_MESSAGE);
                expect(msgs.map(v => toStr(v))).to.deep.equal(['alice']);
            });

            describe('after changed state', () => {
                beforeEach(async () => {
                    await this.instance.handleRelayMessage('', SRC_NETWORK_ID, 0, RELAY_MESSAGE);
                });

                shouldHaveThisState.call(this, {
                    height: new BN(20),
                    messageRoot: '0x9c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501',
                    messageSn: new BN(1),
                    messageCount: new BN(1),
                    remainMessageCount: new BN(0),
                    networkSectionHash: '0x064be11ce64b5e8c007b34bebbf39dd0df0f77afcaf50cf31cc379af17924f4a',
                    validators
                });
            });
        });

        describe('when send RELAY_MESSAGES = [[BlockUpdate, MessageProof], [MessageProof]]', () => {
            const RELAY_MESSAGES = [
                '0xf901c2f901bff9018c01b90188f90185f86b1401a0177b09e6b788d2a5f9d7572265f8a54176a83783e6d1d99d1865bc24e04fb493c00100a0b791b4b069c561ca31093f825f083f6cc3c8e5ad5135625becd2ff77a8ccfa1e02a02bdf15e9913a52d9f36bb7e62634a6079cc32fc2fe975aadfcbc67b7e3a8a61bf800f90115b90112f9010ff9010cb841e9a77b99ecb2a7dc4a323ed058df52e657ea3daf7ccf37000e3cd58ecc94bb7e415218711ffaa015b942d44b4a21bcf305f2b57164fe38d25652bca249a3cb8801b841d1db81140886f8c7892e67756e08133efbc35a6caf5f2ea9f20ae0a94e571a126b1dd47879f768453164d85b21eb8844dceb46636a478f33522420b5e47273d100b841435fe512230a4f2d7c8ec33856bf1f43b1f284c41dbd5cb7a8cb0f55a6ec78ab5d38c49f034523be89968ec2398de923ad4ca20de7e510dd6bcd219f14c1199701b841cd642ff89fcb4790239b4a6e402168e7401bd75768c11d707f4b3ad1bfc38a4540d530beb91a4127b5282d4f5b36f1ace733db37903fa2280b30ec3989de8d6500ef02adecc0c685616c696365e3e201a038e47a7b719dce63662aeaf43440326f551b8a7ee198cee35cb5d517f2d296a2',
                '0xefeeed02abeae3e201a09c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501c483626f62c0'
            ];

            it('returns messages: RELAY_MESSAGES[0]', async () => {
                let msgs = await this.instance.handleRelayMessage.call('', SRC_NETWORK_ID, 0, RELAY_MESSAGES[0]);
                expect(msgs.map(v => toStr(v))).to.deep.equal(['alice']);
            });

            describe('after changed state: RELAY_MESSAGES[0]', () => {
                beforeEach(async () => {
                    await this.instance.handleRelayMessage('', SRC_NETWORK_ID, 0, RELAY_MESSAGES[0]);
                });

                shouldHaveThisState.call(this, {
                    height: new BN(20),
                    messageRoot: '0x2bdf15e9913a52d9f36bb7e62634a6079cc32fc2fe975aadfcbc67b7e3a8a61b',
                    messageSn: new BN(1),
                    messageCount: new BN(2),
                    remainMessageCount: new BN(1),
                    networkSectionHash: '0x4f8b2d17e51d233e0b1a89413a490633b1fee96a17265e7f697190118975daff',
                    validators
                });

                describe('when send RELAY_MESSAGES[1]', () => {
                    const RELAY_MESSAGE = '0xefeeed02abeae3e201a09c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501c483626f62c0';

                    it('returns messages: RELAY_MESSAGES[1]', async () => {
                        let msgs = await this.instance.handleRelayMessage.call('', SRC_NETWORK_ID, 1, RELAY_MESSAGE);
                        expect(msgs.map(v => toStr(v))).to.deep.equal(['bob']);
                    });

                    describe('after changed state: RELAY_MESSAGES[1]', () => {
                        beforeEach(async () => {
                            await this.instance.handleRelayMessage('', SRC_NETWORK_ID, 1, RELAY_MESSAGE);
                        });

                        shouldHaveThisState.call(this, {
                            height: new BN(20),
                            messageRoot: '0x2bdf15e9913a52d9f36bb7e62634a6079cc32fc2fe975aadfcbc67b7e3a8a61b',
                            messageSn: new BN(2),
                            messageCount: new BN(2),
                            remainMessageCount: new BN(0),
                            networkSectionHash: '0x4f8b2d17e51d233e0b1a89413a490633b1fee96a17265e7f697190118975daff',
                            validators
                        });
                    });
                });
            });
        });

        describe('when send RELAY_MESSAGES = [[BlockUpdate, MessageProof], [MessageProof], [MessageProof]]', () => {
            const RELAY_MESSAGES = [
                '0xf901e9f901e6f9018c01b90188f90185f86b1401a0177b09e6b788d2a5f9d7572265f8a54176a83783e6d1d99d1865bc24e04fb493c00100a0b791b4b069c561ca31093f825f083f6cc3c8e5ad5135625becd2ff77a8ccfa1e03a04a466308d2644bc8e169b3202117709fb424f2ec2df2e6c8280291744d2cbaa7f800f90115b90112f9010ff9010cb841ecc36972e9f7f8aeba9dac2375944e17bf341b0e831520cb4dddfdc526d8de6e110007edb46290a9feeb238b7f62a036a9e8c07fc70d650fd4db381248e41a8a00b841fe90cf7b2ac7a56231e42eb6baf0ecc4d3b732973c18396933f25a51461f11381026f37fcbbbca6c21e4a762d3510fa2b9f6a63041fd6a9a20891ce0a544d75500b8414a802b48b332834eb6a6c71b685642094afa8a58b41abbe88999e6a3b9ae93d651f6639512bb09702e079a06b681281932e071cfc07b59fda1196ccfa3a8baef00b841cb86ee4e7ddacaa923421ff3f15efd3eb15309e77566c8a321d6568406434fbe3fc4c526fe6721b4ddd09b16e7385ce02f0720043851b90ea5d17cac97a6f0e600f85502b852f850c0c685616c696365f846e201a038e47a7b719dce63662aeaf43440326f551b8a7ee198cee35cb5d517f2d296a2e201a0a2c791857d936d97cc584df15995fb9e6a3aff25630796d718e2f8ba105b0488',
                '0xf856f854f85202b84ff84de3e201a09c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501c483626f62e3e201a0a2c791857d936d97cc584df15995fb9e6a3aff25630796d718e2f8ba105b0488',
                '0xf3f2f102afeee3e202a02bdf15e9913a52d9f36bb7e62634a6079cc32fc2fe975aadfcbc67b7e3a8a61bc8876372797374616cc0'
            ];

            it('returns messages', async () => {
                let msgs = await this.instance.handleRelayMessage.call('', SRC_NETWORK_ID, 0, RELAY_MESSAGES[0]);
                expect(msgs.map(v => toStr(v))).to.deep.equal(['alice']);
            });

            describe('after changed state', () => {
                beforeEach(async () => {
                    await this.instance.handleRelayMessage('', SRC_NETWORK_ID, 0, RELAY_MESSAGES[0]);
                });

                shouldHaveThisState.call(this, {
                    height: new BN(20),
                    messageRoot: '0x4a466308d2644bc8e169b3202117709fb424f2ec2df2e6c8280291744d2cbaa7',
                    messageSn: new BN(1),
                    messageCount: new BN(3),
                    remainMessageCount: new BN(2),
                    networkSectionHash: '0xccb41e470a2b1d74baacdda37355c199e8a1dba368bfaefd4516eb5668e36e60',
                    validators
                });

                describe('when send RELAY_MESSAGES[1]', () => {
                    it('returns messages', async () => {
                        let msgs = await this.instance.handleRelayMessage.call('', SRC_NETWORK_ID, 1, RELAY_MESSAGES[1]);
                        expect(msgs.map(v => toStr(v))).to.deep.equal(['bob']);
                    });

                    describe('after changed state', () => {
                        beforeEach(async () => {
                            await this.instance.handleRelayMessage('', SRC_NETWORK_ID, 1, RELAY_MESSAGES[1]);
                        });

                        shouldHaveThisState.call(this, {
                            height: new BN(20),
                            messageRoot: '0x4a466308d2644bc8e169b3202117709fb424f2ec2df2e6c8280291744d2cbaa7',
                            messageSn: new BN(2),
                            messageCount: new BN(3),
                            remainMessageCount: new BN(1),
                            networkSectionHash: '0xccb41e470a2b1d74baacdda37355c199e8a1dba368bfaefd4516eb5668e36e60',
                            validators
                        });

                        describe('when send RELAY_MESSAGE[2]', () => {
                            it('returns messages', async () => {
                                let msgs = await this.instance.handleRelayMessage.call('', SRC_NETWORK_ID, 2, RELAY_MESSAGES[2]);
                                expect(msgs.map(v => toStr(v))).to.deep.equal(['crystal']);
                            });

                            describe('after changed state', () => {
                                beforeEach(async () => {
                                    await this.instance.handleRelayMessage('', SRC_NETWORK_ID, 2, RELAY_MESSAGES[2]);
                                });

                                shouldHaveThisState.call(this, {
                                    height: new BN(20),
                                    messageRoot: '0x4a466308d2644bc8e169b3202117709fb424f2ec2df2e6c8280291744d2cbaa7',
                                    messageSn: new BN(3),
                                    messageCount: new BN(3),
                                    remainMessageCount: new BN(0),
                                    networkSectionHash: '0xccb41e470a2b1d74baacdda37355c199e8a1dba368bfaefd4516eb5668e36e60',
                                    validators
                                });
                            });
                        });
                    });
                });
            });
        });

        describe('when send RELAY_MESSAGE = [BlockUpdate, MessageProof, MessageProof, MessageProof]', () => {
            const RELAY_MESSAGE = '0xf9026ff9026cf9018c01b90188f90185f86b1401a0177b09e6b788d2a5f9d7572265f8a54176a83783e6d1d99d1865bc24e04fb493c00100a0b791b4b069c561ca31093f825f083f6cc3c8e5ad5135625becd2ff77a8ccfa1e03a04a466308d2644bc8e169b3202117709fb424f2ec2df2e6c8280291744d2cbaa7f800f90115b90112f9010ff9010cb8414ea5201093fb9bfceae99016a8dda2655c23d5a82e6c692ec2d829a18ef2411a4656f20d7ff98a4b80a28294a9030f18c9b2821c0451fb85c927dd340353c47100b8414dc6594d5d0cfde5bc84d23459bae8d5ba959496fce3acbcf1fd0d211716c9ae30caf23b8380946a050d9b8d9b24f9f6935cfc77853d03746b60c5c9030e8a8500b84183a67494b3b5bfb10257330dab5aff20e3bb030312e3c8066c3a9544b7ef1b820720e2a36de1ea29316bec8dd395a1889748e2eb6adc18807e75b6f6e5aa98b200b841502274515c8867ee8570065a0a07fafda9b58a91046583073b495e513ea8722c05fc1063ae629c59f70264e5fc6f05d5a37597e87d166cd6f992137fd170373900f85502b852f850c0c685616c696365f846e201a038e47a7b719dce63662aeaf43440326f551b8a7ee198cee35cb5d517f2d296a2e201a0a2c791857d936d97cc584df15995fb9e6a3aff25630796d718e2f8ba105b0488f85202b84ff84de3e201a09c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501c483626f62e3e201a0a2c791857d936d97cc584df15995fb9e6a3aff25630796d718e2f8ba105b0488f102afeee3e202a02bdf15e9913a52d9f36bb7e62634a6079cc32fc2fe975aadfcbc67b7e3a8a61bc8876372797374616cc0';

            it('returns messages', async () => {
                let msgs = await this.instance.handleRelayMessage.call('', SRC_NETWORK_ID, 0, RELAY_MESSAGE);
                expect(msgs.map(v => toStr(v))).to.deep.equal(['alice', 'bob', 'crystal']);
            });

            describe('after changed state', () => {
                beforeEach(async () => {
                    await this.instance.handleRelayMessage('', SRC_NETWORK_ID, 0, RELAY_MESSAGE);
                });

                shouldHaveThisState.call(this, {
                    height: new BN(20),
                    messageRoot: '0x4a466308d2644bc8e169b3202117709fb424f2ec2df2e6c8280291744d2cbaa7',
                    messageSn: new BN(3),
                    messageCount: new BN(3),
                    remainMessageCount: new BN(0),
                    networkSectionHash: '0xccb41e470a2b1d74baacdda37355c199e8a1dba368bfaefd4516eb5668e36e60',
                    validators
                });
            });
        });

        describe('when send RELAY_MESSAGES = [[BlockUpdate], [MessageProof, MessageProof, MessageProof]]', () => {
            const RELAY_MESSAGES = [
                '0xf90192f9018ff9018c01b90188f90185f86b1401a0177b09e6b788d2a5f9d7572265f8a54176a83783e6d1d99d1865bc24e04fb493c00100a0b791b4b069c561ca31093f825f083f6cc3c8e5ad5135625becd2ff77a8ccfa1e03a04a466308d2644bc8e169b3202117709fb424f2ec2df2e6c8280291744d2cbaa7f800f90115b90112f9010ff9010cb8414c21c67f32585e680bc5079977ff0974f34c58461e4b88e089966012b5a0520f37b620fd62913abba15905befc6f83b17da391e1c6999aa01be0703f00ca989600b841743358b7078f2c8612ec8d6ac0f2470fa151526410c5967f2f132ff7b659d68558259029c092968ccf21435ddd4f89592f0f1b757c31baa75341db3d69cf977c00b841b97ad5e607b759e7d711acaa9ad4ddde8b4dd670cc2ff8c3238ca88b1efc5b5d6df213127b40ee4fac160d730dad3eb9c2e0b9086ada9525b2857eb9f9255eb401b841979098e92e4b947262f72af1a98dd3f2664d550ba8bd581e2afa4559bdb0d11711b252651642f6b49c385e3a9db1229d2fcc3063f9c305caa2f62e6acd3568da01',
                '0xf8dff8ddf85502b852f850c0c685616c696365f846e201a038e47a7b719dce63662aeaf43440326f551b8a7ee198cee35cb5d517f2d296a2e201a0a2c791857d936d97cc584df15995fb9e6a3aff25630796d718e2f8ba105b0488f85202b84ff84de3e201a09c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501c483626f62e3e201a0a2c791857d936d97cc584df15995fb9e6a3aff25630796d718e2f8ba105b0488f102afeee3e202a02bdf15e9913a52d9f36bb7e62634a6079cc32fc2fe975aadfcbc67b7e3a8a61bc8876372797374616cc0'
            ]

            it('returns empty message list: RELAY_MESSAGES[0]', async () => {
                let msgs = await this.instance.handleRelayMessage.call('', SRC_NETWORK_ID, 0, RELAY_MESSAGES[0]);
                expect(msgs.map(v => toStr(v))).to.be.empty;
            });

            describe('after changed state: RELAY_MESSAGES[0]', () => {
                beforeEach(async () => {
                    await this.instance.handleRelayMessage('', SRC_NETWORK_ID, 0, RELAY_MESSAGES[0]);
                });

                shouldHaveThisState.call(this, {
                    height: new BN(20),
                    messageRoot: '0x4a466308d2644bc8e169b3202117709fb424f2ec2df2e6c8280291744d2cbaa7',
                    messageSn: new BN(0),
                    messageCount: new BN(3),
                    remainMessageCount: new BN(3),
                    networkSectionHash: '0xccb41e470a2b1d74baacdda37355c199e8a1dba368bfaefd4516eb5668e36e60',
                    validators
                });

                describe('when send RELAY_MESSAGE[1]', () => {
                    it('returns messages', async () => {
                        let msgs = await this.instance.handleRelayMessage.call('', SRC_NETWORK_ID, 0, RELAY_MESSAGES[1]);
                        expect(msgs.map(v => toStr(v))).to.deep.equal(['alice', 'bob', 'crystal']);
                    });

                    describe('after changed state: RELAY_MESSAGE[1]', () => {
                        beforeEach(async () => {
                            await this.instance.handleRelayMessage('', SRC_NETWORK_ID, 0, RELAY_MESSAGES[1]);
                        });

                        shouldHaveThisState.call(this, {
                            height: new BN(20),
                            messageRoot: '0x4a466308d2644bc8e169b3202117709fb424f2ec2df2e6c8280291744d2cbaa7',
                            messageSn: new BN(3),
                            messageCount: new BN(3),
                            remainMessageCount: new BN(0),
                            networkSectionHash: '0xccb41e470a2b1d74baacdda37355c199e8a1dba368bfaefd4516eb5668e36e60',
                            validators
                        });
                    });
                });
            });
        });

        describe('when send RELAY_MESSAGE for another network', () => {
            const RELAY_MESSAGE = '0xf901a1f9019ef9018c01b90188f90185f86b1e01a0177b09e6b788d2a5f9d7572265f8a54176a83783e6d1d99d1865bc24e04fb493c00200a0273e85733c50a59785a41c066ee49e642480c73882d9ca9e67b19dd69c79417801a09c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501f800f90115b90112f9010ff9010cb8415fa6b0f5498d2c390f0f97158921efd90c4f99534a6f20dd1bc342166e70e7b102dc98ea4cba4914c01c80567033f8cf603ecb8e4f7004dbce9ca1ef2ad0d78e00b8419ab9a2a11d6a196dd0ac7fb87a509cee8422ec1aec1703ed43ad5b304aa16f412c14a5d1823cd430f2d5070a23623e202159fdebacf1c14d1e4372b079ad012101b841ca9e0a69255c4eccb3b702b97a533ce66167510f2a7dbbeca19ded9cdf95a5d309b1bb90f3ea2a5730c1769681cacf73b918b3bec41107b6580d5cd69f82806501b841eb86aba95074b07faba1589e3549dc2407531a7590869983b5b520e31ab1b0827382d813075db80ce064cac7c0c8bdcd20de325ecf1dc2f2def7bcad7bbe0a6400ce028ccbf800c685616c696365f800';

            it('revert', async () => {
                await expectRevert(this.instance.handleRelayMessage.call(
                    '', SRC_NETWORK_ID, 0, RELAY_MESSAGE), "BtpMessageVerifier: BlockUpdate for unknown network",
                );
            });
        });

        describe('when send RELAY_MESSAGE with invalid signatures', () => {
            describe('has enough the quorum', () => {
                const RELAY_MESSAGE = '0xf901f9f901f6f901e401b901e0f901ddf8c31401a0e67c4b3b5bca332a79c0fe9bedb7594ef9bfc429e957587fd47a16525ffd4d45c00101a0b791b4b069c561ca31093f825f083f6cc3c8e5ad5135625becd2ff77a8ccfa1e01a09c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501b858f856f85494944db52183a8cc6e118346780314963ae1de158b943c9e099a48d4bc432dd4c5be73dcb3e069cb3aa194acf71c78a1f0b6def45660f7352d30d0be7d80a394055126ca715a1f532d6ba57b41373bd460241346f90115b90112f9010ff9010cb84136c70ddd4f0ccfe6ee52a2f312d5aca50a7649c7b629b50f549e4c3d211617b64e483bea14f7ff03aa940eee17e9062b5e870fac4d89f80528d86e9c04df411300b8411a8fe4a69174fd0cbc586a72e0ed0e295fe7af0c60d9af2437b30b23f0c723f16bd56b2676d41d07d915883f5cedd3f57269381357420e9979749e0ee409db7600b841d678040fc3d706dde3ecce95ef920752803767c75ec7ff4796a5e4f8803b63361879ba2c464a6fae1a62f5dfdc51dab2ff4e53173064a2574bc7cc9b18ba092c01b841c2218bee0c3ac96e7c978b7f3b394386a657109e098eada5fd99d415720270ef50d1fb73b7ec0e3ebdeab42f530f088e948bc7e17bdda844bb26dcd935c23f1700ce028ccbf800c685616c696365f800';

                it('returns messages', async () => {
                    let msgs = await this.instance.handleRelayMessage.call('', SRC_NETWORK_ID, 0, RELAY_MESSAGE);
                    expect(msgs.map(v => toStr(v))).to.deep.equal(['alice']);
                });
            });

            describe('has lack of quorum', () => {
                const RELAY_MESSAGE = '0xf901f9f901f6f901e401b901e0f901ddf8c31401a0e67c4b3b5bca332a79c0fe9bedb7594ef9bfc429e957587fd47a16525ffd4d45c00101a0b791b4b069c561ca31093f825f083f6cc3c8e5ad5135625becd2ff77a8ccfa1e01a09c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501b858f856f85494944db52183a8cc6e118346780314963ae1de158b943c9e099a48d4bc432dd4c5be73dcb3e069cb3aa194acf71c78a1f0b6def45660f7352d30d0be7d80a394055126ca715a1f532d6ba57b41373bd460241346f90115b90112f9010ff9010cb841d678040fc3d706dde3ecce95ef920752803767c75ec7ff4796a5e4f8803b63361879ba2c464a6fae1a62f5dfdc51dab2ff4e53173064a2574bc7cc9b18ba092c01b841d678040fc3d706dde3ecce95ef920752803767c75ec7ff4796a5e4f8803b63361879ba2c464a6fae1a62f5dfdc51dab2ff4e53173064a2574bc7cc9b18ba092c01b841d678040fc3d706dde3ecce95ef920752803767c75ec7ff4796a5e4f8803b63361879ba2c464a6fae1a62f5dfdc51dab2ff4e53173064a2574bc7cc9b18ba092c01b841c2218bee0c3ac96e7c978b7f3b394386a657109e098eada5fd99d415720270ef50d1fb73b7ec0e3ebdeab42f530f088e948bc7e17bdda844bb26dcd935c23f1700ce028ccbf800c685616c696365f800';

                it('reverts', async () => {
                    await expectRevert(
                        this.instance.handleRelayMessage.call('', SRC_NETWORK_ID, 0, RELAY_MESSAGE),
                        "BtpMessageVerifier: Lack of quorum"
                    );
                });
            });
        });

        describe('when send RELAY_MESSAGE with changing validators', () => {
            const RELAY_MESSAGE = '0xf901cbf901c8f901c501b901c1f901bef8a41401a0e67c4b3b5bca332a79c0fe9bedb7594ef9bfc429e957587fd47a16525ffd4d45c00101a0b791b4b069c561ca31093f825f083f6cc3c8e5ad5135625becd2ff77a8ccfa1e00f800b858f856f85494944db52183a8cc6e118346780314963ae1de158b943c9e099a48d4bc432dd4c5be73dcb3e069cb3aa194acf71c78a1f0b6def45660f7352d30d0be7d80a394055126ca715a1f532d6ba57b41373bd460241346f90115b90112f9010ff9010cb8411f74037a22703ae781200d518e0c1d145827dd9a57686c3a1050b760d545846e09e8dfc6f3bb09c7c6c24b69704af3d6b00f9660676fad56004b3cfe68a1848b00b841e45e17102c4f6e2af99fa87a27ec315fdce5aaa1ea2469dbcc86da23af10a1d80f382a7a4a7d5a3b52ebd7cf5b7d7abcea6a5484e4be658e2bf1501e379dc46700b8415bfd44f792ec0f2fe08b70325d6a1708ff420107b7b0d07a2658c53fc40b6ae763cb4c122141679b67c44a7fb3268a3604b41d21cf2c1f580fc69b0f6c94058d00b84115a783dee29200fd25e7ddbe9171850142a03e389fe1c38b391951fc0e2be63d00406a04e862e970037f8f8f165290ac26522e1363466ef8ae1490469c7ed72701';

            const validators = [
                '0x944db52183a8cc6e118346780314963ae1de158b',
                '0x3c9e099a48d4bc432dd4c5be73dcb3e069cb3aa1',
                '0xacf71c78a1f0b6def45660f7352d30d0be7d80a3',
                '0x055126ca715a1f532d6ba57b41373bd460241346'
            ];

            describe('after changed state', () => {
                beforeEach(async () => {
                    await this.instance.handleRelayMessage('', SRC_NETWORK_ID, 0, RELAY_MESSAGE);
                });

                shouldHaveThisState.call(this, {
                    height: new BN(20),
                    messageRoot:ZB32,
                    messageSn: new BN(0),
                    messageCount: new BN(0),
                    remainMessageCount: new BN(0),
                    networkSectionHash: '0x39ea97ede1d7b3019f4f1b4ce6b463d4189dc86f620e5ced3611fded5ae41995',
                    validators
                });

                describe('send RELAY_MESSAGE signed by changed validators', () => {
                    const RELAY_MESSAGE = '0xf901a1f9019ef9018c01b90188f90185f86b1e01a0e67c4b3b5bca332a79c0fe9bedb7594ef9bfc429e957587fd47a16525ffd4d45c00100a039ea97ede1d7b3019f4f1b4ce6b463d4189dc86f620e5ced3611fded5ae4199501a09c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501f800f90115b90112f9010ff9010cb8419a25843917fe773046b812733fa62b25c9d10d2845eede8ccab8f335797ddea81fd2697ad3acdf6e4434bb2ab5a6e899405f411138172ac15dc3ee6edc043bfd00b841e35320badc46222b169942dcd257acf697a067a63256942195400171eb69ef3b4f9c37466c1ae7b8ac1b573aea55e4964da7b4bb034cc485ef924f51fc8127e200b84113ab8ac7790218ffabfe1d18e1a9a3e6e1e2a308328df410c99c342ec25823a627910e27287aefe6a6513d20884c572910987fee9fdc7ac4bedf1208d691f4b800b8417dc67f1a979f02c7349badbed8892aebc46f510e9ecfcdb54400c7fcc8f09bf454f1e56986ddd93db96841718c34bfaa36606f356edd1741eab6ea469fc0b90d01ce028ccbf800c685616c696365f800';

                    it('returns messages', async () => {
                        let msgs = await this.instance.handleRelayMessage.call('', SRC_NETWORK_ID, 0, RELAY_MESSAGE);
                        expect(msgs.map(v => toStr(v))).to.deep.equal(['alice']);
                    });
                });
            });
        });
    });

    describe('when has installed with block update which has generated with message', () => {
        const NETWORK_TYPE_ID = new BN(1);
        const NETWORK_ID = new BN(1);
        const FIRST_BLOCK_UPDATE = '0xf8a40a00a0177b09e6b788d2a5f9d7572265f8a54176a83783e6d1d99d1865bc24e04fb493c00101f80001a09c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501b858f856f85494524122f6386c9cd3342ecc377dbc9dcb036dd2e0948103611a29623db06a937b24f52d70cf44c1c41494910d15f35a3e685968d8596512efa56d840bb3c59451226eee21a3d3758727886df161c108f5857f3f';
        const validators = [
            '0x524122f6386c9cd3342ecc377dbc9dcb036dd2e0',
            '0x8103611a29623db06a937b24f52d70cf44c1c414',
            '0x910d15f35a3e685968d8596512efa56d840bb3c5',
            '0x51226eee21a3d3758727886df161c108f5857f3f'
        ];

        beforeEach(async () => {
            this.instance = await BtpMessageVerifier.new();
            await this.instance.initialize(BMC, toBytesString(SRC_NETWORK_ID), NETWORK_TYPE_ID, NETWORK_ID, FIRST_BLOCK_UPDATE, SEQUENCE_OFFSET);
        });

        shouldHaveImmutableState.call(this, {
            srcNetworkId: toBytesString(SRC_NETWORK_ID),
            networkTypeId: NETWORK_TYPE_ID,
            networkId: NETWORK_ID
        });

        shouldHaveThisState.call(this, {
            height: new BN(10),
            messageRoot:'0x9c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501',
            messageSn: new BN(0),
            messageCount: new BN(1),
            remainMessageCount: new BN(1),
            networkSectionHash: '0x7239515338d40afd6e908375975f969a503d75b40821bfa8412736980ff2f2b9',
            validators
        });

        describe('when miss RELAY_MESSAGE = [MessageProof], and send RELAY_MESSAGE = [BlockUpdate]', () => {
            const INVALID_RELAY_MESSAGE = '0xf90192f9018ff9018c01b90188f90185f86b1400a0177b09e6b788d2a5f9d7572265f8a54176a83783e6d1d99d1865bc24e04fb493c00102a07239515338d40afd6e908375975f969a503d75b40821bfa8412736980ff2f2b901a09c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501f800f90115b90112f9010ff9010cb841f7dfe7d5ca9aede7e77c317f7d32d500244e16c4587f22bccf686c9f014f720e573e2358254c6ee7386b22997014488803d9ec5ce2bb7470893eb55d61eb0e9801b841a908985d0bf055dcf0c9ff4c3b8ab2868e26b3ae9025437b539fc26308debf3c422991acbbf78b1fe2df3363c8a1c6d9307b5140df515103f7c20a3fe740fddb00b84170d4b3ada1ecca9d8aa333d5648daeec41aa102c75601aab04f907198a751abc3487874e30f2400bae0506a2601cc297474b66cc336a3d7e80c6500e4afe5d9d00b84160bdb50163ceb69f42fb236d8535d37bb3786b76c9c0ff7ea0ecd5716a50a69f6759fb193614cb92c1942e0c8ea7863d6c77c7e69a32340fa7760407f96f292600';

            it('revert', async () => {
                await expectRevert(
                    this.instance.handleRelayMessage.call('', SRC_NETWORK_ID, 0, INVALID_RELAY_MESSAGE),
                    "BtpMessageVerifier: has messages to be handled"
                );
            });
        });

        describe('when invalid source network sends RELAY_MESSAGE = [MessageProof]', () => {
            const RELAY_MESSAGE = '0xd0cfce028ccbf800c685616c696365f800';

            it('revert', async () => {
                await expectRevert(this.instance.handleRelayMessage.call('', 'btp://0x2.eth', 0, RELAY_MESSAGE),
                    "BtpMessageVerifier: Not allowed source network"
                );
            });
        });

        describe('when invalid sender send RELAY_MESSAGE = [ MessageProof]', () => {
            const RELAY_MESSAGE = '0xd0cfce028ccbf800c685616c696365f800';
            it('revert', async () => {
                await expectRevert(this.instance.handleRelayMessage.call(
                    '', SRC_NETWORK_ID, 0, RELAY_MESSAGE, { from: accounts[1] }),
                    "BtpMessageVerifier: Unauthorized bmc sender"
                );
            });
        });

        describe('when send RELAY_MESSAGE = [MessageProof]', () => {
            const RELAY_MESSAGE = '0xd0cfce028ccbf800c685616c696365f800';

            it('returns messages', async () => {
                let msgs = await this.instance.handleRelayMessage.call('', SRC_NETWORK_ID, 0, RELAY_MESSAGE);
                expect(msgs.map(v => toStr(v))).to.deep.equal(['alice']);
            });

            describe('after changed state', () => {
                beforeEach(async () => {
                    await this.instance.handleRelayMessage('', SRC_NETWORK_ID, 0, RELAY_MESSAGE);
                });

                shouldHaveThisState.call(this, {
                    height: new BN(10),
                    messageRoot:'0x9c0257114eb9399a2985f8e75dad7600c5d89fe3824ffa99ec1c3eb8bf3b0501',
                    messageSn: new BN(1),
                    messageCount: new BN(1),
                    remainMessageCount: new BN(0),
                    networkSectionHash: '0x7239515338d40afd6e908375975f969a503d75b40821bfa8412736980ff2f2b9',
                    validators
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

    it('has network id', async () => {
        expect(await this.instance.getNetworkId())
            .to.be.bignumber.equal(props.networkId);
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

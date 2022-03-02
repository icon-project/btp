const rlp = require('rlp');
const { sha3_256 } = require('js-sha3')
const assert = require('chai').assert;
const urlSafeBase64 = require('urlsafe-base64');
const _ = require('lodash');
const truffleAssert = require('truffle-assertions');

const { deployProxy, upgradeProxy } = require('@openzeppelin/truffle-upgrades');

const BMV = artifacts.require('BMV');
const DataValidator = artifacts.require('DataValidator');

const MockBMC = artifacts.require('MockBMC');
const MockBMV = artifacts.require('MockBMV');
const MockDataValidator = artifacts.require('MockDataValidator');

const BMVV2 = artifacts.require('BMVV2');
const DataValidatorV2 = artifacts.require('DataValidatorV2');

let sha3FIPS256 = (input) => {
    return '0x' + sha3_256.update(input).hex();
}

/* 
   Since ICON RLP library is not supported in Javascript, ETH RLP library is used instead.
   ETH ELP encode of `null` is `80` but the result ICON RLP encode is `f800`.
   So `[[]]` is encoded by ETH RLP encode, resulted into `c1c0` then replace it by `f800`.
   It means ICON RLP encode (`None`) = ETH ELP encode (`[[]]`).
 */
let convertEthRlpToIconRlp = (buff, prefix=true) => {
    return ((prefix) ? '0x': '') + buff.toString('hex').replace(/c1c0/g, 'f800');
};

contract('BMV integration tests', async () => {
    const iconNet = '0x3.icon';
    const praNet = '0x8.pra';
    const prevBtpAddr = 'btp://0x3.icon/cx7a0c2dd9751e592ac4fbd6c70bd5ec574ebf198a'
    const validatorsList = [
        'hxb6b5791be0b5ef67063b3c10b840fb81514db2fd'
    ];
    let encodedValidators = rlp.encode(validatorsList.map(validator => validator.replace('hx', '0x00')));

    const lastBlockHash = '0xe2ea2b413a46c6f35165faad7d570899d4deeb3658c19a04596d42d5473456eb';
    const btpMsgs = ['0xf8f0b8396274703a2f2f3078332e69636f6e2f637837613063326464393735316535393261633466626436633730626435656335373465626631393861b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303490436f696e2f57726170706564436f696e01b867f86500b862f860aa687862366235373931626530623565663637303633623363313062383430666238313531346462326664aa307836353144353230353463386163346231393139316437343237433236333337393735363744333963c9c88449434f4e8200d4'];
    const initOffset = 21171;
    const initRootSize = 8;
    const initCacheSize = 8;

    let bmv, dataValidator, bmc;

    beforeEach(async () => {
        bmc = await MockBMC.new(praNet);
        dataValidator = await deployProxy(MockDataValidator);
        bmv = await deployProxy(
            MockBMV,
            [
                bmc.address,
                dataValidator.address,
                iconNet,
                encodedValidators,
                initOffset,
                initRootSize,
                initCacheSize,
                lastBlockHash
            ]
        );
    });

    it('Get BMV info - Scenario 1: Get connected BMC address', async () => {
        const addr = await bmv.getConnectedBMC();
        assert.equal(addr, bmc.address, 'incorrect bmc address');
    });

    it('Get BMV info - Scenario 2: Get network address', async () => {
        const btpAddr = await bmv.getNetAddress();
        assert.equal(btpAddr, iconNet, 'incorrect btp network address');
    });

    it('Get BMV info - Scenario 3: Get list of validators and hash of RLP encode from given list', async () => {
        const res = await bmv.getValidators();
        const hash = sha3FIPS256(rlp.encode(validatorsList.map(validator => validator.replace('hx', '0x00'))));
        assert.equal(res[0], hash, 'incorrect validators\' hash');
        for (let i = 0; i < res[1].length; i++) {
            assert.deepEqual(res[1][i], web3.utils.toChecksumAddress(validatorsList[i].replace('hx', '0x')), 'incorrect validator address');
        }
    });

    it('Get BMV info - Scenario 4: Get BMV status', async () => {
        const blockUpdate = [
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Block header
                        2,                 // version
                        21172,             // height
                        1624383652806088,  // timestamp
                        '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd',   // proposer
                        '0xe2ea2b413a46c6f35165faad7d570899d4deeb3658c19a04596d42d5473456eb',   // previous hash
                        '0xb3531419f3bf6d9f624f8bad9541ffbeac2e0883484087801e08e7c855bbd339',   // vote hash
                        '0xed9e644e59b2ff65446f5f3d7d77c27858facf8aeb3b969470d7499c79f9757c',   // next validatos hash
                        [[]],                                                                   // patch tx hash
                        [[]],                                                                   // tx hash
                        '0x00802070482c1a0f0884c2a060285c0c21041000221032043801128bc0e331a854363b2090c88000691c0c112684c040',    // logs bloom
                        rlp.encode(
                            [   // result
                                '0x5962b2791303f11a7e663bfba365162408ed03935e472f0ea3e668c268992a9b',    // state hash
                                [[]],                                                                    // patch receipt hash
                                '0x05649c24151f44c4a9dd10b652cd20ffbe2203b1ebba0d388342f999619d2af7',    // receipt hash
                                '0xf867a04f820eefa94c3e731d177461f260b90c6f7c71f78170fad578c136a12033b423a0c37eaafab80062deb7eafa82ffc42719604138eef0234da69f789956f7949d1da0bb87db4b20e1d46a2d8f0e6aea6e32fb0451c474a905942086e8e33b3e2b1ab8f800f800'  // extension data
                            ]
                        )
                    ]
                )
            ),
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Votes
                        '0x00', // round
                        [  // block part set id
                            1,
                            '0x323afffff77a4432b5e0fef58fa27ec793514ce6bae42020c369f28c12cfed10'
                        ],
                        [    // vote items
                            [ 
                                1624383654796604, // time stamp
                                '0x8ac623fb4054e748d5e212ec01c0c263256017b3635ddc412f033a54056cf8306d03173e616605c8717ea5e7a4429cc1a53a0ae1e85c1e70c754d9a02b2bd13800'  // signature
                            ]
                        ]
                    ]
                )
            ),
            // Next validators
            [[]]
        ];
        
        const blockUpdates = [
            convertEthRlpToIconRlp(rlp.encode(blockUpdate))
        ];
        
        const blockProof = [[]]; // block proofs is empty
        
        const receiptProof = rlp.encode(
            [
                '0x00',   // tx index
                '0xf9014ab90147f90144822000b9013ef9013b0095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198a8303e5508303e5508502e90edd00b8ef0100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000200000000000000000000000010000000200010000000400000000000000000000000002000000000000000000000000000000020000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000f800f800a0377898d2f5bbfe29c8601e5ee0a3a5d53568bfbe77e2b726c98cf1242bfcfe6f',
                [
                  [
                    '0x00',  // event index
                    '0xf9016fb9016cf90169822000b90163f9016095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198af852964d657373616765287374722c696e742c627974657329b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303401f8f4b8f2f8f0b8396274703a2f2f3078332e69636f6e2f637837613063326464393735316535393261633466626436633730626435656335373465626631393861b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303490436f696e2f57726170706564436f696e01b867f86500b862f860aa687862366235373931626530623565663637303633623363313062383430666238313531346462326664aa307836353144353230353463386163346231393139316437343237433236333337393735363744333963c9c88449434f4e8200d4'
                  ]
                ]
              ]
        );
        
        const receiptProofs = [
            receiptProof
        ];
        
        let baseMsg = '0x' + convertEthRlpToIconRlp(
            rlp.encode(
                [
                    blockUpdates,
                    blockProof,
                    receiptProofs
                ]
            ),
            false
        );
        await bmc.testHandleRelayMessage(bmv.address, prevBtpAddr, 0, baseMsg);
        const status = await bmv.getStatus();
        assert.isNotEmpty(status, 'invalid status');
        assert.equal(status[0], initOffset + 1, 'incorrect current MTA height');
        assert.equal(status[1], initOffset, 'incorrect offset');
        assert.equal(status[2], initOffset + 1, 'incorrect last block height');
    });

    it('Handle relay message - Scenario 1: Revert if previous BMC is invalid', async () => {
        const invalidBtpAddr = 'btp://0x4.icon/cx7a0c2dd9751e592ac4fbd6c70bd5ec574ebf198a'
        await truffleAssert.reverts(
            bmc.testHandleRelayMessage.call(bmv.address, invalidBtpAddr, 0, '0x'),
            'BMVRevert: Invalid previous BMC'
        );
    });

    it('Handle relay message - Scenario 2: Revert if BMV receives Relay Message not from authorized BMC', async () => {
        const bmcBtpAddr = await bmc.btpAddr();
        await truffleAssert.reverts(
            bmv.handleRelayMessage.call(bmcBtpAddr, prevBtpAddr, 0, '0x'),
            'BMVRevert: Invalid BMC'
        );
    });

    it('Handle relay message - Scenario 3: Revert if BMC’s address, retrieved from provided BTP address, does not match an address of connected BMC', async () => {
        const bmc2 = await MockBMC.new(praNet);
        await truffleAssert.reverts(
            bmc2.testHandleRelayMessage.call(bmv.address, prevBtpAddr, 0, '0x'),
            'BMVRevert: Invalid BMC'
        );
    });

    it('Handle relay message - Scenario 4: Verify and decode relay message', async () => {
        const blockUpdate = [
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Block header
                        2,                 // version
                        21172,             // height
                        1624383652806088,  // timestamp
                        '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd',   // proposer
                        '0xe2ea2b413a46c6f35165faad7d570899d4deeb3658c19a04596d42d5473456eb',   // previous hash
                        '0xb3531419f3bf6d9f624f8bad9541ffbeac2e0883484087801e08e7c855bbd339',   // vote hash
                        '0xed9e644e59b2ff65446f5f3d7d77c27858facf8aeb3b969470d7499c79f9757c',   // next validatos hash
                        [[]],                                                                   // patch tx hash
                        [[]],                                                                   // tx hash
                        '0x00802070482c1a0f0884c2a060285c0c21041000221032043801128bc0e331a854363b2090c88000691c0c112684c040',    // logs bloom
                        rlp.encode(
                            [   // result
                                '0x5962b2791303f11a7e663bfba365162408ed03935e472f0ea3e668c268992a9b',    // state hash
                                [[]],                                                                    // patch receipt hash
                                '0x05649c24151f44c4a9dd10b652cd20ffbe2203b1ebba0d388342f999619d2af7',    // receipt hash
                                '0xf867a04f820eefa94c3e731d177461f260b90c6f7c71f78170fad578c136a12033b423a0c37eaafab80062deb7eafa82ffc42719604138eef0234da69f789956f7949d1da0bb87db4b20e1d46a2d8f0e6aea6e32fb0451c474a905942086e8e33b3e2b1ab8f800f800'  // extension data
                            ]
                        )
                    ]
                )
            ),
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Votes
                        '0x00', // round
                        [  // block part set id
                            1,
                            '0x323afffff77a4432b5e0fef58fa27ec793514ce6bae42020c369f28c12cfed10'
                        ],
                        [    // vote items
                            [ 
                                1624383654796604, // time stamp
                                '0x8ac623fb4054e748d5e212ec01c0c263256017b3635ddc412f033a54056cf8306d03173e616605c8717ea5e7a4429cc1a53a0ae1e85c1e70c754d9a02b2bd13800'  // signature
                            ]
                        ]
                    ]
                )
            ),
            // Next validators
            rlp.encode([
                '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd'
            ])
        ];
        
        const blockUpdates = [
            convertEthRlpToIconRlp(rlp.encode(blockUpdate))
        ];
        
        const blockProof = [[]]; // block proofs is empty
        
        const receiptProof = rlp.encode(
            [
                '0x00',   // tx index
                '0xf9014ab90147f90144822000b9013ef9013b0095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198a8303e5508303e5508502e90edd00b8ef0100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000200000000000000000000000010000000200010000000400000000000000000000000002000000000000000000000000000000020000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000f800f800a0377898d2f5bbfe29c8601e5ee0a3a5d53568bfbe77e2b726c98cf1242bfcfe6f',
                [
                  [
                    '0x00',  // event index
                    '0xf9016fb9016cf90169822000b90163f9016095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198af852964d657373616765287374722c696e742c627974657329b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303401f8f4b8f2f8f0b8396274703a2f2f3078332e69636f6e2f637837613063326464393735316535393261633466626436633730626435656335373465626631393861b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303490436f696e2f57726170706564436f696e01b867f86500b862f860aa687862366235373931626530623565663637303633623363313062383430666238313531346462326664aa307836353144353230353463386163346231393139316437343237433236333337393735363744333963c9c88449434f4e8200d4'
                  ]
                ]
            ]
        );
        
        const receiptProofs = [
            receiptProof
        ];
        
        let baseMsg = '0x' + convertEthRlpToIconRlp(
            rlp.encode(
                [
                    blockUpdates,
                    blockProof,
                    receiptProofs
                ]
            ),
            false
        );
        const res = await bmc.testHandleRelayMessage.call(bmv.address, prevBtpAddr, 0, baseMsg);
        for (let i = 0; i < btpMsgs.length; i++)
            assert.equal(btpMsgs[i], res[i], 'incorrect service messages');
    });

    it('Handle relay message - Scenario 5: Revert if both block updates and block proofs are empty', async () => {
        const blockUpdates = [];
        
        const blockProof = [[]]; // block proofs is empty
        
        const receiptProof = rlp.encode([]);
        
        const receiptProofs = [
            receiptProof
        ];
        
        let baseMsg = '0x' + convertEthRlpToIconRlp(
            rlp.encode(
                [
                    blockUpdates,
                    blockProof,
                    receiptProofs
                ]
            ),
            false
        );
        await truffleAssert.reverts(
            bmc.testHandleRelayMessage.call(bmv.address, prevBtpAddr, 0, baseMsg),
            'BMVRevert: Invalid relay message'
        );
    });

    it('Handle relay message - Scenario 6: Revert if prev hash in block update doesn\'t match last block hash in MTA', async () => {
        const blockUpdate = [
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Block header
                        2,                 // version
                        21172,             // height
                        1624383652806088,  // timestamp
                        '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd',   // proposer
                        '0x0000000000000000000000000000000000000000000000000000000000000001',   // previous hash
                        '0xb3531419f3bf6d9f624f8bad9541ffbeac2e0883484087801e08e7c855bbd339',   // vote hash
                        '0xed9e644e59b2ff65446f5f3d7d77c27858facf8aeb3b969470d7499c79f9757c',   // next validatos hash
                        [[]],                                                                   // patch tx hash
                        [[]],                                                                   // tx hash
                        '0x00802070482c1a0f0884c2a060285c0c21041000221032043801128bc0e331a854363b2090c88000691c0c112684c040',    // logs bloom
                        rlp.encode(
                            [   // result
                                '0x5962b2791303f11a7e663bfba365162408ed03935e472f0ea3e668c268992a9b',    // state hash
                                [[]],                                                                    // patch receipt hash
                                '0x05649c24151f44c4a9dd10b652cd20ffbe2203b1ebba0d388342f999619d2af7',    // receipt hash
                                '0xf867a04f820eefa94c3e731d177461f260b90c6f7c71f78170fad578c136a12033b423a0c37eaafab80062deb7eafa82ffc42719604138eef0234da69f789956f7949d1da0bb87db4b20e1d46a2d8f0e6aea6e32fb0451c474a905942086e8e33b3e2b1ab8f800f800'  // extension data
                            ]
                        )
                    ]
                )
            ),
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Votes
                        '0x00', // round
                        [  // block part set id
                            1,
                            '0x323afffff77a4432b5e0fef58fa27ec793514ce6bae42020c369f28c12cfed10'
                        ],
                        [    // vote items
                            [ 
                                1624383654796604, // time stamp
                                '0x8ac623fb4054e748d5e212ec01c0c263256017b3635ddc412f033a54056cf8306d03173e616605c8717ea5e7a4429cc1a53a0ae1e85c1e70c754d9a02b2bd13800'  // signature
                            ]
                        ]
                    ]
                )
            ),
            // Next validators
            [[]]
        ];
        
        const blockUpdates = [
            convertEthRlpToIconRlp(rlp.encode(blockUpdate))
        ];
        
        const blockProof = [[]]; // block proofs is empty
        
        const receiptProof = rlp.encode(
            [
                '0x00',   // tx index
                '0xf9014ab90147f90144822000b9013ef9013b0095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198a8303e5508303e5508502e90edd00b8ef0100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000200000000000000000000000010000000200010000000400000000000000000000000002000000000000000000000000000000020000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000f800f800a0377898d2f5bbfe29c8601e5ee0a3a5d53568bfbe77e2b726c98cf1242bfcfe6f',
                [
                  [
                    '0x00',  // event index
                    '0xf9016fb9016cf90169822000b90163f9016095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198af852964d657373616765287374722c696e742c627974657329b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303401f8f4b8f2f8f0b8396274703a2f2f3078332e69636f6e2f637837613063326464393735316535393261633466626436633730626435656335373465626631393861b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303490436f696e2f57726170706564436f696e01b867f86500b862f860aa687862366235373931626530623565663637303633623363313062383430666238313531346462326664aa307836353144353230353463386163346231393139316437343237433236333337393735363744333963c9c88449434f4e8200d4'
                  ]
                ]
              ]
        );
        
        const receiptProofs = [
            receiptProof
        ];
        
        let baseMsg = '0x' + convertEthRlpToIconRlp(
            rlp.encode(
                [
                    blockUpdates,
                    blockProof,
                    receiptProofs
                ]
            ),
            false
        );
        await truffleAssert.reverts(
            bmc.testHandleRelayMessage.call(bmv.address, prevBtpAddr, 0, baseMsg),
            'BMVRevertInvalidBlockUpdate: Invalid block hash'
        );
    });

    it('Handle relay message - Scenario 7: Revert if height in block update is higher than MTA current height', async () => {
        const blockUpdate = [
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Block header
                        2,                 // version
                        21175,             // height
                        1624383652806088,  // timestamp
                        '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd',   // proposer
                        '0xe2ea2b413a46c6f35165faad7d570899d4deeb3658c19a04596d42d5473456eb',   // previous hash
                        '0xb3531419f3bf6d9f624f8bad9541ffbeac2e0883484087801e08e7c855bbd339',   // vote hash
                        '0xed9e644e59b2ff65446f5f3d7d77c27858facf8aeb3b969470d7499c79f9757c',   // next validatos hash
                        [[]],                                                                   // patch tx hash
                        [[]],                                                                   // tx hash
                        '0x00802070482c1a0f0884c2a060285c0c21041000221032043801128bc0e331a854363b2090c88000691c0c112684c040',    // logs bloom
                        rlp.encode(
                            [   // result
                                '0x5962b2791303f11a7e663bfba365162408ed03935e472f0ea3e668c268992a9b',    // state hash
                                [[]],                                                                    // patch receipt hash
                                '0x05649c24151f44c4a9dd10b652cd20ffbe2203b1ebba0d388342f999619d2af7',    // receipt hash
                                '0xf867a04f820eefa94c3e731d177461f260b90c6f7c71f78170fad578c136a12033b423a0c37eaafab80062deb7eafa82ffc42719604138eef0234da69f789956f7949d1da0bb87db4b20e1d46a2d8f0e6aea6e32fb0451c474a905942086e8e33b3e2b1ab8f800f800'  // extension data
                            ]
                        )
                    ]
                )
            ),
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Votes
                        '0x00', // round
                        [  // block part set id
                            1,
                            '0x323afffff77a4432b5e0fef58fa27ec793514ce6bae42020c369f28c12cfed10'
                        ],
                        [    // vote items
                            [ 
                                1624383654796604, // time stamp
                                '0x8ac623fb4054e748d5e212ec01c0c263256017b3635ddc412f033a54056cf8306d03173e616605c8717ea5e7a4429cc1a53a0ae1e85c1e70c754d9a02b2bd13800'  // signature
                            ]
                        ]
                    ]
                )
            ),
            // Next validators
            [[]]
        ];
        
        const blockUpdates = [
            convertEthRlpToIconRlp(rlp.encode(blockUpdate))
        ];
        
        const blockProof = [[]]; // block proofs is empty
        
        const receiptProof = rlp.encode(
            [
                '0x00',   // tx index
                '0xf9014ab90147f90144822000b9013ef9013b0095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198a8303e5508303e5508502e90edd00b8ef0100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000200000000000000000000000010000000200010000000400000000000000000000000002000000000000000000000000000000020000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000f800f800a0377898d2f5bbfe29c8601e5ee0a3a5d53568bfbe77e2b726c98cf1242bfcfe6f',
                [
                  [
                    '0x00',  // event index
                    '0xf9016fb9016cf90169822000b90163f9016095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198af852964d657373616765287374722c696e742c627974657329b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303401f8f4b8f2f8f0b8396274703a2f2f3078332e69636f6e2f637837613063326464393735316535393261633466626436633730626435656335373465626631393861b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303490436f696e2f57726170706564436f696e01b867f86500b862f860aa687862366235373931626530623565663637303633623363313062383430666238313531346462326664aa307836353144353230353463386163346231393139316437343237433236333337393735363744333963c9c88449434f4e8200d4'
                  ]
                ]
              ]
        );
        
        const receiptProofs = [
            receiptProof
        ];
        
        let baseMsg = '0x' + convertEthRlpToIconRlp(
            rlp.encode(
                [
                    blockUpdates,
                    blockProof,
                    receiptProofs
                ]
            ),
            false
        );
        await truffleAssert.reverts(
            bmc.testHandleRelayMessage.call(bmv.address, prevBtpAddr, 0, baseMsg),
            'BMVRevertInvalidBlockUpdateHigher'
        );
    });

    it('Handle relay message - Scenario 8: Revert if height in block update is lower than MTA current height', async () => {
        const blockUpdate = [
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Block header
                        2,                 // version
                        21170,             // height
                        1624383652806088,  // timestamp
                        '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd',   // proposer
                        '0xe2ea2b413a46c6f35165faad7d570899d4deeb3658c19a04596d42d5473456eb',   // previous hash
                        '0xb3531419f3bf6d9f624f8bad9541ffbeac2e0883484087801e08e7c855bbd339',   // vote hash
                        '0xed9e644e59b2ff65446f5f3d7d77c27858facf8aeb3b969470d7499c79f9757c',   // next validatos hash
                        [[]],                                                                   // patch tx hash
                        [[]],                                                                   // tx hash
                        '0x00802070482c1a0f0884c2a060285c0c21041000221032043801128bc0e331a854363b2090c88000691c0c112684c040',    // logs bloom
                        rlp.encode(
                            [   // result
                                '0x5962b2791303f11a7e663bfba365162408ed03935e472f0ea3e668c268992a9b',    // state hash
                                [[]],                                                                    // patch receipt hash
                                '0x05649c24151f44c4a9dd10b652cd20ffbe2203b1ebba0d388342f999619d2af7',    // receipt hash
                                '0xf867a04f820eefa94c3e731d177461f260b90c6f7c71f78170fad578c136a12033b423a0c37eaafab80062deb7eafa82ffc42719604138eef0234da69f789956f7949d1da0bb87db4b20e1d46a2d8f0e6aea6e32fb0451c474a905942086e8e33b3e2b1ab8f800f800'  // extension data
                            ]
                        )
                    ]
                )
            ),
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Votes
                        '0x00', // round
                        [  // block part set id
                            1,
                            '0x323afffff77a4432b5e0fef58fa27ec793514ce6bae42020c369f28c12cfed10'
                        ],
                        [    // vote items
                            [ 
                                1624383654796604, // time stamp
                                '0x8ac623fb4054e748d5e212ec01c0c263256017b3635ddc412f033a54056cf8306d03173e616605c8717ea5e7a4429cc1a53a0ae1e85c1e70c754d9a02b2bd13800'  // signature
                            ]
                        ]
                    ]
                )
            ),
            // Next validators
            [[]]
        ];
        
        const blockUpdates = [
            convertEthRlpToIconRlp(rlp.encode(blockUpdate))
        ];
        
        const blockProof = [[]]; // block proofs is empty
        
        const receiptProof = rlp.encode(
            [
                '0x00',   // tx index
                '0xf9014ab90147f90144822000b9013ef9013b0095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198a8303e5508303e5508502e90edd00b8ef0100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000200000000000000000000000010000000200010000000400000000000000000000000002000000000000000000000000000000020000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000f800f800a0377898d2f5bbfe29c8601e5ee0a3a5d53568bfbe77e2b726c98cf1242bfcfe6f',
                [
                  [
                    '0x00',  // event index
                    '0xf9016fb9016cf90169822000b90163f9016095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198af852964d657373616765287374722c696e742c627974657329b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303401f8f4b8f2f8f0b8396274703a2f2f3078332e69636f6e2f637837613063326464393735316535393261633466626436633730626435656335373465626631393861b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303490436f696e2f57726170706564436f696e01b867f86500b862f860aa687862366235373931626530623565663637303633623363313062383430666238313531346462326664aa307836353144353230353463386163346231393139316437343237433236333337393735363744333963c9c88449434f4e8200d4'
                  ]
                ]
              ]
        );
        
        const receiptProofs = [
            receiptProof
        ];
        
        let baseMsg = '0x' + convertEthRlpToIconRlp(
            rlp.encode(
                [
                    blockUpdates,
                    blockProof,
                    receiptProofs
                ]
            ),
            false
        );
        await truffleAssert.reverts(
            bmc.testHandleRelayMessage.call(bmv.address, prevBtpAddr, 0, baseMsg),
            'BMVRevertInvalidBlockUpdateLower'
        );
    });

    it('Handle relay message - Scenario 9: Verify and decode relay message with block proof', async () => {
        // add block #21171 to MTA
        await bmv.setMTAHeight(initOffset - 1);
        await bmv.addRootToMTA('0xe2ea2b413a46c6f35165faad7d570899d4deeb3658c19a04596d42d5473456eb');

        let blockUpdate = [
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Block header
                        2,                 // version
                        21172,             // height
                        1624383652806088,  // timestamp
                        '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd',   // proposer
                        '0xe2ea2b413a46c6f35165faad7d570899d4deeb3658c19a04596d42d5473456eb',   // previous hash
                        '0xb3531419f3bf6d9f624f8bad9541ffbeac2e0883484087801e08e7c855bbd339',   // vote hash
                        '0xed9e644e59b2ff65446f5f3d7d77c27858facf8aeb3b969470d7499c79f9757c',   // next validatos hash
                        [[]],                                                                   // patch tx hash
                        [[]],                                                                   // tx hash
                        '0x00802070482c1a0f0884c2a060285c0c21041000221032043801128bc0e331a854363b2090c88000691c0c112684c040',    // logs bloom
                        rlp.encode(
                            [   // result
                                '0x5962b2791303f11a7e663bfba365162408ed03935e472f0ea3e668c268992a9b',    // state hash
                                [[]],                                                                    // patch receipt hash
                                '0x05649c24151f44c4a9dd10b652cd20ffbe2203b1ebba0d388342f999619d2af7',    // receipt hash
                                '0xf867a04f820eefa94c3e731d177461f260b90c6f7c71f78170fad578c136a12033b423a0c37eaafab80062deb7eafa82ffc42719604138eef0234da69f789956f7949d1da0bb87db4b20e1d46a2d8f0e6aea6e32fb0451c474a905942086e8e33b3e2b1ab8f800f800'  // extension data
                            ]
                        )
                    ]
                )
            ),
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Votes
                        '0x00', // round
                        [  // block part set id
                            1,
                            '0x323afffff77a4432b5e0fef58fa27ec793514ce6bae42020c369f28c12cfed10'
                        ],
                        [    // vote items
                            [ 
                                1624383654796604, // time stamp
                                '0x8ac623fb4054e748d5e212ec01c0c263256017b3635ddc412f033a54056cf8306d03173e616605c8717ea5e7a4429cc1a53a0ae1e85c1e70c754d9a02b2bd13800'  // signature
                            ]
                        ]
                    ]
                )
            ),
            // Next validators
            [[]]
        ];
        
        let blockUpdates = [
            convertEthRlpToIconRlp(rlp.encode(blockUpdate))
        ];
        
        let blockProof = [[]]; // block proof is empty
        
        let receiptProofs = []; // receipt proofs is empty
        
        let baseMsg = '0x' + convertEthRlpToIconRlp(
            rlp.encode(
                [
                    blockUpdates,
                    blockProof,
                    receiptProofs
                ]
            ),
            false
        );
        // add block #21172 to MTA
        await bmc.testHandleRelayMessage(bmv.address, prevBtpAddr, 0, baseMsg);

        // verify missing event from the old block added to MTA
        let oldBlockHeader = convertEthRlpToIconRlp(
            rlp.encode(
                [ // Block header
                    2,                 // version
                    21172,             // height
                    1624383652806088,  // timestamp
                    '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd',   // proposer
                    '0xe2ea2b413a46c6f35165faad7d570899d4deeb3658c19a04596d42d5473456eb',   // previous hash
                    '0xb3531419f3bf6d9f624f8bad9541ffbeac2e0883484087801e08e7c855bbd339',   // vote hash
                    '0xed9e644e59b2ff65446f5f3d7d77c27858facf8aeb3b969470d7499c79f9757c',   // next validatos hash
                    [[]],                                                                   // patch tx hash
                    [[]],                                                                   // tx hash
                    '0x00802070482c1a0f0884c2a060285c0c21041000221032043801128bc0e331a854363b2090c88000691c0c112684c040',    // logs bloom
                    rlp.encode(
                        [   // result
                            '0x5962b2791303f11a7e663bfba365162408ed03935e472f0ea3e668c268992a9b',    // state hash
                            [[]],                                                                    // patch receipt hash
                            '0x05649c24151f44c4a9dd10b652cd20ffbe2203b1ebba0d388342f999619d2af7',    // receipt hash
                            '0xf867a04f820eefa94c3e731d177461f260b90c6f7c71f78170fad578c136a12033b423a0c37eaafab80062deb7eafa82ffc42719604138eef0234da69f789956f7949d1da0bb87db4b20e1d46a2d8f0e6aea6e32fb0451c474a905942086e8e33b3e2b1ab8f800f800'  // extension data
                        ]
                    )
                ]
            )
        );
        
        blockUpdates = []; // block updates is empty
        
        blockProof = rlp.encode(
            [
                oldBlockHeader,
                [
                    21171,  // witness height in mta
                    [
                        '0xe2ea2b413a46c6f35165faad7d570899d4deeb3658c19a04596d42d5473456eb',   // block hash #21171
                    ]
                ]
            ]
        );
        
        receiptProof = rlp.encode(
            [
                '0x00',   // tx index
                '0xf9014ab90147f90144822000b9013ef9013b0095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198a8303e5508303e5508502e90edd00b8ef0100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000200000000000000000000000010000000200010000000400000000000000000000000002000000000000000000000000000000020000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000f800f800a0377898d2f5bbfe29c8601e5ee0a3a5d53568bfbe77e2b726c98cf1242bfcfe6f',
                [
                  [
                    '0x00',  // event index
                    '0xf9016fb9016cf90169822000b90163f9016095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198af852964d657373616765287374722c696e742c627974657329b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303401f8f4b8f2f8f0b8396274703a2f2f3078332e69636f6e2f637837613063326464393735316535393261633466626436633730626435656335373465626631393861b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303490436f696e2f57726170706564436f696e01b867f86500b862f860aa687862366235373931626530623565663637303633623363313062383430666238313531346462326664aa307836353144353230353463386163346231393139316437343237433236333337393735363744333963c9c88449434f4e8200d4'
                  ]
                ]
              ]
        );
        
        receiptProofs = [
            receiptProof
        ];
        
        baseMsg = '0x' + convertEthRlpToIconRlp(
            rlp.encode(
                [
                    blockUpdates,
                    blockProof,
                    receiptProofs
                ]
            ),
            false
        );
        const res = await bmc.testHandleRelayMessage.call(bmv.address, prevBtpAddr, 0, baseMsg);

        for (let i = 0; i < btpMsgs.length; i++)
            assert.equal(btpMsgs[i], res[i], 'incorrect service messages');
    });

    it('Handle relay message - Scenario 10: Revert if block witness is empty', async () => {
        // add block #21171 to MTA
        await bmv.setMTAHeight(initOffset - 1);
        await bmv.addRootToMTA('0xe2ea2b413a46c6f35165faad7d570899d4deeb3658c19a04596d42d5473456eb');

        let blockUpdate = [
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Block header
                        2,                 // version
                        21172,             // height
                        1624383652806088,  // timestamp
                        '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd',   // proposer
                        '0xe2ea2b413a46c6f35165faad7d570899d4deeb3658c19a04596d42d5473456eb',   // previous hash
                        '0xb3531419f3bf6d9f624f8bad9541ffbeac2e0883484087801e08e7c855bbd339',   // vote hash
                        '0xed9e644e59b2ff65446f5f3d7d77c27858facf8aeb3b969470d7499c79f9757c',   // next validatos hash
                        [[]],                                                                   // patch tx hash
                        [[]],                                                                   // tx hash
                        '0x00802070482c1a0f0884c2a060285c0c21041000221032043801128bc0e331a854363b2090c88000691c0c112684c040',    // logs bloom
                        rlp.encode(
                            [   // result
                                '0x5962b2791303f11a7e663bfba365162408ed03935e472f0ea3e668c268992a9b',    // state hash
                                [[]],                                                                    // patch receipt hash
                                '0x05649c24151f44c4a9dd10b652cd20ffbe2203b1ebba0d388342f999619d2af7',    // receipt hash
                                '0xf867a04f820eefa94c3e731d177461f260b90c6f7c71f78170fad578c136a12033b423a0c37eaafab80062deb7eafa82ffc42719604138eef0234da69f789956f7949d1da0bb87db4b20e1d46a2d8f0e6aea6e32fb0451c474a905942086e8e33b3e2b1ab8f800f800'  // extension data
                            ]
                        )
                    ]
                )
            ),
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Votes
                        '0x00', // round
                        [  // block part set id
                            1,
                            '0x323afffff77a4432b5e0fef58fa27ec793514ce6bae42020c369f28c12cfed10'
                        ],
                        [    // vote items
                            [ 
                                1624383654796604, // time stamp
                                '0x8ac623fb4054e748d5e212ec01c0c263256017b3635ddc412f033a54056cf8306d03173e616605c8717ea5e7a4429cc1a53a0ae1e85c1e70c754d9a02b2bd13800'  // signature
                            ]
                        ]
                    ]
                )
            ),
            // Next validators
            [[]]
        ];
        
        let blockUpdates = [
            convertEthRlpToIconRlp(rlp.encode(blockUpdate))
        ];
        
        let blockProof = [[]]; // block proof is empty
        
        let receiptProofs = []; // receipt proofs is empty
        
        let baseMsg = '0x' + convertEthRlpToIconRlp(
            rlp.encode(
                [
                    blockUpdates,
                    blockProof,
                    receiptProofs
                ]
            ),
            false
        );
        // add block #21172 to MTA
        await bmc.testHandleRelayMessage(bmv.address, prevBtpAddr, 0, baseMsg);

        // verify missing event from the old block added to MTA
        let oldBlockHeader = convertEthRlpToIconRlp(
            rlp.encode(
                [ // Block header
                    2,                 // version
                    21172,             // height
                    1624383652806088,  // timestamp
                    '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd',   // proposer
                    '0xe2ea2b413a46c6f35165faad7d570899d4deeb3658c19a04596d42d5473456eb',   // previous hash
                    '0xb3531419f3bf6d9f624f8bad9541ffbeac2e0883484087801e08e7c855bbd339',   // vote hash
                    '0xed9e644e59b2ff65446f5f3d7d77c27858facf8aeb3b969470d7499c79f9757c',   // next validatos hash
                    [[]],                                                                   // patch tx hash
                    [[]],                                                                   // tx hash
                    '0x00802070482c1a0f0884c2a060285c0c21041000221032043801128bc0e331a854363b2090c88000691c0c112684c040',    // logs bloom
                    rlp.encode(
                        [   // result
                            '0x5962b2791303f11a7e663bfba365162408ed03935e472f0ea3e668c268992a9b',    // state hash
                            [[]],                                                                    // patch receipt hash
                            '0x05649c24151f44c4a9dd10b652cd20ffbe2203b1ebba0d388342f999619d2af7',    // receipt hash
                            '0xf867a04f820eefa94c3e731d177461f260b90c6f7c71f78170fad578c136a12033b423a0c37eaafab80062deb7eafa82ffc42719604138eef0234da69f789956f7949d1da0bb87db4b20e1d46a2d8f0e6aea6e32fb0451c474a905942086e8e33b3e2b1ab8f800f800'  // extension data
                        ]
                    )
                ]
            )
        );
        
        blockUpdates = []; // block updates is empty
        
        blockProof = rlp.encode(
            [
                oldBlockHeader,
                [
                    21171,  // witness height in mta
                    [ ]     // empty block witness 
                ]
            ]
        );
        
        receiptProof = rlp.encode(
            [
                '0x00',   // tx index
                '0xf9014ab90147f90144822000b9013ef9013b0095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198a8303e5508303e5508502e90edd00b8ef0100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000200000000000000000000000010000000200010000000400000000000000000000000002000000000000000000000000000000020000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000f800f800a0377898d2f5bbfe29c8601e5ee0a3a5d53568bfbe77e2b726c98cf1242bfcfe6f',
                [
                  [
                    '0x00',  // event index
                    '0xf9016fb9016cf90169822000b90163f9016095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198af852964d657373616765287374722c696e742c627974657329b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303401f8f4b8f2f8f0b8396274703a2f2f3078332e69636f6e2f637837613063326464393735316535393261633466626436633730626435656335373465626631393861b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303490436f696e2f57726170706564436f696e01b867f86500b862f860aa687862366235373931626530623565663637303633623363313062383430666238313531346462326664aa307836353144353230353463386163346231393139316437343237433236333337393735363744333963c9c88449434f4e8200d4'
                  ]
                ]
              ]
        );
        
        receiptProofs = [
            receiptProof
        ];
        
        baseMsg = '0x' + convertEthRlpToIconRlp(
            rlp.encode(
                [
                    blockUpdates,
                    blockProof,
                    receiptProofs
                ]
            ),
            false
        );
        await truffleAssert.reverts(
            bmc.testHandleRelayMessage.call(bmv.address, prevBtpAddr, 0, baseMsg),
            'BMVRevertInvalidBlockWitness'
        );
    });

    it('Handle relay message - Scenario 11: Revert if block header height in block proof is higher than MTA height', async () => {
        // add block #21171 to MTA
        await bmv.setMTAHeight(initOffset - 1);
        await bmv.addRootToMTA('0xe2ea2b413a46c6f35165faad7d570899d4deeb3658c19a04596d42d5473456eb');

        let blockUpdate = [
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Block header
                        2,                 // version
                        21172,             // height
                        1624383652806088,  // timestamp
                        '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd',   // proposer
                        '0xe2ea2b413a46c6f35165faad7d570899d4deeb3658c19a04596d42d5473456eb',   // previous hash
                        '0xb3531419f3bf6d9f624f8bad9541ffbeac2e0883484087801e08e7c855bbd339',   // vote hash
                        '0xed9e644e59b2ff65446f5f3d7d77c27858facf8aeb3b969470d7499c79f9757c',   // next validatos hash
                        [[]],                                                                   // patch tx hash
                        [[]],                                                                   // tx hash
                        '0x00802070482c1a0f0884c2a060285c0c21041000221032043801128bc0e331a854363b2090c88000691c0c112684c040',    // logs bloom
                        rlp.encode(
                            [   // result
                                '0x5962b2791303f11a7e663bfba365162408ed03935e472f0ea3e668c268992a9b',    // state hash
                                [[]],                                                                    // patch receipt hash
                                '0x05649c24151f44c4a9dd10b652cd20ffbe2203b1ebba0d388342f999619d2af7',    // receipt hash
                                '0xf867a04f820eefa94c3e731d177461f260b90c6f7c71f78170fad578c136a12033b423a0c37eaafab80062deb7eafa82ffc42719604138eef0234da69f789956f7949d1da0bb87db4b20e1d46a2d8f0e6aea6e32fb0451c474a905942086e8e33b3e2b1ab8f800f800'  // extension data
                            ]
                        )
                    ]
                )
            ),
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Votes
                        '0x00', // round
                        [  // block part set id
                            1,
                            '0x323afffff77a4432b5e0fef58fa27ec793514ce6bae42020c369f28c12cfed10'
                        ],
                        [    // vote items
                            [ 
                                1624383654796604, // time stamp
                                '0x8ac623fb4054e748d5e212ec01c0c263256017b3635ddc412f033a54056cf8306d03173e616605c8717ea5e7a4429cc1a53a0ae1e85c1e70c754d9a02b2bd13800'  // signature
                            ]
                        ]
                    ]
                )
            ),
            // Next validators
            [[]]
        ];
        
        let blockUpdates = [
            convertEthRlpToIconRlp(rlp.encode(blockUpdate))
        ];
        
        let blockProof = [[]]; // block proof is empty
        
        let receiptProofs = []; // receipt proofs is empty
        
        let baseMsg = '0x' + convertEthRlpToIconRlp(
            rlp.encode(
                [
                    blockUpdates,
                    blockProof,
                    receiptProofs
                ]
            ),
            false
        );
        // add block #21172 to MTA
        await bmc.testHandleRelayMessage(bmv.address, prevBtpAddr, 0, baseMsg);

        // verify missing event from the old block added to MTA
        let oldBlockHeader = convertEthRlpToIconRlp(
            rlp.encode(
                [ // Block header
                    2,                 // version
                    30000,             // height
                    1624383652806088,  // timestamp
                    '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd',   // proposer
                    '0xe2ea2b413a46c6f35165faad7d570899d4deeb3658c19a04596d42d5473456eb',   // previous hash
                    '0xb3531419f3bf6d9f624f8bad9541ffbeac2e0883484087801e08e7c855bbd339',   // vote hash
                    '0xed9e644e59b2ff65446f5f3d7d77c27858facf8aeb3b969470d7499c79f9757c',   // next validatos hash
                    [[]],                                                                   // patch tx hash
                    [[]],                                                                   // tx hash
                    '0x00802070482c1a0f0884c2a060285c0c21041000221032043801128bc0e331a854363b2090c88000691c0c112684c040',    // logs bloom
                    rlp.encode(
                        [   // result
                            '0x5962b2791303f11a7e663bfba365162408ed03935e472f0ea3e668c268992a9b',    // state hash
                            [[]],                                                                    // patch receipt hash
                            '0x05649c24151f44c4a9dd10b652cd20ffbe2203b1ebba0d388342f999619d2af7',    // receipt hash
                            '0xf867a04f820eefa94c3e731d177461f260b90c6f7c71f78170fad578c136a12033b423a0c37eaafab80062deb7eafa82ffc42719604138eef0234da69f789956f7949d1da0bb87db4b20e1d46a2d8f0e6aea6e32fb0451c474a905942086e8e33b3e2b1ab8f800f800'  // extension data
                        ]
                    )
                ]
            )
        );
        
        blockUpdates = []; // block updates is empty
        
        blockProof = rlp.encode(
            [
                oldBlockHeader,
                [
                    21171,  // witness height in mta
                    [ '0xe2ea2b413a46c6f35165faad7d570899d4deeb3658c19a04596d42d5473456eb' ]
                ]
            ]
        );
        
        receiptProof = rlp.encode(
            [
                '0x00',   // tx index
                '0xf9014ab90147f90144822000b9013ef9013b0095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198a8303e5508303e5508502e90edd00b8ef0100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000200000000000000000000000010000000200010000000400000000000000000000000002000000000000000000000000000000020000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000f800f800a0377898d2f5bbfe29c8601e5ee0a3a5d53568bfbe77e2b726c98cf1242bfcfe6f',
                [
                  [
                    '0x00',  // event index
                    '0xf9016fb9016cf90169822000b90163f9016095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198af852964d657373616765287374722c696e742c627974657329b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303401f8f4b8f2f8f0b8396274703a2f2f3078332e69636f6e2f637837613063326464393735316535393261633466626436633730626435656335373465626631393861b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303490436f696e2f57726170706564436f696e01b867f86500b862f860aa687862366235373931626530623565663637303633623363313062383430666238313531346462326664aa307836353144353230353463386163346231393139316437343237433236333337393735363744333963c9c88449434f4e8200d4'
                  ]
                ]
              ]
        );
        
        receiptProofs = [
            receiptProof
        ];
        
        baseMsg = '0x' + convertEthRlpToIconRlp(
            rlp.encode(
                [
                    blockUpdates,
                    blockProof,
                    receiptProofs
                ]
            ),
            false
        );
        await truffleAssert.reverts(
            bmc.testHandleRelayMessage.call(bmv.address, prevBtpAddr, 0, baseMsg),
            'BMVRevertInvalidBlockProofHigher'
        );
    });

    it('Handle relay message - Scenario 12: Revert if root hash in MPT receipt/event proofs is invalid', async () => {
        const blockUpdate = [
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Block header
                        2,                 // version
                        21172,             // height
                        1624383652806088,  // timestamp
                        '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd',   // proposer
                        '0xe2ea2b413a46c6f35165faad7d570899d4deeb3658c19a04596d42d5473456eb',   // previous hash
                        '0xb3531419f3bf6d9f624f8bad9541ffbeac2e0883484087801e08e7c855bbd339',   // vote hash
                        '0xed9e644e59b2ff65446f5f3d7d77c27858facf8aeb3b969470d7499c79f9757c',   // next validatos hash
                        [[]],                                                                   // patch tx hash
                        [[]],                                                                   // tx hash
                        '0x00802070482c1a0f0884c2a060285c0c21041000221032043801128bc0e331a854363b2090c88000691c0c112684c040',    // logs bloom
                        rlp.encode(
                            [   // result
                                '0x5962b2791303f11a7e663bfba365162408ed03935e472f0ea3e668c268992a9b',    // state hash
                                [[]],                                                                    // patch receipt hash
                                '0x05649c24151f44c4a9dd10b652cd20ffbe2203b1ebba0d388342f999619d2af7',    // receipt hash
                                '0xf867a04f820eefa94c3e731d177461f260b90c6f7c71f78170fad578c136a12033b423a0c37eaafab80062deb7eafa82ffc42719604138eef0234da69f789956f7949d1da0bb87db4b20e1d46a2d8f0e6aea6e32fb0451c474a905942086e8e33b3e2b1ab8f800f800'  // extension data
                            ]
                        )
                    ]
                )
            ),
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Votes
                        '0x00', // round
                        [  // block part set id
                            1,
                            '0x323afffff77a4432b5e0fef58fa27ec793514ce6bae42020c369f28c12cfed10'
                        ],
                        [    // vote items
                            [ 
                                1624383654796604, // time stamp
                                '0x8ac623fb4054e748d5e212ec01c0c263256017b3635ddc412f033a54056cf8306d03173e616605c8717ea5e7a4429cc1a53a0ae1e85c1e70c754d9a02b2bd13800'  // signature
                            ]
                        ]
                    ]
                )
            ),
            // Next validators
            rlp.encode([
                '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd'
            ])
        ];
        
        const blockUpdates = [
            convertEthRlpToIconRlp(rlp.encode(blockUpdate))
        ];
        
        const blockProof = [[]]; // block proofs is empty
        
        const receiptProof = rlp.encode(
            [
                '0x00',   // tx index
                '0xf90146822000b9013ef9013b0095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198a8303e5508303e5508502e90edd00b8ef0100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000200000000000000000000000010000000200010000000400000000000000000000000002000000000000000000000000000000020000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000f800f800a0377898d2f5bbfe29c8601e5ee0a3a5d53568bfbe77e2b726c98cf1242bfcfe6f81fa',
                [
                  [
                    '0x00',  // event index
                    '0xf9016fb9016cf90169822000b90163f9016095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198af852964d657373616765287374722c696e742c627974657329b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303401f8f4b8f2f8f0b8396274703a2f2f3078332e69636f6e2f637837613063326464393735316535393261633466626436633730626435656335373465626631393861b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303490436f696e2f57726170706564436f696e01b867f86500b862f860aa687862366235373931626530623565663637303633623363313062383430666238313531346462326664aa307836353144353230353463386163346231393139316437343237433236333337393735363744333963c9c88449434f4e8200d4'
                  ]
                ]
            ]
        );
        
        const receiptProofs = [
            receiptProof
        ];
        
        baseMsg = '0x' + convertEthRlpToIconRlp(
            rlp.encode(
                [
                    blockUpdates,
                    blockProof,
                    receiptProofs
                ]
            ),
            false
        );
        await truffleAssert.reverts(
            bmc.testHandleRelayMessage.call(bmv.address, prevBtpAddr, 0, baseMsg),
            'MPTException: Mismatch hash'
        );
    });

    it('Handle relay message - Scenario 13: Revert if nibbles on extension/leaf in MPT receipt/event proofs are invalid', async () => {
        const blockUpdate = [
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Block header
                        2,                 // version
                        21172,             // height
                        1624383652806088,  // timestamp
                        '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd',   // proposer
                        '0xe2ea2b413a46c6f35165faad7d570899d4deeb3658c19a04596d42d5473456eb',   // previous hash
                        '0xb3531419f3bf6d9f624f8bad9541ffbeac2e0883484087801e08e7c855bbd339',   // vote hash
                        '0xed9e644e59b2ff65446f5f3d7d77c27858facf8aeb3b969470d7499c79f9757c',   // next validatos hash
                        [[]],                                                                   // patch tx hash
                        [[]],                                                                   // tx hash
                        '0x00802070482c1a0f0884c2a060285c0c21041000221032043801128bc0e331a854363b2090c88000691c0c112684c040',    // logs bloom
                        rlp.encode(
                            [   // result
                                '0x5962b2791303f11a7e663bfba365162408ed03935e472f0ea3e668c268992a9b',    // state hash
                                [[]],                                                                    // patch receipt hash
                                '0x05649c24151f44c4a9dd10b652cd20ffbe2203b1ebba0d388342f999619d2af7',    // receipt hash
                                '0xf867a04f820eefa94c3e731d177461f260b90c6f7c71f78170fad578c136a12033b423a0c37eaafab80062deb7eafa82ffc42719604138eef0234da69f789956f7949d1da0bb87db4b20e1d46a2d8f0e6aea6e32fb0451c474a905942086e8e33b3e2b1ab8f800f800'  // extension data
                            ]
                        )
                    ]
                )
            ),
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Votes
                        '0x00', // round
                        [  // block part set id
                            1,
                            '0x323afffff77a4432b5e0fef58fa27ec793514ce6bae42020c369f28c12cfed10'
                        ],
                        [    // vote items
                            [ 
                                1624383654796604, // time stamp
                                '0x8ac623fb4054e748d5e212ec01c0c263256017b3635ddc412f033a54056cf8306d03173e616605c8717ea5e7a4429cc1a53a0ae1e85c1e70c754d9a02b2bd13800'  // signature
                            ]
                        ]
                    ]
                )
            ),
            // Next validators
            rlp.encode([
                '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd'
            ])
        ];
        
        const blockUpdates = [
            convertEthRlpToIconRlp(rlp.encode(blockUpdate))
        ];
        
        const blockProof = [[]]; // block proofs is empty
        
        const receiptProof = rlp.encode(
            [
                '0x0234',   // tx index
                '0xf9014ab90147f90144822000b9013ef9013b0095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198a8303e5508303e5508502e90edd00b8ef0100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000200000000000000000000000010000000200010000000400000000000000000000000002000000000000000000000000000000020000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000f800f800a0377898d2f5bbfe29c8601e5ee0a3a5d53568bfbe77e2b726c98cf1242bfcfe6f',
                [
                  [
                    '0x00',  // event index
                    '0xf9016fb9016cf90169822000b90163f9016095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198af852964d657373616765287374722c696e742c627974657329b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303401f8f4b8f2f8f0b8396274703a2f2f3078332e69636f6e2f637837613063326464393735316535393261633466626436633730626435656335373465626631393861b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303490436f696e2f57726170706564436f696e01b867f86500b862f860aa687862366235373931626530623565663637303633623363313062383430666238313531346462326664aa307836353144353230353463386163346231393139316437343237433236333337393735363744333963c9c88449434f4e8200d4'
                  ]
                ]
            ]
        );
        
        const receiptProofs = [
            receiptProof
        ];
        
        let baseMsg = '0x' + convertEthRlpToIconRlp(
            rlp.encode(
                [
                    blockUpdates,
                    blockProof,
                    receiptProofs
                ]
            ),
            false
        );
        await truffleAssert.reverts(
            bmc.testHandleRelayMessage.call(bmv.address, prevBtpAddr, 0, baseMsg),
            'MPTException: Mismatch nibbles on leaf'
        );
    });

    it('Handle relay message - Scenario 14: Revert if next validators does not exist', async () => {
        const validatorsList = [
            'hxb6b5791be0b5ef67063b3c10b840fb81514db2fd'
        ];
        let encodedValidators = rlp.encode(validatorsList.map(validator => validator.replace('hx', '0x01')));

        bmc = await MockBMC.new(praNet);
        dataValidator = await deployProxy(MockDataValidator);
        bmv = await deployProxy(
            MockBMV,
            [
                bmc.address,
                dataValidator.address,
                iconNet,
                encodedValidators,
                initOffset,
                initRootSize,
                initCacheSize,
                lastBlockHash
            ]
        );

        const blockUpdate = [
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Block header
                        2,                 // version
                        21172,             // height
                        1624383652806088,  // timestamp
                        '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd',   // proposer
                        '0xe2ea2b413a46c6f35165faad7d570899d4deeb3658c19a04596d42d5473456eb',   // previous hash
                        '0xb3531419f3bf6d9f624f8bad9541ffbeac2e0883484087801e08e7c855bbd339',   // vote hash
                        '0xed9e644e59b2ff65446f5f3d7d77c27858facf8aeb3b969470d7499c79f9757c',   // next validatos hash
                        [[]],                                                                   // patch tx hash
                        [[]],                                                                   // tx hash
                        '0x00802070482c1a0f0884c2a060285c0c21041000221032043801128bc0e331a854363b2090c88000691c0c112684c040',    // logs bloom
                        rlp.encode(
                            [   // result
                                '0x5962b2791303f11a7e663bfba365162408ed03935e472f0ea3e668c268992a9b',    // state hash
                                [[]],                                                                    // patch receipt hash
                                '0x05649c24151f44c4a9dd10b652cd20ffbe2203b1ebba0d388342f999619d2af7',    // receipt hash
                                '0xf867a04f820eefa94c3e731d177461f260b90c6f7c71f78170fad578c136a12033b423a0c37eaafab80062deb7eafa82ffc42719604138eef0234da69f789956f7949d1da0bb87db4b20e1d46a2d8f0e6aea6e32fb0451c474a905942086e8e33b3e2b1ab8f800f800'  // extension data
                            ]
                        )
                    ]
                )
            ),
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Votes
                        '0x00', // round
                        [  // block part set id
                            1,
                            '0x323afffff77a4432b5e0fef58fa27ec793514ce6bae42020c369f28c12cfed10'
                        ],
                        [    // vote items
                            [ 
                                1624383654796604, // time stamp
                                '0x8ac623fb4054e748d5e212ec01c0c263256017b3635ddc412f033a54056cf8306d03173e616605c8717ea5e7a4429cc1a53a0ae1e85c1e70c754d9a02b2bd13800'  // signature
                            ]
                        ]
                    ]
                )
            ),
            // Next validators
            rlp.encode([])
        ];
        
        const blockUpdates = [
            convertEthRlpToIconRlp(rlp.encode(blockUpdate))
        ];
        
        const blockProof = [[]]; // block proofs is empty
        
        const receiptProof = rlp.encode(
            [
                '0x0234',   // tx index
                '0xf9014ab90147f90144822000b9013ef9013b0095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198a8303e5508303e5508502e90edd00b8ef0100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000200000000000000000000000010000000200010000000400000000000000000000000002000000000000000000000000000000020000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000f800f800a0377898d2f5bbfe29c8601e5ee0a3a5d53568bfbe77e2b726c98cf1242bfcfe6f',
                [
                  [
                    '0x00',  // event index
                    '0xf9016fb9016cf90169822000b90163f9016095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198af852964d657373616765287374722c696e742c627974657329b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303401f8f4b8f2f8f0b8396274703a2f2f3078332e69636f6e2f637837613063326464393735316535393261633466626436633730626435656335373465626631393861b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303490436f696e2f57726170706564436f696e01b867f86500b862f860aa687862366235373931626530623565663637303633623363313062383430666238313531346462326664aa307836353144353230353463386163346231393139316437343237433236333337393735363744333963c9c88449434f4e8200d4'
                  ]
                ]
            ]
        );
        
        const receiptProofs = [
            receiptProof
        ];

        let baseMsg = '0x' + convertEthRlpToIconRlp(
            rlp.encode(
                [
                    blockUpdates,
                    blockProof,
                    receiptProofs
                ]
            ),
            false
        );
        await truffleAssert.reverts(
            bmc.testHandleRelayMessage.call(bmv.address, prevBtpAddr, 0, baseMsg),
            'BMVRevertInvalidBlockUpdate: Not exists next validators'
        );
    });

    it('Handle relay message - Scenario 15: Revert if next validator hash is invalid', async () => {
        const validatorsList = [
            'hxb6b5791be0b5ef67063b3c10b840fb81514db2fd'
        ];
        let encodedValidators = rlp.encode(validatorsList.map(validator => validator.replace('hx', '0x01')));

        bmc = await MockBMC.new(praNet);
        dataValidator = await deployProxy(MockDataValidator);
        bmv = await deployProxy(
            MockBMV,
            [
                bmc.address,
                dataValidator.address,
                iconNet,
                encodedValidators,
                initOffset,
                initRootSize,
                initCacheSize,
                lastBlockHash
            ]
        );

        const blockUpdate = [
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Block header
                        2,                 // version
                        21172,             // height
                        1624383652806088,  // timestamp
                        '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd',   // proposer
                        '0xe2ea2b413a46c6f35165faad7d570899d4deeb3658c19a04596d42d5473456eb',   // previous hash
                        '0xb3531419f3bf6d9f624f8bad9541ffbeac2e0883484087801e08e7c855bbd339',   // vote hash
                        '0xed9e644e59b2ff65446f5f3d7d77c27858facf8aeb3b969470d7499c79f9757c',   // next validatos hash
                        [[]],                                                                   // patch tx hash
                        [[]],                                                                   // tx hash
                        '0x00802070482c1a0f0884c2a060285c0c21041000221032043801128bc0e331a854363b2090c88000691c0c112684c040',    // logs bloom
                        rlp.encode(
                            [   // result
                                '0x5962b2791303f11a7e663bfba365162408ed03935e472f0ea3e668c268992a9b',    // state hash
                                [[]],                                                                    // patch receipt hash
                                '0x05649c24151f44c4a9dd10b652cd20ffbe2203b1ebba0d388342f999619d2af7',    // receipt hash
                                '0xf867a04f820eefa94c3e731d177461f260b90c6f7c71f78170fad578c136a12033b423a0c37eaafab80062deb7eafa82ffc42719604138eef0234da69f789956f7949d1da0bb87db4b20e1d46a2d8f0e6aea6e32fb0451c474a905942086e8e33b3e2b1ab8f800f800'  // extension data
                            ]
                        )
                    ]
                )
            ),
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Votes
                        '0x00', // round
                        [  // block part set id
                            1,
                            '0x323afffff77a4432b5e0fef58fa27ec793514ce6bae42020c369f28c12cfed10'
                        ],
                        [    // vote items
                            [ 
                                1624383654796604, // time stamp
                                '0x8ac623fb4054e748d5e212ec01c0c263256017b3635ddc412f033a54056cf8306d03173e616605c8717ea5e7a4429cc1a53a0ae1e85c1e70c754d9a02b2bd13800'  // signature
                            ]
                        ]
                    ]
                )
            ),
            // Next validators
            rlp.encode([
                '0x01b6b5791be0b5ef67063b3c10b840fb81514db2fd'
            ])
        ];
        
        const blockUpdates = [
            convertEthRlpToIconRlp(rlp.encode(blockUpdate))
        ];
        
        const blockProof = [[]]; // block proofs is empty
        
        const receiptProof = rlp.encode(
            [
                '0x0234',   // tx index
                '0xf9014ab90147f90144822000b9013ef9013b0095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198a8303e5508303e5508502e90edd00b8ef0100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000200000000000000000000000010000000200010000000400000000000000000000000002000000000000000000000000000000020000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000f800f800a0377898d2f5bbfe29c8601e5ee0a3a5d53568bfbe77e2b726c98cf1242bfcfe6f',
                [
                  [
                    '0x00',  // event index
                    '0xf9016fb9016cf90169822000b90163f9016095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198af852964d657373616765287374722c696e742c627974657329b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303401f8f4b8f2f8f0b8396274703a2f2f3078332e69636f6e2f637837613063326464393735316535393261633466626436633730626435656335373465626631393861b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303490436f696e2f57726170706564436f696e01b867f86500b862f860aa687862366235373931626530623565663637303633623363313062383430666238313531346462326664aa307836353144353230353463386163346231393139316437343237433236333337393735363744333963c9c88449434f4e8200d4'
                  ]
                ]
            ]
        );
        
        const receiptProofs = [
            receiptProof
        ];
        
        let baseMsg = '0x' + convertEthRlpToIconRlp(
            rlp.encode(
                [
                    blockUpdates,
                    blockProof,
                    receiptProofs
                ]
            ),
            false
        );
        await truffleAssert.reverts(
            bmc.testHandleRelayMessage.call(bmv.address, prevBtpAddr, 0, baseMsg),
            'BMVRevertInvalidBlockUpdate: Invalid next validator hash'
        );
    });

    it('Handle relay message - Scenario 16: Revert if votes does not exist', async () => {
        const blockUpdate = [
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Block header
                        2,                 // version
                        21172,             // height
                        1624383652806088,  // timestamp
                        '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd',   // proposer
                        '0xe2ea2b413a46c6f35165faad7d570899d4deeb3658c19a04596d42d5473456eb',   // previous hash
                        '0xb3531419f3bf6d9f624f8bad9541ffbeac2e0883484087801e08e7c855bbd339',   // vote hash
                        '0xed9e644e59b2ff65446f5f3d7d77c27858facf8aeb3b969470d7499c79f9757c',   // next validatos hash
                        [[]],                                                                   // patch tx hash
                        [[]],                                                                   // tx hash
                        '0x00802070482c1a0f0884c2a060285c0c21041000221032043801128bc0e331a854363b2090c88000691c0c112684c040',    // logs bloom
                        rlp.encode(
                            [   // result
                                '0x5962b2791303f11a7e663bfba365162408ed03935e472f0ea3e668c268992a9b',    // state hash
                                [[]],                                                                    // patch receipt hash
                                '0x05649c24151f44c4a9dd10b652cd20ffbe2203b1ebba0d388342f999619d2af7',    // receipt hash
                                '0xf867a04f820eefa94c3e731d177461f260b90c6f7c71f78170fad578c136a12033b423a0c37eaafab80062deb7eafa82ffc42719604138eef0234da69f789956f7949d1da0bb87db4b20e1d46a2d8f0e6aea6e32fb0451c474a905942086e8e33b3e2b1ab8f800f800'  // extension data
                            ]
                        )
                    ]
                )
            ),
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Votes
                    
                    ]
                )
            ),
            // Next validators
            rlp.encode([
                '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd'
            ])
        ];
        
        const blockUpdates = [
            convertEthRlpToIconRlp(rlp.encode(blockUpdate))
        ];
        
        const blockProof = [[]]; // block proofs is empty
        
        const receiptProof = rlp.encode(
            [
                '0x00',   // tx index
                '0xf9014ab90147f90144822000b9013ef9013b0095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198a8303e5508303e5508502e90edd00b8ef0100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000200000000000000000000000010000000200010000000400000000000000000000000002000000000000000000000000000000020000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000f800f800a0377898d2f5bbfe29c8601e5ee0a3a5d53568bfbe77e2b726c98cf1242bfcfe6f',
                [
                  [
                    '0x00',  // event index
                    '0xf9016fb9016cf90169822000b90163f9016095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198af852964d657373616765287374722c696e742c627974657329b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303401f8f4b8f2f8f0b8396274703a2f2f3078332e69636f6e2f637837613063326464393735316535393261633466626436633730626435656335373465626631393861b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303490436f696e2f57726170706564436f696e01b867f86500b862f860aa687862366235373931626530623565663637303633623363313062383430666238313531346462326664aa307836353144353230353463386163346231393139316437343237433236333337393735363744333963c9c88449434f4e8200d4'
                  ]
                ]
            ]
        );
        
        const receiptProofs = [
            receiptProof
        ];
        
        let baseMsg = '0x' + convertEthRlpToIconRlp(
            rlp.encode(
                [
                    blockUpdates,
                    blockProof,
                    receiptProofs
                ]
            ),
            false
        );
        await truffleAssert.reverts(
            bmc.testHandleRelayMessage.call(bmv.address, prevBtpAddr, 0, baseMsg),
            'BMVRevertInvalidBlockUpdate: Not exists votes'
        );
    });

    it('Handle relay message - Scenario 17: Revert if votes contain invalid signatures', async () => {
        const validatorsList = [
            'hx5a05b58a25a1e5ea0f1d5715e1f655dffc1fb30a'
        ];
        let encodedValidators = rlp.encode(validatorsList.map(validator => validator.replace('hx', '0x00')));

        bmc = await MockBMC.new(praNet);
        dataValidator = await deployProxy(MockDataValidator);
        bmv = await deployProxy(
            MockBMV,
            [
                bmc.address,
                dataValidator.address,
                iconNet,
                encodedValidators,
                initOffset,
                initRootSize,
                initCacheSize,
                lastBlockHash
            ]
        );
    
        const blockUpdate = [
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Block header
                        2,                 // version
                        21172,             // height
                        1624383652806088,  // timestamp
                        '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd',   // proposer
                        '0xe2ea2b413a46c6f35165faad7d570899d4deeb3658c19a04596d42d5473456eb',   // previous hash
                        '0xb3531419f3bf6d9f624f8bad9541ffbeac2e0883484087801e08e7c855bbd339',   // vote hash
                        '0xed9e644e59b2ff65446f5f3d7d77c27858facf8aeb3b969470d7499c79f9757c',   // next validatos hash
                        [[]],                                                                   // patch tx hash
                        [[]],                                                                   // tx hash
                        '0x00802070482c1a0f0884c2a060285c0c21041000221032043801128bc0e331a854363b2090c88000691c0c112684c040',    // logs bloom
                        rlp.encode(
                            [   // result
                                '0x5962b2791303f11a7e663bfba365162408ed03935e472f0ea3e668c268992a9b',    // state hash
                                [[]],                                                                    // patch receipt hash
                                '0x05649c24151f44c4a9dd10b652cd20ffbe2203b1ebba0d388342f999619d2af7',    // receipt hash
                                '0xf867a04f820eefa94c3e731d177461f260b90c6f7c71f78170fad578c136a12033b423a0c37eaafab80062deb7eafa82ffc42719604138eef0234da69f789956f7949d1da0bb87db4b20e1d46a2d8f0e6aea6e32fb0451c474a905942086e8e33b3e2b1ab8f800f800'  // extension data
                            ]
                        )
                    ]
                )
            ),
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Votes
                        '0x00', // round
                        [  // block part set id
                            1,
                            '0x323afffff77a4432b5e0fef58fa27ec793514ce6bae42020c369f28c12cfed10'
                        ],
                        [    // vote items
                            [ 
                                1624383654796604, // time stamp
                                '0x8ac623fb4054e748d5e212ec01c0c263256017b3635ddc412f033a54056cf8306d03173e616605c8717ea5e7a4429cc1a53a0ae1e85c1e70c754d9a02b2bd13800'  // signature
                            ]
                        ]
                    ]
                )
            ),
            // Next validators
            rlp.encode([
                '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd'
            ])
        ];
        
        const blockUpdates = [
            convertEthRlpToIconRlp(rlp.encode(blockUpdate))
        ];
        
        const blockProof = [[]]; // block proofs is empty
        
        const receiptProof = rlp.encode(
            [
                '0x00',   // tx index
                '0xf9014ab90147f90144822000b9013ef9013b0095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198a8303e5508303e5508502e90edd00b8ef0100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000200000000000000000000000010000000200010000000400000000000000000000000002000000000000000000000000000000020000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000f800f800a0377898d2f5bbfe29c8601e5ee0a3a5d53568bfbe77e2b726c98cf1242bfcfe6f',
                [
                  [
                    '0x00',  // event index
                    '0xf9016fb9016cf90169822000b90163f9016095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198af852964d657373616765287374722c696e742c627974657329b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303401f8f4b8f2f8f0b8396274703a2f2f3078332e69636f6e2f637837613063326464393735316535393261633466626436633730626435656335373465626631393861b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303490436f696e2f57726170706564436f696e01b867f86500b862f860aa687862366235373931626530623565663637303633623363313062383430666238313531346462326664aa307836353144353230353463386163346231393139316437343237433236333337393735363744333963c9c88449434f4e8200d4'
                  ]
                ]
            ]
        );
        
        const receiptProofs = [
            receiptProof
        ];
        
        let baseMsg = '0x' + convertEthRlpToIconRlp(
            rlp.encode(
                [
                    blockUpdates,
                    blockProof,
                    receiptProofs
                ]
            ),
            false
        );
        await truffleAssert.reverts(
            bmc.testHandleRelayMessage.call(bmv.address, prevBtpAddr, 0, baseMsg),
            'BMVRevertInvalidVotes: Invalid signature'
        );
    });

    it('Handle relay message - Scenario 18: Revert if votes are duplicate', async () => {    
        const blockUpdate = [
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Block header
                        2,                 // version
                        21172,             // height
                        1624383652806088,  // timestamp
                        '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd',   // proposer
                        '0xe2ea2b413a46c6f35165faad7d570899d4deeb3658c19a04596d42d5473456eb',   // previous hash
                        '0xb3531419f3bf6d9f624f8bad9541ffbeac2e0883484087801e08e7c855bbd339',   // vote hash
                        '0xed9e644e59b2ff65446f5f3d7d77c27858facf8aeb3b969470d7499c79f9757c',   // next validatos hash
                        [[]],                                                                   // patch tx hash
                        [[]],                                                                   // tx hash
                        '0x00802070482c1a0f0884c2a060285c0c21041000221032043801128bc0e331a854363b2090c88000691c0c112684c040',    // logs bloom
                        rlp.encode(
                            [   // result
                                '0x5962b2791303f11a7e663bfba365162408ed03935e472f0ea3e668c268992a9b',    // state hash
                                [[]],                                                                    // patch receipt hash
                                '0x05649c24151f44c4a9dd10b652cd20ffbe2203b1ebba0d388342f999619d2af7',    // receipt hash
                                '0xf867a04f820eefa94c3e731d177461f260b90c6f7c71f78170fad578c136a12033b423a0c37eaafab80062deb7eafa82ffc42719604138eef0234da69f789956f7949d1da0bb87db4b20e1d46a2d8f0e6aea6e32fb0451c474a905942086e8e33b3e2b1ab8f800f800'  // extension data
                            ]
                        )
                    ]
                )
            ),
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Votes
                        '0x00', // round
                        [  // block part set id
                            1,
                            '0x323afffff77a4432b5e0fef58fa27ec793514ce6bae42020c369f28c12cfed10'
                        ],
                        [    // vote items
                            [ 
                                1624383654796604, // time stamp
                                '0x8ac623fb4054e748d5e212ec01c0c263256017b3635ddc412f033a54056cf8306d03173e616605c8717ea5e7a4429cc1a53a0ae1e85c1e70c754d9a02b2bd13800'  // signature
                            ],
                            [ 
                                1624383654796604, // time stamp
                                '0x8ac623fb4054e748d5e212ec01c0c263256017b3635ddc412f033a54056cf8306d03173e616605c8717ea5e7a4429cc1a53a0ae1e85c1e70c754d9a02b2bd13800'  // signature
                            ]
                        ]
                    ]
                )
            ),
            // Next validators
            rlp.encode([
                '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd'
            ])
        ];
        
        const blockUpdates = [
            convertEthRlpToIconRlp(rlp.encode(blockUpdate))
        ];
        
        const blockProof = [[]]; // block proofs is empty
        
        const receiptProof = rlp.encode(
            [
                '0x00',   // tx index
                '0xf9014ab90147f90144822000b9013ef9013b0095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198a8303e5508303e5508502e90edd00b8ef0100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000200000000000000000000000010000000200010000000400000000000000000000000002000000000000000000000000000000020000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000f800f800a0377898d2f5bbfe29c8601e5ee0a3a5d53568bfbe77e2b726c98cf1242bfcfe6f',
                [
                  [
                    '0x00',  // event index
                    '0xf9016fb9016cf90169822000b90163f9016095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198af852964d657373616765287374722c696e742c627974657329b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303401f8f4b8f2f8f0b8396274703a2f2f3078332e69636f6e2f637837613063326464393735316535393261633466626436633730626435656335373465626631393861b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303490436f696e2f57726170706564436f696e01b867f86500b862f860aa687862366235373931626530623565663637303633623363313062383430666238313531346462326664aa307836353144353230353463386163346231393139316437343237433236333337393735363744333963c9c88449434f4e8200d4'
                  ]
                ]
            ]
        );
        
        const receiptProofs = [
            receiptProof
        ];
        
        let baseMsg = '0x' + convertEthRlpToIconRlp(
            rlp.encode(
                [
                    blockUpdates,
                    blockProof,
                    receiptProofs
                ]
            ),
            false
        );
        await truffleAssert.reverts(
            bmc.testHandleRelayMessage.call(bmv.address, prevBtpAddr, 0, baseMsg),
            'BMVRevertInvalidVotes: Duplicated votes'
        );
    });

    it('Handle relay message - Scenario 19: Revert if number of votes is less than 2/3', async () => {
        const validatorsList = [
            'hxb6b5791be0b5ef67063b3c10b840fb81514db2fd',
            'hx5a05b58a25a1e5ea0f1d5715e1f655dffc1fb30a'
        ];
        let encodedValidators = rlp.encode(validatorsList.map(validator => validator.replace('hx', '0x00')));

        bmc = await MockBMC.new(praNet);
        dataValidator = await deployProxy(MockDataValidator);
        bmv = await deployProxy(
            MockBMV,
            [
                bmc.address,
                dataValidator.address,
                iconNet,
                encodedValidators,
                initOffset,
                initRootSize,
                initCacheSize,
                lastBlockHash
            ]
        );
    
        const blockUpdate = [
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Block header
                        2,                 // version
                        21172,             // height
                        1624383652806088,  // timestamp
                        '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd',   // proposer
                        '0xe2ea2b413a46c6f35165faad7d570899d4deeb3658c19a04596d42d5473456eb',   // previous hash
                        '0xb3531419f3bf6d9f624f8bad9541ffbeac2e0883484087801e08e7c855bbd339',   // vote hash
                        '0xed9e644e59b2ff65446f5f3d7d77c27858facf8aeb3b969470d7499c79f9757c',   // next validatos hash
                        [[]],                                                                   // patch tx hash
                        [[]],                                                                   // tx hash
                        '0x00802070482c1a0f0884c2a060285c0c21041000221032043801128bc0e331a854363b2090c88000691c0c112684c040',    // logs bloom
                        rlp.encode(
                            [   // result
                                '0x5962b2791303f11a7e663bfba365162408ed03935e472f0ea3e668c268992a9b',    // state hash
                                [[]],                                                                    // patch receipt hash
                                '0x05649c24151f44c4a9dd10b652cd20ffbe2203b1ebba0d388342f999619d2af7',    // receipt hash
                                '0xf867a04f820eefa94c3e731d177461f260b90c6f7c71f78170fad578c136a12033b423a0c37eaafab80062deb7eafa82ffc42719604138eef0234da69f789956f7949d1da0bb87db4b20e1d46a2d8f0e6aea6e32fb0451c474a905942086e8e33b3e2b1ab8f800f800'  // extension data
                            ]
                        )
                    ]
                )
            ),
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Votes
                        '0x00', // round
                        [  // block part set id
                            1,
                            '0x323afffff77a4432b5e0fef58fa27ec793514ce6bae42020c369f28c12cfed10'
                        ],
                        [    // vote items
                            [ 
                                1624383654796604, // time stamp
                                '0x8ac623fb4054e748d5e212ec01c0c263256017b3635ddc412f033a54056cf8306d03173e616605c8717ea5e7a4429cc1a53a0ae1e85c1e70c754d9a02b2bd13800'  // signature
                            ]
                        ]
                    ]
                )
            ),
            // Next validators
            rlp.encode([
                '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd'
            ])
        ];
        
        const blockUpdates = [
            convertEthRlpToIconRlp(rlp.encode(blockUpdate))
        ];
        
        const blockProof = [[]]; // block proofs is empty
        
        const receiptProof = rlp.encode(
            [
                '0x00',   // tx index
                '0xf9014ab90147f90144822000b9013ef9013b0095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198a8303e5508303e5508502e90edd00b8ef0100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000200000000000000000000000010000000200010000000400000000000000000000000002000000000000000000000000000000020000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000f800f800a0377898d2f5bbfe29c8601e5ee0a3a5d53568bfbe77e2b726c98cf1242bfcfe6f',
                [
                  [
                    '0x00',  // event index
                    '0xf9016fb9016cf90169822000b90163f9016095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198af852964d657373616765287374722c696e742c627974657329b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303401f8f4b8f2f8f0b8396274703a2f2f3078332e69636f6e2f637837613063326464393735316535393261633466626436633730626435656335373465626631393861b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303490436f696e2f57726170706564436f696e01b867f86500b862f860aa687862366235373931626530623565663637303633623363313062383430666238313531346462326664aa307836353144353230353463386163346231393139316437343237433236333337393735363744333963c9c88449434f4e8200d4'
                  ]
                ]
            ]
        );
        
        const receiptProofs = [
            receiptProof
        ];
        
        let baseMsg = '0x' + convertEthRlpToIconRlp(
            rlp.encode(
                [
                    blockUpdates,
                    blockProof,
                    receiptProofs
                ]
            ),
            false
        );
        await truffleAssert.reverts(
            bmc.testHandleRelayMessage.call(bmv.address, prevBtpAddr, 0, baseMsg),
            'BMVRevertInvalidVotes: Require votes > 2/3'
        );
    });

    it('Handle relay message - Scenario 20: Revert if sequence number of  BTP message is higher or lower than expected', async () => {
        const blockUpdate = [
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Block header
                        2,                 // version
                        21172,             // height
                        1624383652806088,  // timestamp
                        '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd',   // proposer
                        '0xe2ea2b413a46c6f35165faad7d570899d4deeb3658c19a04596d42d5473456eb',   // previous hash
                        '0xb3531419f3bf6d9f624f8bad9541ffbeac2e0883484087801e08e7c855bbd339',   // vote hash
                        '0xed9e644e59b2ff65446f5f3d7d77c27858facf8aeb3b969470d7499c79f9757c',   // next validatos hash
                        [[]],                                                                   // patch tx hash
                        [[]],                                                                   // tx hash
                        '0x00802070482c1a0f0884c2a060285c0c21041000221032043801128bc0e331a854363b2090c88000691c0c112684c040',    // logs bloom
                        rlp.encode(
                            [   // result
                                '0x5962b2791303f11a7e663bfba365162408ed03935e472f0ea3e668c268992a9b',    // state hash
                                [[]],                                                                    // patch receipt hash
                                '0x05649c24151f44c4a9dd10b652cd20ffbe2203b1ebba0d388342f999619d2af7',    // receipt hash
                                '0xf867a04f820eefa94c3e731d177461f260b90c6f7c71f78170fad578c136a12033b423a0c37eaafab80062deb7eafa82ffc42719604138eef0234da69f789956f7949d1da0bb87db4b20e1d46a2d8f0e6aea6e32fb0451c474a905942086e8e33b3e2b1ab8f800f800'  // extension data
                            ]
                        )
                    ]
                )
            ),
            convertEthRlpToIconRlp(
                rlp.encode(
                    [ // Votes
                        '0x00', // round
                        [  // block part set id
                            1,
                            '0x323afffff77a4432b5e0fef58fa27ec793514ce6bae42020c369f28c12cfed10'
                        ],
                        [    // vote items
                            [ 
                                1624383654796604, // time stamp
                                '0x8ac623fb4054e748d5e212ec01c0c263256017b3635ddc412f033a54056cf8306d03173e616605c8717ea5e7a4429cc1a53a0ae1e85c1e70c754d9a02b2bd13800'  // signature
                            ]
                        ]
                    ]
                )
            ),
            // Next validators
            rlp.encode([
                '0x00b6b5791be0b5ef67063b3c10b840fb81514db2fd'
            ])
        ];
        
        const blockUpdates = [
            convertEthRlpToIconRlp(rlp.encode(blockUpdate))
        ];
        
        const blockProof = [[]]; // block proofs is empty
        
        const receiptProof = rlp.encode(
            [
                '0x00',   // tx index
                '0xf9014ab90147f90144822000b9013ef9013b0095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198a8303e5508303e5508502e90edd00b8ef0100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000200000000000000000000000010000000200010000000400000000000000000000000002000000000000000000000000000000020000000000000000000000000000000000000000000000000020000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000008000000000000000000000000000000000000000000000000000000f800f800a0377898d2f5bbfe29c8601e5ee0a3a5d53568bfbe77e2b726c98cf1242bfcfe6f',
                [
                  [
                    '0x00',  // event index
                    '0xf9016fb9016cf90169822000b90163f9016095017a0c2dd9751e592ac4fbd6c70bd5ec574ebf198af852964d657373616765287374722c696e742c627974657329b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303401f8f4b8f2f8f0b8396274703a2f2f3078332e69636f6e2f637837613063326464393735316535393261633466626436633730626435656335373465626631393861b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303490436f696e2f57726170706564436f696e01b867f86500b862f860aa687862366235373931626530623565663637303633623363313062383430666238313531346462326664aa307836353144353230353463386163346231393139316437343237433236333337393735363744333963c9c88449434f4e8200d4'
                  ]
                ]
            ]
        );
        
        const receiptProofs = [
            receiptProof
        ];
        
        let baseMsg = '0x' + convertEthRlpToIconRlp(
            rlp.encode(
                [
                    blockUpdates,
                    blockProof,
                    receiptProofs
                ]
            ),
            false
        );
        await truffleAssert.reverts(
            bmc.testHandleRelayMessage.call(bmv.address, prevBtpAddr, 3, baseMsg),
            'BMVRevertInvalidSequence'
        );
    });
});

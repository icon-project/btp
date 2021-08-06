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
    const praNet = '0x97.bsc';
    const prevBtpAddr = 'btp://0x3.icon/cx7a0c2dd9751e592ac4fbd6c70bd5ec574ebf198a'
    const validatorsList = [
        'hxb6b5791be0b5ef67063b3c10b840fb81514db2fd'
    ];
    let encodedValidators = rlp.encode(validatorsList.map(validator => validator.replace('hx', '0x00')));

    const lastBlockHash = '0x34c27cf1b000a6256f1e63631c43669a0620c3646e39747d60f8ba65205802ca';
    const btpMsgs = ['0xf8f0b8396274703a2f2f3078332e69636f6e2f637837613063326464393735316535393261633466626436633730626435656335373465626631393861b8386274703a2f2f3078382e7072612f30784630653436383437633862464431323243346235454545314434343934464637433546433531303490436f696e2f57726170706564436f696e01b867f86500b862f860aa687862366235373931626530623565663637303633623363313062383430666238313531346462326664aa307836353144353230353463386163346231393139316437343237433236333337393735363744333963c9c88449434f4e8200d4'];
    const initOffset = 917454;
    const initRootSize = 8;
    const initCacheSize = 8;

    let link1 = 'btp://0x97.bsc/cxe284d1774ade175c273b52742c3763ec5fe43844'
    let link = "btp://0x03.icon/cxe284d1774ade175c273b52742c3763ec5fe43844"
    
    let bmv, dataValidator, bmc;

    beforeEach(async () => {
        bmc = await MockBMC.new(praNet);
        console.log(bmc.address)
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
 
    it('Get BMV info - Scenario 4: Get BMV status', async () => {
           let base64Msg = "-Qjv-QWWuQK8-QK5uQGh-QGeAoMN_82HBciUF9L_t5UA1jxMc7Yj6X9nQH1oevTvz-SGpRWgAbDHCRSeZdrXrzjRtViv2Siez6SoZe4-ag8sNIgnMRKgUaJiUgJkAvvz1WvOv9bYhcqDvi1eXcHNHr6Lx_JfrligZ3LUBWCxhGBqqeyRzWgy1JVM3Cqk4bkFTVgzb8VkuP34AKDd89pbri3xcTYkao5LpHL7497EHvJpi2OIHi4hWG0amaIBACBwSCwaDwMEQQAwiGw6HxAAQqIwaJxSLxiMwcgRqBwEuND4zqDpWVmCjkSo_sGOr5BP9VObSozH7rbAYb7nhy043b09qPgAoK5eD6gCuCtfaCPAqo262NbArdJizA7UWV1lixQw7Lj2uIj4hqAKrtuwLI_LNEiboI2Qesus2K69ke-yQfwwV6gGcL5f76DOqWKZsOCc1dJ7M_l5LgRINyydgPW1PZth-cofTpsvD_gAoLDrX8AXFxwYh6W64pPIjmVnH0NzRs94pznds_PDanqqoPABSOCQaKpMEzQDXr6WutDittPI9yuZOhuPADFEF00TuQEQ-QENAOIBoFXgx480SosI1zbxKVYywb8THL7uEPkpTXv7e270pr12-Of4S4cFyJQX8ZN_uEHn8UHAopnHhMUi15usTBjy0whPds6umiKbumkyXDnHonyGBZ3p-ctAjcek_bdQ2ZzeUB5rhd_3R18raIYl62awAPhLhwXIlBfxjIK4QcFhjbhFi5ynR3lsYjfyubiKtHYU2MAuZfC0YDxYJRGEC2AItrKC0rzulH4s9GBPkGRaINtTiFM-nr8UrfDmYPwA-EuHBciUF_GGcrhBHAVjqDWDftM628sJlbISdBWK-b_YETQmrkbB3COYDt8FkB8iCtsrauZCldbpREDcXEFO31vH1ZEIjqHJox9T2QH4ALkC1PkC0bkBufkBtgKDDf_OhwXIlBfxkACVAHiC2s4l_36UfTolF4oqEWKHTP3coDTCfPGwAKYlbx5jYxxDZpoGIMNkbjl0fWD4umUgWALKoMaHh2xxH8xzoMchaYy8x1ISl3tLefDHrglk5cDy3F99oGdy1AVgsYRgaqnskc1oMtSVTNwqpOG5BU1YM2_FZLj9-ACg2nqomUClMqK4tC8ZFO7glJViUEpEIILQWwGX95dhGd-4OQCAIGQAFAwRA4SEITDIbA4MACBDocBInDYhDBBFotCIHAo3IJDE4xDI7IoSgJNJ5XLJRLYjL4HAQLjQ-M6g7FBB-vv9nRnqq06fGZrLskAJnP1yZazm0WVUwb3pnMD4AKCdcUqzpD3i5XowB2vJ3YXP8pj813h7UU5lO5cBl1Zf6biI-IagFNlGvFRYLPiTA6ZcWAIxCTMh5SjVVIF5VBhsjGnh45mgzqlimbDgnNXSezP5eS4ESDcsnYD1tT2bYfnKH06bLw_4AKCw61_AFxccGIeluuKTyI5lZx9Dc0bPeKc53bPzw2p6qqDwAUjgkGiqTBM0A16-lrrQ4rbTyPcrmTobjwAxRBdNE7kBEPkBDQDiAaB78JX8CmJtPdn2qwmiMntIRIxc-9Sw8jxHe3CvFtUyWPjn-EuHBciUGA_-7bhBqFOFTesHqrrOoL9EghrbmrMFCzXNS9ihbp_b381mvCsd70ebNS6NRLpEhlgItyVClMznVc2trlNG5FxNuf9dygH4S4cFyJQYD_ybuEG1HZQkew3PU6cyJZG0_mFa8NXBHQmmUN_oKTRFNRypii8_dksui_p5DADlGL7xxDGCR5rln9CQq5dyHXa-4nUcAfhLhwXIlBgQCe24QXim0OY2j9DLWJxPb6TOFy4puEBlhPNSB929ezNioq_Ofn8bcL3VpK6YfJ0jEdMQBdv34oqUPWDGqxnC_QhCdH4B-AD4APkDUbkDTvkDSwG5AcT5AcGj4hCgdepiau3myZOZ_Arw0zstTz8pGhhTm-4meqZMXKzuPES4U_hRoBm81oI6QeDBOqzunyV4eEY9JJsTgJlGnv_K6RfyUuVdoEEWpA8VTBglCrpKRFRTEuCuQhmrRJYABsuiyjo_LKDTgICAgICAgICAgICAgICAuQFF-QFCILkBPvkBOwCVAf-ag0Yip4BfznuS-ec5DyXaY-J4gwZ9XYMGfV2FAukO3QC47wEAAABAAgAACAAAAAAAEAAAAAAAAAAAAAAAAAAAAgAAQAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAgAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA-AD4AKC4PJeQoXeGerxzJV9cvPq_YO76UB3eJSNC0fbRa5m4ifkBgPkBfQC5AXn5AXa5AXP5AXCCIAC5AWr5AWeVAeKE0XdK3hdcJztSdCw3Y-xf5DhE-FOWTWVzc2FnZShzdHIsaW50LGJ5dGVzKbg5YnRwOi8vMHg5Ny5ic2MvMHhBYUZjOEVlYUVFOGQ5QzhiRDMyNjJDQ0UzRDczRTU2RGVFM0ZCNzc2A_j6uPj49rg6YnRwOi8vMHgwMy5pY29uL2N4ZTI4NGQxNzc0YWRlMTc1YzI3M2I1Mjc0MmMzNzYzZWM1ZmU0Mzg0NLg5YnRwOi8vMHg5Ny5ic2MvMHhBYUZjOEVlYUVFOGQ5QzhiRDMyNjJDQ0UzRDczRTU2RGVFM0ZCNzc2iFRva2VuQlNIB7hz-HEA-G6qaHgxNTA5NjhjZjczMTE0N2ZhNDAzOGY1OWJlNWYwZjI2M2YyNGJjYTgyuDlidHA6Ly8weDk3LmJzYy8weGI2ODc1MDBjZTc3OTkwNjA2Mzk2YzMzYTcyYjJFRTdiNUVEOTY1QkbHxoNFVEgFAA=="
           let msgs = await bmv.handleRelayMessage(link,link1,2,  base64Msg);
           
            console.log(msgs);
    });
 
});

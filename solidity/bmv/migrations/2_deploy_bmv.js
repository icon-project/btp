const BtpMessageVerifier = artifacts.require('BtpMessageVerifier');
require('dotenv').config({ path: '../.env' });

let {
    BMC_ADDR,
    SRC_NETWORK_ID,
    NETWORK_TYPE_ID,
    FIRST_BLOCK_UPDATE,
    SEQUENCE_OFFSET
} = process.env;

module.exports = async function (deployer, network, accounts) {
    if (network === 'development') {
        BMC_ADDR = accounts[0];
    }

    deployer.deploy(BtpMessageVerifier,
        // constructor args
        BMC_ADDR,
        SRC_NETWORK_ID,
        NETWORK_TYPE_ID,
        FIRST_BLOCK_UPDATE,
        SEQUENCE_OFFSET
    );
};

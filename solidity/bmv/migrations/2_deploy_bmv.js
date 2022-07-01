const BtpMessageVerifier = artifacts.require("BtpMessageVerifier");
const RLPEncode = artifacts.require("RLPEncode");
const RLPReader = artifacts.require("RLPReader");
const BlockUpdateLib = artifacts.require("BlockUpdateLib");
const MerkleTreeLib = artifacts.require("MerkleTreeLib");
const MessageProofLib = artifacts.require("MessageProofLib");
const RelayMessageLib = artifacts.require("RelayMessageLib");
const Utils = artifacts.require("Utils");

const { deployProxy } = require('@openzeppelin/truffle-upgrades');
require('dotenv').config({ path: '../.env' });

module.exports = async function (deployer, network, accounts) {
    if (network === 'development') {
        process.env.BMC_ADDR = accounts[0];
    }

    deployer.deploy(RLPReader);

    deployer.link(RLPReader, RelayMessageLib);
    deployer.deploy(RelayMessageLib);

    deployer.deploy(MerkleTreeLib);
    deployer.deploy(RLPEncode);

    deployer.link(MerkleTreeLib, BlockUpdateLib);
    deployer.link(RLPEncode, BlockUpdateLib);
    deployer.deploy(BlockUpdateLib);

    deployer.link(RLPReader, MessageProofLib);
    deployer.deploy(MessageProofLib);

    deployer.link(RelayMessageLib, BtpMessageVerifier);
    deployer.deploy(Utils);
    deployer.link(Utils, BtpMessageVerifier);

    await deployProxy(BtpMessageVerifier, [
        process.env.BMC_ADDR,
        process.env.SRC_NETWORK_ID,
        process.env.NETWORK_TYPE_ID,
        process.env.NETWORK_ID,
        process.env.FIRST_BLOCK_UPDATE,
        process.env.SEQUENCE_OFFSET
    ], { deployer });
};

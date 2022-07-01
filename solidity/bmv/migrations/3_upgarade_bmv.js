const { upgradeProxy } = require('@openzeppelin/truffle-upgrades');

const BtpMessageVerifier = artifacts.require('BtpMessageVerifier');
const BtpMessageVerifierV2 = artifacts.require("BtpMessageVerifierV2");
const RelayMessageLib = artifacts.require("RelayMessageLib");
const Utils = artifacts.require("Utils");

// only for upgrade
module.exports = async function (deployer, network) {
    let old = await BtpMessageVerifier.deployed();

    deployer.link(RelayMessageLib, BtpMessageVerifierV2);
    deployer.link(Utils, BtpMessageVerifierV2);

    await upgradeProxy(old.address, BtpMessageVerifierV2, { deployer });
};

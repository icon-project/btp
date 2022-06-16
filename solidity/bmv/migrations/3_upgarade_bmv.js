const bmv = artifacts.require("BtpMessageVerifierV2");
const { upgradeProxy } = require('@openzeppelin/truffle-upgrades');

module.exports = async function (deployer, network) {
    const UPGRADE_ADDRESS = process.env.BMV_UPGRADE_ADDRESS;
    if (UPGRADE_ADDRESS === undefined) {
        return;
    }
    await upgradeProxy(UPGRADE_ADDRESS, bmv, { deployer });
};

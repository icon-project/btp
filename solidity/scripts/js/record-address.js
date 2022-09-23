const path = require('path');
const { readConfig, writeConfig } = require('./utils');

module.exports = async (cb) => {
    let argv = process.argv.slice(6, process.argv.length);
    let cfg = await readConfig();
    if (cfg.addresses === undefined) {
        cfg.addresses = {};
    }

    switch (argv[0]) {
        case 'bmc':
            const BMCManagement = artifacts.require('BMCManagement');
            const BMCPeriphery = artifacts.require('BMCPeriphery');
            cfg.addresses.bmc_management = BMCManagement.address;
            cfg.addresses.bmc_periphery = BMCPeriphery.address;
            break;

        case 'bmv':
            const BtpMessageVerifier = artifacts.require('BtpMessageVerifier');
            cfg.addresses.bmv = BtpMessageVerifier.address;
            break;

        case 'bsh':
            const DebugBSH = artifacts.require('DebugBSH');
            cfg.addresses.bsh = DebugBSH.address;
            break;

        default:
            throw `unknown option: ${argv[0]}`;
    }

    await writeConfig(cfg, cb);
}

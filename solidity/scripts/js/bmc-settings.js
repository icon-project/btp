const { readConfig } = require('./utils');

const BMCManagement = artifacts.require('BMCManagement');

module.exports = async (cb) => {
    let cfg = await readConfig();
    let bmc = await BMCManagement.deployed();

    const TARGET_NETWORK_ID=cfg.target.network_id;
    const BMV_ADDRESS=cfg.addresses.bmv
    console.log(`Add Verifier - NetworkID(${TARGET_NETWORK_ID}), BMV Address(${BMV_ADDRESS})`);
    await bmc.addVerifier(TARGET_NETWORK_ID, BMV_ADDRESS);

    const LINK='btp://' + cfg.target.network_id + '/' + cfg.target.bmc_address;
    console.log(`Add Link - Link(${LINK})`);
    await bmc.addLink(LINK);

    const TARGET_BMC_BTPADRESS='btp://' + cfg.target.network_id + '/' + cfg.target.bmc_address;
    const RELAYER_ADDRESS=cfg.relayer.address;
    console.log(`Add Relayer - Target BMC BTPAddress(${TARGET_BMC_BTPADRESS}), Relayer Address(${RELAYER_ADDRESS})`);
    await bmc.addRelay(TARGET_BMC_BTPADRESS, RELAYER_ADDRESS);

    const SERVICE_NAME=cfg.deployment.service_name;
    const SERVICE_ADDRESS=cfg.addresses.bsh;
    console.log(`Add Service - Name(${SERVICE_NAME}), Address(${SERVICE_ADDRESS})`);
    await bmc.addService(SERVICE_NAME, SERVICE_ADDRESS);

    cb();
}

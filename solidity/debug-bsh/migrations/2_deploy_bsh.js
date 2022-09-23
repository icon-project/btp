const DebugBSH = artifacts.require("DebugBSH");

let {
    BMC_ADDR,
    BSH_SERVICE_NAME
} = process.env;

module.exports = function (deployer, network, accounts) {
    if (network === 'development') {
        BMC_ADDR = accounts[0];
        BSH_SERVICE_NAME = 'debugbsh';
    }

    if (BMC_ADDR == undefined) {
        console.warn('\x1b[33m%s\x1b[0m', 'No BMC Address: Set `BMC_ADDR` environment variable');
        return;
    }
    if (BSH_SERVICE_NAME == undefined) {
        console.warn('\x1b[33m%s\x1b[0m', 'No Service Name: Set `BSH_SERVICE_NAME` environment variable');
        return;
    }

    deployer.deploy(DebugBSH, BMC_ADDR, BSH_SERVICE_NAME);
};

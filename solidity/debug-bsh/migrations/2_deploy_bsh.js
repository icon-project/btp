const DebugBSH = artifacts.require("DebugBSH");

module.exports = function (deployer, network) {
    if (process.env.BMC_ADDR == undefined) {
        console.warn('\x1b[33m%s\x1b[0m', 'No BMC Address: Set `BMC_ADDR` environment variable');
    }
    deployer.deploy(DebugBSH, process.env.BMC_ADDR, "debug-bsh");
};

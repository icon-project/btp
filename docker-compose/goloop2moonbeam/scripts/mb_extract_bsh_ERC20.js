const BSHPeriphery = artifacts.require("BSHPeriphery");
const BSHCore = artifacts.require("BSHCore");
const fs = require('fs')

module.exports = async function (callback) {
    try {
        await BSHCore.deployed();
        await BSHPeriphery.deployed();

        fs.writeFileSync(process.env.CONFIG_DIR + "/bsh_core_erc20.moonbeam", BSHCore.address, function (err, data) {
            if (err) {
                return console.log(err);
            }
        });
        fs.writeFileSync(process.env.CONFIG_DIR + "/bsh_erc20.moonbeam", BSHPeriphery.address, function (err, data) {
            if (err) {
                return console.log(err);
            }
        });
    }
    catch (error) {
        console.log(error)
    }
    callback()
}

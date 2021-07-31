const BMCManagement = artifacts.require('BMCManagement');
const BMCPeriphery = artifacts.require('BMCPeriphery');
const fs = require('fs')

module.exports = async function (callback) {
    try {
        await BMCManagement.deployed();
        await BMCPeriphery.deployed();

        fs.writeFileSync(process.env.CONFIG_DIR + "/bmc.moonbeam", BMCPeriphery.address, function (err, data) {
            if (err) {
                return console.log(err);
            }
        });
        fs.writeFileSync(process.env.CONFIG_DIR + "/bmc_management.moonbeam", BMCManagement.address, function (err, data) {
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

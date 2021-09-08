const BMCPeriphery = artifacts.require('BMCPeriphery');
const fs = require('fs')

module.exports = async function (callback) {
    try {
        const bmcPeriphery = await BMCPeriphery.deployed();
        let linkStats = await bmcPeriphery.getStatus(process.env.ICON_BTP_ADDRESS);
        var offset = {
            'offsetMTA': linkStats.verifier.offsetMTA
        }
        fs.writeFileSync(process.env.CONFIG_DIR + "/bmc_linkstats.moonbeam", JSON.stringify(offset), function (err, data) {
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
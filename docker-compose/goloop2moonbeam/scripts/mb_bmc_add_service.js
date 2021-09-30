const BMCManagement = artifacts.require('BMCManagement');
const fs = require('fs');

module.exports = async function (callback) {
    try {
        const bmcManagement = await BMCManagement.deployed();
        const addService = await bmcManagement.addService("nativecoin", process.env.BSH_MOONBEAM)
        const addRelay = await bmcManagement.addRelay(process.env.ICON_BTP_ADDRESS, [process.env.RELAY_ADDRESS])

        fs.writeFileSync(process.env.CONFIG_DIR + "/tx.moonbeam.addService", addService.tx);
        fs.writeFileSync(process.env.CONFIG_DIR + "/tx.moonbeam.addRelay", addRelay.tx);
    }
    catch (error) {
        console.log(error)
    }
    callback()
}

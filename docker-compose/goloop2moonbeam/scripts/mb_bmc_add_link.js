const BMCManagement = artifacts.require('BMCManagement');
const fs = require('fs');

module.exports = async function (callback) {
    try {
        const bmcManagement = await BMCManagement.deployed();
        const addLink = await bmcManagement.addLink(process.env.ICON_BTP_ADDRESS);
        const setLink = await bmcManagement.setLink(process.env.ICON_BTP_ADDRESS, 3000, 5, 3)

        fs.writeFileSync(process.env.CONFIG_DIR + "/tx.moonbeam.addLink", addLink.tx);
        fs.writeFileSync(process.env.CONFIG_DIR + "/tx.moonbeam.setLink", setLink.tx);
    }
    catch (error) {
        console.log(error)
    }
    callback()
}

const BMCManagement = artifacts.require('BMCManagement');
const fs = require('fs');

module.exports = async function (callback) {
    try {
        const bmcManagement = await BMCManagement.deployed();
        const addVerifier = await bmcManagement.addVerifier(process.env.ICON_NET, process.env.BMV_MOONBEAM);
        fs.writeFileSync(process.env.CONFIG_DIR + "/tx.moonbeam.addVerifier", addVerifier.tx);
    }
    catch (error) {
        console.log(error)
    }
    callback()
}

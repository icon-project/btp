const BMCManagement = artifacts.require('BMCManagement');
const { assert } = require('chai');

module.exports = async function (callback) {
    try {
        const bmcManagement = await BMCManagement.deployed();
        await bmcManagement.addLink(process.env.ICON_BTP_ADDRESS);
        await bmcManagement.setLink(process.env.ICON_BTP_ADDRESS, 3000, 5, 3)
    }
    catch (error) {
        console.log(error)
    }
    callback()
}

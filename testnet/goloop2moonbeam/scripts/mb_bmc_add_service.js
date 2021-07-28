const BMCManagement = artifacts.require('BMCManagement');
const { assert } = require('chai');

module.exports = async function (callback) {
    try {
        const bmcManagement = await BMCManagement.deployed();
        await bmcManagement.addService("CoinTransfer", process.env.BSH_MOONBEAM)
        await bmcManagement.addRelay(process.env.ICON_BTP_ADDRESS, [process.env.RELAY_ADDRESS])
    }
    catch (error) {
        console.log(error)
    }
    callback()
}

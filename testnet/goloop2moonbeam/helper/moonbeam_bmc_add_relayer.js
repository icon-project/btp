const BMCManagement = artifacts.require('BMCManagement');

module.exports = async function (callback) {
    try {
        const bmcManagement = await BMCManagement.deployed();
        await bmcManagement.addVerifier(process.env.ICON_NET, process.env.BMV_MOONBEAM);
    }
    catch (error) {
        console.log(error)
    }

    callback()
}

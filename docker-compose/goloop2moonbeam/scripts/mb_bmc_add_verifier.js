const BMCManagement = artifacts.require('BMCManagement');
const { assert } = require('chai');

module.exports = async function (callback) {
    try {
        const bmcManagement = await BMCManagement.deployed();
        const tx = await bmcManagement.addVerifier(process.env.ICON_NET, process.env.BMV_MOONBEAM);
        let verifers = await bmcManagement.getVerifiers();
        assert(
            verifers[0].net === process.env.ICON_NET, verifers[0].addr === process.env.BMV_MOONBEAM,
        );
    }
    catch (error) {
        console.log(error)
    }
    callback()
}

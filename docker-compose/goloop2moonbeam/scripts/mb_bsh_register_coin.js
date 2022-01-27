const BSHCore = artifacts.require("BSHCore");
const fs = require('fs');

module.exports = async function (callback) {
    try {
        const bshCore = await BSHCore.deployed();
        const registerICX = await bshCore.register("ICX", "ICX", 18);
        fs.writeFileSync(process.env.CONFIG_DIR + "/tx.moonbeam.registerICX", registerICX.tx);
        await bshCore.coinNames();
    }
    catch (error) {
        console.log(error)
    }
    callback()
}

const BSHCore = artifacts.require("BSHCore");

module.exports = async function (callback) {
    try {
        const bshCore = await BSHCore.deployed();
        await bshCore.register("ICX");
        await bshCore.coinNames();
    }
    catch (error) {
        console.log(error)
    }
    callback()
}

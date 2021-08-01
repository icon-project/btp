const BSHCore = artifacts.require("BSHCore");

module.exports = async function (callback) {
    try {
        const bshCore = await BSHCore.deployed();
        let balance = await bshCore.getBalanceOf(process.env.ADDRESS, 'ICX')
        console.log(web3.utils.BN(balance._usableBalance).toNumber())
    }
    catch (error) {
        console.log(error)
    }
    callback()
}

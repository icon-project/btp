const BMCManagement = artifacts.require('BMCManagement');

module.exports = async function (callback) {
    try {
        const accounts = await web3.eth.getAccounts();
        await web3.eth.sendTransaction(
            {
                from: accounts[0],
                to: process.env.MOON_BMR,
                value: web3.utils.toBN("10000000000000000000000")
            }
        );
        console.log('BMR.MOONBEAM balance: ', await web3.eth.getBalance(process.env.MOON_BMR))
    }
    catch (error) {
        console.log(error)
    }
    callback()
}
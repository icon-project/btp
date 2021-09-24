const BSHCore = artifacts.require("BSHCore");

module.exports = async function (callback) {
  try {
    const bshCore = await BSHCore.deployed();
    await bshCore.register(process.env.NEXTLINK_BTP_NATIVECOIN_NAME);
    await bshCore.coinNames();
  } catch (error) {
    console.log(error)
  } finally {
    callback()
  } 
}

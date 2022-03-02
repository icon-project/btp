const BMCManagement = artifacts.require('BMCManagement');

module.exports = async function (callback) {
  try {
    const bmcManagement = await BMCManagement.deployed();
    console.log(await bmcManagement.addVerifier(process.env.NEXTLINK_BTP_NET, process.env.CURRENTLINK_BMV_ADDRESS));
  } catch (error) {
    console.log(error)
  } finally {
    callback()
  }
}

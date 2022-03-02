const BMCManagement = artifacts.require('BMCManagement');

module.exports = async function (callback) {
  try {
    const bmcManagement = await BMCManagement.deployed();
    console.log(await bmcManagement.addService(process.env.CURRENTLINK_BSH_SERVICENAME, process.env.CURRENTLINK_BSH_ADDRESS))
  } catch (error) {
    console.log(error)
  } finally {
    callback()
  }
}

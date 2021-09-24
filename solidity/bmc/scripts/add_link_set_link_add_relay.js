const BMCManagement = artifacts.require('BMCManagement');

module.exports = async function (callback) {
  try {
    const bmcManagement = await BMCManagement.deployed();
    console.log(await bmcManagement.addLink(process.env.NEXTLINK_BTP_ADDRESS));
    console.log(await bmcManagement.setLink(
      process.env.NEXTLINK_BTP_ADDRESS,
      parseInt(process.env.NEXTLINK_BLOCK_INTERVAL),
      parseInt(process.env.NEXTLINK_ROTATION_MAX_AGGERATION),
      parseInt(process.env.NEXTLINK_ROTATION_DELAY_LIMIT),
    ))
    console.log(await bmcManagement.addRelay(process.env.NEXTLINK_BTP_ADDRESS, process.env.RELAY_ADDRESSES.split(',')))
  } catch (error) {
    console.log(error)
  } finally {
    callback()
  }
}

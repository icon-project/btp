const BMCPeriphery = artifacts.require('BMCPeriphery');

module.exports = async function (callback) {
  try {
    const bmcPeriphery = await BMCPeriphery.deployed();
    console.log(await bmcPeriphery.getStatus(process.env.NEXTLINK_BTP_ADDRESS))
  } catch (error) {
    console.log(error)
  } finally {
    callback()
  }
}

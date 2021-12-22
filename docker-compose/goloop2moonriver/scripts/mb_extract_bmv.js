const BMV = artifacts.require("BMV");
const fs = require('fs')

module.exports = async function (callback) {
    try {
        await BMV.deployed();

        fs.writeFileSync(process.env.CONFIG_DIR + "/bmv.moonbeam", BMV.address, function (err, data) {
            if (err) {
                return console.log(err);
            }
        });
    }
    catch (error) {
        console.log(error)
    }
    callback()
}

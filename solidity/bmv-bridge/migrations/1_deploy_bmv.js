const BMV = artifacts.require("BMV");
const { deployProxy } = require('@openzeppelin/truffle-upgrades');

module.exports = async function (deployer, network) {
    if (network !== "development") {
        await deployProxy(
            BMV,
            [
                process.env.BMC_CONTRACT_ADDRESS,
                process.env.BMV_ICON_NET,
                parseInt(process.env.BMV_ICON_INIT_OFFSET),
            ],
            { deployer }
        );
    } else {
        await deployer.deploy(BMV);
        const bmv = await BMV.deployed();
        const BMC_CONTRACT_ADDRESS='0x2Eca89299FDFea25b07978017221C41caDF19b03';
        const BMV_ICON_NET='0x3.icon';
        const BMV_ICON_INIT_OFFSET=0;
        await bmv.initialize(BMC_CONTRACT_ADDRESS, BMV_ICON_NET, BMV_ICON_INIT_OFFSET);
        let status = await bmv.getStatus();
        console.log(status);
    }
};

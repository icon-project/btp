const BMV = artifacts.require("BMV");
const SUB_BMV = artifacts.require("DataValidator");
const { deployProxy, upgradeProxy } = require('@openzeppelin/truffle-upgrades');

const rlp = require('rlp');
module.exports = async function (deployer, network) { 
    const lastBlockHash = '0x34c27cf1b000a6256f1e63631c43669a0620c3646e39747d60f8ba65205802ca'
    const validatorsList = [
      'hxb6b5791be0b5ef67063b3c10b840fb81514db2fd'
    ];
    let encodedValidators = rlp.encode(validatorsList.map(validator => validator.replace('hx', '0x00')));
    
    await upgradeProxy("", SUB_BMV);
    await upgradeProxy(
      "", BMV
    );
    
  
/*   if (network !== "development") {
    await deployProxy(SUB_BMV, { deployer });
    await deployProxy(
      BMV,
      [
        process.env.BMC_PERIPHERY_ADDRESS,//Periphery
        SUB_BMV.address,
        process.env.BMV_ICON_NET,
        process.env.BMV_ICON_ENCODED_VALIDATORS,
        parseInt(process.env.BMV_ICON_INIT_OFFSET),
        parseInt(process.env.BMV_ICON_INIT_ROOTSSIZE),
        parseInt(process.env.BMV_ICON_INIT_CACHESIZE),
        process.env.BMV_ICON_LASTBLOCK_HASH,
      ],
      { deployer }
    );
  } */
};
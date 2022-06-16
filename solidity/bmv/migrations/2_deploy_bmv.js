const bmv = artifacts.require("BtpMessageVerifier");
const rlpEncoder = artifacts.require("RLPEncode");
const rlp = artifacts.require("RLPReader");

const { deployProxy } = require('@openzeppelin/truffle-upgrades');

module.exports = async function (deployer, network) {
    let args;
    if (network === 'development') {
        let _bmc = deployer.networks[network].from;
        let _srcNetworkId = '0x6274703a2f2f3078312e69636f6e';
        let _networkTypeId = 2;
        let _networkId = 2;
        let _firstBlockUpdate = '0xf8a60a00a0177b09e6b788d2a5f9d7572265f8a54176a83783e6d1d99d1865bc24e04fb493c00201f80001a041791102999c339c844880b23950704cc43aa840f3739e365323cda4dfa89e7af800b858f856f85494524122f6386c9cd3342ecc377dbc9dcb036dd2e0948103611a29623db06a937b24f52d70cf44c1c41494910d15f35a3e685968d8596512efa56d840bb3c59451226eee21a3d3758727886df161c108f5857f3f';

        args = [_bmc, _srcNetworkId, _networkTypeId, _networkId, _firstBlockUpdate];
    }

    await deployProxy(bmv, args, { deployer });
};

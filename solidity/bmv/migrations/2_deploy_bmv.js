const bmv = artifacts.require("BtpMessageVerifier");
const rlpEncoder = artifacts.require("RLPEncode");
const rlp = artifacts.require("RLPReader");

module.exports = function (deployer, network) {
    let args = deployer.options.network_config.args;
    let bmc;
    let validators;
    if (network === 'development') {
        bmc = args.bmc;
        validators = args.validators.addresses;
    }

    deployer.deploy(bmv, bmc, validators);
    deployer.link(bmv, rlpEncoder);
    deployer.deploy(rlpEncoder);

    deployer.link(bmv, rlp);
    deployer.deploy(rlp);
};

const BSHCoreV1 = artifacts.require("BSHCoreV1");
const { deployProxy } = require('@openzeppelin/truffle-upgrades');

var _uri = 'https://1234.iconee/';
var _native_coin = 'PARA';

module.exports = async function (deployer, network) {
    /********************************************************************************************** 
                                Deploy A Contract and the Proxy Contract
    - Before running a script, make sure to delete 
        + a file '.openzeppelin/unknown-1337.json' (if existed)
    - Deploy the first part by a following command: `yarn truffle:deploy`
    **********************************************************************************************/

    await deployProxy(BSHCoreV1, 
        [_uri, _native_coin],   //  calling initialize() and passing params into initialize()
        { deployer }              //  specify an account to deploy a contract
                                //  if omit, default 'deployer' will be chosen
    );
    const version1 = await BSHCoreV1.deployed();
    await version1.register("Coin1");
    await version1.register("Coin2");
}
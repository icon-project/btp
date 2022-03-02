const BSHCoreV2 = artifacts.require("BSHCoreV2");
const ProxyAddr = require('../.openzeppelin/unknown-1337.json');
const { upgradeProxy } = require('@openzeppelin/truffle-upgrades');

var _uri = 'https://1234.iconee/';
var _native_coin = 'PARA';

module.exports = async function (deployer, network) {
    /********************************************************************************************** 
    - Upgrade the contract by a following command: `yarn truffle:upgrade`
    **********************************************************************************************/

    await upgradeProxy(ProxyAddr.proxies[0].address, BSHCoreV2);
    const version2 = await BSHCoreV2.deployed();
    //  It prints out an array of three: 'PARA', 'Coin1', and 'Coin2'
    console.log(await version2.coinNames());
    await version2.register("Coin3");
    //  It prints out an array of four: 'PARA', 'Coin1', 'Coin2', 'Coin3'
    console.log(await version2.coinNames());
    var coin1_id = await version2.coinId('Coin1');
    var coin3_id = await version2.coinId('Coin3');
    console.log('ID of Coin1 ---> ', web3.utils.BN(coin1_id).toString());
    console.log('ID of Coin3 ---> ', web3.utils.BN(coin3_id).toString());
};
const hre = require("hardhat");
const accounts =  hre.userConfig.networks.hardhat.accounts.map((v) => {
    return {secretKey: v.privateKey, balance: v.balance}
})

module.exports = {
    istanbulFolder: 'build/hardhat/coverage',
    istanbulReporter: ['html','text'],
    providerOptions: {
        // default_balance_ether: 100,
        accounts: accounts
    },
    skipFiles: [
        // "CallService.sol",
        "interfaces/IBMC.sol",
        "interfaces/IBSH.sol",
        "interfaces/ICallService.sol",
        "interfaces/ICallServiceReceiver.sol",
        "interfaces/IFeeManage.sol",
        // "libraries/BTPAddress.sol",
        // "libraries/Integers.sol",
        // "libraries/ParseAddress.sol",
        // "libraries/RLPDecode.sol",
        // "libraries/RLPDecodeStruct.sol",
        // "libraries/RLPEncode.sol",
        // "libraries/RLPEncodeStruct.sol",
        // "libraries/Strings.sol",
        // "libraries/Types.sol",
        "test/DAppProxySample.sol",
        "test/LibRLPStruct.sol"
    ],
    mocha: {
        timeout: 600000
    }
};

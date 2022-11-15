module.exports = {
    istanbulFolder: 'build/hardhat/coverage',
    istanbulReporter: ['html'],
    providerOptions: {
        // default_balance_ether: 100,
        accounts: [
           {//0xfe3b557e8fb62b89f4916b721be55ceb828dbd73
               secretKey: "0x8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63",
               balance: 0x84595161401484a000000
           },
            {//0xc38ac5ef1ee98e4a834f6d725318737aec0ac6a8
                secretKey: "0xa6d23a0b704b649a92dd56bdff0f9874eeccc9746f10d78b683159af1617e08f",
                balance: 0x84595161401484a000000
            }
        ]
    },
    skipFiles: [
        // "LibRLP.sol",
        // "MockBMC.sol",
        // "MockBMV.sol",
        // "MockBSH.sol",
        "interfaces/IBMC.sol",
        "interfaces/IBMV.sol",
        "interfaces/IBSH.sol",
        "interfaces/IMockBMC.sol",
        "interfaces/IMockBMV.sol",
        "interfaces/IMockBSH.sol",
        // "libraries/BTPAddress.sol",
        // "libraries/Errors.sol",
        // "libraries/Integers.sol",
        // "libraries/ParseAddress.sol",
        // "libraries/RLPDecode.sol",
        // "libraries/RLPEncode.sol",
        // "libraries/Strings.sol",
        "test/TestBTPAddress.sol",
        "test/TestWeb3jABI.sol"
    ],
    mocha: {
        timeout: 600000
    }
};

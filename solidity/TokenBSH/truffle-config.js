
const HDWalletProvider = require("@truffle/hdwallet-provider");

var privKeys=[
  "17800d306fe7711cf7b68b644758341639a49cb9804b5155e49966cbc0f561a4","df4cf5e6dc6df55bc86a84d2095f85d61572a772eaf431ccc4056718fce9af77"
]
module.exports = {
  networks: {
    testnet: {      
      provider: () => new HDWalletProvider(privKeys,
           `http://127.0.0.1:8545`),
       network_id: 1000,
       confirmations: 0,
       timeoutBlocks: 900,
       skipDryRun: true
 },
    development: {
      host: "localhost",
      port: 9545,
      network_id: "*", // Match any network id
      gas: 5000000
    }
  },
  compilers: {
    solc: {
      version: "^0.8.0",
      settings: {
        optimizer: {
          enabled: true, // Default: false
          runs: 200      // Default: 200
        },
      }
    }
  }
};

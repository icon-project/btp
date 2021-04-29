
const HDWalletProvider = require("@truffle/hdwallet-provider");

var privKeys=[
  "529926ee50bc91ea6c93bf9fcc1d77349ffa3423ce905bb1ea78945a4996d702","a5fc0e1ff7cc16c0d7ed54597413c5063a9dc56209a64603065cf0093cd5efc1"
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
    }
  },
  compilers: {
    solc: {
      version: "0.8.0",
      settings: {
        optimizer: {
          enabled: true, // Default: false
          runs: 200      // Default: 200
        },
        evmVersion: "petersburg"
      }
    }
  },
  plugins: ["truffle-contract-size"]
};

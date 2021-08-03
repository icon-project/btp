const HDWalletProvider = require('@truffle/hdwallet-provider');

const privKeys = (process.env.PRIVATE_KEYS) ? process.env.PRIVATE_KEYS.split(',') : 
  [
   // Add private keys
  ];

module.exports = {
  networks: {
      development: {
          provider: () => new HDWalletProvider({
              privateKeys: privKeys,
              providerOrUrl: "http://localhost:8545",
          }),
          network_id: '*'
      },
      bscTestnet: {
          provider: () => new HDWalletProvider({
              privateKeys: privKeys,
              providerOrUrl: "http://35.214.59.124:8545",
          }),
          network_id: '97',
      },
      bscLocal: {
          provider: () => new HDWalletProvider({
              privateKeys: privKeys,
              providerOrUrl: "http://localhost:8545",
          }),
          network_id: '97'
      }
  },

  // Set default mocha options here, use special reporters etc.
  mocha: {
    // timeout: 100000
  },

  // Configure your compilers
  compilers: {
    solc: {
       version: "0.7.6",    // Fetch exact version from solc-bin (default: truffle's version)
      // docker: true,        // Use "0.5.1" you've installed locally with docker (default: false)
       settings: {          // See the solidity docs for advice about optimization and evmVersion
        optimizer: {
          enabled: true,
          runs: 10
        },
        evmVersion: "petersburg"
      }
    }
  },

  db: {
    enabled: false
  }
};

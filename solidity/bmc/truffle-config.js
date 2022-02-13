const HDWalletProvider = require('@truffle/hdwallet-provider');
const web3 = require("web3")

const privKeys = (process.env.PRIVATE_KEYS) ? process.env.PRIVATE_KEYS.split(',') : 
  [
    '0x5fb92d6e98884f76de468fa3f6278f8807c48bebc13595d45af5bdc4da702133', // Alith
    '0x8075991ce870b93a8870eca0c0f91913d12f47948ca0fd25b49c6fa7cdbeee8b', // Baltathar
    '0x0b6e18cafb6ed99687ec547bd28139cafdd2bffe70e6b688025de6b445aa5c5b', // Charleth
    '0x39539ab1876910bbf3a223d84a29e28f1cb4e2e456503e7e91ed39b2e7223d68', // Dorothy
    '0x7dce9bc8babb68fec1409be38c8e1a52650206a7ed90ff956ae8a6d15eeaaef4', // Ethan
    '0xb9d2ea9a615f3165812e8d44de0d24da9bbd164b65c4f0573e1ce2c8dbd9c8df', // Faith
    '0x96b8a38e12e1a31dee1eab2fffdf9d9990045f5b37e44d8cc27766ef294acf18', // Goliath
    '0x0d6dcaaef49272a5411896be8ad16c01c35d6f8c18873387b71fbc734759b0ab', // Heath
    '0x4c42532034540267bf568198ccec4cb822a025da542861fcb146a5fab6433ff8', // Ida
    '0x94c49300a58d576011096bcb006aa06f5a91b34b4383891e8029c21dc39fbb8b'  // Judith
  ];

module.exports = {
  networks: {
    development: {
      provider: () => new HDWalletProvider({
        privateKeys: privKeys,
        providerOrUrl: "http://localhost:9933",
      }),
      network_id: 1281
    },
    moonbeamlocal: {
      provider: () => new HDWalletProvider({
        privateKeys: privKeys,
        providerOrUrl: "http://localhost:9933",
      }),
      network_id: 1281
    },
    moonbase: {
      provider: () => new HDWalletProvider({
        privateKeys: privKeys,
        providerOrUrl: "https://rpc.api.moonbase.moonbeam.network",
      }),
      network_id: 1287,
      networkCheckTimeout: 1000000,
      timeoutBlocks: 50000,
      deploymentPollingInterval: 5000,
      // Make deploy faster for deployment
      // gasPrice: web3.utils.toWei("2", "Gwei"),
    },
    moonriver: {
      provider: () => new HDWalletProvider({
        privateKeys: privKeys,
        providerOrUrl: "https://rpc.moonriver.moonbeam.network",
      }),
      network_id: 1285,
      networkCheckTimeout: 100000,
    },
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

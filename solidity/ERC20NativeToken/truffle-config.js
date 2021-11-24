
const HDWalletProvider = require("@truffle/hdwallet-provider");

var privKeys = [
  "1deb607f38b0bd1390df3b312a1edc11a00a34f248b5d53f4157de054f3c71ae",
  "a1617c7e1691ee5691d0c750125e96a2630f75ef8e87cdd87f363c00d42163e7",
  "3d5f8ff132c7f10a03e138b952e556976707725c9aae98e4ed3df6172b8aaa4f",
  "fd52d799e21ad6d35a4e0c1679fd82eecbe3e3ccfdeceb8a1eed3a742423f688"
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
    bscLocal: {
      networkCheckTimeout: 10000,
      provider: () => new HDWalletProvider(privKeys, "ws://localhost:8546",
      ),
      network_id: '97',
      confirmations: 0,
      skipDryRun: true,
      gas: 8000000
    },
    bscDocker: {
      provider: () => new HDWalletProvider({
        privateKeys: privKeys,
        providerOrUrl: "BSC_RPC_URI",
        chainId: 97,
      }),
      network_id: '97',
      skipDryRun: true,
      networkCheckTimeout: 1000000000
    },
    development: {
      host: "localhost",
      port: 9545,
      network_id: "*", // Match any network id 
    }
  },
  compilers: {
    solc: {
      version: "0.7.6",
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

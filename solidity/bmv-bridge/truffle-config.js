const HDWalletProvider = require('@truffle/hdwallet-provider');
require("dotenv").config();

var privKeys = [
    "1deb607f38b0bd1390df3b312a1edc11a00a34f248b5d53f4157de054f3c71ae",
    "a1617c7e1691ee5691d0c750125e96a2630f75ef8e87cdd87f363c00d42163e7",
    "3d5f8ff132c7f10a03e138b952e556976707725c9aae98e4ed3df6172b8aaa4f",
    "fd52d799e21ad6d35a4e0c1679fd82eecbe3e3ccfdeceb8a1eed3a742423f688"
]

module.exports = {
    networks: {
        development: {
            provider: () =>
                new HDWalletProvider({
                    privateKeys: privKeys,
                    providerOrUrl: "http://localhost:8545",
                }),
            network_id: "*",
        },
        bscLocal: {
            provider: () => new HDWalletProvider({
                privateKeys: privKeys,
                providerOrUrl: "ws://localhost:8546",
                chainId: 97,
            }),
            network_id: '97'
        },
        bsc: { //from icon-bridge/solidity/bmc/truffle-config.js
            provider: () =>
                new HDWalletProvider({
                    privateKeys: JSON.parse(process.env.PRIVATE_KEY),
                    providerOrUrl: process.env.BSC_RPC_URI,
                }),
            network_id: process.env.BSC_NID,
            skipDryRun: true,
            networkCheckTimeout: 1000000,
            timeoutBlocks: 200,
            gasPrice: 20000000000,
        },
        hmny: { //from icon-bridge/solidity/bmc/truffle-config.js
            provider: () =>
                new HDWalletProvider({
                    privateKeys: [process.env.PRIVATE_KEY],
                    providerOrUrl: process.env.RPC_URL,
                }),
            network_id: process.env.NETWORK_ID,
            skipDryRun: true,
            networkCheckTimeout: 1000000,
            timeoutBlocks: 200,
            gasPrice: 20000000000,
        },
    },

    // Set default mocha options here, use special reporters etc.
    mocha: {
        // timeout: 100000
    },

    // Configure your compilers
    compilers: {
        solc: {
            version: "0.8.0",    // Fetch exact version from solc-bin (default: truffle's version)
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
    plugins: ["truffle-plugin-verify", "truffle-contract-size"],
    db: {
        enabled: false
    }
};

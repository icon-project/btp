const HDWalletProvider = require('@truffle/hdwallet-provider');
require("dotenv").config();

var privKeys = [
    "0x1deb607f38b0bd1390df3b312a1edc11a00a34f248b5d53f4157de054f3c71ae",
    "0xa1617c7e1691ee5691d0c750125e96a2630f75ef8e87cdd87f363c00d42163e7",
    "0x3d5f8ff132c7f10a03e138b952e556976707725c9aae98e4ed3df6172b8aaa4f",
    "0xfd52d799e21ad6d35a4e0c1679fd82eecbe3e3ccfdeceb8a1eed3a742423f688"
]

//const BSC_RPC_URI = "ws://binancesmartchain:8546"

module.exports = {
    networks: {
        development: {
            provider: () => new HDWalletProvider({
                privateKeys: privKeys,
                providerOrUrl: "http://localhost:8545",
            }),
            network_id: '*'
        },
        bscLocal: {
            provider: () => new HDWalletProvider({
                privateKeys: privKeys,
                providerOrUrl: "ws://localhost:8546",
                chainId: 97,
            }),
            network_id: '97'
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
        bsc: {
            provider: () => new HDWalletProvider({
                privateKeys: privKeys,
                providerOrUrl: "http://35.214.59.124:8545",
            }),
            network_id: '97',
            //networkCheckTimeout: 1000000,
            //timeoutBlocks: 2000
        },

        // iconloop testbed
        testbed: {
            host: "20.20.1.222",
            port: 8545,
            network_id: "*"
        }
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
    plugins: ["truffle-plugin-verify", "@chainsafe/truffle-plugin-abigen", "truffle-contract-size", "solidity-coverage"],
    db: {
        enabled: false
    }
};

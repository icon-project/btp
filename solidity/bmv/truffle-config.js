module.exports = {
    networks: {
        development: {
            host: "127.0.0.1",
            port: 8545,
            network_id: '*',
        },

        // iconloop testbed
        testbed: {
            host: "20.20.1.222",
            port: 8545,
            network_id: '*',
        }
    },

    mocha: {
        reporter: 'eth-gas-reporter',
        reporterOptions: {
            outputFile: 'gas-usage.txt',
            noColors: true
        }
    },

    plugins: ['truffle-contract-size', 'solidity-coverage'],

    compilers: {
        solc: {
            version: '0.8.12',
            settings: {
                optimizer: {
                    enabled: true,
                    runs: 200,
                },
            },
        },
    },
};

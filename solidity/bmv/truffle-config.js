module.exports = {
    networks: {
        development: {
            host: "127.0.0.1",
            port: 8545,
            network_id: "*",
            args: {
                bmc: "0xf02D03eC07768fc3a5C2e8C1Be734E529a49725C",
                validators: {
                    addresses: [
                        "0xf02D03eC07768fc3a5C2e8C1Be734E529a49725C",
                        "0x9515864E092308aB8F0634A6273264F86b50B8CE",
                        "0x01179d8BB693BD851f53d65c30741A845fF0fa81",
                        "0x99F276247abce7cfeF24Fbb2e3f0056A3Ca6CFE0"
                    ]
                }
            }
        },
    },

    compilers: {
        solc: {
            version: "0.8.12",
        }
    },
};

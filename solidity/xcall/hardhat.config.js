require("@nomicfoundation/hardhat-toolbox");
require("@nomicfoundation/hardhat-network-helpers");
require('solidity-coverage');
require('hardhat-contract-sizer');

/** @type import('hardhat/config').HardhatUserConfig */
module.exports = {
  defaultNetwork: "hardhat",
  networks: {
    hardhat: {
    },
    bscLocal: {
      url: "http://localhost:8545",
      accounts: [
        "0x8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63",
        "0xa6d23a0b704b649a92dd56bdff0f9874eeccc9746f10d78b683159af1617e08f"
      ]
    }
  },
  solidity: {
    version: "0.8.2",
    settings: {
      optimizer: {
        enabled: true,
        runs: 10
      }
    }
  },
  paths: {
    sources: "./contracts",
    tests: "./test/hardhat",
    cache: "./build/hardhat/cache",
    artifacts: "./build/hardhat/artifacts"
  },
  mocha: {
    timeout: 600000
  },
  contractSizer: {
    only:[
      "CallService",
      "DAppProxySample"
    ],
    except:[
      "V2"
    ]
  }
};

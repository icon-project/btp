var keythereum = require("keythereum");

var datadir = "/mnt/d/workspace/precompiles/bsc_private_node/node1";
var address = "0x4c7a329a44def662e7a82f1c4f7b33176b20a54e";
const password = "admin123";
var keyObject = keythereum.importFromFile(address, datadir);
var privateKey = keythereum.recover(password, keyObject);
console.log(privateKey.toString('hex'));

var keythereum = require("keythereum");

var datadir = "/mnt/d/workspace/precompiles/bsc_private_node/node1";
var address = "0x3cf70ec2561d3cafb8a82f703984c72bdab3aa88";
//var address = "0xebcfa608ab3e9f62b5322d4f9f27395aaaacdade";
const password = "admin123";
var keyObject = keythereum.importFromFile(address, datadir);
var privateKey = keythereum.recover(password, keyObject);
console.log(privateKey.toString('hex'));

const Web3 = require('web3');
const address = require('./addresses.json');

let web3Provider = new Web3.providers.WebsocketProvider("ws://localhost:8546");
var web3Obj = new Web3(web3Provider);
async function init() {
  var tx = await web3Obj.eth.getBlock("latest")
  console.log(web3Obj.utils.keccak256('TransferEnd(address,uint256,uint256,string)'))
}
init()

var subscription = web3Obj.eth.subscribe('logs', {
  address: address.solidity.BSHImpl,//'0xbC7E488a8c459C9Bc32ccB31F951ed0d2A41b250',//address.solidity.BSHImpl, //Smart contract address
  fromBlock: 1,
  topics: [web3Obj.utils.keccak256('TransferEnd(address,uint256,uint256,string)')] //topics for events
}, function (error, result) {
  if (error) console.log(error);
}).on("data", function (trxData) {
  console.log("Event received", trxData);
  //Code from here would be run immediately when event appeared
});
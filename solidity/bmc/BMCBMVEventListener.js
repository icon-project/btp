const Web3 = require('web3');

let web3Provider = new Web3.providers.WebsocketProvider("ws://localhost:9944");
var web3Obj = new Web3(web3Provider);
async function init() {
  var tx = await web3Obj.eth.getBlock("latest")
  console.log(web3Obj.utils.keccak256('TransferEnd(address,uint256,uint256,string)'))
  console.log(web3Obj.utils.keccak256('Debug(string,address,uint256)'))
}
init()

var subscription = web3Obj.eth.subscribe('logs', {
  //address: '0x746DFE0F96789e62CECeeA3CA2a9b5556b3AaD6c',//'0xbC7E488a8c459C9Bc32ccB31F951ed0d2A41b250',//address.solidity.BSH, //Smart contract address
  fromBlock: 1,
  //topics: [web3Obj.utils.keccak256('TransferEnd(address,uint256,uint256,string)')] //topics for events
  topics: [web3Obj.utils.keccak256('Debug1(string,address,uint256)')] //topics for events
}, function (error, result) {
  if (error) console.log(error);
}).on("data", function (trxData) {
  console.log("Event received1111111111111", trxData);
  for(const topic of trxData.topics){
    if(topic == web3Obj.utils.keccak256('Debug1(string,address,uint256)')){
      const result = web3Obj.eth.abi.decodeLog(
        debugEventInputABI,
        trxData.data,
        trxData.topics
      );
      console.log("Decoded debug data111111111111111: ", result); 
    }
  }
  //Code from here would be run immediately when event appeared
});



var subscription = web3Obj.eth.subscribe('logs', {
  address: '0x5CC307268a1393AB9A764A20DACE848AB8275c46',//'0xbC7E488a8c459C9Bc32ccB31F951ed0d2A41b250',//address.solidity.BMC, //Smart contract address
  fromBlock: 1,
  //topics: [web3Obj.utils.keccak256('TransferEnd(address,uint256,uint256,string)')] //topics for events
  topics: [web3Obj.utils.keccak256('Debug(string,address,uint256)')] //topics for events
}, function (error, result) {
  if (error) console.log(error);
}).on("data", function (trxData) {
  console.log("Event received2222222222222222", trxData);
  //Code from here would be run immediately when event appeared
  for(const topic of trxData.topics){
    if(topic == web3Obj.utils.keccak256('Debug(string,address,uint256)')){
      const result = web3Obj.eth.abi.decodeLog(
        debugEventInputABI,
        trxData.data,
        trxData.topics
      );
      console.log("Decoded debug data2222222222222: ", result); 
    }
  }
});
//event Debug(string _msg,address _user, uint256 _val);

debugEventInputABI=[ 
  {
    "indexed": false,
    "internalType": "string",
    "name": "_msg",
    "type": "string"
  },
  {
    "indexed": false,
    "internalType": "address",
    "name": "_user",
    "type": "address"
  },
  {
    "indexed": false,
    "internalType": "uint256",
    "name": "_val",
    "type": "uint256"
  }
]
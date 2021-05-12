const MerkleTreeAccumulator = artifacts.require('MerkleTreeAccumulator');

const truffleAssert = require('truffle-assertions')
const {expectEvent} = require('@openzeppelin/test-helpers')

const { toBuffer, bufferToHex, keccak256 } = require('ethereumjs-util')

String.prototype.hex = function() {
    return web3.utils.stringToHex(this)
}

contract('When testing MerkleTreeAccumulator, it', function () {
    let mta, accounts;

    beforeEach(async () => {
        mta = await MerkleTreeAccumulator.new();
        accounts = await web3.eth.getAccounts();
    });

    it("add", async () => {
        const data = ["dog", "cat", "elephant", "bird", "monkey"]

        for (const i in data) {
            let tx = await mta.addData(data[i].hex())
        }

        const length = (await mta.getLength()).toNumber();

        for (let i=0;i < length; i++){
            let node = await mta.getNode(i)
            console.log(i, node.nodeType, node.hash, node.leftHash, node.rightHash, node.data)
        }

       //await expectEvent.inLogs(receipt.logs, "Claim", args);
    });


});

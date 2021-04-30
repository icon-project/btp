const truffleAssert = require('truffle-assertions');
const BMC = artifacts.require("BMC");
contract('When testing BMC, it', function () {
    var _net = 'bsc';
    let bmc, accounts;
    beforeEach(async () => {
        bmc = await BMC.deployed();
        accounts = await web3.eth.getAccounts()
    });
    it("BMC Get Status", async () => {
        await bmc.addVerifier(_net, accounts[1]);
        await bmc.addLink("btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8")
        await bmc.addRelay("btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8", [accounts[1]])
        let status = await bmc.getStatus("btp://bsc/0xa36a32c114ee13090e35cb086459a690f5c1f8e8")
        //await expectRevert(instance.ownerOf(nodeHash), "ERC721: owner query for nonexistent token");
        //await expectEvent.inLogs(receipt.logs, "Claim", args);
    });
});
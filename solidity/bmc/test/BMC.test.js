const BMC = artifacts.require('MockBMC');
const MockBMV = artifacts.require('MockBMV');
const MockBSH = artifacts.require('MockBSH');
const Holder = artifacts.require('Holder');
const GatherFeeBSH = artifacts.require('GatherFeeBSH');
const { assert } = require('chai');
const truffleAssert = require('truffle-assertions');
const rlp = require('rlp');
const URLSafeBase64 = require('urlsafe-base64');
const { padEnd } = require('lodash');

contract('BMC Basic Unit Tests', () => {
    let bmc, accounts, bmv1, bmv2, bmv3, bmv4; 
    beforeEach(async () => {
        bmc = await BMC.deployed();
        accounts = await web3.eth.getAccounts()
    });

    before(async () => {
        bmv1 = await MockBMV.new();
        bmv2 = await MockBMV.new();
        bmv3 = await MockBMV.new();
        bmv4 = await MockBMV.new();
    });

    /***************************************************************************************
                                Add/Remove Service Unit Tests
    ***************************************************************************************/
    it('Request Add Service - Service not requested nor registered - Success', async () => {
        var service = 'Coin/WrappedCoin';
        
        await bmc.requestAddService(service, accounts[5]);
        
        var output = await bmc.getPendingRequest();
        assert(
            output[0].serviceName === service, output[0].bsh == accounts[5],
        );
    });

    it('Request Add Service - Same Service name is pending - Failure', async () => {
        var service = 'Coin/WrappedCoin';
    
        await truffleAssert.reverts(
            bmc.requestAddService(service, accounts[6]),
            "BMCRevertRequestPending"
        );
    });

    it('Request Add Service - Service registered - Failure', async () => {
        var service = 'Coin/WrappedCoin';
        
        await bmc.approveService(service);
        await truffleAssert.reverts(
            bmc.requestAddService(service, accounts[6]),
            "BMCRevertAlreadyExistsBSH"
        );
    });

    it('Approve Service - Service existed - Failure', async () => {
        var service = 'Coin/WrappedCoin';
        await truffleAssert.reverts(
            bmc.approveService(service),
            "BMCRevertAlreadyExistsBSH"
        );
    });

    it('Approve Service - Service request not existed - Failure', async () => {
        var service = 'Token';
        await truffleAssert.reverts(
            bmc.approveService(service),
            "BMCRevertNotExistRequest"
        );
    });

    it('Approve Service - Without Permission - Failure', async () => {
        var service = 'Token';
        await bmc.requestAddService(service, accounts[6]);
        await truffleAssert.reverts(
            bmc.approveService(service, {from: accounts[1]}),
            "BMCRevertUnauthorized"
        );
    });

    it('Approve Service - With Permission - Success', async () => {
        var service = 'Token';
        
        await bmc.approveService(service);
        var output = await bmc.getServices();
        assert(
            output.length == 2,
            output[1].svc === service, output[1].addr == accounts[6],
        );
    });

    //  Add Service no longer exist. Instead, approveService() is implementing
    //  BSH contract, after deploying, sends a request to add its service to BMC
    //  

    // it('Add Service - With Permission - Success', async () => {
    //     var service = 'Coin/WrappedCoin';
        
    //     await bmc.addService(service, accounts[5]);
        
    //     var output = await bmc.getServices();
    //     // console.log('Supporting services: ', output);
    //     assert(
    //         output[0].svc === service, output[0].addr == accounts[5],
    //     );
    // });

    // it('Add Service - Without Permission - Failure', async () => {
    //     var service = 'Coin/WrappedCoin';
    //     await truffleAssert.reverts(
    //         bmc.addService(service, accounts[5], {from: accounts[1]}),
    //         "VM Exception while processing transaction: revert No permission -- Reason given: No permission"
    //     );
    // });

    // it('Add Service - Service Existed - Failure', async () => {
    //     var service = 'Coin/WrappedCoin';
    //     await truffleAssert.reverts(
    //         bmc.addService(service, accounts[5]),
    //         "VM Exception while processing transaction: revert BSH service existed -- Reason given: BSH service existed"        
    //     );
    // });

    it('Remove Service - Without Permission - Failure', async () => {
        var service = 'Coin/WrappedCoin';
        await truffleAssert.reverts(
            bmc.removeService(service, {from: accounts[1]}),
            "BMCRevertUnauthorized"
        );
    });

    it('Remove Service - Service Not Existed - Failure', async () => {
        var service = 'Token A';
        await truffleAssert.reverts(
            bmc.removeService(service),
            "BMCRevertNotExistsBSH"
        );
    });

    it('Remove Service - Service Existed and With Permission - Success', async () => {
        var service1 = 'Token';
        await bmc.removeService(service1);
        var output = await bmc.getServices();
        assert(
            output.length == 1,
            output[0].svc === 'Coin/WrappedCoin', output[0].addr == accounts[5]
        );
    });  

    /***************************************************************************************
                                Add/Remove Verifier Unit Tests
    ***************************************************************************************/
    it('Add Verifier - With Permission - Success', async () => {
        var network = '1234.iconee';
        await bmc.addVerifier(network, bmv1.address);
        var output = await bmc.getVerifiers();
        assert(
            output[0].net === network, output[0].addr == bmv1.address,
        );
    });

    it('Add Verifier - Without Permission - Failure', async () => {
        var network = '1234.iconee';
        await truffleAssert.reverts(
            bmc.addVerifier(network, accounts[5], {from: accounts[1]}),
            "BMCRevertUnauthorized"
        );
    });

    it('Add Verifier - Service Existed - Failure', async () => {
        var network = '1234.iconee';
        await truffleAssert.reverts(
            bmc.addVerifier(network, accounts[5]),
            "BMCRevertAlreadyExistsBMV"        
        );
    });

    it('Remove Verifier - Without Permission - Failure', async () => {
        var network = '1234.iconee';
        await truffleAssert.reverts(
            bmc.removeVerifier(network, {from: accounts[1]}),
            "BMCRevertUnauthorized"
        );
    });

    it('Remove Verifier - Service Not Existed - Failure', async () => {
        var network = '1234.pra';
        await truffleAssert.reverts(
            bmc.removeVerifier(network),
            "BMCRevertNotExistsBMV"
        );
    });

    it('Remove Verifier - Verifier Existed and With Permission - Success', async () => {
        var network1 = '1234.pra';
        var network2 = '1234.eos';
        var network3 = '1234.eth';
        await bmc.addVerifier(network1, bmv2.address);
        await bmc.addVerifier(network2, bmv3.address);
        await bmc.addVerifier(network3, bmv4.address);
        await bmc.removeVerifier('1234.iconee');
        var output = await bmc.getVerifiers();
        assert(
            output[0].net === network1, output[0].addr == bmv2.address,
            output[1].net === network2, output[0].addr == bmv3.address,
            output[2].net === network3, output[0].addr == bmv4.address,
        );
    });

    /***************************************************************************************
                                Add/Remove Link Unit Tests
    ***************************************************************************************/
    it('Add Link - With Permission - Success', async () => {
        var link = 'btp://1234.pra/0x1234'; 
        await bmc.addLink(link);
        var output = await bmc.getLinks();
        assert(
            output[0] === link
        );
    });

    it('Add Link - Without Permission - Failure', async () => {
        var link = 'btp://1234.pra/0x1234';
        await truffleAssert.reverts(
            bmc.addLink(link, {from: accounts[1]}),
            "BMCRevertUnauthorized"
        );
    });  
    
    //  BMC adds supporting link, but network has not yet registered any BMV
    it('Add Link - Verifier Not Existed - Failure', async () => {
        var link = 'btp://1234.btc/0x1234';
        await truffleAssert.reverts(
            bmc.addLink(link),
            "BMCRevertNotExistsBMV"
        );
    });  

    it('Add Link - Invalid BTP Address Format - Failure', async () => {
        var link = 'btp://1234.eos:0x1234';
        var resp = '';
        try {
            await bmc.addLink(link);
        } catch (err) {
            resp = err.toString();
        }
        assert(resp, 'Error: Returned error: VM Exception while processing transaction: invalid opcode');
    });  
                                
    it('Add Link - Link Existed - Failure', async () => {
        var link = 'btp://1234.pra/0x1234';
        await truffleAssert.reverts(
            bmc.addLink(link),
            "BMCRevertAlreadyExistsLink"
        );
    }); 
    
    it('Remove Link - Without Permission - Failure', async () => {
        var link = 'btp://1234.pra/0x1234';
        await truffleAssert.reverts(
            bmc.removeLink(link, {from: accounts[1]}),
            "BMCRevertUnauthorized"
        );
    });

    it('Remove Link - Link Not Existed - Failure', async () => {
        var link = 'btp://1234.btc/0x1234';
        await truffleAssert.reverts(
            bmc.removeLink(link),
            "BMCRevertNotExistsLink"
        );
    });

    it('Remove Link - Link Existed and With Permission - Success', async () => {
        var link1 = 'btp://1234.eos/0x1234';
        var link2 = 'btp://1234.eth/0x1234';
        await bmc.addLink(link1);
        await bmc.addLink(link2);
        await bmc.removeLink('btp://1234.pra/0x1234');
        var output = await bmc.getLinks();
        assert(
            output[0] === link1,
            output[1] === link2,
        );
    });    

    /***************************************************************************************
                                Set Link Stats Unit Tests
    ***************************************************************************************/
    it('Set Link - Without Permission - Failure', async () => {
        var link = 'btp://1234.eth/0x1234'; 
        var blockInterval = 15000;
        var maxAggregation = 5;
        var delayLimit = 4;
        await truffleAssert.reverts(
            bmc.setLink(link, blockInterval, maxAggregation, delayLimit, {from: accounts[1]}),
            "BMCRevertUnauthorized"
        );
    });

    it('Set Link - Link not Existed - Failure', async () => {
        var link = 'btp://1234.pra/0x1234'; 
        var blockInterval = 6000;
        var maxAggregation = 7;
        var delayLimit = 3;
        await truffleAssert.reverts(
            bmc.setLink(link, blockInterval, maxAggregation, delayLimit),
            "BMCRevertNotExistsLink"
        );
    });

    it('Set Link - Invalid Max Aggregation - Failure', async () => {
        var link = 'btp://1234.eth/0x1234'; 
        var blockInterval = 6000;
        var maxAggregation = 0;
        var delayLimit = 3;
        await truffleAssert.reverts(
            bmc.setLink(link, blockInterval, maxAggregation, delayLimit),
            "BMCRevertInvalidParam"
        );
    });

    it('Set Link - Invalid Delay Limit - Failure', async () => {
        var link = 'btp://1234.eth/0x1234'; 
        var blockInterval = 6000;
        var maxAggregation = 5;
        var delayLimit = 0;
        await truffleAssert.reverts(
            bmc.setLink(link, blockInterval, maxAggregation, delayLimit),
            "BMCRevertInvalidParam"
        );
    });

    it('Set Link - With Permission and Valid Settings - Success', async () => {
        var link = 'btp://1234.eth/0x1234'; 
        var blockInterval = 15000;
        var maxAggregation = 5;
        var delayLimit = 4;
        var height = 1500;
        var offset = 1400;
        var lastHeight = 1450;
        await bmv4.setStatus(height, offset, lastHeight);
        await bmc.setLink(link, blockInterval, maxAggregation, delayLimit);
        var status = await bmc.getStatus(link);
        assert(
            status.delayLimit == delayLimit && status.maxAggregation == maxAggregation &&
            status.blockIntervalDst == blockInterval
        );
    });

    /***************************************************************************************
                                Add/Remove Route Unit Tests
    ***************************************************************************************/
    it('Add Route - With Permission - Success', async () => {
        var dst = 'btp://1234.iconee/0x1234';
        var link = 'btp://1234.pra/0x1234';
        await bmc.addRoute(dst, link);
        var output = await bmc.getRoutes();
        assert(
            output[0].dst === dst, output[0].next === link,
        );
    });
                            
    it('Add Route - Without Permission - Failure', async () => {
        var dst = 'btp://1234.iconee/0x1234';
        var link = 'btp://1234.pra/0x1234';
        await truffleAssert.reverts(
            bmc.addRoute(dst, link, {from: accounts[1]}),
            "BMCRevertUnauthorized"
        );
    });

    it('Add Route - Route Existed - Failure', async () => {
        var dst = 'btp://1234.iconee/0x1234';
        var link = 'btp://1234.pra/0x1234';
        await truffleAssert.reverts(
            bmc.addRoute(dst, link),
            "BTPRevertAlreadyExistRoute"        
        );
    });

    it('Add Route - Destination/Link is invalid BTP Format Address - Failure', async () => {
        var dst = 'btp://1234.iconee:0x1234';
        var link = 'btp://1234.pra/0x1234';
        var resp = '';
        try {
            await bmc.addRoute(dst, link);
        } catch (err) {
            resp = err.toString();
        }
        assert(resp, 'Error: Returned error: VM Exception while processing transaction: invalid opcode');
    });

    it('Remove Route - Without Permission - Failure', async () => {
        var dst = 'btp://1234.iconee/0x1234';
        await truffleAssert.reverts(
            bmc.removeRoute(dst, {from: accounts[1]}),
            "BMCRevertUnauthorized"
        );
    });

    it('Remove Route - Route Not Existed - Failure', async () => {
        var dst = 'btp://1234.eos/0x1234';
        await truffleAssert.reverts(
            bmc.removeRoute(dst),
            "BTPRevertNotExistRoute"
        );
    });

    it('Remove Route - Route Existed and With Permission - Success', async () => {
        var dst1 = 'btp://1234.pra/0x1234';
        var link1 = 'btp://1234.eos/0x1234';
        var dst2 = 'btp://1234.eth/0x1234';
        var link2 = 'btp://1234.iconee/0x1234';
        await bmc.addRoute(dst1, link1);
        await bmc.addRoute(dst2, link2);
        await bmc.removeRoute('btp://1234.iconee/0x1234');
        var output = await bmc.getRoutes();
        assert(
            output[0].dst === dst1, output[0].next == link1,
            output[1].dst === dst2, output[0].addr == link2,
        );
    });     
    
    /***************************************************************************************
                                Add/Remove Relays Unit Tests
    ***************************************************************************************/

    it('Add Relays - Without Permission - Failure', async () => {
        var link = 'btp://1234.eos/0x1234';
        var relays = [accounts[2], accounts[3], accounts[4]];
        await truffleAssert.reverts(
            bmc.addRelay(link, relays, {from: accounts[1]}),
            "BMCRevertUnauthorized"
        );
    });

    it('Add Relays - Link not existed - Failure', async () => {
        var link = 'btp://1234.pra/0x1234';
        var relays = [accounts[2], accounts[3], accounts[4]];
        await truffleAssert.reverts(
            bmc.addRelay(link, relays),
            "BMCRevertNotExistsLink"        
        );
    });

    it('Add Relays - With Permission - Success', async () => {
        var link = 'btp://1234.eos/0x1234';
        var relays = [accounts[2], accounts[3], accounts[4]];
        await bmc.addRelay(link, relays);
        var result = await bmc.getRelays(link);
        assert(
            result[0] == accounts[2], result[1] == accounts[3], result[2] == accounts[4]
        );
    });

    it('Add Relays - Update Relays on existing one (Overwrite) - Success', async () => {
        var link = 'btp://1234.eos/0x1234';
        var relays = [accounts[2], accounts[4], accounts[6], accounts[7]];
        await bmc.addRelay(link, relays);
        var result = await bmc.getRelays(link);
        assert(
            result[0] == accounts[2], result[1] == accounts[4], 
            result[2] == accounts[6], result[3] == accounts[7]
        );
    });

    it('Remove Relay - Without Permission - Failure', async () => {
        var link = 'btp://1234.eos/0x1234';
        var relay = accounts[2];
        await truffleAssert.reverts(
            bmc.removeRelay(link, relay, {from: accounts[1]}),
            "BMCRevertUnauthorized"
        );
    });

    it('Remove Relay - Link Not Existed - Failure', async () => {
        var link = 'btp://1234.pra/0x1234';
        var relay = accounts[2];
        await truffleAssert.reverts(
            bmc.removeRelay(link, relay),
            "BMCRevertUnauthorized"
        );
    });

    it('Remove Relay - None of any relays - Failure', async () => {
        var link = 'btp://1234.eth/0x1234';
        var relay = accounts[2];
        await truffleAssert.reverts(
            bmc.removeRelay(link, relay),
            "BMCRevertUnauthorized"
        );
    });

    it('Remove Relay - Link/Relays existed and With permission - Success', async () => {
        var link = 'btp://1234.eos/0x1234';
        var relay = accounts[6];
        await bmc.removeRelay(link, relay);
        var result = await bmc.getRelays(link);
        assert(
            result[0] == accounts[2], result[1] == accounts[4], 
            result[2] == accounts[7]
        );
    });
});    

contract('BMC Relay Rotation Unit Test - Rotate_term = 0', () => {
    let bmc, accounts, bmv; 
    var network = '1234.iconee';
    var link = 'btp://1234.iconee/0x1234'; 
    var height = 0;
    var offset = 0;
    var lastHeight = 0;
    before(async () => {
        bmc = await BMC.deployed();
        accounts = await web3.eth.getAccounts()
        bmv = await MockBMV.new();
        await bmc.addVerifier(network, bmv.address);
        await bmc.addLink(link);
        var relays = [accounts[2], accounts[3], accounts[4], accounts[5]];
        await bmc.addRelay(link, relays);
        await bmv.setStatus(height, offset, lastHeight);
    });

    it('Link not set - Return address(0)', async () => {
        await bmc.relayRotation(link, 26, false);
        var relay_info = await bmc.getRelay();
        assert(relay_info.r === '0x0000000000000000000000000000000000000000');
    });
});    

contract('BMC Relay Rotation Unit Test - Rotate_term != 0', () => {
    let bmc, accounts, bmv; 
    var network = '1234.iconee';
    var link = 'btp://1234.iconee/0x1234'; 
    var height = 0;
    var offset = 0;
    var lastHeight = 0;
    var blockInterval = 3000;
    var maxAggregation = 5;
    var delayLimit = 3;
    var relays;
    var current;
    before(async () => {
        bmc = await BMC.deployed();
        accounts = await web3.eth.getAccounts()
        bmv = await MockBMV.new();
        await bmc.addVerifier(network, bmv.address);
        await bmc.addLink(link);
        relays = [accounts[2], accounts[3], accounts[4], accounts[5]];
        await bmc.addRelay(link, relays);
        await bmv.setStatus(height, offset, lastHeight);
        await bmc.setLink(link, blockInterval, maxAggregation, delayLimit);
    });

    //  [block(t+1), block(t+15)] => Relay 1
    it('Relay Rotation - Base Message (hasMsg = false) - Relay 1 Allowable', async () => {
        //  After setting a link, consider block_time = block(t+1)
        await bmc.mineOneBlock();       // block(t+2)
        await bmc.relayRotation(link, 0, false);    //  block(t+3)
        var relay_info = await bmc.getRelay();
        assert(relay_info.r == accounts[2]);
    });

    //  [block(t+1), block(t+15)] => Relay 1
    it('Relay Rotation - Base Message (hasMsg = false) - Relay 1 Still Allowable', async () => {
        for (i = 0; i < 10; i++) {
            await bmc.mineOneBlock();
        }
        //  After a loop, block_time = block(t+13)
        await bmc.relayRotation(link, 0, false);    //  block(t+14)
        var relay_info = await bmc.getRelay();
        assert(relay_info.r == accounts[2]);
    });

    //  [block(t+16), block(t+30)] => Relay 2
    it('Relay Rotation - Base Message (hasMsg = false) - Next Relay = Relay 2 Allowable', async () => {
        for (i = 0; i < 10; i++) {
            await bmc.mineOneBlock();
        }
        //  After loop, block_time = block(t+24)
        await bmc.relayRotation(link, 0, false);    //  block(t+25)
        var relay_info = await bmc.getRelay();
        assert(relay_info.r == accounts[3]);
    });

    //  [block(t+31), block(t+45)] => Relay 3
    it('Relay Rotation - Base Message (hasMsg = false) - Next Relay = Relay 3 Allowable', async () => {
        for (i = 0; i < 10; i++) {
            await bmc.mineOneBlock();
        }
        //  After loop, block_time = block(t+35)
        await bmc.relayRotation(link, 0, false);    //  block(t+36)
        var relay_info = await bmc.getRelay();
        current = relay_info.cb;
        assert(relay_info.r == accounts[4]);
    });

    //  [block(t+31), block(t+45)] => Relay 3
    it('Relay Rotation - BTP Message (hasMsg = true) - Relay 3 Still Allowable', async () => {
        for (i = 0; i < 9; i++) {
            await bmc.mineOneBlock();
        }
        //  After loop, block_time = block(t+45)
        var linkStat = await bmc.getStatus(link);
        var scale = parseInt(linkStat.blockIntervalSrc) / parseInt(linkStat.blockIntervalDst);
        var block = Math.floor((parseInt(current)+9)*scale);
        //  At the time calling relayRotation()
        //  block_time = block(t+45). Thus, Relay 3 is still allowable
        await bmc.relayRotation(link, block, true);    //  block(t+46)
        var relay_info = await bmc.getRelay();
        current = relay_info.cb;
        assert(relay_info.r == accounts[4]);
    });

    //  [block(t+46), block(t+61)] => Relay 4
    it('Relay Rotation - BTP Message (hasMsg = true) - Relay 4 Allowable', async () => {
        for (i = 0; i < 5; i++) {
            await bmc.mineOneBlock();
        }
        //  After loop, block_time = block(t+51)
        var linkStat = await bmc.getStatus(link);
        var scale = parseInt(linkStat.blockIntervalSrc) / parseInt(linkStat.blockIntervalDst);
        var block = Math.floor((parseInt(current)+5)*scale);
        await bmc.relayRotation(link, block, true);    //  block(t+52)
        var relay_info = await bmc.getRelay();
        current = relay_info.cb;
        assert(relay_info.r == accounts[5]);
    });

    //  TODO: need to check it again
    //  [block(t+46), block(t+60)] => Relay 4
    //  However, Relay 4 not relay BTP Message on time 'delay_limit'
    //  Move to next
    it.skip('Relay Rotation - BTP Message (hasMsg = true) - Relay 4 Overtime, Relay 1 Allowable', async () => {
        for (i = 0; i < 2; i++) {
            await bmc.mineOneBlock();
        }
        //  After loop, block_time = block(t+54)
        var linkStat = await bmc.getStatus(link);
        var scale = parseInt(linkStat.blockIntervalSrc) / parseInt(linkStat.blockIntervalDst);
        var block = Math.floor((parseInt(current)+3)*scale);
        //  If relayRotation() were called now, there would have been a regular case
        //  Relay 4 were still allowable thereafter
        //  However, Relay 4 passes 'delay_limit' to relay Message
        //  to simulate such thing, call mineOneBlock() 4 times
        //  Even though, block_height = 59 that is less than 60
        //  but Relay 4 is skipped and move to a next Relay
        
        //  If 'delay_limit' == 3 and Relay 4 send BTPMessage at 'delay_limit' + 1. This unit test might be failed
        //  uint _skipCount = ceilDiv((_currentHeight - _guessHeight), link.delayLimit) - 1
        //  There might be a case that uint ceilDiv((_currentHeight - _guessHeight), link.delayLimit);
        //  has not round-up. For example:
        //  'currentHeight' = 100, 'guess_height' = 97, and 'delay_limit' = 3
        //  => 'skipCount' = 1 - 1 = 0. Thus, choosing 4 is quite sensitive
        //  It's better to set a unit test that Relay sends BTPMessage at 'delay_limit' + 2
        for (i = 0; i < 5; i++) {
            await bmc.mineOneBlock();
        }
        //  After loop, block_time = block(t+59)
        await bmc.relayRotation(link, block, true);    //  block(t+60)
        var relay_info = await bmc.getRelay();
        assert(relay_info.r == accounts[2]);
    });
});    

/***************************************************************************************
                                Handle Gather Fee Request Unit Tests
 ***************************************************************************************/
contract('BSH Handle Fee Aggregation', () => {
    let bsh1, bsh2, bsh3, bmc, holder, accounts;
    var service1 = 'Service1';  var service2 = 'Service2';  var service3 = 'Service3'
    var _native = 'PARA';   var _symbol = 'PRA';    var _decimals = 0; var _fee = 10000;
    var _net1 = '1234.iconee';                          var _net2 = '1234.binance';     
    var _txAmt = 10000;

    var _name1 = 'ICON';                                var _name2 = 'BINANCE';    
    var _symbol1 = 'ICX';                               var _symbol2 = 'BNC';
    var _decimals1 = 0;                                 var _decimals2 = 0;
    var _fee1 = 10000;                                  var _fee2 = 10000;
        
    var _name3 = 'ETHEREUM';                            var _name4 = 'TRON'; 
    var _symbol3 = 'ETH';                               var _symbol4 = 'TRX';
    var _decimals3 = 0;                                 var _decimals4 = 0;
    var _fee3 = 10000;                                  var _fee4 = 10000;

    var _from1 = '0x12345678';                          var _from2 = '0x12345678';
    var _value1 = 999999999999999;                      var _value2 = 999999999999999;

    var _to1 = 'btp://1234.iconee/0x12345678';          var _to2 = 'btp://1234.binance/0x12345678';
    var _txAmt1 = 1000000;                              var _txAmt2 = 5000000;                                                                                        
    var _sn1 = 1;                                       var _sn2 = 2;
    var REPONSE_HANDLE_SERVICE = 2;                     var RC_OK = 0;   
    
    beforeEach(async () => {
        bmc = await BMC.new('1234.pra');
        bsh1 = await GatherFeeBSH.new(bmc.address, service1, _native, _symbol, _decimals, _fee);
        bsh2 = await GatherFeeBSH.new(bmc.address, service2, _native, _symbol, _decimals, _fee);
        bsh3 = await GatherFeeBSH.new(bmc.address, service3, _native, _symbol, _decimals, _fee);
        holder1 = await Holder.new();
        holder2 = await Holder.new();
        holder3 = await Holder.new();
        accounts = await web3.eth.getAccounts();
        await bmc.approveService(service1);
        await bmc.approveService(service2);
        await bmc.approveService(service3);
        await bmc.addVerifier(_net1, accounts[1]);
        await bmc.addVerifier(_net2, accounts[2]);
        await holder1.addBSHContract(bsh1.address);
        await holder2.addBSHContract(bsh2.address);
        await holder3.addBSHContract(bsh3.address);
        await bsh1.register(_name1, _symbol1, _decimals1, _fee1);
        await bsh1.register(_name2, _symbol2, _decimals2, _fee2);
        await bsh1.register(_name3, _symbol3, _decimals3, _fee3);
        await bsh1.register(_name4, _symbol4, _decimals4, _fee4);

        await bsh2.register(_name1, _symbol1, _decimals1, _fee1);
        await bsh2.register(_name2, _symbol2, _decimals2, _fee2);
        await bsh2.register(_name3, _symbol3, _decimals3, _fee3);
        await bsh2.register(_name4, _symbol4, _decimals4, _fee4);

        await bsh3.register(_name1, _symbol1, _decimals1, _fee1);
        await bsh3.register(_name2, _symbol2, _decimals2, _fee2);
        await bsh3.register(_name3, _symbol3, _decimals3, _fee3);
        await bsh3.register(_name4, _symbol4, _decimals4, _fee4);
        await bmc.transferRequestWithAddress(
            _net1, service1, _from1, holder1.address, _name1, _value1
        );
        await bmc.transferRequestWithAddress(
            _net2, service1, _from2, holder1.address, _name2, _value2
        );
        await bmc.transferRequestWithAddress(
            _net1, service2, _from1, holder2.address, _name1, _value1
        );
        await bmc.transferRequestWithAddress(
            _net2, service2, _from2, holder2.address, _name2, _value2
        );
        await bsh1.transfer(_to1, {from: accounts[0], value: _txAmt});
        await bmc.response(REPONSE_HANDLE_SERVICE, _net1, service1, 0, RC_OK, "");
        await holder1.setApprove(bsh1.address);
        await holder1.callTransfer(_name1, _txAmt1, _to1);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net1, service1, _sn1, RC_OK, "");
        await holder1.callTransfer(_name2, _txAmt2, _to2);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net2, service1, _sn2, RC_OK, "");

        await bsh2.transfer(_to1, {from: accounts[0], value: _txAmt});
        await bmc.response(REPONSE_HANDLE_SERVICE, _net1, service2, 0, RC_OK, "");
        await holder2.setApprove(bsh2.address);
        await holder2.callTransfer(_name1, _txAmt1, _to1);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net1, service2, _sn1, RC_OK, "");
        await holder2.callTransfer(_name2, _txAmt2, _to2);
        await bmc.response(REPONSE_HANDLE_SERVICE, _net2, service2, _sn2, RC_OK, "");
    });

    it('BMC sends a request GatherFee to BSH contract - Success', async() => {
        var FA1Before = await bsh1.getFAOf(_native);     //  state Aggregation Fee of each type of Coins
        var FA2Before = await bsh1.getFAOf(_name1);
        var FA3Before = await bsh1.getFAOf(_name2);
        await bmc.gatherFee(_to1, "bmc", _to1, [service1]);
        var FA1After = await bsh1.getFAOf(_native);
        var FA2After = await bsh1.getFAOf(_name1);
        var FA3After = await bsh1.getFAOf(_name2);
        var fees = await bsh1.getFees(_sn2 + 1);     //  get pending Aggregation Fee list
        assert(
            FA1Before == Math.floor(_txAmt / 100) && 
            FA2Before == Math.floor(_txAmt1 / 100) &&
            FA3Before == Math.floor(_txAmt2 / 100) &&
            FA1After == 0 && FA2After == 0 && FA3After == 0 && 
            fees[0].coinName == _native && fees[0].value == Math.floor(_txAmt / 100) &&
            fees[1].coinName == _name1 && fees[1].value == Math.floor(_txAmt1 / 100) &&
            fees[2].coinName == _name2 && fees[2].value == Math.floor(_txAmt2 / 100)
        );
    });

    //  If BSH has an empty charged fees, BSH contract ignores and returns
    it('BMC sends a request GatherFee to BSH contract - Empty Charged Fee - Success', async() => {
        var FA1Before = await bsh3.getFAOf(_native);     //  state Aggregation Fee of each type of Coins
        var FA2Before = await bsh3.getFAOf(_name1);
        var FA3Before = await bsh3.getFAOf(_name2);
        var feesBefore = await bsh3.getFees(0);     //  get pending Aggregation Fee list
        await bmc.gatherFee(_to1, "bmc", _to1, [service3]);
        var FA1After = await bsh3.getFAOf(_native);
        var FA2After = await bsh3.getFAOf(_name1);
        var FA3After = await bsh3.getFAOf(_name2);
        var feesAfter = await bsh3.getFees(1);     //  get pending Aggregation Fee list
        assert(
            FA1Before == 0 && FA2Before == 0 && FA3Before == 0 &&
            FA1After == 0 && FA2After == 0 && FA3After == 0 && 
            feesBefore.length == 0 && feesAfter.length == 0
        );
    });

    //  If BSH receives an invalid BTP address of Fee Aggregator, BSH contract reverts
    it('BMC sends a request GatherFee to BSH contract - Invalid Fee Aggregator BTP address - Failure', async() => {
        var FA1Before = await bsh1.getFAOf(_native);     //  state Aggregation Fee of each type of Coins
        var FA2Before = await bsh1.getFAOf(_name1);
        var FA3Before = await bsh1.getFAOf(_name2);
        var _fa = 'btp://1234.iconee:0x12345678';
        await bmc.gatherFee(_to1, "bmc", _fa, [service1]);
        var FA1After = await bsh1.getFAOf(_native);
        var FA2After = await bsh1.getFAOf(_name1);
        var FA3After = await bsh1.getFAOf(_name2);
        var fees = await bsh1.getFees(_sn2 + 1);     //  get pending Aggregation Fee list
        assert(
            web3.utils.BN(FA1Before).toNumber() == web3.utils.BN(FA1After).toNumber() &&
            web3.utils.BN(FA2Before).toNumber() == web3.utils.BN(FA2After).toNumber() &&
            web3.utils.BN(FA3Before).toNumber() == web3.utils.BN(FA3After).toNumber() &&
            fees.length == 0
        );
    });


    it('BMC sends a request GatherFee to multiple BSH contracts - Success', async() => {
        var BSH1_FA1Before = await bsh1.getFAOf(_native);     //  state Aggregation Fee of each type of Coins
        var BSH1_FA2Before = await bsh1.getFAOf(_name1);
        var BSH1_FA3Before = await bsh1.getFAOf(_name2);
        var BSH1_feesBefore = await bsh1.getFees(_sn2);     //  get pending Aggregation Fee list

        var BSH2_FA1Before = await bsh2.getFAOf(_native);     //  state Aggregation Fee of each type of Coins
        var BSH2_FA2Before = await bsh2.getFAOf(_name1);
        var BSH2_FA3Before = await bsh2.getFAOf(_name2);
        var BSH2_feesBefore = await bsh2.getFees(_sn2);     //  get pending Aggregation Fee list

        var BSH3_FA1Before = await bsh3.getFAOf(_native);     //  state Aggregation Fee of each type of Coins
        var BSH3_FA2Before = await bsh3.getFAOf(_name1);
        var BSH3_FA3Before = await bsh3.getFAOf(_name2);
        var BSH3_feesBefore = await bsh3.getFees(0);     //  get pending Aggregation Fee list

        await bmc.gatherFee(_to1, "bmc", _to1, [service1, 'ServiceA', service3, 'Service4']);

        var BSH1_FA1After = await bsh1.getFAOf(_native);
        var BSH1_FA2After = await bsh1.getFAOf(_name1);
        var BSH1_FA3After = await bsh1.getFAOf(_name2);
        var BSH1_feesAfter = await bsh1.getFees(_sn2 + 1);     //  get pending Aggregation Fee list

        var BSH2_FA1After = await bsh2.getFAOf(_native);
        var BSH2_FA2After = await bsh2.getFAOf(_name1);
        var BSH2_FA3After = await bsh2.getFAOf(_name2);
        var BSH2_feesAfter = await bsh2.getFees(_sn2 + 1);     //  get pending Aggregation Fee list

        var BSH3_FA1After = await bsh3.getFAOf(_native);
        var BSH3_FA2After = await bsh3.getFAOf(_name1);
        var BSH3_FA3After = await bsh3.getFAOf(_name2);
        var BSH3_feesAfter = await bsh3.getFees(1);     //  get pending Aggregation Fee list

        assert(
            BSH1_FA1Before == Math.floor(_txAmt / 100) && 
            BSH1_FA2Before == Math.floor(_txAmt1 / 100) &&
            BSH1_FA3Before == Math.floor(_txAmt2 / 100) &&
            BSH1_FA1After == 0 && BSH1_FA2After == 0 && BSH1_FA3After == 0 && 
            BSH1_feesBefore.length == 0 &&
            BSH1_feesAfter[0].coinName == _native && BSH1_feesAfter[0].value == Math.floor(_txAmt / 100) &&
            BSH1_feesAfter[1].coinName == _name1 && BSH1_feesAfter[1].value == Math.floor(_txAmt1 / 100) &&
            BSH1_feesAfter[2].coinName == _name2 && BSH1_feesAfter[2].value == Math.floor(_txAmt2 / 100) &&

            web3.utils.BN(BSH2_FA1Before).toNumber() == web3.utils.BN(BSH2_FA1After).toNumber() &&
            web3.utils.BN(BSH2_FA2Before).toNumber() == web3.utils.BN(BSH2_FA2After).toNumber() &&
            web3.utils.BN(BSH2_FA3Before).toNumber() == web3.utils.BN(BSH2_FA3After).toNumber() &&
            BSH2_feesBefore.length == 0 && BSH2_feesAfter.length == 0 &&

            BSH3_FA1Before == 0 && BSH3_FA2Before == 0 && BSH3_FA3Before == 0 &&
            BSH3_FA1After == 0 && BSH3_FA2After == 0 && BSH3_FA3After == 0 && 
            BSH3_feesBefore.length == 0 && BSH3_feesAfter.length == 0
        );
    });
});    


/***************************************************************************************
                                Handle Relay Message Unit Tests
 ***************************************************************************************/

contract('Handle relay message', () => {
    let bmc, accounts, bmv, bsh; 
    var network = '1234.iconee';
    var link; 
    var height = 0;
    var offset = 0;
    var lastHeight = 0;
    var blockInterval = 3000;
    var maxAggregation = 5;
    var delayLimit = 3;
    var relays;
    var BN = web3.utils.BN;

    beforeEach(async () => {
        bmc = await BMC.new('1234.pra');
        accounts = await web3.eth.getAccounts()
        bmv = await MockBMV.new();
        bsh = await MockBSH.new();
        await bmc.requestAddService('Token', bsh.address);
        await bmc.approveService('Token');
        await bmc.addVerifier(network, bmv.address);
        link = 'btp://1234.iconee/0x1234';
        await bmc.addLink(link);
        relays = [accounts[2], accounts[3], accounts[4], accounts[5]];
        await bmc.addRelay(link, relays);
        await bmv.setStatus(height, offset, lastHeight);
        await bmc.setLink(link, blockInterval, maxAggregation, delayLimit);
    });

    it('should revert if relay is invalid', async() => {
        const btpMsg = rlp.encode([
            'btp://1234.iconee/0x1234',
            'btp://1234.pra/' + bmc.address.toLowerCase(),
            'Token',
            '0x01', // rlp encode of signed int
            'message'
        ]);
        
        let relayMsg = URLSafeBase64.encode(btpMsg);
        relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, "=");

        await truffleAssert.reverts(bmc.handleRelayMessage(link, relayMsg), 'BMCRevertUnauthorized: invalid relay');
        
        let bmcLink = await bmc.getLink('btp://1234.iconee/0x1234');
        assert.equal(bmcLink[2], 0, "failed to update rxSeq");
        assert.equal(bmcLink[3], 0, "failed to update txSeq");
    });

    it('should revert if relay is not registered', async() => {
        await bmc.removeLink(link);

        const btpMsg = rlp.encode([
            'btp://1234.iconee/0x1234',
            'btp://1234.pra/' + bmc.address,
            'Token',
            '0x01',
            'message'
        ]);
        
        let relayMsg = URLSafeBase64.encode(btpMsg);
        relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, "=");

        await truffleAssert.reverts(bmc.handleRelayMessage(link, 'base64EncodeRelayMessage'), 'BMCRevertUnauthorized: not registered relay');
        let bmcLink = await bmc.getLink('btp://1234.iconee/0x1234');
        assert.equal(bmcLink[2], 0, "failed to update rxSeq");
        assert.equal(bmcLink[3], 0, "failed to update txSeq");
    });

    it('should dispatch btp message to BSH', async() => {
        const transferCoin = [
            '0xaaa',
            'btp://1234.pra/0xbbb',
            'ICX',
            12
        ];

        const btpMsg = rlp.encode([
            'btp://1234.iconee/0x1234',
            'btp://1234.pra/' + bmc.address,
            'Token',
            '0x01',
            rlp.encode(transferCoin)
        ]);

        let relayMsg = URLSafeBase64.encode(btpMsg);
        relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, "=");

        let res = await bmc.handleRelayMessage(link, relayMsg, {from: accounts[3]});
        assert.isNotEmpty(res);
        let bmcLink = await bmc.getLink('btp://1234.iconee/0x1234');
        assert.equal(bmcLink[2], 1, "failed to update rxSeq");
        assert.equal(bmcLink[3], 0, "invalid txSeq");
    });

    it('should process LINK and UNLINK via btp messages', async() => {
        const btpAddress = 'btp://1234.eth/' + web3.utils.randomHex(20);
        let eventMsg = [
            'Link', 
            [
                'btp://1234.iconee/0x1234',
                btpAddress,
            ]
        ];

        let btpMsg = rlp.encode([
            'btp://1234.iconee/0x1234',
            'btp://1234.pra/' + bmc.address,
            '_EVENT',
            '0x00',
            rlp.encode(eventMsg)
        ]);

        let relayMsg = URLSafeBase64.encode(btpMsg);
        relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, "=");

        let res = await bmc.handleRelayMessage(link, relayMsg, {from: accounts[3]});
        assert.isNotEmpty(res);

        let bmcLink = await bmc.getLink('btp://1234.iconee/0x1234');
        assert.equal(bmcLink[1][0], btpAddress, "invalid reachable btp address");
        assert.equal(bmcLink[2], 1, "failed to update rxSeq");
        assert.equal(bmcLink[3], 0, "invalid txSeq");

        eventMsg = [
            'Unlink', 
            [
                'btp://1234.iconee/0x1234',
                btpAddress
            ]
        ];

        btpMsg = rlp.encode([
            'btp://1234.iconee/0x1234',
            'btp://1234.pra/' + bmc.address,
            '_EVENT',
            '0x01',
            rlp.encode(eventMsg)
        ]);

        relayMsg = URLSafeBase64.encode(btpMsg);
        relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, "=");

        res = await bmc.handleRelayMessage(link, relayMsg, {from: accounts[4]});
        assert.isNotEmpty(res);

        bmcLink = await bmc.getLink('btp://1234.iconee/0x1234');
        assert.isEmpty(bmcLink[1][0], "failed to unlink");   
        assert.equal(bmcLink[2], 2, "failed to update rxSeq");
        assert.equal(bmcLink[3], 0, "invalid txSeq");
    });

    it('should emit event if routes are failed to resolve', async() => {
        const btpAddress = 'btp://1234.eth/' + web3.utils.randomHex(20);
        let eventMsg = [
            0, // LINK,
            [
                'btp://1234.iconee/0x1234',
                btpAddress,
            ]
        ];

        let btpMsg = rlp.encode([
            'btp://1234.iconee/0x1234',
            'btp://1234.solana/' + bmc.address,
            '_EVENT',
            '0x02',
            rlp.encode(eventMsg)
        ]);

        let relayMsg = URLSafeBase64.encode(btpMsg);
        relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, "=");

        const tx = await bmc.handleRelayMessage(link, relayMsg, {from: accounts[3]});
        let bmcMessage;
        await truffleAssert.eventEmitted(tx, 'Message', (ev) => {
            bmcMessage = rlp.decode(ev[2]);
            return ev[0] === link && ev[1].toNumber() === 1;
        });

        assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[0].toString('hex')), 'btp://1234.pra/' + bmc.address);
        assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[1].toString('hex')), 'btp://1234.iconee/0x1234');
        assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[2].toString('hex')), '_EVENT');
        assert.equal(bmcMessage[3].toString('hex'), 'fe'); // two complement form of -2
        const errResponse = rlp.decode(bmcMessage[4]);
        assert.equal(web3.utils.hexToNumber('0x' + errResponse[0].toString('hex')), 10); // BMC_ERR = 10
        assert.equal(web3.utils.hexToUtf8('0x' + errResponse[1].toString('hex')), 'BMCRevertUnreachable: 1234.solana is unreachable');

        bmcLink = await bmc.getLink('btp://1234.iconee/0x1234');
        assert.equal(bmcLink[2], 1, "failed to update rxSeq");
        assert.equal(bmcLink[3], 1, "failed to update txSeq");
    });

    it('should emit event if routes are succeeded to resolve', async() => {
        const destBtpAddress = 'btp://1234.pra/' + bmc.address;
        const routeBtpAddress = 'btp://1234.solana/' + web3.utils.randomHex(20);
        
        await bmc.addRoute(
            routeBtpAddress,
            destBtpAddress
        );

        await bmc.addVerifier('1234.pra', web3.utils.randomHex(20));
        await bmc.addLink(destBtpAddress);

        const transferCoin = [
            '0xaaa',
            'btp://1234.pra/0xbbb',
            'ICX',
            12
        ];

        let btpMsg = rlp.encode([
            'btp://1234.iconee/0x1234',
            routeBtpAddress,
            'Token',
            '0x02',
            rlp.encode(transferCoin)
        ]);

        let relayMsg = URLSafeBase64.encode(btpMsg);
        relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, "=");

        const tx = await bmc.handleRelayMessage(link, relayMsg, {from: accounts[4]});

        await truffleAssert.eventEmitted(tx, 'Message', (ev) => {
            return ev[0] === destBtpAddress && ev[1].toNumber() === 1 && ev[2].toString() === '0x' + btpMsg.toString('hex');
        });

        let bmcLink = await bmc.getLink('btp://1234.iconee/0x1234');
        assert.equal(bmcLink[2], 1, "failed to update rxSeq");
        assert.equal(bmcLink[3], 0, "invalid txSeq");

        bmcLink = await bmc.getLink(destBtpAddress);
        assert.equal(bmcLink[3], 1, "invalid txSeq");
    });

    it('should emit message to bsh if dest in msg differs from bmc address (msg.sn >= 0)', async() => {
        const transferCoin = [
            '0xaaa',
            'btp://1234.pra/0xbbb',
            'ICX',
            12
        ];

        const btpMsg = rlp.encode([
            'btp://1234.iconee/0x1234',
            'btp://1234.pra/' + bmc.address,
            'Token',
            '0x01',
            rlp.encode(transferCoin)
        ]);

        let relayMsg = URLSafeBase64.encode(btpMsg);
        relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, "=");

        let res = await bmc.handleRelayMessage(link, relayMsg, {from: accounts[3]});
        assert.isNotEmpty(res);
    });

    it('should emit error message to bsh if dest in received msg differs from bmc address (msg.sn >= 0) and bsh gets errors', async() => {
        const transferCoin = [
            '0xaaa',
            'btp://1234.pra/0xbbb',
            'ICX',
            12
        ];

        const btpMsg = rlp.encode([
            'btp://1234.iconee/0x1234',
            'btp://1234.pra/' + bmc.address,
            'Token',
            '0x03e8', // two complement of 1000
            rlp.encode(transferCoin)
        ]);

        let relayMsg = URLSafeBase64.encode(btpMsg);
        relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, "=");

        let tx = await bmc.handleRelayMessage(link, relayMsg, {from: accounts[3]});
        let bmcMessage;
        await truffleAssert.eventEmitted(tx, 'Message', (ev) => {
            bmcMessage = rlp.decode(ev[2]);
            return ev[0] === link && ev[1].toNumber() === 1;
        });

        assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[0].toString('hex')), 'btp://1234.pra/' + bmc.address);
        assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[1].toString('hex')), 'btp://1234.iconee/0x1234');
        assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[2].toString('hex')), 'Token');
        assert.equal(bmcMessage[3].toString('hex'), 'fc18'); // two complement of -1000
        const errResponse = rlp.decode(bmcMessage[4]);
        assert.equal(web3.utils.hexToNumber('0x' + errResponse[0].toString('hex')), 40); // BSH_ERR = 40
        assert.equal(web3.utils.hexToUtf8('0x' + errResponse[1].toString('hex')), 'Mocking error message on handleBTPMessage');

        bmcLink = await bmc.getLink('btp://1234.iconee/0x1234');
        assert.equal(bmcLink[2], 1, "failed to update rxSeq");
        assert.equal(bmcLink[3], 1, "failed to update txSeq");
    });

    it('should emit message to bsh if dest in received msg differs from bmc address (msg.sn < 0)', async() => {
        const errResponse = [
            0,
            'Invalid service',
        ];

        const btpMsg = rlp.encode([
            'btp://1234.iconee/0x1234',
            'btp://1234.pra/' + bmc.address,
            'Token',
            '0xf6', // two complement of -10
            rlp.encode(errResponse)
        ]);

        let relayMsg = URLSafeBase64.encode(btpMsg);
        relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, "=");

        let res = await bmc.handleRelayMessage(link, relayMsg, {from: accounts[3]});
        assert.isNotEmpty(res);
    });

    it('should emit message to bsh if dest in received msg differs from bmc address (msg.sn < 0) and bsh gets errors', async() => {
        const errResponse = [
            0,
            'Invalid service',
        ];

        const btpMsg = rlp.encode([
            'btp://1234.iconee/0x1234',
            'btp://1234.pra/' + bmc.address,
            'Token',
            '0xfc18', // two complement of -1000
            rlp.encode(errResponse)
        ]);

        let relayMsg = URLSafeBase64.encode(btpMsg);
        relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, "=");

        let tx = await bmc.handleRelayMessage(link, relayMsg, {from: accounts[3]});
        await truffleAssert.eventEmitted(tx, 'ErrorOnBTPError', (ev) => {
            return ev[0] === 'Token' && ev[1].toNumber() === 1000 &&
                ev[2].toNumber() === 0 && ev[3].toString() === 'Invalid service' &&
                ev[4].toNumber() === 40 && ev[5].toString() === 'Mocking error message on handleBTPError';
        });
    });

    it('should emit message to bsh if dest in received msg differs from bmc address (msg.sn < 0) and bsh gets low level errors', async() => {
        const errResponse = [
            12,
            'Invalid service',
        ];

        const btpMsg = rlp.encode([
            'btp://1234.iconee/0x1234',
            'btp://1234.pra/' + bmc.address,
            'Token',
            '0x9c', // two complement of -100
            rlp.encode(errResponse)
        ]);

        let relayMsg = URLSafeBase64.encode(btpMsg);
        relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, "=");

        let tx = await bmc.handleRelayMessage(link, relayMsg, {from: accounts[3]});
        await truffleAssert.eventEmitted(tx, 'ErrorOnBTPError', (ev) => {
            return ev[0] === 'Token' && ev[1].toNumber() === 100 &&
                ev[2].toNumber() === 12 && ev[3].toString() === 'Invalid service' &&
                ev[4].toNumber() === 0 && ev[5].toString() === '';
        });
    });
});

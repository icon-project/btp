const BMCManagement = artifacts.require('BMCManagement');
const BMCPeriphery = artifacts.require('BMCPeriphery');
const MockBMCManagement = artifacts.require('MockBMCManagement');
const MockBMV = artifacts.require('MockBMV');
const MockBSH = artifacts.require('MockBSH');
const BMCManagementV2 = artifacts.require('BMCManagementV2');
const BMCPeripheryV2 = artifacts.require('BMCPeripheryV2');

const { assert } = require('chai');
const truffleAssert = require('truffle-assertions');
const URLSafeBase64 = require('urlsafe-base64');
const rlp = require('rlp');

const { deployProxy, upgradeProxy } = require('@openzeppelin/truffle-upgrades');

contract('BMC tests', (accounts) => {
    describe('BMC Basic Unit Tests', () => {
        let bmcManagement, bmcPeriphery, bmv1, bmv2, bmv3, bmv4;
    
        before(async () => {
            bmcManagement = await deployProxy(BMCManagement);
            bmcPeriphery = await deployProxy(BMCPeriphery, ['0x1234.pra', bmcManagement.address]);
            await bmcManagement.setBMCPeriphery(bmcPeriphery.address);
            bmv1 = await MockBMV.new();
            bmv2 = await MockBMV.new();
            bmv3 = await MockBMV.new();
            bmv4 = await MockBMV.new();
        });
    
        it('check contract code size', async () => {
            console.log('BMC Management : ', (BMCManagement.deployedBytecode.length / 2) - 1);
            assert.isBelow((BMCManagement.deployedBytecode.length / 2) - 1, 24576, 'contract size is restricted to 24KB');
            console.log('BMC Periphery : ', (BMCPeriphery.deployedBytecode.length / 2) - 1);
            assert.isBelow((BMCPeriphery.deployedBytecode.length / 2) - 1, 24576, 'contract size is restricted to 24KB');
        });
    
        /***************************************************************************************
                                    Set BMC Periphery Unit Tests
        ***************************************************************************************/
        it('Set BMC Periphery - should revert if param input is address(0)', async () => {
            await truffleAssert.reverts(
                bmcManagement.setBMCPeriphery.call('0x0000000000000000000000000000000000000000'),
                'BMCRevertInvalidAddress'
            );
        });
    
        it('Set BMC Periphery - should revert if param input is duplicate address', async () => {
            await truffleAssert.reverts(
                bmcManagement.setBMCPeriphery.call(bmcPeriphery.address),
                'BMCRevertAlreadyExistsBMCPeriphery'
            );
        });
    
        it('Set BMC Periphery successfully', async () => {
            await bmcManagement.setBMCPeriphery('0x0000000000000000000000000000000000000001');
            await bmcManagement.setBMCPeriphery(bmcPeriphery.address);
        });
    
        /***************************************************************************************
                                    Add/Remove Service Unit Tests
        ***************************************************************************************/
        it('Request Add Service - Service not requested nor registered - Success', async () => {
            let service = 'Coin/WrappedCoin';
            
            await bmcPeriphery.requestAddService(service, accounts[5]);
            
            let output = await bmcManagement.getPendingRequest();
            assert(
                output[0].serviceName === service, output[0].bsh === accounts[5],
            );
        });
    
        it('Request Add Service - Same Service name is pending - Failure', async () => {
            let service = 'Coin/WrappedCoin';
        
            await truffleAssert.reverts(
                bmcPeriphery.requestAddService.call(service, accounts[6]),
                'BMCRevertRequestPending'
            );
        });
    
        it('Request Add Service - Service registered - Failure', async () => {
            let service = 'Coin/WrappedCoin';
            
            await bmcManagement.approveService(service, true);
            await truffleAssert.reverts(
                bmcPeriphery.requestAddService.call(service, accounts[6]),
                'BMCRevertAlreadyExistsBSH'
            );
        });
    
        it('Approve Service - Service existed - Failure', async () => {
            let service = 'Coin/WrappedCoin';
            await truffleAssert.reverts(
                bmcManagement.approveService.call(service, true),
                'BMCRevertAlreadyExistsBSH'
            );
        });
    
        it('Reject Service - Service existed - Failure', async () => {
            let service = 'Coin/WrappedCoin';
            await truffleAssert.reverts(
                bmcManagement.approveService.call(service, false),
                'BMCRevertAlreadyExistsBSH'
            );
        });
    
    
        it('Approve Service - Service request not existed - Failure', async () => {
            let service = 'Token';
            await truffleAssert.reverts(
                bmcManagement.approveService.call(service, true),
                'BMCRevertNotExistRequest'
            );
        });
    
        it('Reject Service - Service request not existed - Failure', async () => {
            let service = 'Token';
            await truffleAssert.reverts(
                bmcManagement.approveService.call(service, true),
                'BMCRevertNotExistRequest'
            );
        });
    
        it('Approve Service - Without Permission - Failure', async () => {
            let service = 'Token';
            await bmcPeriphery.requestAddService(service, accounts[6]);
            await truffleAssert.reverts(
                bmcManagement.approveService.call(service, true, {from: accounts[1]}),
                'BMCRevertUnauthorized'
            );
        });
    
        it('Reject Service - Without Permission - Failure', async () => {
            let service = 'Token';
            await truffleAssert.reverts(
                bmcManagement.approveService.call(service, false, {from: accounts[1]}),
                'BMCRevertUnauthorized'
            );
        });
    
        it('Approve Service - With Permission - Success', async () => {
            let service = 'Token';
            
            await bmcManagement.approveService(service, true);
            let output = await bmcManagement.getServices();
            assert(
                output.length === 2,
                output[1].svc === service,
                output[1].addr === accounts[6]
            );
        });
    
        it('Reject Service - With Permission - Success', async () => {
            let service = 'TokenA';
    
            await bmcPeriphery.requestAddService(service, accounts[6]);
            await bmcManagement.approveService(service, false);
            let output = await bmcManagement.getServices();
            assert(
                output.length === 2,
                output[1].svc === service,
                output[1].addr === accounts[6]
            );
        });
    
        it('Remove Service - Without Permission - Failure', async () => {
            let service = 'Coin/WrappedCoin';
            await truffleAssert.reverts(
                bmcManagement.removeService.call(service, {from: accounts[1]}),
                'BMCRevertUnauthorized'
            );
        });
    
        it('Remove Service - Service Not Existed - Failure', async () => {
            let service = 'Token A';
            await truffleAssert.reverts(
                bmcManagement.removeService.call(service),
                'BMCRevertNotExistsBSH'
            );
        });
    
        it('Remove Service - Service Existed and With Permission - Success', async () => {
            let service1 = 'Token';
            await bmcManagement.removeService(service1);
            let output = await bmcManagement.getServices();
            assert(
                output.length === 1,
                output[0].svc === 'Coin/WrappedCoin',
                output[0].addr == accounts[5]
            );
        });  
    
        /***************************************************************************************
                                    Add/Remove Verifier Unit Tests
        ***************************************************************************************/
        it('Add Verifier - With Permission - Success', async () => {
            let network = '1234.iconee';
            await bmcManagement.addVerifier(network, bmv1.address);
            let output = await bmcManagement.getVerifiers();
            assert(
                output[0].net === network, output[0].addr === bmv1.address,
            );
        });
    
        it('Add Verifier - Without Permission - Failure', async () => {
            let network = '1234.iconee';
            await truffleAssert.reverts(
                bmcManagement.addVerifier.call(network, bmv1.address, {from: accounts[1]}),
                'BMCRevertUnauthorized'
            );
        });
    
        it('Add Verifier - Service Existed - Failure', async () => {
            let network = '1234.iconee';
            await truffleAssert.reverts(
                bmcManagement.addVerifier.call(network, bmv1.address),
                'BMCRevertAlreadyExistsBMV'        
            );
        });
    
        it('Remove Verifier - Without Permission - Failure', async () => {
            let network = '1234.iconee';
            await truffleAssert.reverts(
                bmcManagement.removeVerifier.call(network, {from: accounts[1]}),
                'BMCRevertUnauthorized'
            );
        });
    
        it('Remove Verifier - Service Not Existed - Failure', async () => {
            let network = '1234.pra';
            await truffleAssert.reverts(
                bmcManagement.removeVerifier.call(network),
                'BMCRevertNotExistsBMV'
            );
        });
    
        it('Remove Verifier - Verifier Existed and With Permission - Success', async () => {
            let network1 = '1234.pra';
            let network2 = '1234.eos';
            let network3 = '1234.eth';
            await bmcManagement.addVerifier(network1, bmv2.address);
            await bmcManagement.addVerifier(network2, bmv3.address);
            await bmcManagement.addVerifier(network3, bmv4.address);
            await bmcManagement.removeVerifier('1234.iconee');
            let output = await bmcManagement.getVerifiers();
            assert(
                output[0].net == network3, output[0].addr == bmv4.address,
                output[1].net == network1, output[0].addr == bmv2.address,
                output[2].net == network2, output[0].addr == bmv3.address,
            );
        });
    
        /***************************************************************************************
                                    Add/Remove Link Unit Tests
        ***************************************************************************************/
        it('Add Link - With Permission - Success', async () => {
            let link = 'btp://1234.pra/0x1234';
            await bmcManagement.addLink(link);
            let output = await bmcManagement.getLinks();
            assert(
                output[0] === link
            );
        });
    
        it('Add Link - Without Permission - Failure', async () => {
            let link = 'btp://1234.pra/0x1234';
            await truffleAssert.reverts(
                bmcManagement.addLink.call(link, {from: accounts[1]}),
                'BMCRevertUnauthorized'
            );
        });  
        
        //  BMC adds supporting link, but network has not yet registered any BMV
        it('Add Link - Verifier Not Existed - Failure', async () => {
            let link = 'btp://1234.btc/0x1234';
            await truffleAssert.reverts(
                bmcManagement.addLink.call(link),
                'BMCRevertNotExistsBMV'
            );
        });  
    
        it('Add Link - Invalid BTP Address Format - Failure', async () => {
            let link = 'btp://1234.eos:0x1234';
            await truffleAssert.reverts(
                bmcManagement.addLink.call(link),
                ''
            );
        });  
                                    
        it('Add Link - Link Existed - Failure', async () => {
            let link = 'btp://1234.pra/0x1234';
            await truffleAssert.reverts(
                bmcManagement.addLink.call(link),
                'BMCRevertAlreadyExistsLink'
            );
        }); 
        
        it('Remove Link - Without Permission - Failure', async () => {
            let link = 'btp://1234.pra/0x1234';
            await truffleAssert.reverts(
                bmcManagement.removeLink.call(link, {from: accounts[1]}),
                'BMCRevertUnauthorized'
            );
        });
    
        it('Remove Link - Link Not Existed - Failure', async () => {
            let link = 'btp://1234.btc/0x1234';
            await truffleAssert.reverts(
                bmcManagement.removeLink.call(link),
                'BMCRevertNotExistsLink'
            );
        });
    
        it('Remove Link - Link Existed and With Permission - Success', async () => {
            let link1 = 'btp://1234.eos/0x1234';
            let link2 = 'btp://1234.eth/0x1234';
            await bmcManagement.addLink(link1);
            await bmcManagement.addLink(link2);
            await bmcManagement.removeLink('btp://1234.pra/0x1234');
            let output = await bmcManagement.getLinks();
            assert(
                output[0] === link2,
                output[1] === link1,
            );
        });    
    
        /***************************************************************************************
                                    Set Link Stats Unit Tests
        ***************************************************************************************/
        it('Set Link - Without Permission - Failure', async () => {
            let link = 'btp://1234.eth/0x1234'; 
            let blockInterval = 15000;
            let maxAggregation = 5;
            let delayLimit = 4;
            await truffleAssert.reverts(
                bmcManagement.setLink.call(link, blockInterval, maxAggregation, delayLimit, {from: accounts[1]}),
                'BMCRevertUnauthorized'
            );
        });
    
        it('Set Link - Link not Existed - Failure', async () => {
            let link = 'btp://1234.pra/0x1234'; 
            let blockInterval = 6000;
            let maxAggregation = 7;
            let delayLimit = 3;
            await truffleAssert.reverts(
                bmcManagement.setLink.call(link, blockInterval, maxAggregation, delayLimit),
                'BMCRevertNotExistsLink'
            );
        });
    
        it('Set Link - Invalid Max Aggregation - Failure', async () => {
            let link = 'btp://1234.eth/0x1234'; 
            let blockInterval = 6000;
            let maxAggregation = 0;
            let delayLimit = 3;
            await truffleAssert.reverts(
                bmcManagement.setLink.call(link, blockInterval, maxAggregation, delayLimit),
                'BMCRevertInvalidParam'
            );
        });
    
        it('Set Link - Invalid Delay Limit - Failure', async () => {
            let link = 'btp://1234.eth/0x1234'; 
            let blockInterval = 6000;
            let maxAggregation = 5;
            let delayLimit = 0;
            await truffleAssert.reverts(
                bmcManagement.setLink.call(link, blockInterval, maxAggregation, delayLimit),
                'BMCRevertInvalidParam'
            );
        });
    
        it('Set Link - With Permission and Valid Settings - Success', async () => {
            let link = 'btp://1234.eth/0x1234'; 
            let blockInterval = 15000;
            let maxAggregation = 5;
            let delayLimit = 4;
            let height = 1500;
            let offset = 1400;
            let lastHeight = 1450;
            await bmv4.setStatus(height, offset, lastHeight);
            await bmcManagement.setLink(link, blockInterval, maxAggregation, delayLimit);
            let status = await bmcPeriphery.getStatus(link);
            assert.equal(status.delayLimit, delayLimit, 'invalid delay limit');
            assert.equal(status.maxAggregation, maxAggregation, 'invalid max aggregation');
            assert.equal(status.blockIntervalDst, blockInterval, 'invalid block interval');
        });
    
        /***************************************************************************************
                                    Add/Remove Route Unit Tests
        ***************************************************************************************/
        it('Add Route - With Permission - Success', async () => {
            let dst = 'btp://1234.iconee/0x1234';
            let link = 'btp://1234.pra/0x1234';
            await bmcManagement.addRoute(dst, link);
            let output = await bmcManagement.getRoutes();
            assert(
                output[0].dst === dst, output[0].next === link,
            );
        });
                                
        it('Add Route - Without Permission - Failure', async () => {
            let dst = 'btp://1234.iconee/0x1234';
            let link = 'btp://1234.pra/0x1234';
            await truffleAssert.reverts(
                bmcManagement.addRoute.call(dst, link, {from: accounts[1]}),
                'BMCRevertUnauthorized'
            );
        });
    
        it('Add Route - Route Existed - Failure', async () => {
            let dst = 'btp://1234.iconee/0x1234';
            let link = 'btp://1234.pra/0x1234';
            await truffleAssert.reverts(
                bmcManagement.addRoute.call(dst, link),
                'BTPRevertAlreadyExistRoute'        
            );
        });
    
        it('Add Route - Destination/Link is invalid BTP Format Address - Failure', async () => {
            let dst = 'btp://1234.iconee:0x1234';
            let link = 'btp://1234.pra/0x1234';
            await truffleAssert.reverts(
                bmcManagement.addRoute.call(dst, link),
                ''
            );
        });
    
        it('Remove Route - Without Permission - Failure', async () => {
            let dst = 'btp://1234.iconee/0x1234';
            await truffleAssert.reverts(
                bmcManagement.removeRoute.call(dst, {from: accounts[1]}),
                'BMCRevertUnauthorized'
            );
        });
    
        it('Remove Route - Route Not Existed - Failure', async () => {
            let dst = 'btp://1234.eos/0x1234';
            await truffleAssert.reverts(
                bmcManagement.removeRoute.call(dst),
                'BTPRevertNotExistRoute'
            );
        });
    
        it('Remove Route - Route Existed and With Permission - Success', async () => {
            let dst1 = 'btp://1234.pra/0x1234';
            let link1 = 'btp://1234.eos/0x1234';
            let dst2 = 'btp://1234.eth/0x1234';
            let link2 = 'btp://1234.iconee/0x1234';
            await bmcManagement.addRoute(dst1, link1);
            await bmcManagement.addRoute(dst2, link2);
            await bmcManagement.removeRoute('btp://1234.iconee/0x1234');
            let output = await bmcManagement.getRoutes();
            assert(
                output[0].dst === dst2, output[0].next === link1,
                output[1].dst === dst1, output[0].addr === link1,
            );
        });     
        
        /***************************************************************************************
                                    Add/Remove Relays Unit Tests
        ***************************************************************************************/
    
        it('Add Relays - Without Permission - Failure', async () => {
            let link = 'btp://1234.eos/0x1234';
            let relays = [accounts[2], accounts[3], accounts[4]];
            await truffleAssert.reverts(
                bmcManagement.addRelay.call(link, relays, {from: accounts[1]}),
                'BMCRevertUnauthorized'
            );
        });
    
        it('Add Relays - Link not existed - Failure', async () => {
            let link = 'btp://1234.pra/0x1234';
            let relays = [accounts[2], accounts[3], accounts[4]];
            await truffleAssert.reverts(
                bmcManagement.addRelay.call(link, relays),
                'BMCRevertNotExistsLink'        
            );
        });
    
        it('Add Relays - With Permission - Success', async () => {
            let link = 'btp://1234.eos/0x1234';
            let relays = [accounts[2], accounts[3], accounts[4]];
            await bmcManagement.addRelay(link, relays);
            let result = await bmcManagement.getRelays(link);
            assert(
                result[0] === accounts[2], result[1] === accounts[3], result[2] === accounts[4]
            );
        });
    
        it('Add Relays - Update Relays on existing one (Overwrite) - Success', async () => {
            let link = 'btp://1234.eos/0x1234';
            let relays = [accounts[2], accounts[4], accounts[6], accounts[7]];
            await bmcManagement.addRelay(link, relays);
            let result = await bmcManagement.getRelays(link);
            assert(
                result[0] === accounts[2], result[1] === accounts[4], 
                result[2] === accounts[6], result[3] === accounts[7]
            );
        });
    
        it('Remove Relay - Without Permission - Failure', async () => {
            let link = 'btp://1234.eos/0x1234';
            let relay = accounts[2];
            await truffleAssert.reverts(
                bmcManagement.removeRelay.call(link, relay, {from: accounts[1]}),
                'BMCRevertUnauthorized'
            );
        });
    
        it('Remove Relay - Link Not Existed - Failure', async () => {
            let link = 'btp://1234.pra/0x1234';
            let relay = accounts[2];
            await truffleAssert.reverts(
                bmcManagement.removeRelay.call(link, relay),
                'BMCRevertUnauthorized'
            );
        });
    
        it('Remove Relay - None of any relays - Failure', async () => {
            let link = 'btp://1234.eth/0x1234';
            let relay = accounts[2];
            await truffleAssert.reverts(
                bmcManagement.removeRelay.call(link, relay),
                'BMCRevertUnauthorized'
            );
        });
    
        it('Remove Relay - Link/Relays existed and With permission - Success', async () => {
            let link = 'btp://1234.eos/0x1234';
            let relay = accounts[6];
            await bmcManagement.removeRelay(link, relay);
            let result = await bmcManagement.getRelays(link);
            assert(
                result[0] === accounts[2], result[1] === accounts[4], 
                result[2] === accounts[7]
            );
        });
    });

    describe('BMC Relay Rotation Unit Test - Rotate_term = 0', () => {
        let bmcManagement, bmcPeriphery, bmv;
        let network = '1234.iconee';
        let link = 'btp://1234.iconee/0x1234'; 
        let height = 0;
        let offset = 0;
        let lastHeight = 0;
        let relays;
        before(async () => {
            bmcManagement = await deployProxy(MockBMCManagement);
            bmcPeriphery = await deployProxy(BMCPeriphery, ['1234.iconee', bmcManagement.address]);
            await bmcManagement.setBMCPeriphery(bmcPeriphery.address);
            bmv = await MockBMV.new();
            await bmcManagement.addVerifier(network, bmv.address);
            await bmcManagement.addLink(link);
            relays = [accounts[2], accounts[3], accounts[4], accounts[5]];
            await bmcManagement.addRelay(link, relays);
            await bmv.setStatus(height, offset, lastHeight);
        });
    
        it('Link not set - Return address(0)', async () => {
            await bmcManagement.relayRotation(link, 26, false);
            let relay_info = await bmcManagement.getRelay();
            assert(relay_info.r === '0x0000000000000000000000000000000000000000');
        });
    });

    describe('BMC Relay Rotation Unit Test - Rotate_term != 0', () => {
        let bmcManagement, bmcPeriphery, bmv;
        let network = '1234.iconee';
        let link = 'btp://1234.iconee/0x1234'; 
        let height = 0;
        let offset = 0;
        let lastHeight = 0;
        let blockInterval = 3000;
        let maxAggregation = 5;
        let delayLimit = 3;
        let relays;
        let current;
        before(async () => {
            bmcManagement = await deployProxy(MockBMCManagement);
            bmcPeriphery = await deployProxy(BMCPeriphery, ['1234.iconee', bmcManagement.address]);
            await bmcManagement.setBMCPeriphery(bmcPeriphery.address);
            bmv = await MockBMV.new();
            await bmcManagement.addVerifier(network, bmv.address);
            await bmcManagement.addLink(link);
            relays = [accounts[2], accounts[3], accounts[4], accounts[5]];
            await bmcManagement.addRelay(link, relays);
            await bmv.setStatus(height, offset, lastHeight);
            await bmcManagement.setLink(link, blockInterval, maxAggregation, delayLimit);
        });
    
        //  [block(t+1), block(t+15)] => Relay 1
        it('Relay Rotation - Base Message (hasMsg = false) - Relay 1 Allowable', async () => {
            //  After setting a link, consider block_time = block(t+1)
            await bmcManagement.mineOneBlock();       // block(t+2)
            await bmcManagement.relayRotation(link, 0, false);    //  block(t+3)
            let relay_info = await bmcManagement.getRelay();
            assert(relay_info.r == accounts[2]);
        });
    
        //  [block(t+1), block(t+15)] => Relay 1
        it('Relay Rotation - Base Message (hasMsg = false) - Relay 1 Still Allowable', async () => {
            for (i = 0; i < 10; i++) {
                await bmcManagement.mineOneBlock();
            }
            //  After a loop, block_time = block(t+13)
            await bmcManagement.relayRotation(link, 0, false);    //  block(t+14)
            let relay_info = await bmcManagement.getRelay();
            assert(relay_info.r == accounts[2]);
        });
    
        //  [block(t+16), block(t+30)] => Relay 2
        it('Relay Rotation - Base Message (hasMsg = false) - Next Relay = Relay 2 Allowable', async () => {
            for (i = 0; i < 10; i++) {
                await bmcManagement.mineOneBlock();
            }
            //  After loop, block_time = block(t+24)
            await bmcManagement.relayRotation(link, 0, false);    //  block(t+25)
            let relay_info = await bmcManagement.getRelay();
            assert(relay_info.r == accounts[3]);
        });
    
        //  [block(t+31), block(t+45)] => Relay 3
        it('Relay Rotation - Base Message (hasMsg = false) - Next Relay = Relay 3 Allowable', async () => {
            for (i = 0; i < 10; i++) {
                await bmcManagement.mineOneBlock();
            }
            //  After loop, block_time = block(t+35)
            await bmcManagement.relayRotation(link, 0, false);    //  block(t+36)
            let relay_info = await bmcManagement.getRelay();
            current = relay_info.cb;
            assert(relay_info.r == accounts[4]);
        });
    
        //  [block(t+31), block(t+45)] => Relay 3
        it('Relay Rotation - BTP Message (hasMsg = true) - Relay 3 Still Allowable', async () => {
            for (i = 0; i < 9; i++) {
                await bmcManagement.mineOneBlock();
            }
            //  After loop, block_time = block(t+45)
            let linkStat = await bmcPeriphery.getStatus(link);
            let scale = parseInt(linkStat.blockIntervalSrc) / parseInt(linkStat.blockIntervalDst);
            let block = Math.floor((parseInt(current)+9)*scale);
            //  At the time calling relayRotation()
            //  block_time = block(t+45). Thus, Relay 3 is still allowable
            await bmcManagement.relayRotation(link, block, true);    //  block(t+46)
            let relay_info = await bmcManagement.getRelay();
            current = relay_info.cb;
            assert(relay_info.r == accounts[4]);
        });
    
        //  [block(t+46), block(t+61)] => Relay 4
        it('Relay Rotation - BTP Message (hasMsg = true) - Relay 4 Allowable', async () => {
            for (i = 0; i < 5; i++) {
                await bmcManagement.mineOneBlock();
            }
            //  After loop, block_time = block(t+51)
            let linkStat = await bmcPeriphery.getStatus(link);
            let scale = parseInt(linkStat.blockIntervalSrc) / parseInt(linkStat.blockIntervalDst);
            let block = Math.floor((parseInt(current)+5)*scale);
            await bmcManagement.relayRotation(link, block, true);    //  block(t+52)
            let relay_info = await bmcManagement.getRelay();
            current = relay_info.cb;
            assert(relay_info.r == accounts[5]);
        });
    
        //  [block(t+46), block(t+60)] => Relay 4
        //  However, Relay 4 not relay BTP Message on time 'delay_limit'
        //  Move to next
        it('Relay Rotation - BTP Message (hasMsg = true) - Relay 4 Overtime, Relay 1 Allowable', async () => {
            for (i = 0; i < 2; i++) {
                await bmcManagement.mineOneBlock();
            }
            //  After loop, block_time = block(t+54)
            let linkStat = await bmcPeriphery.getStatus(link);
            let scale = parseInt(linkStat.blockIntervalSrc) / parseInt(linkStat.blockIntervalDst);
            let block = Math.floor((parseInt(current)+2)*scale);
            //  If relayRotation() were called now, there would have been a regular case
            //  Relay 4 were still allowable thereafter
            //  However, Relay 4 passes 'delay_limit' to relay Message
            //  to simulate such thing, call mineOneBlock() 4 times
            //  Even though, block_height = 59 that is less than 60
            //  but Relay 4 is skipped and move to a next Relay
            
            for (i = 0; i < 4; i++) {
                await bmcManagement.mineOneBlock();
            }
            //  After loop, block_time = block(t+58)
            await bmcManagement.relayRotation(link, block, true);    //  block(t+59)
            let relay_info = await bmcManagement.getRelay();
            assert(relay_info.r == accounts[2]);
        });
    });

    describe('Handle relay message tests', () => {
        let bmcManagement, bmcPeriphery, bmv, bsh; 
        let network = '1234.iconee';
        let link; 
        let height = 0;
        let offset = 0;
        let lastHeight = 0;
        let blockInterval = 3000;
        let maxAggregation = 5;
        let delayLimit = 3;
        let relays;
    
        beforeEach(async () => {
            bmcManagement = await deployProxy(BMCManagement);
            bmcPeriphery = await deployProxy(BMCPeriphery, ['1234.pra', bmcManagement.address]);
            await bmcManagement.setBMCPeriphery(bmcPeriphery.address);
            bmv = await MockBMV.new();
            bsh = await MockBSH.new();
            await bmcPeriphery.requestAddService('Token', bsh.address);
            await bmcManagement.approveService('Token', true);
            await bmcManagement.addVerifier(network, bmv.address);
            link = 'btp://1234.iconee/0x1234';
            await bmcManagement.addLink(link); // txSeq += 1 due to link progagation
            relays = [accounts[2], accounts[3], accounts[4], accounts[5]];
            await bmcManagement.addRelay(link, relays);
            await bmv.setStatus(height, offset, lastHeight);
            await bmcManagement.setLink(link, blockInterval, maxAggregation, delayLimit);
        });
    
        it('should revert if relay is invalid', async() => {
            const btpMsg = rlp.encode([
                'btp://1234.iconee/0x1234',
                'btp://1234.pra/' + bmcPeriphery.address,
                'Token',
                '0x01', // rlp encode of signed int
                'message'
            ]);
            
            let relayMsg = URLSafeBase64.encode(btpMsg);
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');
    
            await truffleAssert.reverts(bmcPeriphery.handleRelayMessage.call(link, relayMsg), 'BMCRevertUnauthorized: invalid relay');
    
            let bmcLink = await bmcManagement.getLink('btp://1234.iconee/0x1234');
            assert.equal(bmcLink.rxSeq, 0, 'failed to update rxSeq');
            assert.equal(bmcLink.txSeq, 1, 'failed to update txSeq');
        });
    
        it('should revert if relay is not registered', async() => {
            await bmcManagement.removeLink(link);
    
            const btpMsg = rlp.encode([
                'btp://1234.iconee/0x1234',
                'btp://1234.pra/' + bmcPeriphery.address,
                'Token',
                '0x01',
                'message'
            ]);
            
            let relayMsg = URLSafeBase64.encode(btpMsg);
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');
    
            await truffleAssert.reverts(bmcPeriphery.handleRelayMessage.call(link, 'base64EncodeRelayMessage'), 'BMCRevertUnauthorized: not registered relay');
            
            let bmcLink = await bmcManagement.getLink('btp://1234.iconee/0x1234');
            assert.equal(bmcLink.rxSeq, 0, 'failed to update rxSeq');
            assert.equal(bmcLink.txSeq, 0, 'failed to update txSeq');
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
                'btp://1234.pra/' + bmcPeriphery.address,
                'Token',
                '0x01',
                rlp.encode(transferCoin)
            ]);
    
            let relayMsg = URLSafeBase64.encode(btpMsg);
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');
    
            let res = await bmcPeriphery.handleRelayMessage(link, relayMsg, {from: accounts[3]});
            assert.isNotEmpty(res);
            let bmcLink = await bmcManagement.getLink('btp://1234.iconee/0x1234');
            assert.equal(bmcLink.rxSeq, 1, 'failed to update rxSeq');
            assert.equal(bmcLink.txSeq, 1, 'invalid txSeq');
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
                'btp://1234.pra/' + bmcPeriphery.address,
                '_event',
                '0x00',
                rlp.encode(eventMsg)
            ]);
    
            let relayMsg = URLSafeBase64.encode(btpMsg);
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');
    
            let res = await bmcPeriphery.handleRelayMessage(link, relayMsg, {from: accounts[3]});
            assert.isNotEmpty(res);
    
            let bmcLink = await bmcManagement.getLink('btp://1234.iconee/0x1234');
            assert.equal(bmcLink.reachable[0], btpAddress, 'invalid reachable btp address');
            assert.equal(bmcLink.rxSeq, 1, 'failed to update rxSeq');
            assert.equal(bmcLink.txSeq, 1, 'invalid txSeq');
    
            eventMsg = [
                'Unlink', 
                [
                    'btp://1234.iconee/0x1234',
                    btpAddress
                ]
            ];
    
            btpMsg = rlp.encode([
                'btp://1234.iconee/0x1234',
                'btp://1234.pra/' + bmcPeriphery.address,
                '_event',
                '0x01',
                rlp.encode(eventMsg)
            ]);
    
            relayMsg = URLSafeBase64.encode(btpMsg);
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');
    
            res = await bmcPeriphery.handleRelayMessage(link, relayMsg, {from: accounts[4]});
            assert.isNotEmpty(res);
    
            bmcLink = await bmcManagement.getLink('btp://1234.iconee/0x1234');
            assert.isUndefined(bmcLink.reachable[0], 'failed to unlink');   
            assert.equal(bmcLink.rxSeq, 2, 'failed to update rxSeq');
            assert.equal(bmcLink.txSeq, 1, 'invalid txSeq');
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
                'btp://1234.solana/' + bmcPeriphery.address,
                '_event',
                '0x02',
                rlp.encode(eventMsg)
            ]);
    
            let relayMsg = URLSafeBase64.encode(btpMsg);
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');
    
            const tx = await bmcPeriphery.handleRelayMessage(link, relayMsg, {from: accounts[3]});
            let bmcMessage;
            await truffleAssert.eventEmitted(tx, 'Message', (ev) => {
                bmcMessage = rlp.decode(ev._msg);
                return ev._next === link && ev._seq.toNumber() === 2;
            });
    
            assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[0].toString('hex')), 'btp://1234.pra/' + bmcPeriphery.address);
            assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[1].toString('hex')), 'btp://1234.iconee/0x1234');
            assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[2].toString('hex')), '_event');
            assert.equal(bmcMessage[3].toString('hex'), 'fe'); // two complement form of -2
            const errResponse = rlp.decode(bmcMessage[4]);
            assert.equal(web3.utils.hexToNumber('0x' + errResponse[0].toString('hex')), 10); // BMC_ERR = 10
            assert.equal(web3.utils.hexToUtf8('0x' + errResponse[1].toString('hex')), 'BMCRevertUnreachable: 1234.solana is unreachable');
    
            bmcLink = await bmcManagement.getLink('btp://1234.iconee/0x1234');
            assert.equal(bmcLink.rxSeq, 1, 'failed to update rxSeq');
            assert.equal(bmcLink.txSeq, 2, 'failed to update txSeq');
        });
    
        it('should emit event if routes are succeeded to resolve', async() => {
            const destBtpAddress = 'btp://1234.pra/' + bmcPeriphery.address;
            const routeBtpAddress = 'btp://1234.solana/' + web3.utils.randomHex(20);
            
            await bmcManagement.addRoute(
                routeBtpAddress,
                destBtpAddress
            );
    
            await bmcManagement.addVerifier('1234.pra', web3.utils.randomHex(20));
            await bmcManagement.addLink(destBtpAddress);
    
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
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');
    
            const tx = await bmcPeriphery.handleRelayMessage(link, relayMsg, {from: accounts[4]});
    
            await truffleAssert.eventEmitted(tx, 'Message', (ev) => {
                return ev._next === destBtpAddress && ev._seq.toNumber() === 2 && ev._msg.toString() === '0x' + btpMsg.toString('hex');
            });
    
            let bmcLink = await bmcManagement.getLink('btp://1234.iconee/0x1234');
            assert.equal(bmcLink.rxSeq, 1, 'failed to update rxSeq');
            assert.equal(bmcLink.txSeq, 2, 'invalid txSeq');
    
            bmcLink = await bmcManagement.getLink(destBtpAddress);
            assert.equal(bmcLink.txSeq, 2, 'invalid txSeq');
        });
    
        it('should emit message if dest in msg differs from bmc address (msg.sn >= 0)', async() => {
            const transferCoin = [
                '0xaaa',
                'btp://1234.pra/0xbbb',
                'ICX',
                12
            ];
    
            const btpMsg = rlp.encode([
                'btp://1234.iconee/0x1234',
                'btp://1234.pra/' + bmcPeriphery.address,
                'Token',
                '0x01',
                rlp.encode(transferCoin)
            ]);
    
            let relayMsg = URLSafeBase64.encode(btpMsg);
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');
    
            let res = await bmcPeriphery.handleRelayMessage(link, relayMsg, {from: accounts[3]});
            assert.isNotEmpty(res);
        });
    
        it('should emit error message if dest in received msg differs from bmc address (msg.sn >= 0) and bsh gets errors', async() => {
            const transferCoin = [
                '0xaaa',
                'btp://1234.pra/0xbbb',
                'ICX',
                12
            ];
    
            const btpMsg = rlp.encode([
                'btp://1234.iconee/0x1234',
                'btp://1234.pra/' + bmcPeriphery.address,
                'Token',
                '0x03e8', // two complement of 1000
                rlp.encode(transferCoin)
            ]);
    
            let relayMsg = URLSafeBase64.encode(btpMsg);
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');
    
            let tx = await bmcPeriphery.handleRelayMessage(link, relayMsg, {from: accounts[3]});
            let bmcMessage;
            await truffleAssert.eventEmitted(tx, 'Message', (ev) => {
                bmcMessage = rlp.decode(ev._msg);
                return ev._next === link && ev._seq.toNumber() === 2;
            });
    
            assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[0].toString('hex')), 'btp://1234.pra/' + bmcPeriphery.address);
            assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[1].toString('hex')), 'btp://1234.iconee/0x1234');
            assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[2].toString('hex')), 'Token');
            assert.equal(bmcMessage[3].toString('hex'), 'fc18'); // two complement of -1000
            const errResponse = rlp.decode(bmcMessage[4]);
            assert.equal(web3.utils.hexToNumber('0x' + errResponse[0].toString('hex')), 40); // BSH_ERR = 40
            assert.equal(web3.utils.hexToUtf8('0x' + errResponse[1].toString('hex')), 'Mocking error message on handleBTPMessage');
    
            bmcLink = await bmcManagement.getLink('btp://1234.iconee/0x1234');
            assert.equal(bmcLink.rxSeq, 1, 'failed to update rxSeq');
            assert.equal(bmcLink.txSeq, 2, 'failed to update txSeq');
        });
    
        it('should emit message if dest in received msg differs from bmc address (msg.sn < 0)', async() => {
            const errResponse = [
                0,
                'Invalid service',
            ];
    
            const btpMsg = rlp.encode([
                'btp://1234.iconee/0x1234',
                'btp://1234.pra/' + bmcPeriphery.address,
                'Token',
                '0xf6', // two complement of -10
                rlp.encode(errResponse)
            ]);
    
            let relayMsg = URLSafeBase64.encode(btpMsg);
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');
    
            let res = await bmcPeriphery.handleRelayMessage(link, relayMsg, {from: accounts[3]});
            assert.isNotEmpty(res);
        });
    
        it('should emit message if dest in received msg differs from bmc address (msg.sn < 0) and bsh gets errors', async() => {
            const errResponse = [
                0,
                'Invalid service',
            ];
    
            const btpMsg = rlp.encode([
                'btp://1234.iconee/0x1234',
                'btp://1234.pra/' + bmcPeriphery.address,
                'Token',
                '0xfc18', // two complement of -1000
                rlp.encode(errResponse)
            ]);
    
            let relayMsg = URLSafeBase64.encode(btpMsg);
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');
    
            let tx = await bmcPeriphery.handleRelayMessage(link, relayMsg, {from: accounts[3]});
            await truffleAssert.eventEmitted(tx, 'ErrorOnBTPError', (ev) => {
                return ev[0] === 'Token' && ev[1].toNumber() === 1000 &&
                    ev[2].toNumber() === 0 && ev[3].toString() === 'Invalid service' &&
                    ev[4].toNumber() === 40 && ev[5].toString() === 'Mocking error message on handleBTPError';
            });
        });
    
        it('should emit message if dest in received msg differs from bmc address (msg.sn < 0) and bsh gets low level errors', async() => {
            const errResponse = [
                12,
                'Invalid service',
            ];
    
            const btpMsg = rlp.encode([
                'btp://1234.iconee/0x1234',
                'btp://1234.pra/' + bmcPeriphery.address,
                'Token',
                '0x9c', // two complement of -100
                rlp.encode(errResponse)
            ]);
    
            let relayMsg = URLSafeBase64.encode(btpMsg);
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');
    
            let tx = await bmcPeriphery.handleRelayMessage(link, relayMsg, {from: accounts[3]});
            await truffleAssert.eventEmitted(tx, 'ErrorOnBTPError', (ev) => {
                return ev[0] === 'Token' && ev[1].toNumber() === 100 &&
                    ev[2].toNumber() === 12 && ev[3].toString() === 'Invalid service' &&
                    ev[4].toNumber() === 0 && ev[5].toString() === '';
            });
        });
    
        it('should emit error message if fee service message is failed to decode', async() => {
            const btpMsg = rlp.encode([
                'btp://1234.iconee/0x1234',
                'btp://1234.pra/' + bmcPeriphery.address,
                'bmc',
                '0x02',
                'invalide rlp of service message'
            ]);
    
            let relayMsg = URLSafeBase64.encode(btpMsg);
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');
    
            let tx = await bmcPeriphery.handleRelayMessage(link, relayMsg, { from: accounts[3] });
            let bmcMessage;
            await truffleAssert.eventEmitted(tx, 'Message', (ev) => {
                bmcMessage = rlp.decode(ev._msg);
                return ev._next === link && ev._seq.toNumber() === 2;
            });
    
            assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[0].toString('hex')), 'btp://1234.pra/' + bmcPeriphery.address);
            assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[1].toString('hex')), 'btp://1234.iconee/0x1234');
            assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[2].toString('hex')), 'bmc');
            assert.equal(bmcMessage[3].toString('hex'), 'fe'); // two complement of -2
            const errResponse = rlp.decode(bmcMessage[4]);
            assert.equal(web3.utils.hexToNumber('0x' + errResponse[0].toString('hex')), 10); // BMC_ERR = 10
            assert.equal(web3.utils.hexToUtf8('0x' + errResponse[1].toString('hex')), 'BMCRevertParseFailure');
    
            bmcLink = await bmcManagement.getLink('btp://1234.iconee/0x1234');
            assert.equal(bmcLink.rxSeq, 1, 'failed to update rxSeq');
            assert.equal(bmcLink.txSeq, 2, 'failed to update txSeq');
        });
    
        it('should emit error message if fee gathering message is failed to decode', async() => {
            const serviceMsg = [
                'FeeGathering',
                'invalid rlp of fee gather messgage',
            ];
    
            const btpMsg = rlp.encode([
                'btp://1234.iconee/0x1234',
                'btp://1234.pra/' + bmcPeriphery.address,
                'bmc',
                '0x02',
                rlp.encode(serviceMsg)
            ]);
    
            let relayMsg = URLSafeBase64.encode(btpMsg);
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');
    
            let tx = await bmcPeriphery.handleRelayMessage(link, relayMsg, { from: accounts[3] });
            let bmcMessage;
            await truffleAssert.eventEmitted(tx, 'Message', (ev) => {
                bmcMessage = rlp.decode(ev._msg);
                return ev._next === link && ev._seq.toNumber() === 2;
            });
    
            assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[0].toString('hex')), 'btp://1234.pra/' + bmcPeriphery.address);
            assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[1].toString('hex')), 'btp://1234.iconee/0x1234');
            assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[2].toString('hex')), 'bmc');
            assert.equal(bmcMessage[3].toString('hex'), 'fe'); // two complement of -2
            const errResponse = rlp.decode(bmcMessage[4]);
            assert.equal(web3.utils.hexToNumber('0x' + errResponse[0].toString('hex')), 10); // BMC_ERR = 10
            assert.equal(web3.utils.hexToUtf8('0x' + errResponse[1].toString('hex')), 'BMCRevertParseFailure');
    
            bmcLink = await bmcManagement.getLink('btp://1234.iconee/0x1234');
            assert.equal(bmcLink.rxSeq, 1, 'failed to update rxSeq');
            assert.equal(bmcLink.txSeq, 2, 'failed to update txSeq');
        });
    
        it('should dispatch gather fee message to bsh services', async() => {
            const gatherFeeMsg = [
                'btp://1234.iconee/0x12345678',
                ['service1', 'service2', 'service3']
            ]
            
            const serviceMsg = [
                'FeeGathering',
                rlp.encode(gatherFeeMsg),
            ];
    
            const btpMsg = rlp.encode([
                'btp://1234.iconee/0x1234',
                'btp://1234.pra/' + bmcPeriphery.address,
                'bmc',
                '0x02',
                rlp.encode(serviceMsg)
            ]);
    
            let relayMsg = URLSafeBase64.encode(btpMsg);
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');
    
            let tx = await bmcPeriphery.handleRelayMessage(link, relayMsg, { from: accounts[3] });
            assert.isNotEmpty(tx);
        });
    });

    describe('Test upgradable contracts', () => {
        let bmcManagement, bmcPeriphery;

        before(async () => {
            bmcManagement = await deployProxy(BMCManagement);
            bmcPeriphery = await deployProxy(BMCPeriphery, ['0x1234.pra', bmcManagement.address]);
            await bmcManagement.setBMCPeriphery(bmcPeriphery.address);
        });

        it('should upgrade BMC Management', async() => {
            let bsh = await MockBSH.new();
            await bmcPeriphery.requestAddService('Token', bsh.address);
            await bmcManagement.approveService('Token', true);
            let address = await bmcManagement.getBshServiceByName('Token');
            assert.equal(address, bsh.address);

            const upgradeBMCManagement = await upgradeProxy(bmcManagement.address, BMCManagementV2);
            address = await upgradeBMCManagement.getBshServiceByName('Token');
            assert.equal(address, '0x0000000000000000000000000000000000000000');
        });

        it('should upgrade BMC Periphery', async() => {
            let bsh = await MockBSH.new();
            await bmcPeriphery.requestAddService('Token', bsh.address);
            let res = await bmcManagement.getPendingRequest();
            assert.equal(res[0].serviceName, 'Token');
            assert.equal(res[0].bsh, bsh.address);

            const upgradeBMCPeriphery = await upgradeProxy(bmcPeriphery.address, BMCPeripheryV2);
            await truffleAssert.reverts(
                upgradeBMCPeriphery.requestAddService.call('RandomService', bsh.address),
                'test upgradable for BMC Periphery'
            );    
        });
    });
});    

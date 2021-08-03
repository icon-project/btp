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

const { deployProxy } = require('@openzeppelin/truffle-upgrades');

contract('BMC tests', (accounts) => {
    describe('BMC ingration tests', () => {
        let bmcManagement, bmcPeriphery, bmv1, bmv2, bmv3, bmv4;
    
        before(async () => {
            bmcManagement = await deployProxy(BMCManagement);
            bmcPeriphery = await deployProxy(BMCPeriphery, ['0x1281.moonriver', bmcManagement.address]);
            await bmcManagement.setBMCPeriphery(bmcPeriphery.address);
            bmv1 = await MockBMV.new();
            bmv2 = await MockBMV.new();
            bmv3 = await MockBMV.new();
            bmv4 = await MockBMV.new();
        });
    
        /***************************************************************************************
                                    Add/Remove Service Integration Tests
        ***************************************************************************************/
        it('Service management - Scenario 1: Add service successfully if service is not registered', async () => {
            let service = 'Coin/WrappedCoin';
            
            await bmcManagement.addService(service, accounts[5]);
            
            let output = await bmcManagement.getServices();
            assert(
                output[0].svc === service, output[0].addr === accounts[5],
            );
        });

        it('Service management - Scenario 2: Fail to add service if service is registered', async () => {
            let service = 'Coin/WrappedCoin';
            
            await truffleAssert.reverts(
                bmcManagement.addService.call(service, accounts[4]),
                'BMCRevertAlreadyExistsBSH'
            );
        });

        it('Service management - Scenario 3: Fail to add service if caller is not contract owner', async () => {
            let service = 'Coin/WrappedCoin';
            
            await truffleAssert.reverts(
                bmcManagement.addService.call(service, accounts[4], { from: accounts[6] }),
                'BMCRevertUnauthorized'
            );
        });

        it('Service management - Scenario 4: Fail to add service if service\'scontract address is invalid', async () => {
            let service = 'Coin/WrappedCoin';
            
            await truffleAssert.reverts(
                bmcManagement.addService.call(service, '0x0000000000000000000000000000000000000000'),
                'BMCRevertInvalidAddress'
            );
        });
    
        it('Service management - Scenario 5: Fail to remove service if caller is not contract owner', async () => {
            let service = 'Coin/WrappedCoin';
            await truffleAssert.reverts(
                bmcManagement.removeService.call(service, {from: accounts[1]}),
                'BMCRevertUnauthorized'
            );
        });
    
        it('Service management - Scenario 6: Fail to remove service if service does not exist', async () => {
            let service = 'Token A';
            await truffleAssert.reverts(
                bmcManagement.removeService.call(service),
                'BMCRevertNotExistsBSH'
            );
        });
    
        it('Service management - Scenario 7: Remove service successfully if service does exist and caller is contract owner', async () => {
            let service = 'Token';
            await bmcManagement.addService(service, accounts[4]);
            await bmcManagement.removeService(service);
            let output = await bmcManagement.getServices();
            assert(
                output.length === 1,
                output[0].svc === 'Coin/WrappedCoin',
                output[0].addr == accounts[5]
            );
        });  
    
        /***************************************************************************************
                                    Add/Remove Verifier Integration Tests
        ***************************************************************************************/
        it('Verifier Management - Scenario 1: Add verifier successfully if caller is contract owner', async () => {
            let network = '0x03.icon';
            await bmcManagement.addVerifier(network, bmv1.address);
            let output = await bmcManagement.getVerifiers();
            assert(
                output[0].net === network, output[0].addr === bmv1.address,
            );
        });
    
        it('Verifier Management - Scenario 2: Fail to add verifier if caller is not contract owner', async () => {
            let network = '0x03.icon';
            await truffleAssert.reverts(
                bmcManagement.addVerifier.call(network, bmv1.address, {from: accounts[1]}),
                'BMCRevertUnauthorized'
            );
        });
    
        it('Verifier Management - Scenario 3: Fail to add verifier if verifier is already registered', async () => {
            let network = '0x03.icon';
            await truffleAssert.reverts(
                bmcManagement.addVerifier.call(network, bmv1.address),
                'BMCRevertAlreadyExistsBMV'        
            );
        });
    
        it('Verifier Management - Scenario 4: Fail to remove verifier if caller is not contract owner', async () => {
            let network = '0x03.icon';
            await truffleAssert.reverts(
                bmcManagement.removeVerifier.call(network, {from: accounts[1]}),
                'BMCRevertUnauthorized'
            );
        });
    
        it('Verifier Management - Scenario 5: Fail to remove verifer if verifier is not registered', async () => {
            let network = '0x04.pra';
            await truffleAssert.reverts(
                bmcManagement.removeVerifier.call(network),
                'BMCRevertNotExistsBMV'
            );
        });
    
        it('Verifier Management - Scenario 6: Remove verifier successfully if verifier is registered and caller is contract owner', async () => {
            let network1 = '0x27.pra';
            let network2 = '0x1387.eos';
            let network3 = '0x01.eth';
            await bmcManagement.addVerifier(network1, bmv2.address);
            await bmcManagement.addVerifier(network2, bmv3.address);
            await bmcManagement.addVerifier(network3, bmv4.address);
            await bmcManagement.removeVerifier('0x03.icon');
            let output = await bmcManagement.getVerifiers();
            assert(
                output[0].net == network3, output[0].addr == bmv4.address,
                output[1].net == network1, output[0].addr == bmv2.address,
                output[2].net == network2, output[0].addr == bmv3.address,
            );
        });
    
        /***************************************************************************************
                                    Add/Remove Link Integration Tests
        ***************************************************************************************/
        it('Link management - Scenario 1: Add link successfully if caller is contract owner', async () => {
            let link = 'btp://0x27.pra/cx10c8c08724e7a95c84829c07239ae2b839a262a3';
            await bmcManagement.addLink(link);
            let output = await bmcManagement.getLinks();
            assert(
                output[0] === link
            );
        });
    
        it('Link management - Scenario 2: Fail to add link if caller is not contract owner', async () => {
            let link = 'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3';
            await truffleAssert.reverts(
                bmcManagement.addLink.call(link, {from: accounts[1]}),
                'BMCRevertUnauthorized'
            );
        });  
        
        //  BMC adds supporting link, but network has not yet registered any BMV
        it('Link management - Scenario 3: Fail to add link if verifier used for this link does not exist', async () => {
            let link = 'btp://0x62.sol/0x954024575b469e6c0cd1bc39d489dd25185ec728';
            await truffleAssert.reverts(
                bmcManagement.addLink.call(link),
                'BMCRevertNotExistsBMV'
            );
        });  
    
        it('Link management - Scenario 4: Fail to add link if input invalid BTP address', async () => {
            let link = 'btp://1234.eos:0x954024575b469e6c0cd1bc39d489dd25185ec728';
            await truffleAssert.reverts(
                bmcManagement.addLink.call(link),
                ''
            );
        });  
                                    
        it('Link management - Scenario 5: Fail to add link if link is already added', async () => {
            let link = 'btp://0x27.pra/cx10c8c08724e7a95c84829c07239ae2b839a262a3';
            await truffleAssert.reverts(
                bmcManagement.addLink.call(link),
                'BMCRevertAlreadyExistsLink'
            );
        }); 
        
        it('Link management - Scenario 6: Fail to remove link if caller is not contract owner', async () => {
            let link = 'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3';
            await truffleAssert.reverts(
                bmcManagement.removeLink.call(link, {from: accounts[1]}),
                'BMCRevertUnauthorized'
            );
        });
    
        it('Link management - Scenario 7: Fail to remove link if link does not exist', async () => {
            let link = 'btp://0x62.sol/0x954024575b469e6c0cd1bc39d489dd25185ec728';
            await truffleAssert.reverts(
                bmcManagement.removeLink.call(link),
                'BMCRevertNotExistsLink'
            );
        });
    
        it('Link management - Scenario 8: Remove link successfully if link does exist and caller is contract owner', async () => {
            let link1 = 'btp://0x1387.eos/0x2d4504a081f342e5f31a2f710c1e2c5a2e94d79e';
            let link2 = 'btp://0x01.eth/0x55c45fa2bf3a31d6eedb63400d0805208aa556d5';
            await bmcManagement.addLink(link1);
            await bmcManagement.addLink(link2);
            await bmcManagement.removeLink('btp://0x27.pra/cx10c8c08724e7a95c84829c07239ae2b839a262a3');
            let output = await bmcManagement.getLinks();
            assert(
                output[0] === link2,
                output[1] === link1,
            );
        });    
    
        /***************************************************************************************
                                    Set Link Stats Integration Tests
        ***************************************************************************************/
        it('Link configuration - Scenario 1: Fail to set link config if caller is not contract owner', async () => {
            let link = 'btp://0x01.eth/0x55c45fa2bf3a31d6eedb63400d0805208aa556d5'; 
            let blockInterval = 15000;
            let maxAggregation = 5;
            let delayLimit = 4;
            await truffleAssert.reverts(
                bmcManagement.setLink.call(link, blockInterval, maxAggregation, delayLimit, {from: accounts[1]}),
                'BMCRevertUnauthorized'
            );
        });
    
        it('Link configuration - Scenario 2: Fail to set link config if link does not exist ', async () => {
            let link = 'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3'; 
            let blockInterval = 6000;
            let maxAggregation = 7;
            let delayLimit = 3;
            await truffleAssert.reverts(
                bmcManagement.setLink.call(link, blockInterval, maxAggregation, delayLimit),
                'BMCRevertNotExistsLink'
            );
        });
    
        it('Link configuration - Scenario 3: Fail to set link config if max aggregation is invalid', async () => {
            let link = 'btp://0x01.eth/0x55c45fa2bf3a31d6eedb63400d0805208aa556d5'; 
            let blockInterval = 6000;
            let maxAggregation = 0;
            let delayLimit = 3;
            await truffleAssert.reverts(
                bmcManagement.setLink.call(link, blockInterval, maxAggregation, delayLimit),
                'BMCRevertInvalidParam'
            );
        });
    
        it('Link configuration - Scenario 4: Fail to set link config if delay limit is invalid', async () => {
            let link = 'btp://0x01.eth/0x55c45fa2bf3a31d6eedb63400d0805208aa556d5'; 
            let blockInterval = 6000;
            let maxAggregation = 5;
            let delayLimit = 0;
            await truffleAssert.reverts(
                bmcManagement.setLink.call(link, blockInterval, maxAggregation, delayLimit),
                'BMCRevertInvalidParam'
            );
        });
    
        it('Link configuration - Scenario 5: Set link config successfully if setting is valid and caller is contract owner', async () => {
            let link = 'btp://0x01.eth/0x55c45fa2bf3a31d6eedb63400d0805208aa556d5'; 
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
                                    Add/Remove Route Integration Tests
        ***************************************************************************************/
        it('Route management - Scenario 1: Add route successfully if caller is contract owner', async () => {
            let dst = 'btp://0x01.eth/0x55c45fa2bf3a31d6eedb63400d0805208aa556d5';
            let link = 'btp://0x1387.eos/0x2d4504a081f342e5f31a2f710c1e2c5a2e94d79e';
            await bmcManagement.addRoute(dst, link);
            let output = await bmcManagement.getRoutes();
            assert(
                output[0].dst === dst, output[0].next === link,
            );
        });
                                
        it('Route management - Scenario 2: Fail to add route if caller is not contract owner', async () => {
            let dst = 'btp://0x01.eth/0x55c45fa2bf3a31d6eedb63400d0805208aa556d5';
            let link = 'btp://0x1387.eos/0x2d4504a081f342e5f31a2f710c1e2c5a2e94d79e';
            await truffleAssert.reverts(
                bmcManagement.addRoute.call(dst, link, {from: accounts[1]}),
                'BMCRevertUnauthorized'
            );
        });
    
        it('Route management - Scenario 3: Fail to add route if route already exists', async () => {
            let dst = 'btp://0x01.eth/0x55c45fa2bf3a31d6eedb63400d0805208aa556d5';
            let link = 'btp://0x1387.eos/0x2d4504a081f342e5f31a2f710c1e2c5a2e94d79e';
            await truffleAssert.reverts(
                bmcManagement.addRoute.call(dst, link),
                'BTPRevertAlreadyExistRoute'        
            );
        });
    
        it('Route management - Scenario 4: Fail to add route if BTP addresses of links are invalid', async () => {
            let dst = 'btp://0x1387.eos:0x2d4504a081f342e5f31a2f710c1e2c5a2e94d79e';
            let link = 'btp://0x27.pra/cx10c8c08724e7a95c84829c07239ae2b839a262a3';
            await truffleAssert.reverts(
                bmcManagement.addRoute.call(dst, link),
                ''
            );
        });
    
        it('Route management - Scenario 5: Fail to remove route if caller is not contract owner', async () => {
            let dst = 'btp://0x01.eth/0x55c45fa2bf3a31d6eedb63400d0805208aa556d5';
            await truffleAssert.reverts(
                bmcManagement.removeRoute.call(dst, {from: accounts[1]}),
                'BMCRevertUnauthorized'
            );
        });
    
        it('Route management - Scenario 6: Fail to remove route if route does not exist', async () => {
            let dst = 'btp://0x27.pra/cx10c8c08724e7a95c84829c07239ae2b839a262a3';
            await truffleAssert.reverts(
                bmcManagement.removeRoute.call(dst),
                'BTPRevertNotExistRoute'
            );
        });
    
        it('Route management - Scenario 7: Remove route successfully if route exists and caller is contract owner', async () => {
            let dst1 = 'btp://0x27.pra/cx10c8c08724e7a95c84829c07239ae2b839a262a3';
            let link1 = 'btp://0x1387.eos/0x2d4504a081f342e5f31a2f710c1e2c5a2e94d79e';
            let dst2 = 'btp://0x02.eth/0x55c45fa2bf3a31d6eedb63400d0805208aa556d5';
            let link2 = 'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3';
            await bmcManagement.addRoute(dst1, link1);
            await bmcManagement.addRoute(dst2, link2);
            await bmcManagement.removeRoute('btp://0x01.eth/0x55c45fa2bf3a31d6eedb63400d0805208aa556d5');
            let output = await bmcManagement.getRoutes();
            assert(
                output[0].dst === dst2, output[0].next === link1,
                output[1].dst === dst1, output[0].addr === link1,
            );
        });     
        
        /***************************************************************************************
                                    Add/Remove Relays Integration Tests
        ***************************************************************************************/
    
        it('Relay management - Scenario 1: Fail to add relays if caller is not contract owner', async () => {
            let link = 'btp://0x1387.eos/0x2d4504a081f342e5f31a2f710c1e2c5a2e94d79e';
            let relays = [accounts[2], accounts[3], accounts[4]];
            await truffleAssert.reverts(
                bmcManagement.addRelay.call(link, relays, {from: accounts[1]}),
                'BMCRevertUnauthorized'
            );
        });
    
        it('Relay management - Scenario 2: Fail to add relays if link does not exist', async () => {
            let link = 'btp://0x27.pra/cx10c8c08724e7a95c84829c07239ae2b839a262a3';
            let relays = [accounts[2], accounts[3], accounts[4]];
            await truffleAssert.reverts(
                bmcManagement.addRelay.call(link, relays),
                'BMCRevertNotExistsLink'        
            );
        });
    
        it('Relay management - Scenario 3: Add relays successfully if caller is contract owner', async () => {
            let link = 'btp://0x1387.eos/0x2d4504a081f342e5f31a2f710c1e2c5a2e94d79e';
            let relays = [accounts[2], accounts[3], accounts[4]];
            await bmcManagement.addRelay(link, relays);
            let result = await bmcManagement.getRelays(link);
            assert(
                result[0] === accounts[2], result[1] === accounts[3], result[2] === accounts[4]
            );
        });
    
        it('Relay management - Scenario 4: Overwrite existing relays successfully ', async () => {
            let link = 'btp://0x1387.eos/0x2d4504a081f342e5f31a2f710c1e2c5a2e94d79e';
            let relays = [accounts[2], accounts[4], accounts[6], accounts[7]];
            await bmcManagement.addRelay(link, relays);
            let result = await bmcManagement.getRelays(link);
            assert(
                result[0] === accounts[2], result[1] === accounts[4], 
                result[2] === accounts[6], result[3] === accounts[7]
            );
        });
    
        it('Relay management - Scenario 5: Fail to remove relay if caller is not contract owner', async () => {
            let link = 'btp://0x1387.eos/0x2d4504a081f342e5f31a2f710c1e2c5a2e94d79e';
            let relay = accounts[2];
            await truffleAssert.reverts(
                bmcManagement.removeRelay.call(link, relay, {from: accounts[1]}),
                'BMCRevertUnauthorized'
            );
        });
    
        it('Relay management - Scenario 6: Fail to remove relay if link does not exist', async () => {
            let link = 'btp://0x27.pra/cx10c8c08724e7a95c84829c07239ae2b839a262a3';
            let relay = accounts[2];
            await truffleAssert.reverts(
                bmcManagement.removeRelay.call(link, relay),
                'BMCRevertUnauthorized'
            );
        });
    
        it('Relay management - Scenario 7: Fail to remove relay if relays does not exist', async () => {
            let link = 'btp://0x01.eth/0x55c45fa2bf3a31d6eedb63400d0805208aa556d5';
            let relay = accounts[8];
            await truffleAssert.reverts(
                bmcManagement.removeRelay.call(link, relay),
                'BMCRevertUnauthorized'
            );
        });
    
        it('Relay management - Scenario 8: Remove relay successfully if relay/link does exist and caller is contract owner', async () => {
            let link = 'btp://0x1387.eos/0x2d4504a081f342e5f31a2f710c1e2c5a2e94d79e';
            let relay = accounts[6];
            await bmcManagement.removeRelay(link, relay);
            let result = await bmcManagement.getRelays(link);
            assert(
                result[0] === accounts[2], result[1] === accounts[4], 
                result[2] === accounts[7]
            );
        });

        /***************************************************************************************
                                    Owner Tests
        ***************************************************************************************/
        it('Owners management - Scenario 1: Add a new owner successfully if caller is contract owner', async () => {
            await bmcManagement.addOwner(accounts[1]);
            const res = await bmcManagement.isOwner(accounts[1]);
            assert.isTrue(res);
        });

        it('Owners management - Scenario 2: Fail to add a new owner if caller is not contract owner', async () => {
            await truffleAssert.reverts(
                bmcManagement.addOwner.call(accounts[1], { from: accounts[2] }),
                'BMCRevertUnauthorized'
            );
        });

        it('Owners management - Scenario 3: Fail to remove an owner if number of owners is 1', async () => {
            await bmcManagement.removeOwner(accounts[1]);
            await truffleAssert.reverts(
                bmcManagement.removeOwner.call(accounts[0]),
                'BMCRevertLastOwner'
            );
        });

        it('Owners management - Scenario 4: Fail to remove an owner if caller is not contract owner', async () => {
            await bmcManagement.addOwner(accounts[1]);
            await truffleAssert.reverts(
                bmcManagement.removeOwner.call(accounts[1], { from: accounts[2] }),
                'BMCRevertUnauthorized'
            );
        });

        it('Owners management - Scenario 5: Remove an owner successfully', async () => {
            await bmcManagement.removeOwner(accounts[1]);
            const res = await bmcManagement.isOwner(accounts[1]);
            assert.isFalse(res);
        });
    });

    describe('Handle relay message tests', () => {
        let bmcManagement, bmcPeriphery, bmv, bsh; 
        let network = '0x03.icon';
        let link = 'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3'; 
        let height = 0;
        let offset = 0;
        let lastHeight = 0;
        let blockInterval = 3000;
        let maxAggregation = 5;
        let delayLimit = 3;
        let relays;
    
        beforeEach(async () => {
            bmcManagement = await deployProxy(BMCManagement);
            bmcPeriphery = await deployProxy(BMCPeriphery, ['0x27.pra', bmcManagement.address]);
            await bmcManagement.setBMCPeriphery(bmcPeriphery.address);
            bmv = await MockBMV.new();
            bsh = await MockBSH.new();
            await bmcManagement.addService('Token', bsh.address);
            await bmcManagement.addVerifier(network, bmv.address);
            await bmcManagement.addLink(link);
            relays = [accounts[2], accounts[3], accounts[4], accounts[5]];
            await bmcManagement.addRelay(link, relays);
            await bmv.setStatus(height, offset, lastHeight);
            await bmcManagement.setLink(link, blockInterval, maxAggregation, delayLimit);
        });

        it('Scenario 1: Revert if relay is invalid', async() => {
            const btpMsg = rlp.encode([
                'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3',
                'btp://0x27.pra/' + bmcPeriphery.address,
                'Token',
                '0x01', // rlp encode of signed int
                'message'
            ]);
            
            let relayMsg = URLSafeBase64.encode(btpMsg);
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');
    
            await truffleAssert.reverts(bmcPeriphery.handleRelayMessage.call(link, relayMsg), 'BMCRevertUnauthorized: invalid relay');
    
            let bmcLink = await bmcManagement.getLink('btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3');
            assert.equal(bmcLink.rxSeq, 0, 'failed to update rxSeq');
            assert.equal(bmcLink.txSeq, 1, 'failed to update txSeq');
        });
    
        it('Scenario 2: Revert if relay is not registered', async() => {
            await bmcManagement.removeLink(link);
    
            const btpMsg = rlp.encode([
                'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3',
                'btp://0x27.pra/' + bmcPeriphery.address,
                'Token',
                '0x01',
                'message'
            ]);
            
            let relayMsg = URLSafeBase64.encode(btpMsg);
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');
    
            await truffleAssert.reverts(bmcPeriphery.handleRelayMessage.call(link, 'base64EncodeRelayMessage'), 'BMCRevertUnauthorized: not registered relay');
            
            let bmcLink = await bmcManagement.getLink('btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3');
            assert.equal(bmcLink.rxSeq, 0, 'failed to update rxSeq');
            assert.equal(bmcLink.txSeq, 0, 'failed to update txSeq');
        });
    
        it('Scenario 3: Dispatch service message to BSH', async() => {
            const transferCoin = [
                '0xaaa',
                'btp://0x27.pra/0xbbb',
                'ICX',
                12
            ];
    
            const btpMsg = rlp.encode([
                'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3',
                'btp://0x27.pra/' + bmcPeriphery.address,
                'Token',
                '0x01',
                rlp.encode(transferCoin)
            ]);
    
            let relayMsg = URLSafeBase64.encode(btpMsg);
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');
    
            let res = await bmcPeriphery.handleRelayMessage(link, relayMsg, {from: accounts[3]});
            assert.isNotEmpty(res);
            let bmcLink = await bmcManagement.getLink('btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3');
            assert.equal(bmcLink.rxSeq, 1, 'failed to update rxSeq');
            assert.equal(bmcLink.txSeq, 1, 'invalid txSeq');
        });
    
        it('Scenario 4: Init link via BTP messages', async() => {
            bmcManagement = await deployProxy(BMCManagement);
            bmcPeriphery = await deployProxy(BMCPeriphery, ['0x27.pra', bmcManagement.address]);
            await bmcManagement.setBMCPeriphery(bmcPeriphery.address);
            bmv = await MockBMV.new();
            await bmcManagement.addVerifier(network, bmv.address);
            
            let tx = await bmcManagement.addLink(link);
            let events = await bmcPeriphery.getPastEvents('Message', { fromBlock: tx.receipt.blockNumber, toBlock: 'latest' });
            assert.equal(events[0].event, 'Message');
            
            let eventData = events[0].returnValues;
            assert.equal(eventData._next, link);
            assert.equal(eventData._seq, 1);

            const bmcBtpAddr = await bmcPeriphery.getBmcBtpAddress();

            const encodedSendingBtpMsg = '0x' + rlp.encode([
                bmcBtpAddr,
                link,
                'bmc',
                '0x00',
                rlp.encode([
                    'Init',
                    rlp.encode([
                        []
                    ])
                ])
            ]).toString('hex');
            assert.equal(eventData._msg, encodedSendingBtpMsg);

            const encodedReceivedBtpMsg = rlp.encode([
                link,
                bmcBtpAddr,
                'bmc',
                '0x00',
                rlp.encode([
                    'Init',
                    rlp.encode([
                        []
                    ])
                ])
            ]);

            let relayMsg = URLSafeBase64.encode(encodedReceivedBtpMsg);
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');

            await bmcManagement.addRelay(link, relays);
            await bmcPeriphery.handleRelayMessage(link, relayMsg, {from: accounts[2]});

            const linkInfo = await bmcManagement.getLink(link);
            assert.isEmpty(linkInfo.reachable);
        });

        it('Scenario 5: Process LINK and UNLINK via BTP messages', async() => {
            const bmcBtpAddr = await bmcPeriphery.getBmcBtpAddress();
            
            const encodedReceivedBtpMsg = rlp.encode([
                link,
                bmcBtpAddr,
                'bmc',
                '0x00',
                rlp.encode([
                    'Init',
                    rlp.encode([
                        []
                    ])
                ])
            ]);

            let relayMsg = URLSafeBase64.encode(encodedReceivedBtpMsg);
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');

            await bmcPeriphery.handleRelayMessage(link, relayMsg, {from: accounts[3]});

            const linkInfo = await bmcManagement.getLink(link);
            assert.isEmpty(linkInfo.reachable);
            
            const btpAddress = 'btp://0x01.eth/' + web3.utils.randomHex(20);

            relayMsg = rlp.encode([
                link,
                bmcBtpAddr,
                'bmc',
                '0x01',
                rlp.encode([
                    'Link', 
                    rlp.encode([
                        btpAddress,
                    ])
                ])
            ]);
    
            relayMsg = URLSafeBase64.encode(relayMsg);
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');
    
            await bmcPeriphery.handleRelayMessage(link, relayMsg, {from: accounts[4]});
    
            let bmcLink = await bmcManagement.getLink(link);
            assert.equal(bmcLink.reachable[0], btpAddress, 'invalid reachable btp address');
            assert.equal(bmcLink.rxSeq, 2, 'failed to update rxSeq');
            assert.equal(bmcLink.txSeq, 1, 'invalid txSeq');
    
            relayMsg = rlp.encode([
                link,
                bmcBtpAddr,
                'bmc',
                '0x02',
                rlp.encode([
                    'Unlink', 
                    rlp.encode([
                        btpAddress,
                    ])
                ])
            ]);
    
            relayMsg = URLSafeBase64.encode(relayMsg);
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');
    
            await bmcPeriphery.handleRelayMessage(link, relayMsg, {from: accounts[5]});
    
            bmcLink = await bmcManagement.getLink(link);
            assert.isEmpty(bmcLink.reachable, 'failed to remove link in reachable');
            assert.equal(bmcLink.rxSeq, 3, 'failed to update rxSeq');
            assert.equal(bmcLink.txSeq, 1, 'invalid txSeq');
        });

        it('Scenario 6: Revert if internal handler does not exist', async () => {
            const btpAddress = 'btp://0x01.eth/' + web3.utils.randomHex(20);
            let eventMsg = [
                'Unknown', 
                [
                    'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3',
                    btpAddress,
                ]
            ];
    
            let btpMsg = rlp.encode([
                'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3',
                'btp://0x27.pra/' + bmcPeriphery.address,
                'bmc',
                '0x00',
                rlp.encode(eventMsg)
            ]);
    
            let relayMsg = URLSafeBase64.encode(btpMsg);
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');
    
            await truffleAssert.reverts(
                bmcPeriphery.handleRelayMessage.call(link, relayMsg, {from: accounts[3]}),
                'BMCRevert: not exists internal handler'
            );
        });
    
        it('Scenario 7: Emit event to send BTP error response if routes are failed to resolve', async() => {
            const btpAddress = 'btp://0x01.eth/' + web3.utils.randomHex(20);
            let eventMsg = [
                'Link',
                [
                    'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3',
                    btpAddress,
                ]
            ];
    
            let btpMsg = rlp.encode([
                'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3',
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
    
            assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[0].toString('hex')), 'btp://0x27.pra/' + bmcPeriphery.address);
            assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[1].toString('hex')), 'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3');
            assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[2].toString('hex')), '_event');
            assert.equal(bmcMessage[3].toString('hex'), 'fe'); // two complement form of -2
            const errResponse = rlp.decode(bmcMessage[4]);
            assert.equal(web3.utils.hexToNumber('0x' + errResponse[0].toString('hex')), 10); // BMC_ERR = 10
            assert.equal(web3.utils.hexToUtf8('0x' + errResponse[1].toString('hex')), 'BMCRevertUnreachable: 1234.solana is unreachable');
    
            bmcLink = await bmcManagement.getLink('btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3');
            assert.equal(bmcLink.rxSeq, 1, 'failed to update rxSeq');
            assert.equal(bmcLink.txSeq, 2, 'failed to update txSeq');
        });
    
        it('Scenario 8: Emit event to send BTP message to next BMC if routes are succeeded to resolve', async() => {
            const destBtpAddress = 'btp://0x27.pra/' + bmcPeriphery.address;
            const routeBtpAddress = 'btp://0x1028.solana/' + web3.utils.randomHex(20);
            
            await bmcManagement.addRoute(
                routeBtpAddress,
                destBtpAddress
            );
    
            await bmcManagement.addVerifier('0x27.pra', web3.utils.randomHex(20));
            await bmcManagement.addLink(destBtpAddress);
    
            const transferCoin = [
                '0xaaa',
                'btp://0x27.pra/0xbbb',
                'ICX',
                12
            ];
    
            let btpMsg = rlp.encode([
                'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3',
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
    
            let bmcLink = await bmcManagement.getLink('btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3');
            assert.equal(bmcLink.rxSeq, 1, 'failed to update rxSeq');
            assert.equal(bmcLink.txSeq, 2, 'invalid txSeq');
    
            bmcLink = await bmcManagement.getLink(destBtpAddress);
            assert.equal(bmcLink.txSeq, 2, 'invalid txSeq');
        });
    
        it('Scenario 9: Emit event to send BTP response message if destined BMC in BTP message is current BMC address (msg.sn >= 0)', async() => {
            const transferCoin = [
                '0xaaa',
                'btp://0x27.pra/0xbbb',
                'ICX',
                12
            ];
    
            const btpMsg = rlp.encode([
                'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3',
                'btp://0x27.pra/' + bmcPeriphery.address,
                'Token',
                '0x01',
                rlp.encode(transferCoin)
            ]);
    
            let relayMsg = URLSafeBase64.encode(btpMsg);
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');
    
            let res = await bmcPeriphery.handleRelayMessage(link, relayMsg, {from: accounts[3]});
            assert.isNotEmpty(res);
        });
    
        it('Scenario 10: Emit event to send BTP error response if destined BMC in BTP message is current BMC address (msg.sn >= 0) in case that BSH gets errors', async() => {
            const transferCoin = [
                '0xaaa',
                'btp://0x27.pra/0xbbb',
                'ICX',
                12
            ];
    
            const btpMsg = rlp.encode([
                'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3',
                'btp://0x27.pra/' + bmcPeriphery.address,
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
    
            assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[0].toString('hex')), 'btp://0x27.pra/' + bmcPeriphery.address);
            assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[1].toString('hex')), 'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3');
            assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[2].toString('hex')), 'Token');
            assert.equal(bmcMessage[3].toString('hex'), 'fc18'); // two complement of -1000
            const errResponse = rlp.decode(bmcMessage[4]);
            assert.equal(web3.utils.hexToNumber('0x' + errResponse[0].toString('hex')), 40); // BSH_ERR = 40
            assert.equal(web3.utils.hexToUtf8('0x' + errResponse[1].toString('hex')), 'Mocking error message on handleBTPMessage');
    
            bmcLink = await bmcManagement.getLink('btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3');
            assert.equal(bmcLink.rxSeq, 1, 'failed to update rxSeq');
            assert.equal(bmcLink.txSeq, 2, 'failed to update txSeq');
        });
    
        it('Scenario 11: BSH handle BTP error successfully if destied BMC in BTP message is current BMC address (msg.sn < 0)', async() => {
            const errResponse = [
                0,
                'Invalid service',
            ];
    
            const btpMsg = rlp.encode([
                'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3',
                'btp://0x27.pra/' + bmcPeriphery.address,
                'Token',
                '0xf6', // two complement of -10
                rlp.encode(errResponse)
            ]);
    
            let relayMsg = URLSafeBase64.encode(btpMsg);
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');
    
            let res = await bmcPeriphery.handleRelayMessage(link, relayMsg, {from: accounts[3]});
            assert.isNotEmpty(res);
        });
    
        it('Scenario 12: Emit error event if destied BMC in BTP message is current BMC address (msg.sn < 0) and BSH reverts', async() => {
            const errResponse = [
                0,
                'Invalid service',
            ];
    
            const btpMsg = rlp.encode([
                'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3',
                'btp://0x27.pra/' + bmcPeriphery.address,
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
    
        it('Scenario 13: Emit error event if destied BMC in BTP message is current BMC address (msg.sn < 0) and BSH reverts by low level error', async() => {
            const errResponse = [
                12,
                'Invalid service',
            ];
    
            const btpMsg = rlp.encode([
                'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3',
                'btp://0x27.pra/' + bmcPeriphery.address,
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
    
        it('Scenario 14: Emit event to send BTP error response if fee gathering message is failed to decode', async() => {
            const btpMsg = rlp.encode([
                'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3',
                'btp://0x27.pra/' + bmcPeriphery.address,
                'bmc',
                '0x02',
                'invalid rlp of service message'
            ]);
    
            let relayMsg = URLSafeBase64.encode(btpMsg);
            relayMsg = relayMsg.padEnd(relayMsg.length + (4 - relayMsg.length % 4) % 4, '=');
    
            let tx = await bmcPeriphery.handleRelayMessage(link, relayMsg, { from: accounts[3] });
            let bmcMessage;
            await truffleAssert.eventEmitted(tx, 'Message', (ev) => {
                bmcMessage = rlp.decode(ev._msg);
                return ev._next === link && ev._seq.toNumber() === 2;
            });
    
            assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[0].toString('hex')), 'btp://0x27.pra/' + bmcPeriphery.address);
            assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[1].toString('hex')), 'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3');
            assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[2].toString('hex')), 'bmc');
            assert.equal(bmcMessage[3].toString('hex'), 'fe'); // two complement of -2
            const errResponse = rlp.decode(bmcMessage[4]);
            assert.equal(web3.utils.hexToNumber('0x' + errResponse[0].toString('hex')), 10); // BMC_ERR = 10
            assert.equal(web3.utils.hexToUtf8('0x' + errResponse[1].toString('hex')), 'BMCRevertParseFailure');
    
            bmcLink = await bmcManagement.getLink('btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3');
            assert.equal(bmcLink.rxSeq, 1, 'failed to update rxSeq');
            assert.equal(bmcLink.txSeq, 2, 'failed to update txSeq');
        });
    
        it('Scenario 15: Emit event to send BTP error response if fee gathering message is failed to decode', async() => {
            const serviceMsg = [
                'FeeGathering',
                'invalid rlp of fee gather messgage',
            ];
    
            const btpMsg = rlp.encode([
                'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3',
                'btp://0x27.pra/' + bmcPeriphery.address,
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
    
            assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[0].toString('hex')), 'btp://0x27.pra/' + bmcPeriphery.address);
            assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[1].toString('hex')), 'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3');
            assert.equal(web3.utils.hexToUtf8('0x' + bmcMessage[2].toString('hex')), 'bmc');
            assert.equal(bmcMessage[3].toString('hex'), 'fe'); // two complement of -2
            const errResponse = rlp.decode(bmcMessage[4]);
            assert.equal(web3.utils.hexToNumber('0x' + errResponse[0].toString('hex')), 10); // BMC_ERR = 10
            assert.equal(web3.utils.hexToUtf8('0x' + errResponse[1].toString('hex')), 'BMCRevertParseFailure');
    
            bmcLink = await bmcManagement.getLink('btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3');
            assert.equal(bmcLink.rxSeq, 1, 'failed to update rxSeq');
            assert.equal(bmcLink.txSeq, 2, 'failed to update txSeq');
        });
    
        it('Scenario 16: Dispatch gather fee message to BSH services', async() => {
            const gatherFeeMsg = [
                'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a35678',
                ['service1', 'service2', 'service3']
            ]
            
            const serviceMsg = [
                'FeeGathering',
                rlp.encode(gatherFeeMsg),
            ];
    
            const btpMsg = rlp.encode([
                'btp://0x03.icon/cx10c8c08724e7a95c84829c07239ae2b839a262a3',
                'btp://0x27.pra/' + bmcPeriphery.address,
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
});    

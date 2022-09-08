/*
 * Copyright 2022 ICON Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package foundation.icon.btp.xcall;

import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.mock.MockBMC;
import foundation.icon.btp.test.AssertTransactionException;
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.EVMIntegrationTest;
import foundation.icon.btp.test.MockBMCIntegrationTest;
import foundation.icon.btp.util.StringUtil;
import org.junit.jupiter.api.*;
import org.web3j.abi.datatypes.Address;
import org.web3j.crypto.Hash;
import org.web3j.crypto.Keys;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CallServiceTest implements CSIntegrationTest {
    static final String SERVICE = "xcall";
    static final int MAX_DATA_SIZE = 2048;
    static final int MAX_ROLLBACK_SIZE = 1024;
    static final BigInteger EXA = BigInteger.TEN.pow(18);
    static String csAddress = callService.getContractAddress();
    static String sampleAddress = dAppProxySample.getContractAddress();
    static Address fakeAddress = EVMIntegrationTest.Faker.address();
    static String linkNet = BTPIntegrationTest.Faker.btpNetwork();
    static BTPAddress to = new BTPAddress(linkNet, sampleAddress);
    static BTPAddress fakeTo = new BTPAddress(linkNet, fakeAddress.toString());
    static BigInteger srcSn = BigInteger.ZERO;
    static BigInteger reqId = BigInteger.ZERO;
    static Map<BigInteger, MessageRequest> requestMap = new HashMap<>();
    static Map<String, BigInteger> accruedFees = new HashMap<>();

    private static class MessageRequest {
        private final byte[] data;
        private final byte[] rollback;

        public MessageRequest(byte[] data, byte[] rollback) {
            this.data = data;
            this.rollback = rollback;
        }

        public byte[] getData() {
            return data;
        }

        public byte[] getRollback() {
            return rollback;
        }
    }

    static BigInteger getNextSn() {
        return getNextSn(BigInteger.ONE);
    }

    static BigInteger getNextSn(BigInteger inc) {
        srcSn = srcSn.add(inc);
        return srcSn;
    }

    static BigInteger getNextReqId() {
        reqId = reqId.add(BigInteger.ONE);
        return reqId;
    }

    static BigInteger getFixedFee(String net, String type) throws Exception {
        try {
            return callService.fixedFee(net, type).send();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static BigInteger getTotalFixedFees(String net) throws Exception {
        try {
            return callService.totalFixedFees(net).send();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static BigInteger getAccruedFees(String type) {
        try {
            return callService.accruedFees(type).send();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void accumulateFees(String net) throws Exception {
        final String[] types = {"relay", "protocol"};
        if (accruedFees.isEmpty()) {
            for (String type : types) {
                accruedFees.put(type, callService.fixedFee(net, type).send());
            }
        } else {
            for (String type : types) {
                BigInteger fee = callService.fixedFee(net, type).send();
                accruedFees.put(type, fee.add(accruedFees.get(type)));
            }
        }
    }

    @Order(0)
    @Test
    void sendCallMessageFromDAppProxy() throws Exception {
        byte[] data = "sendCallMessageFromDAppProxy".getBytes();
        var sn = getNextSn();
        requestMap.put(sn, new MessageRequest(data, null));
        var request = new CSMessageRequest(sampleAddress, to.account(), sn, false, data);
        var checker = MockBMCIntegrationTest.sendMessageEvent(
                (el) -> {
                    assertEquals(linkNet, el._to);
                    assertEquals(SERVICE, el._svc);
                    assertEquals(sn, el._sn);
                    CSMessage csMessage = CSMessage.fromBytes(el._msg);
                    assertEquals(CSMessage.REQUEST, csMessage.getType());
                    AssertCallService.assertEqualsCSMessageRequest(
                            request, CSMessageRequest.fromBytes(csMessage.getData()));
                });
        //TODO callMessageSentEvent
        BigInteger fee = getTotalFixedFees(to.net());
        accumulateFees(to.net());
        checker.accept(dAppProxySample.sendMessage(
                to.toString(), data, new byte[]{}, fee).send());
    }

    @Order(1)
    @Test
    void handleBTPMessageShouldEmitCallMessage() throws Exception {
        var from = new BTPAddress(linkNet, sampleAddress.toString());
        var reqId = getNextReqId();
        byte[] data = requestMap.get(srcSn).getData();
        var request = new CSMessageRequest(from.account(), to.account(), srcSn, false, data);
        var csMsg = new CSMessage(CSMessage.REQUEST, request.toBytes());
        var checker = MockBMCIntegrationTest.shouldSuccessHandleBTPMessage();
        checker = checker.andThen(
                CSIntegrationTest.callMessageEvent((el) -> {
                    assertEquals(Hash.sha3String(from.toString()), StringUtil.bytesToHex(el._from));
                    assertEquals(Hash.sha3String(sampleAddress), StringUtil.bytesToHex(el._to));
                    assertEquals(srcSn, el._sn);
                    assertEquals(reqId, el._reqId);
                    assertArrayEquals(request.getData(), el._data);
                }));
        checker.accept(MockBMCIntegrationTest.mockBMC.handleBTPMessage(
                csAddress,
                linkNet, SERVICE, srcSn, csMsg.toBytes()).send());
    }

    @Order(2)
    @Test
    void executeCallWithoutSuccessResponse() throws Exception {
        var from = new BTPAddress(linkNet, sampleAddress);
        var checker = CSIntegrationTest.messageReceivedEvent(
                (el) -> {
                    assertEquals(from.toString(), el._from);
                    assertArrayEquals(requestMap.get(srcSn).getData(), el._data);
                });
        checker = checker.andThen(MockBMCIntegrationTest.sendMessageEventShouldNotExists());
        checker.accept(callService.executeCall(reqId).send());
    }

    @Order(3)
    @Test
    void handleBTPMessageWithSuccessResponseButNoRequestEntry() throws Exception {
        var dstSn = BigInteger.ONE;
        var response = new CSMessageResponse(srcSn, CSMessageResponse.SUCCESS, null);
        var csMsg = new CSMessage(CSMessage.RESPONSE, response.toBytes());
        var checker = MockBMCIntegrationTest.shouldSuccessHandleBTPMessage();
        checker = checker.andThen(CSIntegrationTest.callRequestClearedEventShouldNotExists());
        checker.accept(MockBMCIntegrationTest.mockBMC.handleBTPMessage(
                csAddress,
                linkNet, SERVICE, dstSn, csMsg.toBytes()).send());
    }

    @Order(4)
    @Test
    void handleBTPMessageWithFailureResponseButNoRequestEntry() throws Exception {
        var dstSn = BigInteger.TWO;
        var response = new CSMessageResponse(srcSn, CSMessageResponse.FAILURE, "java.lang.IllegalArgumentException");
        var csMsg = new CSMessage(CSMessage.RESPONSE, response.toBytes());
        System.out.println(CSMessageResponse.fromBytes(csMsg.getData()));
        var checker = MockBMCIntegrationTest.shouldSuccessHandleBTPMessage();
        checker = checker.andThen(CSIntegrationTest.rollbackMessageEventShouldNotExists());
        checker.accept(MockBMCIntegrationTest.mockBMC.handleBTPMessage(
                csAddress,
                linkNet, SERVICE, dstSn, csMsg.toBytes()).send());
    }

    @Order(5)
    @Test
    @SuppressWarnings("ThrowableNotThrown")
    void maxPayloadsTest() throws Exception {
        BigInteger fee = getTotalFixedFees(to.net());
        byte[][][] cases = {
                {new byte[MAX_DATA_SIZE + 1], new byte[]{}},
                {new byte[MAX_DATA_SIZE], new byte[MAX_ROLLBACK_SIZE + 1]},
        };
        for (var c : cases) {
            AssertTransactionException.assertRevertReason(null, () ->
                    dAppProxySample.sendMessage(to.toString(), c[0], c[1], fee).send()
            );
        }
    }

    @Order(9)
    @Test
    void fixedFeesTest() throws Exception {
        BigInteger defaultTotalFees = getTotalFixedFees(to.net());
        assertEquals(BigInteger.valueOf(11).multiply(EXA), defaultTotalFees);

        // new fees for the net
        BigInteger relayFee = BigInteger.TWO.multiply(EXA);
        BigInteger protocolFee = EXA;

        var checker = CSIntegrationTest.fixedFeesUpdatedEvent(
                (el) -> {
                    assertEquals(Hash.sha3String(to.net()), StringUtil.bytesToHex(el._net));
                    assertEquals(relayFee, el._relay);
                    assertEquals(protocolFee, el._protocol);
                });
        checker.accept(callService.setFixedFees(
                to.net(), relayFee, protocolFee).send());

        assertEquals(relayFee, getFixedFee(to.net(), "relay"));
        assertEquals(protocolFee, getFixedFee(to.net(), "protocol"));
        assertEquals(relayFee.add(protocolFee), getTotalFixedFees(to.net()));
        assertEquals(defaultTotalFees, getTotalFixedFees("default"));
    }

    @Order(10)
    @Test
    void sendCallMessageWithRollback() throws Exception {
        _sendCallMessageWithRollback(BigInteger.ONE);
    }

    private void _sendCallMessageWithRollback(BigInteger inc) throws Exception {
        byte[] data = "sendCallMessageWithRollback".getBytes();
        byte[] rollback = "ThisIsRollbackMessage".getBytes();
        var sn = getNextSn(inc);
        requestMap.put(sn, new MessageRequest(data, rollback));
        var request = new CSMessageRequest(sampleAddress.toString(), fakeTo.account(), sn, true, data);
        var checker = MockBMCIntegrationTest.sendMessageEvent(
                (el) -> {
                    assertEquals(linkNet, el._to);
                    assertEquals(SERVICE, el._svc);
                    assertEquals(sn, el._sn);
                    CSMessage csMessage = CSMessage.fromBytes(el._msg);
                    assertEquals(CSMessage.REQUEST, csMessage.getType());
                    AssertCallService.assertEqualsCSMessageRequest(
                            request, CSMessageRequest.fromBytes(csMessage.getData()));
                });
        BigInteger fee = getTotalFixedFees(to.net());
        accumulateFees(to.net());
        checker.accept(dAppProxySample.sendMessage(
                fakeTo.toString(), data, rollback, fee).send());
    }

    @Order(11)
    @Test
    void executeCallWithFailureResponse() throws Exception {
        // relay the message first
        var from = new BTPAddress(linkNet, sampleAddress.toString());
//        byte[] data = requestMap.get(srcSn).getData();
        byte[] data = "sendCallMessageWithRollback".getBytes();
        var request = new CSMessageRequest(from.account(), fakeTo.account(), srcSn, true, data);
        var csMsg = new CSMessage(CSMessage.REQUEST, request.toBytes());
        MockBMCIntegrationTest.shouldSuccessHandleBTPMessage()
                .accept(MockBMCIntegrationTest.mockBMC.handleBTPMessage(
                        csAddress,
                        linkNet, SERVICE, srcSn, csMsg.toBytes()).send());

        // expect executeCall failure
        var reqId = getNextReqId();
        var response = new CSMessageResponse(srcSn, CSMessageResponse.FAILURE, "unknownError");
        var checker = MockBMCIntegrationTest.sendMessageEvent(
                (el) -> {
                    assertEquals(linkNet, el._to);
                    assertEquals(SERVICE, el._svc);
                    CSMessage csMessage = CSMessage.fromBytes(el._msg);
                    assertEquals(CSMessage.RESPONSE, csMessage.getType());
                    AssertCallService.assertEqualsCSMessageResponse(
                            response, CSMessageResponse.fromBytes(csMessage.getData()));
                });
        checker.accept(callService.executeCall(reqId).send());
    }

    @Order(12)
    @Test
    @SuppressWarnings("ThrowableNotThrown")
    void executeRollbackEarlyCallShouldFail() {
        AssertTransactionException.assertRevertReason(null, () ->
                callService.executeRollback(srcSn).send()
        );
    }

    @Order(13)
    @Test
    void handleBTPMessageWithFailureResponse() throws Exception {
        var dstSn = BigInteger.TWO;
        var response = new CSMessageResponse(srcSn, CSMessageResponse.FAILURE, "unknownError");
        var csMsg = new CSMessage(CSMessage.RESPONSE, response.toBytes());
        var checker = MockBMCIntegrationTest.shouldSuccessHandleBTPMessage();
        checker = checker.andThen(CSIntegrationTest.rollbackMessageEvent(
                (el) -> {
                    assertEquals(srcSn, el._sn);
                    assertArrayEquals(requestMap.get(srcSn).getRollback(), el._rollback);
                }));
        checker = checker.andThen(CSIntegrationTest.callRequestClearedEventShouldNotExists());
        checker.accept(MockBMCIntegrationTest.mockBMC.handleBTPMessage(
                csAddress,
                linkNet, SERVICE, dstSn, csMsg.toBytes()).send());
    }

    @Order(14)
    @Test
    void executeRollbackWithFailureResponse() throws Exception {
        var btpAddress = MockBMCIntegrationTest.btpAddress();
        var from = new BTPAddress(btpAddress.net(), Keys.toChecksumAddress(csAddress));
        var checker = CSIntegrationTest.messageReceivedEvent(
                (el) -> {
                    assertEquals(from.toString(), el._from);
                    assertArrayEquals(requestMap.get(srcSn).getRollback(), el._data);
                });
        checker = checker.andThen(CSIntegrationTest.callRequestClearedEvent(
                (el) -> {
                    assertEquals(srcSn, el._sn);
                }));
        checker.accept(callService.executeRollback(srcSn).send());
    }

    @Order(15)
    @Test
    void handleBTPMessageWithSuccessResponse() throws Exception {
        // prepare another call request
        _sendCallMessageWithRollback(BigInteger.TWO); // +1 due to executeCall response

        // check the CallRequestCleared event
        var dstSn = BigInteger.TEN;
        var response = new CSMessageResponse(srcSn, CSMessageResponse.SUCCESS, null);
        var csMsg = new CSMessage(CSMessage.RESPONSE, response.toBytes());
        var checker = MockBMCIntegrationTest.shouldSuccessHandleBTPMessage();
        checker = checker.andThen(
                CSIntegrationTest.callRequestClearedEvent(
                        (el) -> {
                            assertEquals(srcSn, el._sn);
                        }));
        checker = checker.andThen(CSIntegrationTest.rollbackMessageEventShouldNotExists());
        checker.accept(MockBMCIntegrationTest.mockBMC.handleBTPMessage(
                csAddress,
                linkNet, SERVICE, dstSn, csMsg.toBytes()).send());
    }

    @Order(19)
    @Test
    void verifyAccruedFees() throws Exception {
        final String[] types = {"relay", "protocol"};
        for (String type : types) {
            assertEquals(accruedFees.get(type), getAccruedFees(type));
        }
    }

    @Order(20)
    @Test
    void handleBTPErrorTest() throws Exception {
        // prepare another call request
        _sendCallMessageWithRollback(BigInteger.ONE);

        var btpAddress = MockBMCIntegrationTest.btpAddress();
        // check the BTP error message
        var checker = MockBMCIntegrationTest.shouldSuccessHandleBTPError();
        checker = checker.andThen(
                CSIntegrationTest.rollbackMessageEvent(
                        (el) -> {
                            assertEquals(srcSn, el._sn);
                            assertArrayEquals(requestMap.get(srcSn).getRollback(), el._rollback);
                        }));
        checker = checker.andThen(CSIntegrationTest.callRequestClearedEventShouldNotExists());
        checker.accept(MockBMCIntegrationTest.mockBMC.handleBTPError(
                csAddress,
                btpAddress.toString(), SERVICE, srcSn, BigInteger.ONE, "BTPError").send());
    }

    @Order(21)
    @Test
    void executeRollbackWithBTPError() throws Exception {
        var btpAddress = MockBMCIntegrationTest.btpAddress();
        var from = new BTPAddress(btpAddress.net(), Keys.toChecksumAddress(csAddress));
        var checker = CSIntegrationTest.messageReceivedEvent(
                (el) -> {
                    assertEquals(from.toString(), el._from);
                    assertArrayEquals(requestMap.get(srcSn).getRollback(), el._data);
                });
        checker = checker.andThen(
                CSIntegrationTest.callRequestClearedEvent(
                        (el) -> {
                            assertEquals(srcSn, el._sn);
                        }));
        checker.accept(callService.executeRollback(srcSn).send());
    }
}

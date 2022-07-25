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
import foundation.icon.btp.mock.MockBMCScoreClient;
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.MockBMCIntegrationTest;
import foundation.icon.btp.test.SendMessageEventLog;
import foundation.icon.jsonrpc.Address;
import foundation.icon.score.test.AssertRevertedException;
import foundation.icon.score.test.ScoreIntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CallServiceImplTest implements CSIntegrationTest {
    static final BigInteger EXA = BigInteger.TEN.pow(18);
    static Address csAddress = csClient._address();
    static Address sampleAddress = sampleClient._address();
    static Address fakeAddress = ScoreIntegrationTest.Faker.address(Address.Type.CONTRACT);
    static String linkNet = BTPIntegrationTest.Faker.btpNetwork();
    static BTPAddress to = new BTPAddress(linkNet, sampleAddress.toString());
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

    @BeforeAll
    static void beforeAll() {
    }

    private BigInteger getNextSn() {
        return getNextSn(BigInteger.ONE);
    }

    private BigInteger getNextSn(BigInteger inc) {
        srcSn = srcSn.add(inc);
        return srcSn;
    }

    private BigInteger getNextReqId() {
        reqId = reqId.add(BigInteger.ONE);
        return reqId;
    }

    private BigInteger getFixedFee(String net, String type) {
        Map<String, Object> params = new HashMap<>();
        params.put("_net", net);
        params.put("_type", type);
        return csClient._call(BigInteger.class, "fixedFee", params);
    }

    private BigInteger getTotalFixedFees(String net) {
        Map<String, Object> params = new HashMap<>();
        params.put("_net", net);
        return csClient._call(BigInteger.class, "totalFixedFees", params);
    }

    private BigInteger getAccruedFees(String type) {
        Map<String, Object> params = new HashMap<>();
        params.put("_type", type);
        return csClient._call(BigInteger.class, "accruedFees", params);
    }

    private void accumulateFees(String net) {
        final String[] types = {"relay", "protocol"};
        if (accruedFees.isEmpty()) {
            for (String type : types) {
                accruedFees.put(type, getFixedFee(net, type));
            }
        } else {
            for (String type : types) {
                BigInteger fee = getFixedFee(net, type);
                accruedFees.put(type, fee.add(accruedFees.get(type)));
            }
        }
    }

    @Order(0)
    @Test
    void sendCallMessageFromDAppProxy() {
        byte[] data = "sendCallMessageFromDAppProxy".getBytes();
        var sn = getNextSn();
        requestMap.put(sn, new MessageRequest(data, null));
        var request = new CSMessageRequest(sampleAddress.toString(), to.account(), sn, false, data);
        var checker = MockBMCIntegrationTest.eventLogChecker(SendMessageEventLog::eventLogs, (el) -> {
            assertEquals(linkNet, el.getTo());
            assertEquals(CallServiceImpl.SERVICE, el.getSvc());
            assertEquals(sn, el.getSn());
            CSMessage csMessage = CSMessage.fromBytes(el.getMsg());
            assertEquals(CSMessage.REQUEST, csMessage.getType());
            AssertCallService.assertEqualsCSMessageRequest(request, CSMessageRequest.fromBytes(csMessage.getData()));
        });
        BigInteger fee = getTotalFixedFees(to.net());
        accumulateFees(to.net());
        Map<String, Object> params = new HashMap<>();
        params.put("_to", to.toString());
        params.put("_data", data);
        checker.accept(sampleClient._send(fee, "sendMessage", params));
    }

    @Order(1)
    @Test
    void handleBTPMessageShouldEmitCallMessage() {
        var from = new BTPAddress(linkNet, sampleAddress.toString());
        var reqId = getNextReqId();
        byte[] data = requestMap.get(srcSn).getData();
        var request = new CSMessageRequest(from.account(), to.account(), srcSn, false, data);
        var csMsg = new CSMessage(CSMessage.REQUEST, request.toBytes());
        var checker = CSIntegrationTest.eventLogChecker(CallMessageEventLog::eventLogs, (el) -> {
            assertEquals(from.toString(), el.getFrom());
            assertEquals(sampleAddress.toString(), el.getTo());
            assertEquals(srcSn, el.getSn());
            assertEquals(reqId, el.getReqId());
            assertArrayEquals(request.getData(), el.getData());
        });
        ((MockBMCScoreClient) MockBMCIntegrationTest.mockBMC).intercallHandleBTPMessage(
                checker, csAddress,
                linkNet, CallServiceImpl.SERVICE, srcSn, csMsg.toBytes());
    }

    @Order(2)
    @Test
    void executeCallWithoutSuccessResponse() {
        var from = new BTPAddress(linkNet, sampleAddress.toString());
        var checker = ScoreIntegrationTest.eventLogChecker(sampleAddress, MessageReceivedEventLog::eventLogs, (el) -> {
            assertEquals(from.toString(), el.getFrom());
            assertArrayEquals(requestMap.get(srcSn).getData(), el.getData());
        }).andThen(CSIntegrationTest.notExistsEventLogChecker(SendMessageEventLog::eventLogs));
        ((CallServiceScoreClient) callSvc).executeCall(checker, reqId);
    }

    @Order(3)
    @Test
    void handleBTPMessageWithSuccessResponseButNoRequestEntry() {
        var dstSn = BigInteger.ONE;
        var response = new CSMessageResponse(srcSn, CSMessageResponse.SUCCESS, null);
        var csMsg = new CSMessage(CSMessage.RESPONSE, response.toBytes());
        var checker = CSIntegrationTest.notExistsEventLogChecker(CallRequestClearedEventLog::eventLogs);
        ((MockBMCScoreClient) MockBMCIntegrationTest.mockBMC).intercallHandleBTPMessage(
                checker, csAddress,
                linkNet, CallServiceImpl.SERVICE, dstSn, csMsg.toBytes());
    }

    @Order(4)
    @Test
    void handleBTPMessageWithFailureResponseButNoRequestEntry() {
        var dstSn = BigInteger.TWO;
        var response = new CSMessageResponse(srcSn, CSMessageResponse.FAILURE, "java.lang.IllegalArgumentException");
        var csMsg = new CSMessage(CSMessage.RESPONSE, response.toBytes());
        var checker = CSIntegrationTest.notExistsEventLogChecker(RollbackMessageEventLog::eventLogs);
        ((MockBMCScoreClient) MockBMCIntegrationTest.mockBMC).intercallHandleBTPMessage(
                checker, csAddress,
                linkNet, CallServiceImpl.SERVICE, dstSn, csMsg.toBytes());
    }

    @Order(5)
    @Test
    @SuppressWarnings("ThrowableNotThrown")
    void maxPayloadsTest() {
        BigInteger fee = getTotalFixedFees(to.net());
        byte[][][] cases = {
                {new byte[CallServiceImpl.MAX_DATA_SIZE + 1], null},
                {new byte[CallServiceImpl.MAX_DATA_SIZE], new byte[CallServiceImpl.MAX_ROLLBACK_SIZE + 1]},
        };
        for (var c : cases) {
            Map<String, Object> params = new HashMap<>();
            params.put("_to", to.toString());
            params.put("_data", c[0]);
            if (c[1] != null) {
                params.put("_rollback", c[1]);
            }
            AssertRevertedException.assertUserReverted(0, () ->
                    sampleClient._send(fee, "sendMessage", params)
            );
        }
    }

    @Order(9)
    @Test
    void fixedFeesTest() {
        BigInteger defaultTotalFees = getTotalFixedFees(to.net());
        assertEquals(BigInteger.valueOf(11).multiply(EXA), defaultTotalFees);

        // new fees for the net
        BigInteger relayFee = BigInteger.TWO.multiply(EXA);
        BigInteger protocolFee = EXA;

        var checker = CSIntegrationTest.eventLogChecker(FixedFeesEventLog::eventLogs, (el) -> {
            assertEquals(to.net(), el.getNet());
            assertEquals(relayFee, el.getRelayFee());
            assertEquals(protocolFee, el.getProtocolFee());
        });
        Map<String, Object> params = new HashMap<>();
        params.put("_net", to.net());
        params.put("_relay", relayFee);
        params.put("_protocol", protocolFee);
        checker.accept(csClient._send("setFixedFees", params));

        assertEquals(relayFee, getFixedFee(to.net(), "relay"));
        assertEquals(protocolFee, getFixedFee(to.net(), "protocol"));
        assertEquals(relayFee.add(protocolFee), getTotalFixedFees(to.net()));
        assertEquals(defaultTotalFees, getTotalFixedFees("default"));
    }

    @Order(10)
    @Test
    void sendCallMessageWithRollback() {
        _sendCallMessageWithRollback(BigInteger.ONE);
    }

    private void _sendCallMessageWithRollback(BigInteger inc) {
        byte[] data = "sendCallMessageWithRollback".getBytes();
        byte[] rollback = "ThisIsRollbackMessage".getBytes();
        var sn = getNextSn(inc);
        requestMap.put(sn, new MessageRequest(data, rollback));
        var request = new CSMessageRequest(sampleAddress.toString(), fakeTo.account(), sn, true, data);
        var checker = MockBMCIntegrationTest.eventLogChecker(SendMessageEventLog::eventLogs, (el) -> {
            assertEquals(linkNet, el.getTo());
            assertEquals(CallServiceImpl.SERVICE, el.getSvc());
            assertEquals(sn, el.getSn());
            CSMessage csMessage = CSMessage.fromBytes(el.getMsg());
            assertEquals(CSMessage.REQUEST, csMessage.getType());
            AssertCallService.assertEqualsCSMessageRequest(request, CSMessageRequest.fromBytes(csMessage.getData()));
        });
        BigInteger fee = getTotalFixedFees(to.net());
        accumulateFees(to.net());
        Map<String, Object> params = new HashMap<>();
        params.put("_to", fakeTo.toString());
        params.put("_data", data);
        params.put("_rollback", rollback);
        checker.accept(sampleClient._send(fee, "sendMessage", params));
    }

    @Order(11)
    @Test
    void executeCallWithFailureResponse() {
        // relay the message first
        var from = new BTPAddress(linkNet, sampleAddress.toString());
        byte[] data = requestMap.get(srcSn).getData();
        var request = new CSMessageRequest(from.account(), fakeTo.account(), srcSn, true, data);
        var csMsg = new CSMessage(CSMessage.REQUEST, request.toBytes());
        MockBMCIntegrationTest.mockBMC.intercallHandleBTPMessage(csAddress,
                linkNet, CallServiceImpl.SERVICE, srcSn, csMsg.toBytes());

        // expect executeCall failure
        var reqId = getNextReqId();
        var response = new CSMessageResponse(srcSn, CSMessageResponse.FAILURE, "java.lang.IllegalArgumentException");
        var checker = MockBMCIntegrationTest.eventLogChecker(SendMessageEventLog::eventLogs, (el) -> {
            assertEquals(linkNet, el.getTo());
            assertEquals(CallServiceImpl.SERVICE, el.getSvc());
            CSMessage csMessage = CSMessage.fromBytes(el.getMsg());
            assertEquals(CSMessage.RESPONSE, csMessage.getType());
            AssertCallService.assertEqualsCSMessageResponse(response, CSMessageResponse.fromBytes(csMessage.getData()));
        });
        ((CallServiceScoreClient) callSvc).executeCall(checker, reqId);
    }

    @Order(12)
    @Test
    @SuppressWarnings("ThrowableNotThrown")
    void executeRollbackEarlyCallShouldFail() {
        AssertRevertedException.assertUserReverted(0, () ->
                callSvc.executeRollback(srcSn)
        );
    }

    @Order(13)
    @Test
    void handleBTPMessageWithFailureResponse() {
        var dstSn = BigInteger.TWO;
        var response = new CSMessageResponse(srcSn, CSMessageResponse.FAILURE, "java.lang.IllegalArgumentException");
        var csMsg = new CSMessage(CSMessage.RESPONSE, response.toBytes());
        var checker = CSIntegrationTest.eventLogChecker(RollbackMessageEventLog::eventLogs, (el) -> {
            assertEquals(srcSn, el.getSn());
            assertArrayEquals(requestMap.get(srcSn).getRollback(), el.getRollback());
        }).andThen(CSIntegrationTest.notExistsEventLogChecker(CallRequestClearedEventLog::eventLogs));
        ((MockBMCScoreClient) MockBMCIntegrationTest.mockBMC).intercallHandleBTPMessage(
                checker, csAddress,
                linkNet, CallServiceImpl.SERVICE, dstSn, csMsg.toBytes());
    }

    @Order(14)
    @Test
    void executeRollbackWithFailureResponse() {
        var btpAddress = BTPAddress.valueOf(MockBMCIntegrationTest.mockBMC.getBtpAddress());
        var from = new BTPAddress(btpAddress.net(), csAddress.toString());
        var checker = ScoreIntegrationTest.eventLogChecker(sampleAddress, MessageReceivedEventLog::eventLogs, (el) -> {
            assertEquals(from.toString(), el.getFrom());
            assertArrayEquals(requestMap.get(srcSn).getRollback(), el.getData());
        }).andThen(CSIntegrationTest.eventLogChecker(CallRequestClearedEventLog::eventLogs, (el) -> {
            assertEquals(srcSn, el.getSn());
        }));
        ((CallServiceScoreClient) callSvc).executeRollback(checker, srcSn);
    }

    @Order(15)
    @Test
    void handleBTPMessageWithSuccessResponse() {
        // prepare another call request
        _sendCallMessageWithRollback(BigInteger.TWO); // +1 due to executeCall response

        // check the CallRequestCleared event
        var dstSn = BigInteger.TEN;
        var response = new CSMessageResponse(srcSn, CSMessageResponse.SUCCESS, null);
        var csMsg = new CSMessage(CSMessage.RESPONSE, response.toBytes());
        var checker = CSIntegrationTest.eventLogChecker(CallRequestClearedEventLog::eventLogs, (el) -> {
            assertEquals(srcSn, el.getSn());
        }).andThen(CSIntegrationTest.notExistsEventLogChecker(RollbackMessageEventLog::eventLogs));
        ((MockBMCScoreClient) MockBMCIntegrationTest.mockBMC).intercallHandleBTPMessage(
                checker, csAddress,
                linkNet, CallServiceImpl.SERVICE, dstSn, csMsg.toBytes());
    }

    @Order(19)
    @Test
    void verifyAccruedFees() {
        final String[] types = {"relay", "protocol"};
        for (String type : types) {
            assertEquals(accruedFees.get(type), getAccruedFees(type));
        }
    }

    @Order(20)
    @Test
    void handleBTPErrorTest() {
        // prepare another call request
        _sendCallMessageWithRollback(BigInteger.ONE);

        // check the BTP error message
        var checker = CSIntegrationTest.eventLogChecker(RollbackMessageEventLog::eventLogs, (el) -> {
            assertEquals(srcSn, el.getSn());
            assertArrayEquals(requestMap.get(srcSn).getRollback(), el.getRollback());
        }).andThen(CSIntegrationTest.notExistsEventLogChecker(CallRequestClearedEventLog::eventLogs));
        var btpAddress = BTPAddress.valueOf(MockBMCIntegrationTest.mockBMC.getBtpAddress());
        ((MockBMCScoreClient) MockBMCIntegrationTest.mockBMC).intercallHandleBTPError(
                checker, csAddress,
                btpAddress.toString(), CallServiceImpl.SERVICE, srcSn, 1, "BTPError");
    }

    @Order(21)
    @Test
    void executeRollbackWithBTPError() {
        var btpAddress = BTPAddress.valueOf(MockBMCIntegrationTest.mockBMC.getBtpAddress());
        var from = new BTPAddress(btpAddress.net(), csAddress.toString());
        var checker = ScoreIntegrationTest.eventLogChecker(sampleAddress, MessageReceivedEventLog::eventLogs, (el) -> {
            assertEquals(from.toString(), el.getFrom());
            assertArrayEquals(requestMap.get(srcSn).getRollback(), el.getData());
        }).andThen(CSIntegrationTest.eventLogChecker(CallRequestClearedEventLog::eventLogs, (el) -> {
            assertEquals(srcSn, el.getSn());
        }));
        ((CallServiceScoreClient) callSvc).executeRollback(checker, srcSn);
    }
}

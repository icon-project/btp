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

package foundation.icon.btp.arbcall;

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

    private BigInteger getFixedFee(String net) {
        Map<String, Object> params = new HashMap<>();
        params.put("_net", net);
        return csClient._call(BigInteger.class, "fixedFee", params);
    }

    @Order(0)
    @Test
    void sendCallMessageFromDAppProxy() {
        byte[] data = "sendCallMessageFromDAppProxy".getBytes();
        var sn = getNextSn();
        requestMap.put(sn, new MessageRequest(data, null));
        var request = new CSMessageRequest(sampleAddress.toString(), to.account(), data);
        var checker = MockBMCIntegrationTest.eventLogChecker(SendMessageEventLog::eventLogs, (el) -> {
            assertEquals(linkNet, el.getTo());
            assertEquals(CallServiceImpl.SERVICE, el.getSvc());
            assertEquals(sn, el.getSn());
            CSMessage csMessage = CSMessage.fromBytes(el.getMsg());
            assertEquals(CSMessage.REQUEST, csMessage.getType());
            AssertCallService.assertEqualsCSMessageRequest(request, CSMessageRequest.fromBytes(csMessage.getData()));
        });
        BigInteger fee = getFixedFee(to.net());
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
        var request = new CSMessageRequest(from.account(), to.account(), data);
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
    void executeCallWithSuccessResponse() {
        var from = new BTPAddress(linkNet, sampleAddress.toString());
        var response = new CSMessageResponse(srcSn, CSMessageResponse.SUCCESS, null);
        var checker = MockBMCIntegrationTest.eventLogChecker(SendMessageEventLog::eventLogs, (el) -> {
            assertEquals(linkNet, el.getTo());
            assertEquals(CallServiceImpl.SERVICE, el.getSvc());
            CSMessage csMessage = CSMessage.fromBytes(el.getMsg());
            assertEquals(CSMessage.RESPONSE, csMessage.getType());
            AssertCallService.assertEqualsCSMessageResponse(response, CSMessageResponse.fromBytes(csMessage.getData()));
        }).andThen(ScoreIntegrationTest.eventLogChecker(sampleAddress, MessageReceivedEventLog::eventLogs, (el) -> {
            assertEquals(from.toString(), el.getFrom());
            assertArrayEquals(requestMap.get(srcSn).getData(), el.getData());
        }));
        ((CallServiceScoreClient) callSvc).executeCall(checker, reqId);
    }

    @Order(3)
    @Test
    void handleBTPMessageWithSuccessResponse() {
        var dstSn = BigInteger.ONE;
        var response = new CSMessageResponse(srcSn, CSMessageResponse.SUCCESS, null);
        var csMsg = new CSMessage(CSMessage.RESPONSE, response.toBytes());
        var checker = CSIntegrationTest.eventLogChecker(CallRequestClearedEventLog::eventLogs, (el) -> {
            assertEquals(srcSn, el.getSn());
        });
        ((MockBMCScoreClient) MockBMCIntegrationTest.mockBMC).intercallHandleBTPMessage(
                checker, csAddress,
                linkNet, CallServiceImpl.SERVICE, dstSn, csMsg.toBytes());
    }

    @Order(9)
    @Test
    void fixedFeeTest() {
        BigInteger fee = getFixedFee(to.net());
        assertEquals(BigInteger.TEN.multiply(EXA), fee);

        final BigInteger nineIcx = BigInteger.valueOf(9).multiply(EXA);
        var checker = CSIntegrationTest.eventLogChecker(FixedFeeEventLog::eventLogs, (el) -> {
            assertEquals(to.net(), el.getNet());
            assertEquals(nineIcx, el.getFee());
        });
        BigInteger fee2 = fee.subtract(EXA);
        Map<String, Object> params = new HashMap<>();
        params.put("_net", to.net());
        params.put("_fee", fee2);
        checker.accept(csClient._send("setFixedFee", params));

        assertEquals(nineIcx, getFixedFee(to.net()));
        assertEquals(fee, getFixedFee("default"));
    }

    @Order(10)
    @Test
    void sendCallMessageWithRollback() {
        byte[] data = "sendCallMessageWithRollback".getBytes();
        byte[] rollback = "ThisIsRollbackMessage".getBytes();
        var sn = getNextSn(BigInteger.TWO); // +1 due to executeCall response
        requestMap.put(sn, new MessageRequest(data, rollback));
        var request = new CSMessageRequest(sampleAddress.toString(), fakeTo.account(), data);
        var checker = MockBMCIntegrationTest.eventLogChecker(SendMessageEventLog::eventLogs, (el) -> {
            assertEquals(linkNet, el.getTo());
            assertEquals(CallServiceImpl.SERVICE, el.getSvc());
            assertEquals(sn, el.getSn());
            CSMessage csMessage = CSMessage.fromBytes(el.getMsg());
            assertEquals(CSMessage.REQUEST, csMessage.getType());
            AssertCallService.assertEqualsCSMessageRequest(request, CSMessageRequest.fromBytes(csMessage.getData()));
        });
        BigInteger fee = getFixedFee(to.net());
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
        var request = new CSMessageRequest(from.account(), fakeTo.account(), data);
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

    @Order(20)
    @Test
    void handleBTPErrorTest() {
        // prepare another call request
        sendCallMessageWithRollback();

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

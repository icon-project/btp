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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CallServiceImplTest implements CSIntegrationTest {
    static Address csAddress = csClient._address();
    static Address sampleAddress = sampleClient._address();
    static String linkNet = BTPIntegrationTest.Faker.btpNetwork();
    static BTPAddress to = new BTPAddress(linkNet, sampleAddress.toString());
    static BigInteger srcSn = BigInteger.ZERO;
    static BigInteger reqId = BigInteger.ZERO;

    @BeforeAll
    static void beforeAll() {
    }

    private BigInteger getNextSn() {
        srcSn = srcSn.add(BigInteger.ONE);
        return srcSn;
    }

    private BigInteger getNextReqId() {
        reqId = reqId.add(BigInteger.ONE);
        return reqId;
    }

    @Order(0)
    @Test
    void sendCallMessageFromDAppProxy() {
        byte[] data = "Hello BTP".getBytes();
        var sn = getNextSn();
        var request = new CSMessageRequest(sampleAddress.toString(), to.account(), data);
        var checker = MockBMCIntegrationTest.eventLogChecker(SendMessageEventLog::eventLogs, (el) -> {
            assertEquals(linkNet, el.getTo());
            assertEquals(CallServiceImpl.SERVICE, el.getSvc());
            assertEquals(sn, el.getSn());
            CSMessage csMessage = CSMessage.fromBytes(el.getMsg());
            assertEquals(CSMessage.REQUEST, csMessage.getType());
            AssertCallService.assertEqualsCSMessageRequest(request, CSMessageRequest.fromBytes(csMessage.getData()));
        });
        Map<String, Object> params = new HashMap<>();
        params.put("_to", to.toString());
        params.put("_data", data);
        checker.accept(sampleClient._send("sendMessage", params));
    }

    @Order(1)
    @Test
    void handleBTPMessageShouldEmitCallMessage() {
        var from = new BTPAddress(linkNet, sampleAddress.toString());
        var reqId = getNextReqId();
        byte[] data = "handleBTPMessageShouldEmitCallMessage".getBytes();
        var reqMsg = new CSMessageRequest(from.account(), to.account(), data);
        var csMsg = new CSMessage(CSMessage.REQUEST, reqMsg.toBytes());
        var checker = CSIntegrationTest.eventLogChecker(CallMessageEventLog::eventLogs, (el) -> {
            assertEquals(from.toString(), el.getFrom());
            assertEquals(sampleAddress.toString(), el.getTo());
            assertEquals(srcSn, el.getSn());
            assertEquals(reqId, el.getReqId());
            assertArrayEquals(reqMsg.getData(), el.getData());
        });
        ((MockBMCScoreClient) MockBMCIntegrationTest.mockBMC).intercallHandleBTPMessage(
                checker, csAddress,
                linkNet, CallServiceImpl.SERVICE, srcSn, csMsg.toBytes());
    }

    @Order(2)
    @Test
    void executeCallWithSuccessResponse() {
        var response = new CSMessageResponse(srcSn, CSMessageResponse.SUCCESS, null);
        var checker = MockBMCIntegrationTest.eventLogChecker(SendMessageEventLog::eventLogs, (el) -> {
            assertEquals(linkNet, el.getTo());
            assertEquals(CallServiceImpl.SERVICE, el.getSvc());
            CSMessage csMessage = CSMessage.fromBytes(el.getMsg());
            assertEquals(CSMessage.RESPONSE, csMessage.getType());
            AssertCallService.assertEqualsCSMessageResponse(response, CSMessageResponse.fromBytes(csMessage.getData()));
        });
        ((CallServiceScoreClient) callSvc).executeCall(checker, reqId);
    }

    @Order(3)
    @Test
    void handleBTPMessageWithSuccessResponse() {
        var resMsg = new CSMessageResponse(srcSn, CSMessageResponse.SUCCESS, null);
        var csMsg = new CSMessage(CSMessage.RESPONSE, resMsg.toBytes());
        var checker = CSIntegrationTest.eventLogChecker(CallRequestClearedEventLog::eventLogs, (el) -> {
            assertEquals(srcSn, el.getSn());
        });
        ((MockBMCScoreClient) MockBMCIntegrationTest.mockBMC).intercallHandleBTPMessage(
                checker, csAddress,
                linkNet, CallServiceImpl.SERVICE, BigInteger.ONE, csMsg.toBytes());
    }
}

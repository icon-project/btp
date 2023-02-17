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

import static org.junit.jupiter.api.Assertions.*;

public class CallServiceTest implements CSIntegrationTest {
    static String csAddress = callService.getContractAddress();
    static String sampleAddress = dAppProxySample.getContractAddress();
    static String fakeAddress = EVMIntegrationTest.Faker.address().toString();
    static String linkNet = BTPIntegrationTest.Faker.btpNetwork();
    static BTPAddress to = new BTPAddress(linkNet, sampleAddress);
    static BTPAddress fakeTo = new BTPAddress(linkNet, fakeAddress);
    static BTPAddress bmcBtpAddress;
    static BigInteger srcSn = BigInteger.ZERO;
    static BigInteger reqId = BigInteger.ZERO;
    static Map<BigInteger, MessageRequest> requestMap = new HashMap<>();

    static BigInteger forwardFee, backwardFee, protocolFee;
    static String protocolFeeHandler = EVMIntegrationTest.Faker.address().toString();
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

    static final String NULL_ADDRESS = new Address(BigInteger.ZERO).toString();

    @BeforeAll
    static void beforeAll() throws Exception {
        forwardFee = BigInteger.TEN;
        backwardFee = BigInteger.TWO;
        MockBMCIntegrationTest.mockBMC.setFee(forwardFee, backwardFee).send();

        protocolFee = BigInteger.valueOf(3);
        feeManager.setProtocolFee(protocolFee).send();
        assertEquals(NULL_ADDRESS, feeManager.getProtocolFeeHandler().send());

        bmcBtpAddress = MockBMCIntegrationTest.btpAddress();
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

    private BigInteger getFee(String net, boolean rollback) throws Exception {
        return feeManager.getFee(net, rollback).send();
    }

    private void accumulateFee(BigInteger totalFee, BigInteger protocolFee) throws Exception {
        BigInteger relayFee = totalFee.subtract(protocolFee);
        accruedFees.merge("relay", relayFee, (a, b) -> b.add(a));
        accruedFees.merge("protocol", protocolFee, (a, b) -> b.add(a));
    }

    @Order(0)
    @Test
    void sendCallMessageWithoutRollback() throws Exception {
        byte[] data = "sendCallMessageWithoutRollback".getBytes();
        var sn = getNextSn();
        requestMap.put(sn, new MessageRequest(data, null));
        var request = new CSMessageRequest(sampleAddress, to.account(), sn, false, data);
        var checker = MockBMCIntegrationTest.sendMessageEvent((el) -> {
            assertEquals(linkNet, el._to);
            assertEquals(NAME, el._svc);
            assertEquals(BigInteger.ZERO, el._sn);
            CSMessage csMessage = CSMessage.fromBytes(el._msg);
            assertEquals(CSMessage.REQUEST, csMessage.getType());
            AssertCallService.assertEqualsCSMessageRequest(request, CSMessageRequest.fromBytes(csMessage.getData()));
        }).andThen(CSIntegrationTest.callMessageSentEvent((el) -> {
            assertEquals(sampleAddress, el._from);
            assertEquals(Hash.sha3String(to.toString()), StringUtil.bytesToHex(el._to));
            assertEquals(sn, el._sn);
            try {
                assertEquals(MockBMCIntegrationTest.mockBMC.getNetworkSn().send(), el._nsn);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
        BigInteger fee = getFee(to.net(), false);
        assertEquals(forwardFee.add(protocolFee), fee);
        accumulateFee(fee, protocolFee);
        checker.accept(dAppProxySample.sendMessage(
                to.toString(), data, new byte[]{}, fee).send());
    }

    @Order(1)
    @Test
    void handleBTPMessageShouldEmitCallMessage() throws Exception {
        var from = new BTPAddress(linkNet, sampleAddress);
        var reqId = getNextReqId();
        byte[] data = requestMap.get(srcSn).getData();
        var request = new CSMessageRequest(from.account(), to.account(), srcSn, false, data);
        var csMsg = new CSMessage(CSMessage.REQUEST, request.toBytes());
        var checker = CSIntegrationTest.callMessageEvent((el) -> {
            assertEquals(Hash.sha3String(from.toString()), StringUtil.bytesToHex(el._from));
            assertEquals(Hash.sha3String(sampleAddress), StringUtil.bytesToHex(el._to));
            assertEquals(srcSn, el._sn);
            assertEquals(reqId, el._reqId);
        });
        checker.accept(MockBMCIntegrationTest.mockBMC.handleBTPMessage(
                csAddress,
                // _sn field should be zero since this is a one-way message
                linkNet, NAME, BigInteger.ZERO, csMsg.toBytes()).send());
    }

    @Order(2)
    @Test
    void executeCallWithoutSuccessResponse() throws Exception {
        var from = new BTPAddress(linkNet, sampleAddress);
        var checker = CSIntegrationTest.messageReceivedEvent((el) -> {
            assertEquals(from.toString(), el._from);
            assertArrayEquals(requestMap.get(srcSn).getData(), el._data);
        }).andThen(CSIntegrationTest.callExecutedEvent((el) -> {
            assertEquals(reqId, el._reqId);
            assertEquals(BigInteger.ZERO, el._code);
            assertEquals("", el._msg);
        })).andThen(MockBMCIntegrationTest.sendMessageEventShouldNotExists());
        checker.accept(callService.executeCall(reqId).send());
    }

    @Order(3)
    @Test
    void handleBTPMessageWithSuccessResponseButNoRequestEntry() throws Exception {
        var dstSn = BigInteger.ONE;
        var response = new CSMessageResponse(srcSn, CSMessageResponse.SUCCESS, null);
        var csMsg = new CSMessage(CSMessage.RESPONSE, response.toBytes());
        var checker = CSIntegrationTest.responseMessageEventShouldNotExists();
        checker.accept(MockBMCIntegrationTest.mockBMC.handleBTPMessage(
                csAddress,
                linkNet, NAME, dstSn, csMsg.toBytes()).send());
    }

    @Order(4)
    @Test
    void handleBTPMessageWithFailureResponseButNoRequestEntry() throws Exception {
        var dstSn = BigInteger.TWO;
        var response = new CSMessageResponse(srcSn, CSMessageResponse.FAILURE, "java.lang.IllegalArgumentException");
        var csMsg = new CSMessage(CSMessage.RESPONSE, response.toBytes());
        System.out.println(CSMessageResponse.fromBytes(csMsg.getData()));
        var checker = CSIntegrationTest.rollbackMessageEventShouldNotExists();
        checker.accept(MockBMCIntegrationTest.mockBMC.handleBTPMessage(
                csAddress,
                linkNet, NAME, dstSn, csMsg.toBytes()).send());
    }

    @Order(5)
    @Test
    @SuppressWarnings("ThrowableNotThrown")
    void maxPayloadsTest() throws Exception {
        byte[][][] cases = {
                {new byte[MAX_DATA_SIZE + 1], new byte[]{}},
                {new byte[MAX_DATA_SIZE], new byte[MAX_ROLLBACK_SIZE + 1]},
        };
        for (var c : cases) {
            BigInteger fee = getFee(to.net(), c[1] != null);
            AssertTransactionException.assertRevertReason(null, () ->
                    dAppProxySample.sendMessage(to.toString(), c[0], c[1], fee).send()
            );
        }
    }

    @Order(6)
    @Test
    void sendCallMessageFromEOA() throws Exception {
        byte[] data = "sendCallMessageFromEOA".getBytes();
        var sn = getNextSn();
        requestMap.put(sn, new MessageRequest(data, null));
        var caller = EVMIntegrationTest.credentials.getAddress();
        var request = new CSMessageRequest(caller, to.account(), sn, false, data);
        var checker = MockBMCIntegrationTest.sendMessageEvent((el) -> {
            assertEquals(linkNet, el._to);
            assertEquals(NAME, el._svc);
            assertEquals(BigInteger.ZERO, el._sn);
            CSMessage csMessage = CSMessage.fromBytes(el._msg);
            assertEquals(CSMessage.REQUEST, csMessage.getType());
            AssertCallService.assertEqualsCSMessageRequest(request, CSMessageRequest.fromBytes(csMessage.getData()));
        }).andThen(CSIntegrationTest.callMessageSentEvent((el) -> {
            assertEquals(caller, el._from);
            assertEquals(Hash.sha3String(to.toString()), StringUtil.bytesToHex(el._to));
            assertEquals(sn, el._sn);
            try {
                assertEquals(MockBMCIntegrationTest.mockBMC.getNetworkSn().send(), el._nsn);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
        BigInteger fee = getFee(to.net(), false);
        assertEquals(forwardFee.add(protocolFee), fee);
        accumulateFee(fee, protocolFee);

        // fail if rollback is provided
        AssertTransactionException.assertRevertReason(null, () ->
                callService.sendCallMessage(to.toString(), data, "fakeRollback".getBytes(), fee).send()
        );
        // success if rollback is null
        checker.accept(callService.sendCallMessage(to.toString(), data, new byte[]{}, fee).send());
    }

    @Order(7)
    @Test
    void handleBTPMessageShouldEmitCallMessageFromEOA() throws Exception {
        var caller = EVMIntegrationTest.credentials.getAddress();
        var from = new BTPAddress(linkNet, caller);
        var reqId = getNextReqId();
        byte[] data = requestMap.get(srcSn).getData();
        var request = new CSMessageRequest(from.account(), to.account(), srcSn, false, data);
        var csMsg = new CSMessage(CSMessage.REQUEST, request.toBytes());
        var checker = CSIntegrationTest.callMessageEvent((el) -> {
            assertEquals(Hash.sha3String(from.toString()), StringUtil.bytesToHex(el._from));
            assertEquals(Hash.sha3String(to.account()), StringUtil.bytesToHex(el._to));
            assertEquals(srcSn, el._sn);
            assertEquals(reqId, el._reqId);
        });
        checker.accept(MockBMCIntegrationTest.mockBMC.handleBTPMessage(
                csAddress,
                // _sn field should be zero since this is a one-way message
                linkNet, NAME, BigInteger.ZERO, csMsg.toBytes()).send());
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
        var request = new CSMessageRequest(sampleAddress, fakeTo.account(), sn, true, data);
        var checker = MockBMCIntegrationTest.sendMessageEvent((el) -> {
            assertEquals(linkNet, el._to);
            assertEquals(NAME, el._svc);
            assertEquals(sn, el._sn);
            CSMessage csMessage = CSMessage.fromBytes(el._msg);
            assertEquals(CSMessage.REQUEST, csMessage.getType());
            AssertCallService.assertEqualsCSMessageRequest(
                    request, CSMessageRequest.fromBytes(csMessage.getData()));
        }).andThen(CSIntegrationTest.callMessageSentEvent((el) -> {
            assertEquals(sampleAddress, el._from);
            assertEquals(Hash.sha3String(fakeTo.toString()), StringUtil.bytesToHex(el._to));
            assertEquals(sn, el._sn);
            try {
                assertEquals(MockBMCIntegrationTest.mockBMC.getNetworkSn().send(), el._nsn);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }));
        BigInteger fee = getFee(to.net(), true);
        accumulateFee(fee, protocolFee);
        checker.accept(dAppProxySample.sendMessage(
                fakeTo.toString(), data, rollback, fee).send());
    }

    @Order(11)
    @Test
    void executeCallWithFailureResponse() throws Exception {
        // relay the message first
        var from = new BTPAddress(linkNet, sampleAddress);
        byte[] data = requestMap.get(srcSn).getData();
        var request = new CSMessageRequest(from.account(), fakeTo.account(), srcSn, true, data);
        var csMsg = new CSMessage(CSMessage.REQUEST, request.toBytes());
        MockBMCIntegrationTest.mockBMC.handleBTPMessage(
                csAddress,
                linkNet, NAME, srcSn, csMsg.toBytes()).send();
        // add response info to BMC mock
        MockBMCIntegrationTest.mockBMC.addResponse(fakeTo.net(), NAME, srcSn).send();

        // expect executeCall failure
        var reqId = getNextReqId();
        var response = new CSMessageResponse(srcSn, CSMessageResponse.FAILURE, "unknownError");
        var checker = MockBMCIntegrationTest.sendMessageEvent((el) -> {
            assertEquals(linkNet, el._to);
            assertEquals(NAME, el._svc);
            CSMessage csMessage = CSMessage.fromBytes(el._msg);
            assertEquals(CSMessage.RESPONSE, csMessage.getType());
            AssertCallService.assertEqualsCSMessageResponse(
                    response, CSMessageResponse.fromBytes(csMessage.getData()));
        }).andThen(CSIntegrationTest.callExecutedEvent((el) -> {
            assertEquals(reqId, el._reqId);
            assertEquals(CSMessageResponse.FAILURE, el._code.intValue());
            assertEquals(response.getMsg(), el._msg);
        }));
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

    @Order(12)
    @Test
    @SuppressWarnings("ThrowableNotThrown")
    void handleCallMessageFromInvalidCaller() {
        var from = new BTPAddress(bmcBtpAddress.net(), Keys.toChecksumAddress(csAddress));
        byte[] fakeRollback = "ThisIsFakeRollback".getBytes();
        AssertTransactionException.assertRevertReason(null, () ->
                dAppProxySample.handleCallMessage(from.toString(), fakeRollback).send()
        );
    }

    @Order(13)
    @Test
    void handleBTPMessageWithFailureResponse() throws Exception {
        var dstSn = BigInteger.TWO;
        var response = new CSMessageResponse(srcSn, CSMessageResponse.FAILURE, "unknownError");
        var csMsg = new CSMessage(CSMessage.RESPONSE, response.toBytes());
        var checker = CSIntegrationTest.responseMessageEvent((el) -> {
            assertEquals(srcSn, el._sn);
            assertEquals(CSMessageResponse.FAILURE, el._code.intValue());
            assertEquals(response.getMsg(), el._msg);
        }).andThen(CSIntegrationTest.rollbackMessageEvent((el) -> {
            assertEquals(srcSn, el._sn);
        }));
        checker.accept(MockBMCIntegrationTest.mockBMC.handleBTPMessage(
                csAddress,
                linkNet, NAME, dstSn, csMsg.toBytes()).send());
    }

    @Order(14)
    @Test
    void executeRollbackWithFailureResponse() throws Exception {
        var from = new BTPAddress(bmcBtpAddress.net(), Keys.toChecksumAddress(csAddress));
        var checker = CSIntegrationTest.rollbackDataReceivedEvent((el) -> {
            assertEquals(from.toString(), el._from);
            assertEquals(srcSn, el._ssn);
            assertArrayEquals(requestMap.get(srcSn).getRollback(), el._rollback);
        }).andThen(CSIntegrationTest.rollbackExecutedEvent((el) -> {
            assertEquals(srcSn, el._sn);
            assertEquals(CSMessageResponse.SUCCESS, el._code.intValue());
            assertEquals("", el._msg);
        }));
        checker.accept(callService.executeRollback(srcSn).send());
    }

    @Order(15)
    @Test
    void setProtocolFeeHandler() throws Exception {
        BigInteger feeBalance = EVMIntegrationTest.getBalance(callService);
        assertEquals(accruedFees.get("protocol"), feeBalance);

        assertEquals(BigInteger.ZERO, EVMIntegrationTest.getBalance(protocolFeeHandler));
        feeManager.setProtocolFeeHandler(protocolFeeHandler).send();
        assertEquals(feeBalance, EVMIntegrationTest.getBalance(protocolFeeHandler));
    }

    @Order(16)
    @Test
    void handleBTPMessageWithSuccessResponse() throws Exception {
        // prepare another call request
        _sendCallMessageWithRollback(BigInteger.ONE);

        var dstSn = BigInteger.TEN;
        var response = new CSMessageResponse(srcSn, CSMessageResponse.SUCCESS, null);
        var csMsg = new CSMessage(CSMessage.RESPONSE, response.toBytes());
        var checker = CSIntegrationTest.responseMessageEvent((el) -> {
            assertEquals(srcSn, el._sn);
            assertEquals(CSMessageResponse.SUCCESS, el._code.intValue());
            assertEquals("", el._msg);
        }).andThen(CSIntegrationTest.rollbackMessageEventShouldNotExists());
        checker.accept(MockBMCIntegrationTest.mockBMC.handleBTPMessage(
                csAddress,
                linkNet, NAME, dstSn, csMsg.toBytes()).send());
    }

    @Order(17)
    @Test
    void verifyFeeTransfer() {
        assertEquals(BigInteger.ZERO, EVMIntegrationTest.getBalance(callService));
        assertEquals(accruedFees.get("protocol"), EVMIntegrationTest.getBalance(protocolFeeHandler));
    }

    @Order(18)
    @Test
    void resetProtocolFeeHandler() throws Exception {
        assertEquals(protocolFeeHandler, feeManager.getProtocolFeeHandler().send());
        feeManager.setProtocolFeeHandler(NULL_ADDRESS).send();
        assertEquals(NULL_ADDRESS, feeManager.getProtocolFeeHandler().send());
    }

    @Order(20)
    @Test
    void handleBTPErrorTest() throws Exception {
        // prepare another call request
        _sendCallMessageWithRollback(BigInteger.ONE);

        // check the BTP error message
        var checker = CSIntegrationTest.responseMessageEvent((el) -> {
            assertEquals(srcSn, el._sn);
            assertEquals(CSMessageResponse.BTP_ERROR, el._code.intValue());
            assertTrue(el._msg.startsWith("BTPError"));
        }).andThen(CSIntegrationTest.rollbackMessageEvent((el) -> {
            assertEquals(srcSn, el._sn);
        }));
        checker.accept(MockBMCIntegrationTest.mockBMC.handleBTPError(
                csAddress,
                bmcBtpAddress.toString(), NAME, srcSn, BigInteger.ONE, "BTPError").send());
    }

    @Order(21)
    @Test
    void executeRollbackWithBTPError() throws Exception {
        var from = new BTPAddress(bmcBtpAddress.net(), Keys.toChecksumAddress(csAddress));
        var checker = CSIntegrationTest.rollbackDataReceivedEvent((el) -> {
            assertEquals(from.toString(), el._from);
            assertEquals(srcSn, el._ssn);
            assertArrayEquals(requestMap.get(srcSn).getRollback(), el._rollback);
        }).andThen(CSIntegrationTest.rollbackExecutedEvent((el) -> {
            assertEquals(srcSn, el._sn);
            assertEquals(CSMessageResponse.SUCCESS, el._code.intValue());
            assertEquals("", el._msg);
        }));
        checker.accept(callService.executeRollback(srcSn).send());
    }
}

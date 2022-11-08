/*
 * Copyright 2021 ICON Foundation
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

package foundation.icon.btp.mock;

import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.lib.BTPException;
import foundation.icon.btp.test.*;
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Keys;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MockBMCTest implements BTPIntegrationTest, MockBMCIntegrationTest {
    static BTPAddress btpAddress = new BTPAddress(
            BTPAddress.PROTOCOL_BTP,
            Faker.btpLink().net(),
            Keys.toChecksumAddress(mockBMC.getContractAddress()));
    static BTPAddress prevBtpAddress = Faker.btpLink();
    static String prev = prevBtpAddress.toString();
    static BigInteger seq = BigInteger.ONE;
    static byte[] msg = EVMIntegrationTest.Faker.bytes(32);
    static String to = prevBtpAddress.net();
    static String svc = Faker.btpService();
    static BigInteger sn = BigInteger.ONE;
    static long errCode = 1;
    static String errMsg = "err" + svc;
    static BigInteger forward = BigInteger.ONE;
    static BigInteger backward = BigInteger.TWO;

    @Test
    void btpAddress() throws Exception {
        mockBMC.setNetworkAddress(btpAddress.net()).send();
        assertEquals(btpAddress.net(), mockBMC.getNetworkAddress().send());
        assertEquals(btpAddress.toString(), mockBMC.getBtpAddress().send());
    }

    @Test
    void handleRelayMessageShouldMakeEventLog() throws Exception {
        byte[][] msgs = new byte[][]{msg};
        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setBtpMessages(msgs);

        Consumer<TransactionReceipt> checker = MockBMCIntegrationTest.handleRelayMessageEvent(
                (el) -> assertArrayEquals(msgs, el._ret.toArray()));
        checker.accept(mockBMC.handleRelayMessage(
                MockBMVIntegrationTest.mockBMV.getContractAddress(),
                prev, seq, relayMessage.toBytes()).send());
    }

    @Test
    void sendMessageShouldMakeEventLog() throws Exception {
        Consumer<TransactionReceipt> checker = MockBMCIntegrationTest.sendMessageEvent(
                (el) -> {
                    try {
                        assertEquals(mockBMC.getNetworkSn().send(), el._nsn);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    assertEquals(to, el._to);
                    assertEquals(svc, el._svc);
                    assertEquals(sn, el._sn);
                    assertArrayEquals(msg, el._msg);
                });
        checker.accept(mockBMC.sendMessage(
                to, svc, sn, msg,
                mockBMC.getFee(to, true).send()).send());
    }

    @Test
    void sendMessageWithFee() throws Exception {
        BigInteger forward = BigInteger.ONE;
        BigInteger backward = BigInteger.TWO;
        mockBMC.setFee(forward, backward);
        assertEquals(forward, mockBMC.getFee(to, false).send());
        assertEquals(forward.add(backward), mockBMC.getFee(to, true).send());

        Consumer<TransactionReceipt> checker = MockBMCIntegrationTest.sendMessageEvent(
                (el) -> {
                    try {
                        assertEquals(mockBMC.getNetworkSn().send(), el._nsn);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    assertEquals(to, el._to);
                    assertEquals(svc, el._svc);
                    assertEquals(sn, el._sn);
                    assertArrayEquals(msg, el._msg);
                }
        );
        checker.accept(
                mockBMC.sendMessage(
                        to, svc, sn, msg,
                        mockBMC.getFee(to, true).send()).send()
        );

        checker = MockBMCIntegrationTest.sendMessageEvent(
                (el) -> {
                    try {
                        assertEquals(mockBMC.getNetworkSn().send(), el._nsn);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    assertEquals(to, el._to);
                    assertEquals(svc, el._svc);
                    assertEquals(BigInteger.ZERO, el._sn);
                    assertArrayEquals(msg, el._msg);
                }
        );
        checker.accept(
                mockBMC.sendMessage(
                        to, svc, BigInteger.ZERO, msg,
                        mockBMC.getFee(to, false).send()).send()
        );
    }

    @SuppressWarnings("ThrowableNotThrown")
    @Test
    void sendMessageShouldRevertIfNotEnoughFee() throws Exception {
        mockBMC.setFee(forward, backward).send();
        assertEquals(forward, mockBMC.getFee(to, false).send());
        assertEquals(forward.add(backward), mockBMC.getFee(to, true).send());

        AssertBTPException.assertBTPException(new BTPException.BMC(0, "not enough fee"), () ->
                mockBMC.sendMessage(
                        to, svc, sn, msg,
                        BigInteger.ZERO).send());

        AssertBTPException.assertBTPException(new BTPException.BMC(0, "not enough fee"), () ->
                mockBMC.sendMessage(
                        to, svc, BigInteger.ZERO, msg,
                        BigInteger.ZERO).send());
    }

    @Test
    void sendMessageShouldMakeEventLogIfResponse() throws Exception {
        mockBMC.addResponse(to, svc, sn);
        assertTrue(mockBMC.hasResponse(to, svc, sn).send());

        Consumer<TransactionReceipt> checker = MockBMCIntegrationTest.sendMessageEvent(
                (el) -> {
                    try {
                        assertEquals(mockBMC.getNetworkSn().send(), el._nsn);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    assertEquals(to, el._to);
                    assertEquals(svc, el._svc);
                    assertEquals(BigInteger.ZERO, el._sn);
                    assertArrayEquals(msg, el._msg);
                }
        );
        checker.accept(
                mockBMC.sendMessage(
                        to, svc, sn.negate(), msg,
                        BigInteger.ZERO).send()
        );
        assertFalse(mockBMC.hasResponse(to, svc, sn).send());
    }

    @SuppressWarnings("ThrowableNotThrown")
    @Test
    void sendMessageShouldRevertIfNotExistsResponse() {
        AssertBTPException.assertBTPException(new BTPException.BMC(0, "not exists response"), () ->
                mockBMC.sendMessage(
                        to, svc, sn.negate(), msg,
                        BigInteger.ZERO).send());
    }

    @Test
    void handleBTPMessage() throws Exception {
        Consumer<TransactionReceipt> checker = MockBSHIntegrationTest.handleBTPMessageEvent(
                (el) -> {
                    assertEquals(to, el._from);
                    assertEquals(svc, el._svc);
                    assertEquals(sn, el._sn);
                    assertArrayEquals(msg, el._msg);
                });
        checker.accept(mockBMC.handleBTPMessage(
                MockBSHIntegrationTest.mockBSH.getContractAddress(),
                to, svc, sn, msg).send());
    }

    @Test
    void handleBTPError() throws Exception {
        Consumer<TransactionReceipt> checker = MockBSHIntegrationTest.handleBTPErrorEvent(
                (el) -> {
                    assertEquals(prev, el._src);
                    assertEquals(svc, el._svc);
                    assertEquals(sn, el._sn);
                    assertEquals(errCode, el._code.longValue());
                    assertEquals(errMsg, el._msg);
                }
        );
        checker.accept(mockBMC.handleBTPError(
                MockBSHIntegrationTest.mockBSH.getContractAddress(),
                prev, svc, sn, BigInteger.valueOf(errCode), errMsg).send());
    }

}

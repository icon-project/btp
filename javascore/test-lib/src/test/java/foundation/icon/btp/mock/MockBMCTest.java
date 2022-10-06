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
import foundation.icon.jsonrpc.Address;
import foundation.icon.score.test.AssertRevertedException;
import foundation.icon.score.test.ScoreIntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MockBMCTest implements BTPIntegrationTest, MockBMCIntegrationTest {
    static BTPAddress btpAddress = new BTPAddress(
            BTPAddress.PROTOCOL_BTP,
            BTPIntegrationTest.Faker.btpLink().net(),
            mockBMC._address().toString());
    static BTPAddress prevBtpAddress = BTPIntegrationTest.Faker.btpLink();
    static String prev = prevBtpAddress.toString();
    static BigInteger seq = BigInteger.ONE;
    static byte[] msg = ScoreIntegrationTest.Faker.bytes(32);
    static String to = prevBtpAddress.net();
    static String svc = BTPIntegrationTest.Faker.btpService();
    static BigInteger sn = BigInteger.ONE;
    static long errCode = 1;
    static String errMsg = "err" + svc;
    static BigInteger forward = BigInteger.ONE;
    static BigInteger backward = BigInteger.TWO;

    @Test
    void btpAddress() {
        mockBMC.setNet(btpAddress.net());
        assertEquals(btpAddress.net(), mockBMC.getNet());
        assertEquals(btpAddress.toString(), mockBMC.getBtpAddress());
    }

    @Test
    void handleRelayMessageShouldMakeEventLog() {
        byte[][] msgs = new byte[][]{msg};
        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setBtpMessages(msgs);

        mockBMC.handleRelayMessage(
                MockBMCIntegrationTest.handleRelayMessageEvent(
                        (el) -> assertArrayEquals(msgs, el.getRet())),
                MockBMVIntegrationTest.mockBMV._address(),
                prev, seq, relayMessage.toBytes());
    }

    @Test
    void sendMessageShouldMakeEventLog() {
        mockBMC.sendMessage(
                MockBMCIntegrationTest.sendMessageEvent(
                        (el) -> {
                            assertEquals(mockBMC.getNsn(), el.getNsn());
                            assertEquals(to, el.getTo());
                            assertEquals(svc, el.getSvc());
                            assertEquals(sn, el.getSn());
                            assertArrayEquals(msg, el.getMsg());
                        }
                ),
                mockBMC.getFee(to, true),
                to, svc, sn, msg);
    }

    @Test
    void sendMessageWithFee() {
        BigInteger forward = BigInteger.ONE;
        BigInteger backward = BigInteger.TWO;
        mockBMC.setFee(forward, backward);
        assertEquals(forward, mockBMC.getFee(to, false));
        assertEquals(forward.add(backward), mockBMC.getFee(to, true));

        mockBMC.sendMessage(
                MockBMCIntegrationTest.sendMessageEvent(
                        (el) -> {
                            assertEquals(mockBMC.getNsn(), el.getNsn());
                            assertEquals(to, el.getTo());
                            assertEquals(svc, el.getSvc());
                            assertEquals(sn, el.getSn());
                            assertArrayEquals(msg, el.getMsg());
                        }
                ),
                mockBMC.getFee(to, true),
                to, svc, sn, msg);

        mockBMC.sendMessage(
                MockBMCIntegrationTest.sendMessageEvent(
                        (el) -> {
                            assertEquals(mockBMC.getNsn(), el.getNsn());
                            assertEquals(to, el.getTo());
                            assertEquals(svc, el.getSvc());
                            assertEquals(BigInteger.ZERO, el.getSn());
                            assertArrayEquals(msg, el.getMsg());
                        }
                ),
                mockBMC.getFee(to, false),
                to, svc, BigInteger.ZERO, msg);
    }

    @SuppressWarnings("ThrowableNotThrown")
    @Test
    void sendMessageShouldRevertIfNotEnoughFee() {
        mockBMC.setFee(forward, backward);
        assertEquals(forward, mockBMC.getFee(to, false));
        assertEquals(forward.add(backward), mockBMC.getFee(to, true));

        AssertBTPException.assertBTPException(new BTPException.BMC(0, "not enough fee"), () ->
                mockBMC.sendMessage(
                        BigInteger.ZERO,
                        to, svc, sn, msg));

        AssertBTPException.assertBTPException(new BTPException.BMC(0, "not enough fee"), () ->
                mockBMC.sendMessage(
                        BigInteger.ZERO,
                        to, svc, BigInteger.ZERO, msg));
    }

    @Test
    void sendMessageShouldMakeEventLogIfResponse() {
        mockBMC.addResponse(to, svc, sn);
        assertTrue(mockBMC.hasResponse(to, svc, sn));

        mockBMC.sendMessage(
                MockBMCIntegrationTest.sendMessageEvent(
                        (el) -> {
                            assertEquals(mockBMC.getNsn(), el.getNsn());
                            assertEquals(to, el.getTo());
                            assertEquals(svc, el.getSvc());
                            assertEquals(BigInteger.ZERO, el.getSn());
                            assertArrayEquals(msg, el.getMsg());
                        }
                ),
                BigInteger.ZERO,
                to, svc, sn.negate(), msg);

        assertFalse(mockBMC.hasResponse(to, svc, sn));
    }

    @SuppressWarnings("ThrowableNotThrown")
    @Test
    void sendMessageShouldRevertIfNotExistsResponse() {
        AssertBTPException.assertBTPException(new BTPException.BMC(0, "not exists response"), () ->
                mockBMC.sendMessage(
                        BigInteger.ZERO,
                        to, svc, sn.negate(), msg));
    }

    @Test
    void handleBTPMessage() {
        mockBMC.handleBTPMessage(
                MockBSHIntegrationTest.handleBTPMessageEvent(
                        (el) -> {
                            assertEquals(to, el.getFrom());
                            assertEquals(svc, el.getSvc());
                            assertEquals(sn, el.getSn());
                            assertArrayEquals(msg, el.getMsg());
                        }
                ),
                MockBSHIntegrationTest.mockBSH._address(),
                to, svc, sn, msg);
    }

    @Test
    void handleBTPError() {
        mockBMC.handleBTPError(
                MockBSHIntegrationTest.handleBTPErrorEvent(
                        (el) -> {
                            assertEquals(prev, el.getSrc());
                            assertEquals(svc, el.getSvc());
                            assertEquals(sn, el.getSn());
                            assertEquals(errCode, el.getCode());
                            assertEquals(errMsg, el.getMsg());
                        }
                ),
                MockBSHIntegrationTest.mockBSH._address(),
                prev, svc, sn, errCode, errMsg);
    }

}

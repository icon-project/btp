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
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.MockBMCIntegrationTest;
import foundation.icon.btp.test.MockBMVIntegrationTest;
import foundation.icon.btp.test.MockBSHIntegrationTest;
import foundation.icon.jsonrpc.Address;
import foundation.icon.score.test.ScoreIntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MockBMCTest implements BTPIntegrationTest, MockBMCIntegrationTest {
    static BTPAddress btpAddress = new BTPAddress(
            BTPAddress.PROTOCOL_BTP,
            BTPIntegrationTest.Faker.btpLink().net(),
            mockBMCClient._address().toString());
    static BTPAddress prevBtpAddress = BTPIntegrationTest.Faker.btpLink();
    static String prev = prevBtpAddress.toString();
    static BigInteger seq = BigInteger.ONE;
    static byte[] msg = ScoreIntegrationTest.Faker.bytes(32);
    static String to = prevBtpAddress.net();
    static String svc = BTPIntegrationTest.Faker.btpService();
    static BigInteger sn = BigInteger.ONE;
    static long errCode = 1;
    static String errMsg = "err"+svc;
    static String fa = ScoreIntegrationTest.Faker.address(Address.Type.CONTRACT).toString();

    @Test
    void btpAddress() {
        mockBMC.setNet(btpAddress.net());
        assertEquals(btpAddress.net(), mockBMC.getNet());
        assertEquals(btpAddress.toString(), mockBMC.getBtpAddress());
    }

    @Test
    void intercallBMVhandleRelayMessageShouldMakeEventLog() {
        byte[][] msgs = new byte[][]{msg};
        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setBtpMessages(msgs);

        ((MockBMCScoreClient)mockBMC).intercallHandleRelayMessage(
                MockBMCIntegrationTest.handleRelayMessageEventLogChecker((ret) -> {
                    assertArrayEquals(msgs, ret);
                }),
                MockBMVIntegrationTest.mockBMVClient._address(),
                prev, seq, relayMessage.toBase64String());
    }

    @Test
    void sendMessageShouldMakeEventLog() {
        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setBtpMessages(new byte[][]{msg});

        ((MockBMCScoreClient)mockBMC).sendMessage(
                MockBMCIntegrationTest.sendMessageEventLogChecker(
                        (el) -> {
                            assertEquals(to, el.getTo());
                            assertEquals(svc, el.getSvc());
                            assertEquals(sn, el.getSn());
                            assertArrayEquals(msg, el.getMsg());
                        }),
                to, svc, sn, msg);
    }

    @Test
    void handleBTPMessage() {
        ((MockBMCScoreClient)mockBMC).intercallHandleBTPMessage(
                MockBSHIntegrationTest.handleBTPMessageEventLogChecker(
                        (el) -> {
                            assertEquals(to, el.getFrom());
                            assertEquals(svc, el.getSvc());
                            assertEquals(sn, el.getSn());
                            assertArrayEquals(msg, el.getMsg());
                        }),
                MockBSHIntegrationTest.mockBSHClient._address(),
                to, svc, sn, msg);
    }

    @Test
    void handleBTPError() {
        ((MockBMCScoreClient)mockBMC).intercallHandleBTPError(
                MockBSHIntegrationTest.handleBTPErrorEventLogChecker(
                        (el) -> {
                            assertEquals(prev, el.getSrc());
                            assertEquals(svc, el.getSvc());
                            assertEquals(sn, el.getSn());
                            assertEquals(errCode, el.getCode());
                            assertEquals(errMsg, el.getMsg());
                        }),
                MockBSHIntegrationTest.mockBSHClient._address(),
                prev, svc, sn, errCode, errMsg);
    }

    @Test
    void handleFeeGathering() {
        ((MockBMCScoreClient)mockBMC).intercallHandleFeeGathering(
                MockBSHIntegrationTest.handleFeeGatheringEventLogChecker(
                        (el) -> {
                            assertEquals(fa, el.getFa());
                            assertEquals(svc, el.getSvc());
                        }),
                MockBSHIntegrationTest.mockBSHClient._address(),
                fa, svc);
    }
}
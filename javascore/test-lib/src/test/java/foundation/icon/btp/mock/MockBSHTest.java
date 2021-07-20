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
import foundation.icon.btp.test.*;
import foundation.icon.jsonrpc.Address;
import foundation.icon.score.test.ScoreIntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MockBSHTest implements BTPIntegrationTest, MockBSHIntegrationTest {
    static BTPAddress prevBtpAddress = BTPIntegrationTest.Faker.btpLink();
    static String prev = prevBtpAddress.toString();
    static byte[] msg = ScoreIntegrationTest.Faker.bytes(32);
    static String to = prevBtpAddress.net();
    static String svc = BTPIntegrationTest.Faker.btpService();
    static BigInteger sn = BigInteger.ONE;
    static long errCode = 1;
    static String errMsg = "err" + svc;
    static String fa = ScoreIntegrationTest.Faker.address(Address.Type.CONTRACT).toString();

    @Test
    void intercallSendMessage() {
        ((MockBSHScoreClient) mockBSH).intercallSendMessage(
                MockBMCIntegrationTest.eventLogChecker(SendMessageEventLog::eventLogs, (el) -> {
                    assertEquals(to, el.getTo());
                    assertEquals(svc, el.getSvc());
                    assertEquals(sn, el.getSn());
                    assertArrayEquals(msg, el.getMsg());
                }),
                MockBMCIntegrationTest.mockBMCClient._address(),
                to, svc, sn, msg);
    }

    @Test
    void handleBTPMessageShouldMakeEventLog() {
        ((MockBSHScoreClient) mockBSH).handleBTPMessage(
                MockBSHIntegrationTest.eventLogChecker(HandleBTPMessageEventLog::eventLogs, (el) -> {
                    assertEquals(to, el.getFrom());
                    assertEquals(svc, el.getSvc());
                    assertEquals(sn, el.getSn());
                    assertArrayEquals(msg, el.getMsg());
                }),
                to, svc, sn, msg);
    }

    @Test
    void handleBTPErrorShouldMakeEventLog() {
        ((MockBSHScoreClient) mockBSH).handleBTPError(
                MockBSHIntegrationTest.eventLogChecker(HandleBTPErrorEventLog::eventLogs, (el) -> {
                    assertEquals(prev, el.getSrc());
                    assertEquals(svc, el.getSvc());
                    assertEquals(sn, el.getSn());
                    assertEquals(errCode, el.getCode());
                    assertEquals(errMsg, el.getMsg());
                }),
                prev, svc, sn, errCode, errMsg);
    }

    @Test
    void handleFeeGatheringShouldMakeEventLog() {
        ((MockBSHScoreClient) mockBSH).handleFeeGathering(
                MockBSHIntegrationTest.eventLogChecker(HandleFeeGatheringEventLog::eventLogs, (el) -> {
                    assertEquals(fa, el.getFa());
                    assertEquals(svc, el.getSvc());
                }),
                fa, svc);
    }
}
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
    void sendMessageShouldMakeEventLogs() {
        mockBSH.sendMessage(
                MockBMCIntegrationTest.sendMessageEvent(
                        (el) -> {
                            assertEquals(to, el.get_to());
                            assertEquals(svc, el.get_svc());
                            assertEquals(sn, el.get_sn());
                            assertArrayEquals(msg, el.get_msg());
                        }
                ),
                MockBMCIntegrationTest.mockBMC._address(),
                to, svc, sn, msg);
    }

    @Test
    void handleBTPMessageShouldMakeEventLog() {
        mockBSH.handleBTPMessage(
                MockBSHIntegrationTest.handleBTPMessageEvent(
                        (el) -> {
                            assertEquals(to, el.get_from());
                            assertEquals(svc, el.get_svc());
                            assertEquals(sn, el.get_sn());
                            assertArrayEquals(msg, el.get_msg());
                        }
                ),
                to, svc, sn, msg);
    }

    @Test
    void handleBTPErrorShouldMakeEventLog() {
        mockBSH.handleBTPError(
                MockBSHIntegrationTest.handleBTPErrorEvent(
                        (el) -> {
                            assertEquals(prev, el.get_src());
                            assertEquals(svc, el.get_svc());
                            assertEquals(sn, el.get_sn());
                            assertEquals(errCode, el.get_code());
                            assertEquals(errMsg, el.get_msg());
                        }
                ),
                prev, svc, sn, errCode, errMsg);
    }
}

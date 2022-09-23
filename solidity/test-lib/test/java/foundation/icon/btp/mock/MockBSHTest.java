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
import foundation.icon.btp.test.EVMIntegrationTest;
import foundation.icon.btp.test.MockBMCIntegrationTest;
import foundation.icon.btp.test.MockBSHIntegrationTest;
import org.junit.jupiter.api.Test;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MockBSHTest implements BTPIntegrationTest, MockBSHIntegrationTest {
    static BTPAddress prevBtpAddress = Faker.btpLink();
    static String prev = prevBtpAddress.toString();
    static byte[] msg = EVMIntegrationTest.Faker.bytes(32);
    static String to = prevBtpAddress.net();
    static String svc = Faker.btpService();
    static BigInteger sn = BigInteger.ONE;
    static long errCode = 1;
    static String errMsg = "err" + svc;

    @Test
    void sendMessage() throws Exception {
        Consumer<TransactionReceipt> checker = MockBMCIntegrationTest.sendMessageEvent(
                (el) -> {
                    assertEquals(to, el._to);
                    assertEquals(svc, el._svc);
                    assertEquals(sn, el._sn);
                    assertArrayEquals(msg, el._msg);
                });
        checker.accept(mockBSH.sendMessage(
                MockBMCIntegrationTest.mockBMC.getContractAddress(),
                to, svc, sn, msg).send());
    }

    @Test
    void handleBTPMessageShouldMakeEventLog() throws Exception {
        Consumer<TransactionReceipt> checker = EVMIntegrationTest.eventLogChecker(
                mockBSH.getContractAddress(),
                MockBSH::getHandleBTPMessageEvents, (el) -> {
                    assertEquals(to, el._from);
                    assertEquals(svc, el._svc);
                    assertEquals(sn, el._sn);
                    assertArrayEquals(msg, el._msg);
                });
        checker.accept(mockBSH.handleBTPMessage(
                to, svc, sn, msg).send());
    }

    @Test
    void handleBTPErrorShouldMakeEventLog() throws Exception {
        Consumer<TransactionReceipt> checker = EVMIntegrationTest.eventLogChecker(
                mockBSH.getContractAddress(),
                MockBSH::getHandleBTPErrorEvents, (el) -> {
                    assertEquals(prev, el._src);
                    assertEquals(svc, el._svc);
                    assertEquals(sn, el._sn);
                    assertEquals(errCode, el._code.longValue());
                    assertEquals(errMsg, el._msg);
                });
        checker.accept(mockBSH.handleBTPError(
                prev, svc, sn, BigInteger.valueOf(errCode), errMsg).send());
    }
}

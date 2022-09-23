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
import org.junit.jupiter.api.Test;
import org.web3j.crypto.Keys;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    void btpAddress() throws Exception {
        mockBMC.setNet(btpAddress.net()).send();
        assertEquals(btpAddress.net(), mockBMC.getNet().send());
        assertEquals(btpAddress.toString(), MockBMCIntegrationTest.btpAddress());
    }

    @Test
    void handleRelayMessageShouldMakeEventLog() throws Exception {
        byte[][] msgs = new byte[][]{msg};
        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setBtpMessages(msgs);

        Consumer<TransactionReceipt> checker = EVMIntegrationTest.eventLogChecker(
                mockBMC.getContractAddress(),
                MockBMC::getHandleRelayMessageEvents, (el) -> {
                    assertArrayEquals(msgs, el._ret.toArray(new byte[0][]));
                });
        checker.accept(mockBMC.handleRelayMessage(
                MockBMVIntegrationTest.mockBMV.getContractAddress(),
                prev, seq, relayMessage.toBytes()).send());
    }

    @Test
    void sendMessageShouldMakeEventLog() throws Exception {
        Consumer<TransactionReceipt> checker = EVMIntegrationTest.eventLogChecker(
                mockBMC.getContractAddress(),
                MockBMC::getMessageEvents, (el) -> {
                    assertEquals(to, el._to);
                    assertEquals(svc, el._svc);
                    assertEquals(sn, el._sn);
                    assertArrayEquals(msg, el._msg);
                });
        checker.accept(mockBMC.sendMessage(
                to, svc, sn, msg).send());
    }

    @Test
    void handleBTPMessage() throws Exception {
        Consumer<TransactionReceipt> checker =
                MockBMCIntegrationTest.shouldSuccessHandleBTPMessage();
        checker = checker.andThen(MockBSHIntegrationTest.handleBTPMessageEvent(
                (el) -> {
                    assertEquals(to, el._from);
                    assertEquals(svc, el._svc);
                    assertEquals(sn, el._sn);
                    assertArrayEquals(msg, el._msg);
                }));
        checker.accept(mockBMC.handleBTPMessage(
                MockBSHIntegrationTest.mockBSH.getContractAddress(),
                to, svc, sn, msg).send());
    }

    @Test
    void handleBTPError() throws Exception {
        Consumer<TransactionReceipt> checker =
                MockBMCIntegrationTest.shouldSuccessHandleBTPError();
        checker = checker.andThen(MockBSHIntegrationTest.handleBTPErrorEvent(
                (el) -> {
                    assertEquals(prev, el._src);
                    assertEquals(svc, el._svc);
                    assertEquals(sn, el._sn);
                    assertEquals(errCode, el._code.longValue());
                    assertEquals(errMsg, el._msg);
                }));
        checker.accept(mockBMC.handleBTPError(
                MockBSHIntegrationTest.mockBSH.getContractAddress(),
                prev, svc, sn, BigInteger.valueOf(errCode), errMsg).send());
    }

}

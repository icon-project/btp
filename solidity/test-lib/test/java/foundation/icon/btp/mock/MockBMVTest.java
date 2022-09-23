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

import com.github.javafaker.Number;
import foundation.icon.btp.lib.BMVStatus;
import foundation.icon.btp.test.AssertTransactionException;
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.MockBMVIntegrationTest;
import org.junit.jupiter.api.Test;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MockBMVTest implements BTPIntegrationTest, MockBMVIntegrationTest {

    static String bmc = Faker.btpLink().toString();
    static String prev = Faker.btpLink().toString();
    static BigInteger seq = BigInteger.ONE;

    @Test
    void handleRelayMessageShouldMakeEventLog() throws Exception {
        List<byte[]> btpMessages = new ArrayList<>();
        btpMessages.add(bmc.getBytes());
        btpMessages.add(prev.getBytes());
        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setBtpMessages(btpMessages.toArray(new byte[btpMessages.size()][]));

        Consumer<TransactionReceipt> checker = MockBMVIntegrationTest.handleRelayMessageEvent(
                (el) -> {
                    assertArrayEquals(relayMessage.getBtpMessages(), MockRelayMessage.toBytesArray(el._ret));
                });
        checker.accept(mockBMV.handleRelayMessage(
                bmc, prev, seq, relayMessage.toBytes()).send());
    }

    @Test
    void handleRelayMessageShouldRevert() {
        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setRevertCode(1);
        relayMessage.setRevertMessage("handleRelayMessageShouldRevert");
        //noinspection ThrowableNotThrown
        AssertTransactionException.assertRevertReason(relayMessage.getRevertMessage(), () ->
                mockBMV.handleRelayMessage(
                        bmc, prev, seq, relayMessage.toBytes()).send());
    }

    @Test
    void handleRelayMessageShouldUpdateProperties() throws Exception {
        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setHeight(3L);
        relayMessage.setLastHeight(2L);
        relayMessage.setOffset(1L);

        Consumer<TransactionReceipt> checker = MockBMVIntegrationTest.handleRelayMessageEvent(
                (el) -> {
                    assertArrayEquals(new byte[][]{}, MockRelayMessage.toBytesArray(el._ret));
                });
        checker.accept(mockBMV.handleRelayMessage(
                bmc, prev, seq, relayMessage.toBytes()).send());


        BMVStatus status = MockBMVIntegrationTest.getStatus();
        assertEquals(relayMessage.getHeight(), status.getHeight());
        MockBMVStatusExtra extra = MockBMVStatusExtra.fromBytes(status.getExtra());
        assertEquals(relayMessage.getOffset(), extra.getOffset());
        assertEquals(relayMessage.getLastHeight(), extra.getLastHeight());
    }

    static MockBMVStatusExtra getStatusExtra() {
        return getStatusExtra(MockBMVIntegrationTest.getStatus());
    }

    static MockBMVStatusExtra getStatusExtra(BMVStatus status) {
        return MockBMVStatusExtra.fromBytes(status.getExtra());
    }

    @Test
    void updateProperties() throws Exception {
        Number number = com.github.javafaker.Faker.instance().number();
        long height = number.numberBetween(0, Long.MAX_VALUE);
        mockBMV.setHeight(BigInteger.valueOf(height)).send();
        assertEquals(height, MockBMVIntegrationTest.getStatus().getHeight());

        long lastHeight = number.numberBetween(0, Long.MAX_VALUE);
        mockBMV.setLastHeight(BigInteger.valueOf(lastHeight));
        assertEquals(lastHeight, getStatusExtra().getLastHeight());

        long offset = number.numberBetween(0, Long.MAX_VALUE);
        mockBMV.setOffset(BigInteger.valueOf(offset));
        assertEquals(offset, getStatusExtra().getOffset());
    }

}

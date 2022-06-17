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
import foundation.icon.btp.lib.BTPException;
import foundation.icon.btp.test.AssertBTPException;
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.HandleRelayMessageEventLog;
import foundation.icon.btp.test.MockBMVIntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MockBMVTest implements BTPIntegrationTest, MockBMVIntegrationTest {

    static String bmc = BTPIntegrationTest.Faker.btpLink().toString();
    static String prev = BTPIntegrationTest.Faker.btpLink().toString();
    static BigInteger seq = BigInteger.ONE;

    @Test
    void handleRelayMessageShouldMakeEventLog() {
        List<byte[]> btpMessages = new ArrayList<>();
        btpMessages.add(bmc.getBytes());
        btpMessages.add(prev.getBytes());
        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setBtpMessages(btpMessages.toArray(new byte[btpMessages.size()][]));

        ((MockBMVScoreClient) mockBMV).handleRelayMessage(
                MockBMVIntegrationTest.eventLogChecker(HandleRelayMessageEventLog::eventLogs, (el) -> {
                    assertArrayEquals(relayMessage.getBtpMessages(), el.getRet());
                }),
                bmc, prev, seq, relayMessage.toBytes());
    }

    @Test
    void handleRelayMessageShouldRevert() {
        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setRevertCode(1);
        relayMessage.setRevertMessage("handleRelayMessageShouldRevert");
        //noinspection ThrowableNotThrown
        AssertBTPException.assertBTPException(
                new BTPException.BMV(relayMessage.getRevertCode(), relayMessage.getRevertMessage()),
                () -> ((MockBMVScoreClient) mockBMV).handleRelayMessage(
                        (txr)->{},
                        bmc, prev, seq, relayMessage.toBytes())
        );
    }

    @Test
    void handleRelayMessageShouldUpdateProperties() {
        MockRelayMessage relayMessage = new MockRelayMessage();
        relayMessage.setHeight(3L);
        relayMessage.setLastHeight(2L);
        relayMessage.setOffset(1L);

        ((MockBMVScoreClient) mockBMV).handleRelayMessage(
                MockBMVIntegrationTest.eventLogChecker(HandleRelayMessageEventLog::eventLogs, (el) -> {
                    assertArrayEquals(new byte[][]{}, el.getRet());
                }),
                bmc, prev, seq, relayMessage.toBytes());
        BMVStatus status = mockBMV.getStatus();
        assertEquals(relayMessage.getHeight(), status.getHeight());
        assertEquals(relayMessage.getLastHeight(), status.getLast_height());
        assertEquals(relayMessage.getOffset(), status.getOffset());
    }

    @Test
    void updateProperties() {
        Number number = com.github.javafaker.Faker.instance().number();
        long height = number.numberBetween(0, Long.MAX_VALUE);
        mockBMV.setHeight(height);
        assertEquals(height, mockBMV.getStatus().getHeight());

        long lastHeight = number.numberBetween(0, Long.MAX_VALUE);
        mockBMV.setLast_height(lastHeight);
        assertEquals(lastHeight, mockBMV.getStatus().getLast_height());

        long offset = number.numberBetween(0, Long.MAX_VALUE);
        mockBMV.setOffset(offset);
        assertEquals(offset, mockBMV.getStatus().getOffset());
    }

}

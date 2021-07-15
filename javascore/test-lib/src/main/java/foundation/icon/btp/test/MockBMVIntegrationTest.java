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

package foundation.icon.btp.test;

import foundation.icon.btp.mock.MockBMV;
import foundation.icon.btp.mock.MockBMVScoreClient;
import foundation.icon.btp.mock.MockRelayMessage;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.client.DefaultScoreClient;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public interface MockBMVIntegrationTest {

    DefaultScoreClient mockBMVClient = DefaultScoreClient.of("bmv-mock.", System.getProperties());
    MockBMV mockBMV = new MockBMVScoreClient(mockBMVClient);

    static Consumer<TransactionResult> handleRelayMessageEventLogChecker(
            MockRelayMessage relayMessage) {
        return (txr) -> {
            List<HandleRelayMessageEventLog> eventLogs =
                    HandleRelayMessageEventLog.eventLogs(txr, mockBMVClient._address(), null);
            assertEquals(1, eventLogs.size());
            byte[][] ret = relayMessage.getBtpMessages();
            assertArrayEquals(
                    ret == null ? new byte[][]{} : ret,
                    eventLogs.get(0).getRet());
        };
    }
}

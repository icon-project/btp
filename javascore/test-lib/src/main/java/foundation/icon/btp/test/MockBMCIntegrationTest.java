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

import foundation.icon.btp.mock.MockBMC;
import foundation.icon.btp.mock.MockBMCScoreClient;
import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.client.DefaultScoreClient;
import foundation.icon.score.test.ScoreIntegrationTest;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public interface MockBMCIntegrationTest {

    DefaultScoreClient mockBMCClient = DefaultScoreClient.of("bmc-mock.", System.getProperties());
    MockBMC mockBMC = new MockBMCScoreClient(mockBMCClient);

    static Consumer<TransactionResult> handleRelayMessageEventLogChecker(
            Consumer<byte[][]> consumer) {
        return (txr) -> {
            List<HandleRelayMessageEventLog> eventLogs =
                    HandleRelayMessageEventLog.eventLogs(txr, mockBMCClient._address(), null);
            assertEquals(1, eventLogs.size());
            if (consumer != null) {
                consumer.accept(eventLogs.get(0).getRet());
            }
        };
    }

    static Consumer<TransactionResult> sendMessageEventLogChecker(
            Consumer<SendMessageEventLog> consumer) {
        return (txr) -> {
            List<SendMessageEventLog> eventLogs =
                    SendMessageEventLog.eventLogs(txr, mockBMCClient._address(), null);
            assertEquals(1, eventLogs.size());
            if (consumer != null) {
                consumer.accept(eventLogs.get(0));
            }
        };
    }

}

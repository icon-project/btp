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

import foundation.icon.btp.mock.MockBSH;
import foundation.icon.btp.mock.MockBSHScoreClient;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.client.DefaultScoreClient;
import foundation.icon.score.test.ScoreIntegrationTest;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

public interface MockBSHIntegrationTest {

    DefaultScoreClient mockBSHClient = DefaultScoreClient.of("bsh-mock.", System.getProperties());
    MockBSH mockBSH = new MockBSHScoreClient(mockBSHClient);

    static Consumer<TransactionResult> handleBTPMessageEventLogChecker(
            Consumer<HandleBTPMessageEventLog> consumer) {
        return (txr) -> {
            List<HandleBTPMessageEventLog> eventLogs =
                    HandleBTPMessageEventLog.eventLogs(txr, mockBSHClient._address(), null);
            assertEquals(1, eventLogs.size());
            if (consumer != null) {
                consumer.accept(eventLogs.get(0));
            }
        };
    }

    static Consumer<TransactionResult> handleBTPErrorEventLogChecker(
            Consumer<HandleBTPErrorEventLog> consumer) {
        return (txr) -> {
            List<HandleBTPErrorEventLog> eventLogs =
                    HandleBTPErrorEventLog.eventLogs(txr, mockBSHClient._address(), null);
            assertEquals(1, eventLogs.size());
            if (consumer != null) {
                consumer.accept(eventLogs.get(0));
            }
        };
    }

    static Consumer<TransactionResult> handleFeeGatheringEventLogChecker(
            Consumer<HandleFeeGatheringEventLog> consumer) {
        return (txr) -> {
            List<HandleFeeGatheringEventLog> eventLogs =
                    HandleFeeGatheringEventLog.eventLogs(txr, mockBSHClient._address(), null);
            assertEquals(1, eventLogs.size());
            if (consumer != null) {
                consumer.accept(eventLogs.get(0));
            }
        };
    }

    static List<HandleFeeGatheringEventLog> handleFeeGatheringEventLogs(
            TransactionResult txr, Predicate<HandleFeeGatheringEventLog> filter) {
        return ScoreIntegrationTest.eventLogs(txr,
                HandleFeeGatheringEventLog.SIGNATURE,
                mockBSHClient._address(),
                HandleFeeGatheringEventLog::new,
                filter);
    }
}

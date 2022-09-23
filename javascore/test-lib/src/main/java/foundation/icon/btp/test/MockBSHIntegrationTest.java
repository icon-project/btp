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

import foundation.icon.btp.mock.MockBSHScoreClient;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.test.ScoreIntegrationTest;

import java.util.function.Consumer;

public interface MockBSHIntegrationTest {
    String SERVICE = "mock";

    MockBSHScoreClient mockBSH = MockBSHScoreClient._of("bsh-mock.", System.getProperties());

    static Consumer<TransactionResult> handleBTPMessageEvent(
            Consumer<HandleBTPMessageEventLog> consumer) {
        return eventLogChecker(
                HandleBTPMessageEventLog::eventLogs,
                consumer);
    }

    static Consumer<TransactionResult> handleBTPErrorEvent(
            Consumer<HandleBTPErrorEventLog> consumer) {
        return eventLogChecker(
                HandleBTPErrorEventLog::eventLogs,
                consumer);
    }

    static <T> Consumer<TransactionResult> eventLogChecker(
            ScoreIntegrationTest.EventLogsSupplier<T> supplier, Consumer<T> consumer) {
        return ScoreIntegrationTest.eventLogChecker(
                mockBSH._address(), supplier, consumer);
    }

}

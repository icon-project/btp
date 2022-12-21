/*
 * Copyright 2022 ICON Foundation
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

package foundation.icon.btp.xcall;

import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.MockBMCIntegrationTest;
import foundation.icon.btp.xcall.sample.DAppProxySampleScoreClient;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.client.DefaultScoreClient;
import foundation.icon.score.test.ScoreIntegrationTest;

import java.util.Map;
import java.util.function.Consumer;

public interface CSIntegrationTest extends BTPIntegrationTest {

    CallServiceScoreClient callSvc = new CallServiceScoreClient(DefaultScoreClient.of(
            System.getProperties(), Map.of(
                    "_bmc", MockBMCIntegrationTest.mockBMC._address())));

    FeeManageScoreClient feeManager = new FeeManageScoreClient(callSvc);

    DAppProxySampleScoreClient sampleClient = new DAppProxySampleScoreClient(
            DefaultScoreClient.of("sample.",
                    System.getProperties(), Map.of(
                            "_callService", callSvc._address())));

    static Consumer<TransactionResult> callMessageEvent(
            Consumer<CallMessageEventLog> consumer) {
        return eventLogChecker(
                CallMessageEventLog::eventLogs,
                consumer);
    }

    static Consumer<TransactionResult> rollbackMessageEvent(
            Consumer<RollbackMessageEventLog> consumer) {
        return eventLogChecker(
                RollbackMessageEventLog::eventLogs,
                consumer);
    }

    static Consumer<TransactionResult> rollbackMessageEventShouldNotExists() {
        return eventLogShouldNotExistsChecker(RollbackMessageEventLog::eventLogs);
    }

    static Consumer<TransactionResult> callRequestClearedEvent(
            Consumer<CallRequestClearedEventLog> consumer) {
        return eventLogChecker(
                CallRequestClearedEventLog::eventLogs,
                consumer);
    }

    static Consumer<TransactionResult> callRequestClearedEventShouldNotExists() {
        return eventLogShouldNotExistsChecker(CallRequestClearedEventLog::eventLogs);
    }

    static <T> Consumer<TransactionResult> eventLogChecker(
            EventLogsSupplier<T> supplier, Consumer<T> consumer) {
        return ScoreIntegrationTest.eventLogChecker(
                callSvc._address(), supplier, consumer);
    }

    static <T> Consumer<TransactionResult> eventLogShouldNotExistsChecker(
            ScoreIntegrationTest.EventLogsSupplier<T> supplier) {
        return ScoreIntegrationTest.eventLogShouldNotExistsChecker(
                callSvc._address(), supplier);
    }

    static Consumer<TransactionResult> messageReceivedEvent(
            Consumer<MessageReceivedEventLog> consumer) {
        return ScoreIntegrationTest.eventLogChecker(
                sampleClient._address(),
                MessageReceivedEventLog::eventLogs,
                consumer);
    }

    static Consumer<TransactionResult> rollbackDataReceivedEvent (
            Consumer<RollbackDataReceivedEventLog> consumer) {
        return ScoreIntegrationTest.eventLogChecker(
                sampleClient._address(),
                RollbackDataReceivedEventLog::eventLogs,
                consumer);
    }
}

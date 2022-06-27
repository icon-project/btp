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

import foundation.icon.btp.lib.BSH;
import foundation.icon.btp.lib.BSHScoreClient;
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.MockBMCIntegrationTest;
import foundation.icon.icx.Wallet;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.client.DefaultScoreClient;
import foundation.icon.score.client.ScoreClient;
import foundation.icon.score.test.ScoreIntegrationTest;

import java.util.Map;
import java.util.function.Consumer;

public interface CSIntegrationTest extends BTPIntegrationTest {

    DefaultScoreClient csClient = DefaultScoreClient.of(
            System.getProperties(), Map.of(
                    "_bmc", MockBMCIntegrationTest.mockBMCClient._address()));
    DefaultScoreClient sampleClient = DefaultScoreClient.of("sample.",
            System.getProperties(), Map.of(
                    "_callService", csClient._address()));

    @ScoreClient
    CallService callSvc = new CallServiceScoreClient(csClient);

    @ScoreClient
    BSH callSvcBSH = new BSHScoreClient(csClient);

    Wallet tester = ScoreIntegrationTest.getOrGenerateWallet("tester.", System.getProperties());
    DefaultScoreClient csClientWithTester = new DefaultScoreClient(
            csClient.endpoint(), csClient._nid(), tester, csClient._address());
    CallService callSvcWithTester = new CallServiceScoreClient(csClientWithTester);

    static <T> Consumer<TransactionResult> eventLogChecker(
            EventLogsSupplier<T> supplier, Consumer<T> consumer) {
        return ScoreIntegrationTest.eventLogChecker(
                csClient._address(), supplier, consumer);
    }

    static <T> Consumer<TransactionResult> notExistsEventLogChecker(
            ScoreIntegrationTest.EventLogsSupplier<T> supplier) {
        return ScoreIntegrationTest.notExistsEventLogChecker(
                csClient._address(), supplier);
    }
}

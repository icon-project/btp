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

package foundation.icon.btp.irc2Tradeable;

import foundation.icon.btp.lib.OwnerManager;
import foundation.icon.btp.lib.OwnerManagerScoreClient;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.client.DefaultScoreClient;
import foundation.icon.score.client.ScoreClient;
import foundation.icon.score.test.ScoreIntegrationTest;
import org.junit.jupiter.api.TestInfo;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public interface IRC2IntegrationTest extends ScoreIntegrationTest {
    @Override
    default void internalBeforeEach(TestInfo testInfo) {}

    @Override
    default void internalAfterEach(TestInfo testInfo) {}

    @Override
    default void clearIfExists(TestInfo testInfo) {}

    DefaultScoreClient irc2Client = DefaultScoreClient.of("irc2.", System.getProperties());
    @ScoreClient
    IRC2Supplier irc2Supplier = new IRC2SupplierScoreClient(irc2Client);
    @ScoreClient
    OwnerManager irc2OwnerManager = new OwnerManagerScoreClient(irc2Client);

    DefaultScoreClient irc2ClientWithTester = new DefaultScoreClient(
            irc2Client.endpoint(), irc2Client._nid(), tester, irc2Client._address());
    IRC2Supplier irc2SupplierWithTester = new IRC2SupplierScoreClient(irc2ClientWithTester);
    OwnerManager irc2OwnerManagerWithTester = new OwnerManagerScoreClient(irc2ClientWithTester);

    static <T> Consumer<TransactionResult> eventLogChecker(
            EventLogsSupplier<T> supplier, Consumer<T> consumer) {
        return ScoreIntegrationTest.eventLogChecker(
                irc2Client._address(), supplier, consumer);
    }

}

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

package foundation.icon.btp.nativecoin.irc31;

import foundation.icon.btp.lib.OwnerManager;
import foundation.icon.btp.lib.OwnerManagerScoreClient;
import foundation.icon.btp.nativecoin.TransferStartEventLog;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.client.DefaultScoreClient;
import foundation.icon.score.client.ScoreClient;
import foundation.icon.score.test.ScoreIntegrationTest;
import org.junit.jupiter.api.TestInfo;

import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public interface IRC31IntegrationTest extends ScoreIntegrationTest {
    @Override
    default void internalBeforeEach(TestInfo testInfo) {}

    @Override
    default void internalAfterEach(TestInfo testInfo) {}

    @Override
    default void clearIfExists(TestInfo testInfo) {}

    DefaultScoreClient irc31Client = DefaultScoreClient.of("irc31.", System.getProperties());
    @ScoreClient
    IRC31Supplier irc31Supplier = new IRC31SupplierScoreClient(irc31Client);
    @ScoreClient
    OwnerManager irc31OwnerManager = new OwnerManagerScoreClient(irc31Client);

    DefaultScoreClient irc31ClientWithTester = new DefaultScoreClient(
            irc31Client.endpoint(), irc31Client._nid(), tester, irc31Client._address());
    IRC31Supplier irc31SupplierWithTester = new IRC31SupplierScoreClient(irc31ClientWithTester);
    OwnerManager irc31OwnerManagerWithTester = new OwnerManagerScoreClient(irc31ClientWithTester);

    static <T> Consumer<TransactionResult> eventLogChecker(
            EventLogsSupplier<T> supplier, Consumer<T> consumer) {
        return ScoreIntegrationTest.eventLogChecker(
                irc31Client._address(), supplier, consumer);
    }

}

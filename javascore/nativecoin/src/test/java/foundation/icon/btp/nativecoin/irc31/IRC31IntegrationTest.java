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
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.test.ScoreIntegrationTest;
import org.junit.jupiter.api.TestInfo;

import java.util.function.Consumer;

public interface IRC31IntegrationTest extends ScoreIntegrationTest {
    @Override
    default void clearIfExists(TestInfo testInfo) {}

    IRC31SupplierScoreClient irc31Supplier = IRC31SupplierScoreClient._of("irc31.", System.getProperties());
    OwnerManager irc31OwnerManager = new OwnerManagerScoreClient(irc31Supplier);

    IRC31SupplierScoreClient irc31SupplierWithTester = new IRC31SupplierScoreClient(
            irc31Supplier.endpoint(), irc31Supplier._nid(), tester, irc31Supplier._address());
    OwnerManager irc31OwnerManagerWithTester = new OwnerManagerScoreClient(irc31SupplierWithTester);

    static <T> Consumer<TransactionResult> eventLogChecker(
            EventLogsSupplier<T> supplier, Consumer<T> consumer) {
        return ScoreIntegrationTest.eventLogChecker(
                irc31Supplier._address(), supplier, consumer);
    }
}

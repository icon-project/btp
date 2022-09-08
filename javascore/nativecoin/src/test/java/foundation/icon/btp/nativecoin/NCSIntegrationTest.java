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

package foundation.icon.btp.nativecoin;

import foundation.icon.btp.lib.BSH;
import foundation.icon.btp.lib.BSHScoreClient;
import foundation.icon.btp.lib.OwnerManager;
import foundation.icon.btp.lib.OwnerManagerScoreClient;
import foundation.icon.btp.nativecoin.irc31.IRC31IntegrationTest;
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.MockBMCIntegrationTest;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.client.DefaultScoreClient;
import foundation.icon.score.test.ScoreIntegrationTest;

import java.util.Map;
import java.util.function.Consumer;

public interface NCSIntegrationTest extends BTPIntegrationTest {

    NCSScoreClient ncs = new NCSScoreClient(DefaultScoreClient.of(
            System.getProperties(), Map.of(
                    "_bmc", MockBMCIntegrationTest.mockBMC._address(),
                    "_irc31", IRC31IntegrationTest.irc31Supplier._address())));
    OwnerManager ncsOwnerManager = new OwnerManagerScoreClient(ncs);
    BSH ncsBSH = new BSHScoreClient(ncs);

    NCSScoreClient ncsWithTester = new NCSScoreClient(ncs.endpoint(), ncs._nid(), tester, ncs._address());
    OwnerManager ncsOwnerManagerWithTester = new OwnerManagerScoreClient(ncsWithTester);

    static <T> Consumer<TransactionResult> eventLogChecker(
            EventLogsSupplier<T> supplier, Consumer<T> consumer) {
        return ScoreIntegrationTest.eventLogChecker(
                ncs._address(), supplier, consumer);
    }

}

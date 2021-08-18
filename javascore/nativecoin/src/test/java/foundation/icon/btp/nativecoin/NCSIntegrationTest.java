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
import foundation.icon.btp.test.SendMessageEventLog;
import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.client.DefaultScoreClient;
import foundation.icon.score.client.ScoreClient;
import foundation.icon.score.test.ScoreIntegrationTest;
import foundation.icon.icx.Wallet;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(value = MethodOrderer.OrderAnnotation.class)
public interface NCSIntegrationTest extends BTPIntegrationTest {

    DefaultScoreClient ncsClient = DefaultScoreClient.of(
            System.getProperties(), Map.of(
                    "_bmc", MockBMCIntegrationTest.mockBMCClient._address(),
                    "_irc31", IRC31IntegrationTest.irc31Client._address()));

    @ScoreClient
    NCS ncs = new NCSScoreClient(ncsClient);

    @ScoreClient
    BSH ncsBSH = new BSHScoreClient(ncsClient);

    @ScoreClient
    OwnerManager ncsOwnerManager = new OwnerManagerScoreClient(ncsClient);

    Wallet tester = ScoreIntegrationTest.getOrGenerateWallet("tester.", System.getProperties());
    DefaultScoreClient ncsClientWithTester = new DefaultScoreClient(
            ncsClient.endpoint(), ncsClient._nid(), tester, ncsClient._address());
    NCS ncsWithTester = new NCSScoreClient(ncsClientWithTester);
    OwnerManager ncsOwnerManagerWithTester = new OwnerManagerScoreClient(ncsClientWithTester);

    static <T> Consumer<TransactionResult> eventLogChecker(
            EventLogsSupplier<T> supplier, Consumer<T> consumer) {
        return ScoreIntegrationTest.eventLogChecker(
                ncsClient._address(), supplier, consumer);
    }

}

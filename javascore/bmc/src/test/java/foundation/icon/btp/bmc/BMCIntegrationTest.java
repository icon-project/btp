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

package foundation.icon.btp.bmc;

import foundation.icon.btp.lib.BMC;
import foundation.icon.btp.lib.BMCScoreClient;
import foundation.icon.btp.lib.OwnerManager;
import foundation.icon.btp.lib.OwnerManagerScoreClient;
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.HandleBTPErrorEventLog;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.client.DefaultScoreClient;
import foundation.icon.score.client.ScoreClient;
import foundation.icon.score.test.ScoreIntegrationTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(value = MethodOrderer.OrderAnnotation.class)
public interface BMCIntegrationTest extends BTPIntegrationTest {

    DefaultScoreClient bmcClient = DefaultScoreClient.of(System.getProperties());
    @ScoreClient
    BMC bmc = new BMCScoreClient(bmcClient);
    @ScoreClient
    ICONSpecific iconSpecific = new ICONSpecificScoreClient(bmcClient);
    @ScoreClient
    OwnerManager ownerManager = new OwnerManagerScoreClient(bmcClient);
    @ScoreClient
    RelayerManager relayerManager = new RelayerManagerScoreClient(bmcClient);

    DefaultScoreClient bmcClientWithTester = new DefaultScoreClient(bmcClient.endpoint(), bmcClient._nid(), tester, bmcClient._address());
    BMC bmcWithTester = new BMCScoreClient(bmcClientWithTester);
    ICONSpecific iconSpecificWithTester = new ICONSpecificScoreClient(bmcClientWithTester);
    OwnerManager ownerManagerWithTester = new OwnerManagerScoreClient(bmcClientWithTester);
    RelayerManager relayerManagerWithTester = new RelayerManagerScoreClient(bmcClientWithTester);

    static Consumer<TransactionResult> messageEventLogChecker(
            Consumer<MessageEventLog> consumer) {
        return (txr) -> {
            List<MessageEventLog> eventLogs =
                    MessageEventLog.eventLogs(txr, bmcClient._address(), null);
            assertEquals(1, eventLogs.size());
            if (consumer != null) {
                consumer.accept(eventLogs.get(0));
            }
        };
    }

    static List<BTPMessage> btpMessages(TransactionResult txr, Predicate<MessageEventLog> filter) {
        return MessageEventLog.eventLogs(txr, bmcClient._address(), filter).stream()
                .map(MessageEventLog::getMsg)
                .collect(Collectors.toList());
    }

    static List<BMCMessage> bmcMessages(TransactionResult txr, Predicate<String> nextPredicate) {
        return btpMessages(txr,
                (el) -> el.getMsg().getSvc().equals(BTPMessageCenter.INTERNAL_SERVICE) &&
                            nextPredicate.test(el.getNext())).stream()
                .map((msg) -> BMCMessage.fromBytes(msg.getPayload()))
                .collect(Collectors.toList());
    }

    static <T> List<T> internalMessages(
            List<BMCMessage> bmcMessages,
            BTPMessageCenter.Internal internal,
            Function<byte[], T> mapFunc) {
        return bmcMessages.stream()
                .filter((bmcMsg) -> bmcMsg.getType().equals(internal.name()))
                .map((bmcMsg) -> mapFunc.apply(bmcMsg.getPayload()))
                .collect(Collectors.toList());
    }
}

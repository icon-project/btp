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

import foundation.icon.btp.lib.BMCScoreClient;
import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.lib.OwnerManagerScoreClient;
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.test.ScoreIntegrationTest;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface BMCIntegrationTest extends BTPIntegrationTest {

    BMCScoreClient bmc = BMCScoreClient._of(System.getProperties());
    ICONSpecificScoreClient iconSpecific = new ICONSpecificScoreClient(bmc);
    OwnerManagerScoreClient ownerManager = new OwnerManagerScoreClient(bmc);

    BTPAddress btpAddress = BTPAddress.parse(bmc.getBtpAddress());

    BMCScoreClient bmcWithTester = new BMCScoreClient(bmc.endpoint(), bmc._nid(), tester, bmc._address());
    ICONSpecificScoreClient iconSpecificWithTester = new ICONSpecificScoreClient(bmcWithTester);
    OwnerManagerScoreClient ownerManagerWithTester = new OwnerManagerScoreClient(bmcWithTester);

    static <T> Consumer<TransactionResult> eventLogChecker(
            ScoreIntegrationTest.EventLogsSupplier<T> supplier, Consumer<T> consumer, Predicate<T> filter) {
        return ScoreIntegrationTest.eventLogChecker(
                bmc._address(), supplier, consumer, filter);
    }

    static <T> Consumer<TransactionResult> eventLogsChecker(
            ScoreIntegrationTest.EventLogsSupplier<T> supplier, Consumer<List<T>> consumer, Predicate<T> filter) {
        return ScoreIntegrationTest.eventLogsChecker(
                bmc._address(), supplier, consumer, filter);
    }

    static Consumer<TransactionResult> messageEvent(
            Consumer<BMCScoreClient.Message> consumer) {
        return messageEvent(consumer, null);
    }

    static Consumer<TransactionResult> messageEvent(
            Consumer<BMCScoreClient.Message> consumer, Predicate<BMCScoreClient.Message> filter) {
        return eventLogChecker(
                BMCScoreClient.Message::eventLogs,
                consumer,
                filter);
    }

    static List<BTPMessage> btpMessages(TransactionResult txr, Predicate<BMCScoreClient.Message> filter) {
        return BMCScoreClient.Message.eventLogs(txr, bmc._address(), filter).stream()
                .map((el) -> BTPMessage.fromBytes(el.get_msg()))
                .collect(Collectors.toList());
    }

    static List<BMCMessage> bmcMessages(TransactionResult txr, Predicate<String> nextPredicate) {
        Predicate<BMCScoreClient.Message> filter = nextPredicate != null ?
                (el) -> nextPredicate.test(el.get_next()) : null;
        return btpMessages(txr, filter).stream()
                .filter((msg) -> msg.getSvc().equals(BTPMessageCenter.INTERNAL_SERVICE))
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

    static Consumer<TransactionResult> claimRewardEvent(
            Consumer<BMCScoreClient.ClaimReward> consumer) {
        return eventLogChecker(
                BMCScoreClient.ClaimReward::eventLogs,
                consumer, null);
    }

    static Consumer<TransactionResult> claimRewardResultEvent(
            Consumer<BMCScoreClient.ClaimRewardResult> consumer) {
        return eventLogChecker(
                BMCScoreClient.ClaimRewardResult::eventLogs,
                consumer, null);
    }

    static Consumer<TransactionResult> messageDroppedEvent(
            Consumer<ICONSpecificScoreClient.MessageDropped> consumer) {
        return eventLogChecker(
                ICONSpecificScoreClient.MessageDropped::eventLogs,
                consumer, null);
    }

    static Consumer<TransactionResult> btpEvent(
            Consumer<List<BMCScoreClient.BTPEvent>> consumer) {
        return eventLogsChecker(
                BMCScoreClient.BTPEvent::eventLogs,
                consumer, null);
    }

}

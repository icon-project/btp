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

import com.fasterxml.jackson.databind.ObjectMapper;
import foundation.icon.btp.lib.BMC;
import foundation.icon.btp.lib.BMCScoreClient;
import foundation.icon.btp.lib.BMCStatus;
import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.lib.OwnerManagerScoreClient;
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.test.ScoreIntegrationTest;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
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

    static Consumer<TransactionResult> messageEvent(
            Consumer<MessageEventLog> consumer) {
        return eventLogChecker(
                MessageEventLog::eventLogs,
                consumer);
    }

    static <T> Consumer<TransactionResult> eventLogChecker(
            ScoreIntegrationTest.EventLogsSupplier<T> supplier, Consumer<T> consumer) {
        return ScoreIntegrationTest.eventLogChecker(
                bmc._address(), supplier, consumer);
    }

    static List<BTPMessage> btpMessages(TransactionResult txr, Predicate<MessageEventLog> filter) {
        return MessageEventLog.eventLogs(txr, bmc._address(), filter).stream()
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

    static Consumer<TransactionResult> claimRewardEvent(
            Consumer<ClaimRewardEventLog> consumer) {
        return eventLogChecker(
                ClaimRewardEventLog::eventLogs,
                consumer);
    }

    static Consumer<TransactionResult> messageDroppedEvent(
            Consumer<MessageDroppedEventLog> consumer) {
        return eventLogChecker(
                MessageDroppedEventLog::eventLogs,
                consumer);
    }

    static Consumer<TransactionResult> btpEvent(
            Consumer<BTPEventEventLog> consumer) {
        return eventLogChecker(
                BTPEventEventLog::eventLogs,
                consumer);
    }


    static BMCStatus getStatus(BMC bmc, String _link) {
        ObjectMapper mapper = client.mapper();
        try {
            return mapper.readValue(mapper.writeValueAsString(bmc.getStatus(_link)), BMCStatus.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

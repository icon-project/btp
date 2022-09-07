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

package foundation.icon.btp.bmc;

import foundation.icon.btp.lib.BMCStatus;
import foundation.icon.btp.lib.BMVStatus;
import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.EVMIntegrationTest;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface BMCIntegrationTest extends BTPIntegrationTest {

    BMCManagement bmcManagement = deployBMC();

    static BMCManagement deployBMC() {
        BMCManagement bmcManagement = EVMIntegrationTest.deployWithInitialize(BMCManagement.class);
        BMCPeriphery bmcPeriphery = EVMIntegrationTest.deployWithInitialize(BMCPeriphery.class,
                "0x" + EVMIntegrationTest.chainId.toString(16) + ".bsc", bmcManagement.getContractAddress());
        try {
            bmcManagement.setBMCPeriphery(bmcPeriphery.getContractAddress()).send();
            System.out.println("BMCIntegrationTest:beforeAll bmcManagement.setBMCPeriphery address:" +
                    bmcPeriphery.getContractAddress());
            return bmcManagement;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    BMCPeriphery bmcPeriphery = loadBMCPeriphery(bmcManagement);

    static BMCPeriphery loadBMCPeriphery(BMCManagement bmcManagement) {
        try {
            String bmcPeripheryAddress = bmcManagement.getBMCPeriphery().send();
            return BMCPeriphery.load(bmcPeripheryAddress, w3j, tm, cgp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static BTPAddress btpAddress() {
        try {
            return BTPAddress.valueOf(bmcPeriphery.getBmcBtpAddress().send());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static BMCStatus getStatus(String link) {
        try {
            BMCPeriphery.LinkStats status = bmcPeriphery.getStatus(link).send();
            return new BMCStatus(
                    status.rxSeq,
                    status.txSeq,
                    new BMVStatus(
                            status.verifier.height.longValue(),
                            status.verifier.extra),
                    status.currentHeight.longValue());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    BMCManagement bmcWithTester = EVMIntegrationTest.load(bmcManagement, tester);

    static Consumer<TransactionReceipt> messageEvent(
            Consumer<BMCPeriphery.MessageEventResponse> consumer) {
        return EVMIntegrationTest.eventLogChecker(
                bmcPeriphery.getContractAddress(),
                BMCPeriphery::getMessageEvents,
                consumer);
    }

    static Consumer<TransactionReceipt> messageEvents(
            Consumer<List<BMCPeriphery.MessageEventResponse>> consumer,
            Predicate<BMCPeriphery.MessageEventResponse> filter) {
        return EVMIntegrationTest.eventLogsChecker(
                bmcPeriphery.getContractAddress(),
                BMCPeriphery::getMessageEvents,
                (l) -> {
                    if (filter != null) {
                        l = l.stream()
                                .filter(filter)
                                .collect(Collectors.toList());
                    }
                    consumer.accept(l);
                });
    }

    static List<BTPMessage> btpMessages(TransactionReceipt txr, Predicate<BMCPeriphery.MessageEventResponse> filter) {
        List<BTPMessage> msgs = new ArrayList<>();
        messageEvents((l) -> {
            msgs.addAll(l.stream().map((el) -> BTPMessage.fromBytes(el._msg))
                    .collect(Collectors.toList()));
        }, filter).accept(txr);
        return msgs;
    }

    String INTERNAL_SERVICE = "bmc";
    static List<BMCMessage> bmcMessages(TransactionReceipt txr, Predicate<String> nextPredicate) {
        return btpMessages(txr, (el) -> nextPredicate.test(el._next)).stream()
                .filter((msg) -> msg.getSvc().equals(INTERNAL_SERVICE))
                .map((msg) -> BMCMessage.fromBytes(msg.getPayload()))
                .collect(Collectors.toList());
    }

    enum Internal {Init, Link, Unlink}

    static <T> List<T> internalMessages(
            List<BMCMessage> bmcMessages,
            Internal internal,
            Function<byte[], T> mapFunc) {
        return bmcMessages.stream()
                .filter((bmcMsg) -> bmcMsg.getType().equals(internal.name()))
                .map((bmcMsg) -> mapFunc.apply(bmcMsg.getPayload()))
                .collect(Collectors.toList());
    }

    static Consumer<TransactionReceipt> messageDroppedEvent(
            Consumer<BMCPeriphery.MessageDroppedEventResponse> consumer) {
        return EVMIntegrationTest.eventLogChecker(
                bmcPeriphery.getContractAddress(),
                BMCPeriphery::getMessageDroppedEvents,
                consumer);
    }

    static Consumer<TransactionReceipt> messageDroppedEvents(
            Consumer<List<BMCPeriphery.MessageDroppedEventResponse>> consumer,
            Predicate<BMCPeriphery.MessageDroppedEventResponse> filter) {
        return EVMIntegrationTest.eventLogsChecker(
                bmcPeriphery.getContractAddress(),
                BMCPeriphery::getMessageDroppedEvents,
                (l) -> {
                    if (filter != null) {
                        l = l.stream()
                                .filter(filter)
                                .collect(Collectors.toList());
                    }
                    consumer.accept(l);
                });
    }
}

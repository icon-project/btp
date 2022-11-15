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
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public interface BMCIntegrationTest extends BTPIntegrationTest {

    BMCManagement bmcManagement = deployBMC();

    static BMCManagement deployBMC() {
        EVMIntegrationTest.replaceContractBinary(BMCManagement.class, "bmcm.", System.getProperties());
        EVMIntegrationTest.replaceContractBinary(BMCService.class, "bmcs.", System.getProperties());
        EVMIntegrationTest.replaceContractBinary(BMCPeriphery.class, "bmcp.", System.getProperties());
        BMCManagement bmcManagement = EVMIntegrationTest.deployWithInitialize(BMCManagement.class);
        BMCService bmcService = EVMIntegrationTest.deployWithInitialize(BMCService.class,
                bmcManagement.getContractAddress());
        BMCPeriphery bmcPeriphery = EVMIntegrationTest.deployWithInitialize(BMCPeriphery.class,
                "0x" + EVMIntegrationTest.chainId.toString(16) + ".bsc",
                bmcManagement.getContractAddress(),
                bmcService.getContractAddress());
        try {
            bmcManagement.setBMCPeriphery(bmcPeriphery.getContractAddress()).send();
            System.out.println("BMCIntegrationTest:beforeAll bmcManagement.setBMCPeriphery address:" +
                    bmcPeriphery.getContractAddress());
            bmcManagement.setBMCService(bmcService.getContractAddress()).send();
            System.out.println("BMCIntegrationTest:beforeAll bmcManagement.setBMCService address:" +
                    bmcService.getContractAddress());
            bmcService.setBMCPeriphery(bmcPeriphery.getContractAddress()).send();
            System.out.println("BMCIntegrationTest:beforeAll bmcService.setBMCPeriphery address:" +
                    bmcPeriphery.getContractAddress());
            return bmcManagement;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    BMCService bmcService = loadBMCService(bmcManagement);

    static BMCService loadBMCService(BMCManagement bmcManagement) {
        try {
            String bmcServiceAddress = bmcManagement.getBMCService().send();
            return BMCService.load(bmcServiceAddress, w3j, tm, cgp);
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
            return BTPAddress.valueOf(bmcPeriphery.getBtpAddress().send());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    BTPAddress btpAddress = btpAddress();

    BMCManagement bmcManagementWithTester = EVMIntegrationTest.load(bmcManagement, testerTm);
    BMCService bmcServiceWithTester = EVMIntegrationTest.load(bmcService, testerTm);
    BMCPeriphery bmcPeripheryWithTester = EVMIntegrationTest.load(bmcPeriphery, testerTm);

    static <T extends BaseEventResponse> Consumer<TransactionReceipt> eventLogChecker(
            EVMIntegrationTest.EventLogsSupplier<T> supplier, Consumer<T> consumer) {
        return EVMIntegrationTest.eventLogChecker(
                bmcPeriphery.getContractAddress(), supplier, consumer);
    }

    static <T extends BaseEventResponse> Consumer<TransactionReceipt> eventLogsChecker(
            EVMIntegrationTest.EventLogsSupplier<T> supplier, Consumer<List<T>> consumer) {
        return EVMIntegrationTest.eventLogsChecker(
                bmcPeriphery.getContractAddress(), supplier, consumer);
    }

    static Consumer<TransactionReceipt> messageEvent(
            Consumer<BMCPeriphery.MessageEventResponse> consumer) {
        return eventLogChecker(
                BMCPeriphery::getMessageEvents,
                consumer);
    }

    static List<BTPMessage> btpMessages(TransactionReceipt txr, Predicate<BMCPeriphery.MessageEventResponse> filter) {
        return EVMIntegrationTest.eventLogs(
                txr,
                bmcPeriphery.getContractAddress(),
                BMCPeriphery::getMessageEvents,
                filter).stream()
                .map((el) -> BTPMessage.fromBytes(el._msg))
                .collect(Collectors.toList());
    }

    String INTERNAL_SERVICE = "bmc";
    enum Internal {Init, Link, Unlink, Claim, Response}

    static List<BMCMessage> bmcMessages(TransactionReceipt txr, Predicate<String> nextPredicate) {
        Predicate<BMCPeriphery.MessageEventResponse> filter = (el) ->
                BTPMessage.fromBytes(el._msg).getSvc().equals(INTERNAL_SERVICE);
        if (nextPredicate != null) {
            filter = filter.and((el) -> nextPredicate.test(el._next));
        }
        return btpMessages(txr, filter).stream()
                .map((msg) -> BMCMessage.fromBytes(msg.getPayload()))
                .collect(Collectors.toList());
    }

    static <T> List<T> internalMessages(
            List<BMCMessage> bmcMessages,
            Internal internal,
            Function<byte[], T> mapFunc) {
        return bmcMessages.stream()
                .filter((bmcMsg) -> bmcMsg.getType().equals(internal.name()))
                .map((bmcMsg) -> mapFunc.apply(bmcMsg.getPayload()))
                .collect(Collectors.toList());
    }

    static Consumer<TransactionReceipt> claimRewardEvent(
            Consumer<BMCPeriphery.ClaimRewardEventResponse> consumer) {
        return eventLogChecker(
                BMCPeriphery::getClaimRewardEvents,
                consumer);
    }

    static Consumer<TransactionReceipt> claimRewardResultEvent(
            Consumer<BMCPeriphery.ClaimRewardResultEventResponse> consumer) {
        return eventLogChecker(
                BMCPeriphery::getClaimRewardResultEvents,
                consumer);
    }

    static Consumer<TransactionReceipt> messageDroppedEvent(
            Consumer<BMCPeriphery.MessageDroppedEventResponse> consumer) {
        return eventLogChecker(
                BMCPeriphery::getMessageDroppedEvents,
                consumer);
    }

    enum Event {SEND, DROP, ROUTE, ERROR, RECEIVE, REPLY}

    static Consumer<TransactionReceipt> btpEvent(
            Consumer<List<BMCPeriphery.BTPEventEventResponse>> consumer) {
        return eventLogsChecker(
                BMCPeriphery::getBTPEventEvents,
                consumer);
    }


    static BMCStatus getStatus(String link) {
        try {
            BMCPeriphery.LinkStatus status = bmcPeriphery.getStatus(link).send();
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
}

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
import foundation.icon.btp.lib.BMCStatus;
import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.MockBMVIntegrationTest;
import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.test.ScoreIntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class LinkManagementTest implements BMCIntegrationTest {

    static BTPAddress linkBtpAddress = BTPIntegrationTest.Faker.btpLink();
    static String link = linkBtpAddress.toString();
    static BTPAddress secondLinkBtpAddress = BTPIntegrationTest.Faker.btpLink();
    static String secondLink = secondLinkBtpAddress.toString();
    static String dst = BTPIntegrationTest.Faker.btpLink().toString();
    static Address address = ScoreIntegrationTest.Faker.address(Address.Type.EOA);

    static int blockInterval = 1000;
    static int maxAgg = 100;
    static int delayLimit = 2;
    static int sackTerm = 10;

    static Consumer<List<BMCMessage>> initMessageChecker(List<String> links) {
        return (bmcMessages) -> {
            List<InitMessage> initMessages = BMCIntegrationTest.internalMessages(
                    bmcMessages, BTPMessageCenter.Internal.Init, InitMessage::fromBytes);
            assertEquals(1, initMessages.size());
            InitMessage initMessage = initMessages.get(0);
            assertEquals(links == null ? 0 : links.size(), initMessage.getLinks().length);
            assertTrue(links == null || links.containsAll(
                    Arrays.stream(initMessage.getLinks())
                            .map((BTPAddress::toString))
                            .collect(Collectors.toList())));
        };
    }

    static Consumer<List<BMCMessage>> linkMessageChecker(String link, int size) {
        return (bmcMessages) -> {
            List<LinkMessage> linkMessages = BMCIntegrationTest.internalMessages(
                    bmcMessages, BTPMessageCenter.Internal.Link, LinkMessage::fromBytes);
            assertEquals(size, linkMessages.size());
            assertTrue(linkMessages.stream()
                    .allMatch((linkMsg) -> linkMsg.getLink().toString().equals(link)));
        };
    }

    static Consumer<List<BMCMessage>> unlinkMessageChecker(String link, int size) {
        return (bmcMessages) -> {
            List<UnlinkMessage> unlinkMessages = BMCIntegrationTest.internalMessages(
                    bmcMessages, BTPMessageCenter.Internal.Unlink, UnlinkMessage::fromBytes);
            assertEquals(size, unlinkMessages.size());
            assertTrue(unlinkMessages.stream()
                    .allMatch((unlinkMsg) -> unlinkMsg.getLink().toString().equals(link)));
        };
    }

    static boolean isExistsLink(String link) {
        return ScoreIntegrationTest.indexOf(bmc.getLinks(), link) >= 0;
    }

    static void addLink(String link) {
        List<String> links = Arrays.asList(bmc.getLinks());
        Consumer<TransactionResult> transactionResultChecker = (txr) -> {
            initMessageChecker(links)
                    .accept(BMCIntegrationTest.bmcMessages(txr, (next) -> next.equals(link)));
        };
        ((BMCScoreClient) bmc).addLink(transactionResultChecker, link);
        assertTrue(isExistsLink(link));
    }

    static void removeLink(String link) {
        bmc.removeLink(link);
        assertFalse(isExistsLink(link));
    }

    static void clearLink(String link) {
        if (isExistsLink(link)) {
            System.out.println("clear link btpAddress:" + link);
            removeLink(link);
        }
    }

    void setLinkRotateTerm(String link) {
        iconSpecific.setLinkRotateTerm(link, blockInterval, maxAgg);
        BMCStatus status = bmc.getStatus(link);
        assertEquals(blockInterval, status.getBlock_interval_dst());
        assertEquals(maxAgg, status.getMax_agg());
    }

    static boolean isExistsRoute(String dst, String link) {
        return ScoreIntegrationTest.contains(
                bmc.getRoutes(), dst, (o) -> link.equals(o));
    }

    static boolean isExistsRoute(String dst) {
        return bmc.getRoutes().containsKey(dst);
    }

    static void addRoute(String dst, String link) {
        bmc.addRoute(dst, link);
        assertTrue(isExistsRoute(dst, link));
    }

    static void removeRoute(String dst) {
        bmc.removeRoute(dst);
        assertFalse(isExistsRoute(dst));
    }

    static void clearRoute(String dst) {
        if (isExistsRoute(dst)) {
            System.out.println("clear route dst:" + dst);
            removeRoute(dst);
        }
    }

    @BeforeAll
    static void beforeAll() {
        System.out.println("beforeAll start");
        Address mockBMVAddress = MockBMVIntegrationTest.mockBMVClient._address();
        BMVManagementTest.addVerifier(
                linkBtpAddress.net(), mockBMVAddress);
        BMVManagementTest.addVerifier(
                secondLinkBtpAddress.net(), mockBMVAddress);
        System.out.println("beforeAll end");
    }

    @AfterAll
    static void afterAll() {
        System.out.println("afterAll start");
        BMVManagementTest.clearVerifier(linkBtpAddress.net());
        BMVManagementTest.clearVerifier(secondLinkBtpAddress.net());
        System.out.println("afterAll end");
    }

    @Override
    public void internalBeforeEach(TestInfo testInfo) {
        beforeLinkRequiredTests(testInfo);
    }

//    @Override
//    public void internalAfterEach(TestInfo testInfo) {
//        afterLinkRequiredTests(testInfo);
//    }

    @Override
    public void clearIfExists(TestInfo testInfo) {
        clearRoute(dst);
        clearLink(link);
        clearLink(secondLink);
    }

    @Test
    void addLinkShouldSuccess() {
        addLink(link);
    }

    @Test
    void addLinkShouldRevertAlreadyExists() {
        addLink(link);

        AssertBMCException.assertAlreadyExistsLink(() -> addLink(link));
    }

    @Test
    void addLinkShouldRevertAlreadyExistsIfRegisteredNetwork() {
        addLink(link);

        BTPAddress registeredNetworkLink = new BTPAddress(BTPAddress.PROTOCOL_BTP, linkBtpAddress.net(),
                ScoreIntegrationTest.Faker.address(Address.Type.CONTRACT).toString());
        AssertBMCException.assertAlreadyExistsLink(() -> addLink(registeredNetworkLink.toString()));
    }

    @Test
    void addLinkShouldRevertNotExistsBMV() {
        AssertBMCException.assertNotExistsBMV(
                () -> addLink(BTPIntegrationTest.Faker.btpLink().toString()));
    }

    @Test
    void removeLinkShouldSuccess() {
        addLink(link);

        removeLink(link);
    }

    @Test
    void removeLinkShouldRevertNotExists() {
        AssertBMCException.assertNotExistsLink(
                () -> removeLink(link));
    }

    @Test
    void removeLinkShouldRevertReferred() {
        addLink(link);
        addRoute(dst, link);

        AssertBMCException.assertUnknown(() -> removeLink(link));
    }

    @Test
    void removeLinkShouldClearRelays() {
        addLink(link);

        removeLink(link);

        //check relays of link is empty
        addLink(link);
        assertEquals(0, iconSpecific.getRelays(link).length);
    }

    @Test
    void addLinkShouldSendLinkMessageAndRemoveLinkShouldSendUnlinkMessage() {
        addLink(link);

        //addLinkShouldSendLinkMessage
        String secondLink = secondLinkBtpAddress.toString();
        List<String> links = new ArrayList<>();
        links.add(link);

        Consumer<TransactionResult> linkMessageCheck = (txr) -> {
            initMessageChecker(links)
                    .accept(BMCIntegrationTest.bmcMessages(txr, (next) -> next.equals(secondLink)));
            List<String> copy = new ArrayList<>(links);
            linkMessageChecker(secondLink, links.size())
                    .accept(BMCIntegrationTest.bmcMessages(txr, copy::remove));
            assertEquals(0, copy.size());
        };
        ((BMCScoreClient) bmc).addLink(linkMessageCheck, secondLink);
        assertTrue(isExistsLink(secondLink));

        //RemoveLinkShouldSendUnlinkMessage
        Consumer<TransactionResult> unlinkMessageCheck = (txr) -> {
            List<String> copy = new ArrayList<>(links);
            unlinkMessageChecker(secondLink, links.size())
                    .accept(BMCIntegrationTest.bmcMessages(txr, copy::remove));
            assertEquals(0, copy.size());
        };
        ((BMCScoreClient) bmc).removeLink(unlinkMessageCheck, secondLink);
        assertFalse(isExistsLink(secondLink));
    }

    static boolean isLinkRequiredTests(TestInfo testInfo) {
        return !testInfo.getDisplayName().contains("LinkShould");
    }

    void beforeLinkRequiredTests(TestInfo testInfo) {
        System.out.println("beforeLinkRequiredTests start on " + testInfo.getDisplayName());
        if (isLinkRequiredTests(testInfo)) {
            addLink(link);
        }
        System.out.println("beforeLinkRequiredTests end on " + testInfo.getDisplayName());
    }

//    void afterLinkRequiredTests(TestInfo testInfo) {
//        System.out.println("afterLinkRequiredTests start on "+testInfo.getDisplayName());
//        if (isLinkRequiredTests(testInfo)) {
//            clearLink(link);
//        }
//        System.out.println("afterLinkRequiredTests end on "+testInfo.getDisplayName());
//    }

    @Test
    void setLinkRotateTermShouldSuccess() {
        setLinkRotateTerm(link);
    }

    @Test
    void setLinkRotateTermShouldRevertNotExistsLink() {
        AssertBMCException.assertNotExistsLink(
                () -> setLinkRotateTerm(secondLink));
    }

    @Test
    void setLinkRotateTermShouldRevertIllegalArgument() {
        int invalidValue = -1;
        AssertBMCException.assertUnknown(
                () -> iconSpecific.setLinkRotateTerm(link, invalidValue, maxAgg));
        AssertBMCException.assertUnknown(
                () -> iconSpecific.setLinkRotateTerm(link, blockInterval, invalidValue));
    }

    @Test
    void setLinkDelayLimitShouldSuccess() {
        iconSpecific.setLinkDelayLimit(link, delayLimit);
        BMCStatus status = bmc.getStatus(link);
        assertEquals(delayLimit, status.getDelay_limit());
    }

    @Test
    void setLinkDelayLimitShouldRevertNotExistsLink() {
        AssertBMCException.assertNotExistsLink(
                () -> iconSpecific.setLinkDelayLimit(secondLink, delayLimit));
    }

    @Test
    void setLinkDelayLimitShouldRevertIllegalArgument() {
        int invalidValue = -1;
        AssertBMCException.assertUnknown(
                () -> iconSpecific.setLinkDelayLimit(link, invalidValue));
    }

    @Test
    void setLinkSackTermShouldSuccess() {
        iconSpecific.setLinkSackTerm(link, sackTerm);
        BMCStatus status = bmc.getStatus(link);
        assertEquals(sackTerm, status.getSack_term());
    }

    @Test
    void setLinkSackTermShouldRevertNotExistsLink() {
        AssertBMCException.assertNotExistsLink(
                () -> iconSpecific.setLinkSackTerm(secondLink, sackTerm));
    }

    @Test
    void setLinkSackTermShouldRevertIllegalArgument() {
        int invalidValue = -1;
        AssertBMCException.assertUnknown(
                () -> iconSpecific.setLinkSackTerm(link, invalidValue));
    }

    @Test
    void addRouteShouldSuccess() {
        addRoute(dst, link);
    }

    @Test
    void addRouteShouldRevertAlreadyExists() {
        addRoute(dst, link);

        AssertBMCException.assertUnknown(() -> addRoute(dst, link));
    }

    @Test
    void addRouteShouldRevertNotExistsLink() {
        AssertBMCException.assertNotExistsLink(
                () -> addRoute(dst, secondLink));
    }

    @Test
    void removeRouteShouldSuccess() {
        addRoute(dst, link);

        removeRoute(dst);
    }

    @Test
    void removeRouteShouldRevertNotExists() {
        AssertBMCException.assertUnknown(() -> removeRoute(dst));
    }

}

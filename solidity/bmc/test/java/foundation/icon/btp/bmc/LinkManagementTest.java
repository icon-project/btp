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

import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.test.EVMIntegrationTest;
import foundation.icon.btp.test.MockBMVIntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class LinkManagementTest implements BMCIntegrationTest {

    static BTPAddress linkBtpAddress = Faker.btpLink();
    static String link = linkBtpAddress.toString();
    static BTPAddress secondLinkBtpAddress = Faker.btpLink();
    static String secondLink = secondLinkBtpAddress.toString();
    static String dst = Faker.btpLink().toString();

    static Consumer<List<BMCMessage>> initMessageChecker(List<String> links) {
        return (bmcMessages) -> {
            List<InitMessage> initMessages = BMCIntegrationTest.internalMessages(
                    bmcMessages, BMCIntegrationTest.Internal.Init, InitMessage::fromBytes);
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
                    bmcMessages, BMCIntegrationTest.Internal.Link, LinkMessage::fromBytes);
            assertEquals(size, linkMessages.size());
            assertTrue(linkMessages.stream()
                    .allMatch((linkMsg) -> linkMsg.getLink().toString().equals(link)));
        };
    }

    static Consumer<List<BMCMessage>> unlinkMessageChecker(String link, int size) {
        return (bmcMessages) -> {
            List<UnlinkMessage> unlinkMessages = BMCIntegrationTest.internalMessages(
                    bmcMessages, BMCIntegrationTest.Internal.Unlink, UnlinkMessage::fromBytes);
            assertEquals(size, unlinkMessages.size());
            assertTrue(unlinkMessages.stream()
                    .allMatch((unlinkMsg) -> unlinkMsg.getLink().toString().equals(link)));
        };
    }

    @SuppressWarnings("unchecked")
    static List<String> getLinks() {
        try {
            return bmcManagement.getLinks().send();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static boolean isExistsLink(String link) {
        return getLinks().stream()
                .anyMatch((v) -> v.equals(link));
    }

    static void addLink(String link) {
        List<String> links = getLinks();
        Consumer<TransactionReceipt> checker = (txr) -> {
            initMessageChecker(links)
                    .accept(BMCIntegrationTest.bmcMessages(txr, (next) -> next.equals(link)));
        };
        try {
            checker.accept(bmcManagement.addLink(link).send());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertTrue(isExistsLink(link));
    }

    static void removeLink(String link) {
        try {
            bmcManagement.removeLink(link).send();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertFalse(isExistsLink(link));
    }

    static void clearLink(String link) {
        if (isExistsLink(link)) {
            System.out.println("clear link btpAddress:" + link);
            removeLink(link);
        }
    }

    @SuppressWarnings("unchecked")
    static List<BMCManagement.Route> getRoutes() {
        try {
            return bmcManagement.getRoutes().send();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static boolean isExistsRoute(String dst, String link) {
        return getRoutes().stream()
                .anyMatch((v) -> v.dst.equals(dst) && v.next.equals(link));
    }

    static boolean isExistsRoute(String dst) {
        return getRoutes().stream()
                .anyMatch((v) -> v.dst.equals(dst));
    }

    static void addRoute(String dst, String link) {
        try {
            bmcManagement.addRoute(dst, link).send();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertTrue(isExistsRoute(dst, link));
    }

    static void removeRoute(String dst) {
        try {
            bmcManagement.removeRoute(dst).send();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        System.out.println("LinkManagementTest:beforeAll start");
        String mockBMVAddress = MockBMVIntegrationTest.mockBMV.getContractAddress();
        BMVManagementTest.addVerifier(
                linkBtpAddress.net(), mockBMVAddress);
        BMVManagementTest.addVerifier(
                secondLinkBtpAddress.net(), mockBMVAddress);
        System.out.println("LinkManagementTest:beforeAll end");
    }

    @AfterAll
    static void afterAll() {
        System.out.println("LinkManagementTest:afterAll start");
        BMVManagementTest.clearVerifier(linkBtpAddress.net());
        BMVManagementTest.clearVerifier(secondLinkBtpAddress.net());
        System.out.println("LinkManagementTest:afterAll end");
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
                EVMIntegrationTest.Faker.address().toString());
        AssertBMCException.assertAlreadyExistsLink(() -> addLink(registeredNetworkLink.toString()));
    }

    @Test
    void addLinkShouldRevertNotExistsBMV() {
        AssertBMCException.assertNotExistsBMV(
                () -> addLink(Faker.btpLink().toString()));
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

        assertEquals(0, BMRManagementTest.getRelays(link).size());
    }

    @Test
    void addLinkShouldSendLinkMessageAndRemoveLinkShouldSendUnlinkMessage() {
        addLink(link);

        //addLinkShouldSendLinkMessage
        String secondLink = secondLinkBtpAddress.toString();
        List<String> links = new ArrayList<>();
        links.add(link);

        Consumer<TransactionReceipt> linkMessageCheck = (txr) -> {
            initMessageChecker(links)
                    .accept(BMCIntegrationTest.bmcMessages(txr, (next) -> next.equals(secondLink)));
            List<String> copy = new ArrayList<>(links);
            linkMessageChecker(secondLink, links.size())
                    .accept(BMCIntegrationTest.bmcMessages(txr, copy::remove));
            assertEquals(0, copy.size());
        };
        try {
            linkMessageCheck.accept(bmcManagement.addLink(secondLink).send());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertTrue(isExistsLink(secondLink));

        //RemoveLinkShouldSendUnlinkMessage
        Consumer<TransactionReceipt> unlinkMessageCheck = (txr) -> {
            List<String> copy = new ArrayList<>(links);
            unlinkMessageChecker(secondLink, links.size())
                    .accept(BMCIntegrationTest.bmcMessages(txr, copy::remove));
            assertEquals(0, copy.size());
        };
        try {
            unlinkMessageCheck.accept(bmcManagement.removeLink(secondLink).send());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

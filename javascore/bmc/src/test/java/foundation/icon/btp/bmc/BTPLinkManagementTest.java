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

import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.MockBMVIntegrationTest;
import foundation.icon.btp.test.MockGovIntegrationTest;
import foundation.icon.jsonrpc.Address;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BTPLinkManagementTest implements BMCIntegrationTest {
    static BTPAddress linkBtpAddress = BTPIntegrationTest.Faker.btpLink();
    static String link = linkBtpAddress.toString();
    static BTPAddress secondLinkBtpAddress = BTPIntegrationTest.Faker.btpLink();
    static String secondLink = secondLinkBtpAddress.toString();
    static String networkTypeName = "eth";

    static void addBTPLink(String link, long networkId) {
        List<String> links = Arrays.asList(bmc.getLinks());
        iconSpecific.addBTPLink(
                BTPBlockMessageTest.bmcMessageChecker(
                        networkId,
                        LinkManagementTest.initMessageChecker(links)),
                link, networkId);
        assertTrue(LinkManagementTest.isExistsLink(link));
        assertEquals(networkId, iconSpecific.getBTPLinkNetworkId(link));
    }

    @BeforeAll
    static void beforeAll() {
        System.out.println("BTPLinkManagementTest:beforeAll start");
        Address mockBMVAddress = MockBMVIntegrationTest.mockBMV._address();
        BMVManagementTest.addVerifier(linkBtpAddress.net(), mockBMVAddress);
        BMVManagementTest.addVerifier(secondLinkBtpAddress.net(), mockBMVAddress);
        System.out.println("BTPLinkManagementTest:beforeAll end");
    }

    @AfterAll
    static void afterAll() {
        System.out.println("BTPLinkManagementTest:afterAll start");
        BMVManagementTest.clearVerifier(linkBtpAddress.net());
        BMVManagementTest.clearVerifier(secondLinkBtpAddress.net());
        System.out.println("BTPLinkManagementTest:afterAll end");
    }

    @Override
    public void clearIfExists(TestInfo testInfo) {
        LinkManagementTest.clearLink(link);
        LinkManagementTest.clearLink(secondLink);
    }

    @Test
    void addBTPLinkAndRemoveLinkShouldSuccess() {
        long networkId = MockGovIntegrationTest.openBTPNetwork(networkTypeName, link, bmc._address());
        addBTPLink(link, networkId);

        LinkManagementTest.removeLink(link);
    }

    @Test
    void addBTPLinkShouldRevertInvalidNetworkId() {
        AssertBMCException.assertUnknown(() -> iconSpecific.addBTPLink(link, 0));
    }

    @Test
    void addBTPLinkShouldRevertDuplicatedNetworkId() {
        long networkId = MockGovIntegrationTest.openBTPNetwork(networkTypeName, link, bmc._address());
        addBTPLink(link, networkId);

        AssertBMCException.assertUnknown(() -> iconSpecific.addBTPLink(secondLink, networkId));
    }

    @Test
    void setBTPLinkNetworkIdShouldSuccess() {
        LinkManagementTest.addLink(link);
        assertEquals(0, iconSpecific.getBTPLinkNetworkId(link));

        long networkId = MockGovIntegrationTest.openBTPNetwork(networkTypeName, link, bmc._address());
        iconSpecific.setBTPLinkNetworkId(link, networkId);
        assertEquals(networkId, iconSpecific.getBTPLinkNetworkId(link));
    }

    @Test
    void setBTPLinkNetworkIdShouldRevertInvalidNetworkId() {
        LinkManagementTest.addLink(link);
        AssertBMCException.assertUnknown(() -> iconSpecific.setBTPLinkNetworkId(link, 0));
    }

    @Test
    void setBTPLinkNetworkIdShouldRevertDuplicatedNetworkId() {
        long networkId = MockGovIntegrationTest.openBTPNetwork(networkTypeName, link, bmc._address());
        addBTPLink(link, networkId);

        LinkManagementTest.addLink(secondLink);
        AssertBMCException.assertUnknown(() -> iconSpecific.setBTPLinkNetworkId(secondLink, networkId));
    }
}

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
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.MockBMVIntegrationTest;
import foundation.icon.jsonrpc.Address;
import foundation.icon.score.test.AssertRevertedException;
import foundation.icon.score.test.ScoreIntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BMRManagementTest implements BMCIntegrationTest {
    static BTPAddress linkBtpAddress = BMCIntegrationTest.Faker.btpLink();
    static String link = linkBtpAddress.toString();
    static String net = linkBtpAddress.net();
    static Address address = ScoreIntegrationTest.Faker.address(Address.Type.EOA);

    @BeforeAll
    static void beforeAll() {
        System.out.println("BMRManagementTest:beforeAll start");
        BMVManagementTest.addVerifier(net, MockBMVIntegrationTest.mockBMV._address());
        LinkManagementTest.addLink(link);
        System.out.println("BMRManagementTest:beforeAll end");
    }

    @AfterAll
    static void afterAll() {
        System.out.println("BMRManagementTest:afterAll start");
        LinkManagementTest.clearLink(link);
        BMVManagementTest.clearVerifier(net);
        System.out.println("BMRManagementTest:afterAll end");
    }

    @Override
    public void clearIfExists(TestInfo testInfo) {
        clearRelay(link, address);
    }

    static boolean isExistsRelay(String link, Address address) {
        return Arrays.stream(iconSpecific.getRelays(link))
                .anyMatch((v) -> v.getAddress().equals(address));
    }

    static void addRelay(String link, Address address) {
        iconSpecific.addRelay(link, address);
        assertTrue(isExistsRelay(link, address));
    }

    static void removeRelay(String link, Address address) {
        iconSpecific.removeRelay(link, address);
        assertFalse(isExistsRelay(link, address));
    }

    static void clearRelay(String link, Address address) {
        if (LinkManagementTest.isExistsLink(link) && isExistsRelay(link, address)) {
            System.out.println("clear relay link:" + link + ", address:" + address);
            removeRelay(link, address);
        }
    }

    @Test
    void addRelayShouldSuccess() {
        addRelay(link, address);
    }

    @Test
    void addRelayShouldRevertAlreadyExists() {
        addRelay(link, address);

        AssertBMCException.assertAlreadyExistsBMR(() ->
                addRelay(link, address));
    }

    @Test
    void addRelayShouldRevertNotExistsLink() {
        AssertBMCException.assertNotExistsLink(() ->
                addRelay(BTPIntegrationTest.Faker.btpLink().toString(), address));
    }

    @Test
    void removeRelayShouldSuccess() {
        addRelay(link, address);

        removeRelay(link, address);
    }

    @Test
    void removeRelayShouldRevertNotExists() {
        AssertBMCException.assertNotExistsBMR(() ->
                removeRelay(link, address));
    }

    @Test
    void removeRelayShouldRevertNotExistsLink() {
        AssertBMCException.assertNotExistsLink(() -> removeRelay(
                BTPIntegrationTest.Faker.btpLink().toString(), address));
    }

    @Disabled("readonly call revert test")
    @Test
    void getRelaysShouldRevertNotExistsLink() {
        //noinspection ThrowableNotThrown
        AssertRevertedException.assertUserRevertFromJsonrpcError(
                BMCException.notExistsLink(),
                () -> iconSpecific.getRelays(BTPIntegrationTest.Faker.btpLink().toString()),
                null);
    }

}

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
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BMRManagementTest implements BMCIntegrationTest {
    static BTPAddress linkBtpAddress = Faker.btpLink();
    static String link = linkBtpAddress.toString();
    static String net = linkBtpAddress.net();
    static String address = EVMIntegrationTest.Faker.address().toString();

    @SuppressWarnings("unchecked")
    static List<BMCManagement.RelayStats> getRelays(String link) {
        try {
            return bmcManagement.getRelays(link).send();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static boolean isExistsRelay(String link, String address) {
        return getRelays(link).stream()
                .anyMatch((v) -> v.addr.equals(address));
    }

    static void addRelay(String link, String address) {
        try {
            bmcManagement.addRelay(link, address).send();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertTrue(isExistsRelay(link, address));
    }

    static void removeRelay(String link, String address) {
        try {
            bmcManagement.removeRelay(link, address).send();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertFalse(isExistsRelay(link, address));
    }

    static void clearRelay(String link, String address) {
        if (LinkManagementTest.isExistsLink(link) && isExistsRelay(link, address)) {
            System.out.println("clear relay link:" + link + ", address:" + address);
            removeRelay(link, address);
        }
    }

    @BeforeAll
    static void beforeAll() {
        System.out.println("BMRManagementTest:beforeAll start");
        String mockBMVAddress = MockBMVIntegrationTest.mockBMV.getContractAddress();
        BMVManagementTest.addVerifier(
                linkBtpAddress.net(), mockBMVAddress);
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
                addRelay(Faker.btpLink().toString(), address));
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
                Faker.btpLink().toString(), address));
    }

    @Disabled("readonly call revert test")
    @Test
    void getRelaysShouldRevertNotExistsLink() {
        //noinspection ThrowableNotThrown
//        AssertRevertedException.assertUserRevertFromJsonrpcError(
//                BMCException.notExistsLink(),
//                () -> iconSpecific.getRelays(Faker.btpLink().toString()),
//                null);
    }

}

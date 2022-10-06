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
import foundation.icon.jsonrpc.Address;
import foundation.icon.score.test.ScoreIntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RouteManagementTest implements BMCIntegrationTest {
    static BTPAddress link = BTPIntegrationTest.Faker.btpLink();
    static BTPAddress dst = BTPIntegrationTest.Faker.btpLink();

    @SuppressWarnings("unchecked")
    static boolean isExistsRoute(BTPAddress dst, BTPAddress link) {
        System.out.println(bmc.getRoutes());
        return ScoreIntegrationTest.contains(
                bmc.getRoutes(), dst.net(),
                (obj) -> {
                    Map<String, String> map = (Map<String, String>) obj;
                    return map.get("destination").equals(dst.toString()) &&
                            map.get("next").equals(link.toString());
                });
    }

    static boolean isExistsRoute(BTPAddress dst) {
        return bmc.getRoutes().containsKey(dst.net());
    }

    static void addRoute(BTPAddress dst, BTPAddress link) {
        bmc.addRoute(dst.toString(), link.toString());
        assertTrue(isExistsRoute(dst, link));
    }

    static void removeRoute(BTPAddress dst) {
        bmc.removeRoute(dst.toString());
        assertFalse(isExistsRoute(dst));
    }

    static void clearRoute(BTPAddress dst) {
        if (isExistsRoute(dst)) {
            System.out.println("clear route dst:" + dst);
            removeRoute(dst);
        }
    }

    @BeforeAll
    static void beforeAll() {
        System.out.println("RouteManagementTest:beforeAll start");
        Address mockBMVAddress = MockBMVIntegrationTest.mockBMV._address();
        BMVManagementTest.addVerifier(link.net(), mockBMVAddress);
        LinkManagementTest.addLink(link.toString());
        System.out.println("RouteManagementTest:beforeAll end");
    }

    @AfterAll
    static void afterAll() {
        System.out.println("RouteManagementTest:afterAll start");
        LinkManagementTest.clearLink(link.toString());
        BMVManagementTest.clearVerifier(link.net());
        System.out.println("RouteManagementTest:afterAll end");
    }

    @Override
    public void clearIfExists(TestInfo testInfo) {
        clearRoute(dst);
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
                () -> addRoute(dst, Faker.btpLink()));
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

    @Test
    void removeLinkShouldRevertReferred() {
        addRoute(dst, link);

        AssertBMCException.assertUnknown(() -> LinkManagementTest.removeLink(link.toString()));
    }

}

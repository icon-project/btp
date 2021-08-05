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
import foundation.icon.score.test.ScoreIntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DropMessageTest implements BMCIntegrationTest {
    static BTPAddress linkBtpAddress = BMCIntegrationTest.Faker.btpLink();
    static String link = linkBtpAddress.toString();
    static String net = linkBtpAddress.net();
    static BigInteger seq = BigInteger.ONE;

    @BeforeAll
    static void beforeAll() {
        System.out.println("beforeAll start");
        BMVManagementTest.addVerifier(net, MockBMVIntegrationTest.mockBMVClient._address());
        LinkManagementTest.addLink(link);
        System.out.println("beforeAll end");
    }

    @AfterAll
    static void afterAll() {
        System.out.println("afterAll start");
        LinkManagementTest.clearLink(link);
        BMVManagementTest.clearVerifier(net);
        System.out.println("afterAll end");
    }

    @Override
    public void clearIfExists(TestInfo testInfo) {
        clearDrop(link, seq);
    }

    static boolean isExistsDrop(String link, BigInteger seq) {
        return ScoreIntegrationTest.indexOf(
                iconSpecific.getDrops(link), seq) >= 0;
    }

    static void addDrop(String link, BigInteger seq) {
        iconSpecific.addDrop(link, seq);
        assertTrue(isExistsDrop(link, seq));
    }

    static void removeDrop(String link, BigInteger seq) {
        iconSpecific.removeDrop(link, seq);
        assertFalse(isExistsDrop(link, seq));
    }

    static void clearDrop(String link, BigInteger seq) {
        if (LinkManagementTest.isExistsLink(link) && isExistsDrop(link, seq)) {
            System.out.println("clear drop link:" + link + ", seq:" + seq);
            removeDrop(link, seq);
        }
    }

    @Test
    void addDropShouldSuccess() {
        addDrop(link, seq);
    }

    @Test
    void addDropShouldRevertAlreadyExists() {
        addDrop(link, seq);

        AssertBMCException.assertUnknown(() ->
                addDrop(link, seq));
    }

    @Test
    void addDropShouldRevertNotExistsLink() {
        AssertBMCException.assertNotExistsLink(() ->
                addDrop(BTPIntegrationTest.Faker.btpLink().toString(), seq));
    }

    @Test
    void removeDropShouldSuccess() {
        addDrop(link, seq);

        removeDrop(link, seq);
    }

    @Test
    void removeDropShouldRevertNotExists() {
        AssertBMCException.assertUnknown(() ->
                removeDrop(link, seq));
    }

    @Test
    void removeDropShouldRevertNotExistsLink() {
        AssertBMCException.assertNotExistsLink(() -> removeDrop(
                BTPIntegrationTest.Faker.btpLink().toString(), seq));
    }

}

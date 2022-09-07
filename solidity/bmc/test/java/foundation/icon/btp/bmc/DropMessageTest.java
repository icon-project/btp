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
import foundation.icon.btp.test.MockBMVIntegrationTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DropMessageTest implements BMCIntegrationTest {
    static BTPAddress linkBtpAddress = Faker.btpLink();
    static String link = linkBtpAddress.toString();
    static String net = linkBtpAddress.net();
    static BigInteger seq = BigInteger.ONE;

    @BeforeAll
    static void beforeAll() {
        System.out.println("DropMessageTest:beforeAll start");
        BMVManagementTest.addVerifier(net, MockBMVIntegrationTest.mockBMV.getContractAddress());
        LinkManagementTest.addLink(link);
        System.out.println("DropMessageTest:beforeAll end");
    }

    @AfterAll
    static void afterAll() {
        System.out.println("DropMessageTest:afterAll start");
        LinkManagementTest.clearLink(link);
        BMVManagementTest.clearVerifier(net);
        System.out.println("DropMessageTest:afterAll end");
    }

    @Override
    public void clearIfExists(TestInfo testInfo) {
        clearDrop(link, seq);
    }

    @SuppressWarnings("unchecked")
    static List<BigInteger> getScheduledDropMessages(String link) {
        try {
            return bmcManagement.getScheduledDropMessages(link).send();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static boolean isExistsScheduledDropMessage(String link, BigInteger seq) {
        return getScheduledDropMessages(link).stream()
                .anyMatch((v) -> v.compareTo(seq) == 0);
    }

    static void scheduleDropMessage(String link, BigInteger seq) {
        try {
            bmcManagement.scheduleDropMessage(link, seq).send();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertTrue(isExistsScheduledDropMessage(link, seq));
    }

    static void cancelDropMessage(String link, BigInteger seq) {
        try {
            bmcManagement.cancelDropMessage(link, seq).send();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertFalse(isExistsScheduledDropMessage(link, seq));
    }

    static void clearDrop(String link, BigInteger seq) {
        if (LinkManagementTest.isExistsLink(link) && isExistsScheduledDropMessage(link, seq)) {
            System.out.println("clear drop link:" + link + ", seq:" + seq);
            cancelDropMessage(link, seq);
        }
    }

    @Test
    void scheduleDropMessageShouldSuccess() {
        scheduleDropMessage(link, seq);
    }

    @Test
    void scheduleDropMessageShouldRevertAlreadyExists() {
        scheduleDropMessage(link, seq);

        AssertBMCException.assertUnknown(() ->
                scheduleDropMessage(link, seq));
    }

    @Test
    void scheduleDropMessageShouldRevertNotExistsLink() {
        AssertBMCException.assertNotExistsLink(() ->
                scheduleDropMessage(Faker.btpLink().toString(), seq));
    }

    @Test
    void cancelDropMessageShouldSuccess() {
        scheduleDropMessage(link, seq);

        cancelDropMessage(link, seq);
    }

    @Test
    void cancelDropMessageShouldRevertNotExists() {
        AssertBMCException.assertUnknown(() ->
                cancelDropMessage(link, seq));
    }

    @Test
    void cancelDropMessageShouldRevertNotExistsLink() {
        AssertBMCException.assertNotExistsLink(() -> cancelDropMessage(
                Faker.btpLink().toString(), seq));
    }

}

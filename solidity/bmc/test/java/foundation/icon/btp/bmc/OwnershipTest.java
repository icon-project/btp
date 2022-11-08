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

import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.EVMIntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.function.Executable;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OwnershipTest implements BMCIntegrationTest {
    static String address = EVMIntegrationTest.Faker.address().toString();
    static String string = "string";
    static String btpAddress = BTPIntegrationTest.Faker.btpLink().toString();
    static BigInteger bigInteger = BigInteger.ZERO;

    static boolean isExistsOwner(String address) {
        try {
            return bmcManagement.isOwner(address).send();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static void addOwner(String address) {
        try {
            bmcManagement.addOwner(address).send();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertTrue(isExistsOwner(address));
    }

    static void removeOwner(String address) {
        try {
            bmcManagement.removeOwner(address).send();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertFalse(isExistsOwner(address));
    }

    static void clearOwner(String address) {
        if (isExistsOwner(address)) {
            System.out.println("clear owner address:"+address);
            removeOwner(address);
        }
    }

    @BeforeAll
    static void beforeAll() {
        System.out.println("OwnershipTest:beforeAll start");
        if (EVMIntegrationTest.getBalance(tester)
                .compareTo(BigInteger.ZERO) <= 0) {
            EVMIntegrationTest.transfer(tester, BigInteger.TEN);
        }
        System.out.println("OwnershipTest:beforeAll end");
    }

    @Override
    public void clearIfExists(TestInfo testInfo) {
        String testMethod = testInfo.getTestMethod().orElseThrow().getName();
        if (!testMethod.endsWith("RevertUnauthorized")) {
            clearOwner(address);
        }
    }

    @Test
    void addOwnerShouldSuccess() {
        addOwner(address);
    }

    static void assertAlreadyExists(Executable executable) {
        AssertBMCException.assertUnknown(executable);
    }

    @Test
    void addOwnerShouldRevertAlreadyExists() {
        addOwner(address);

        assertAlreadyExists(() -> addOwner(address));
    }

    @Test
    void removeOwnerShouldSuccess() {
        addOwner(address);

        removeOwner(address);
    }

    static void assertNotExists(Executable executable) {
        AssertBMCException.assertUnknown(executable);
    }

    @Test
    void removeOwnerShouldRevertNotExists() {
        assertNotExists(() -> removeOwner(address));
    }

    static void assertUnauthorized(Executable executable) {
        AssertBMCException.assertUnauthorized(executable);
    }

    @Test
    void addOwnerShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcManagementWithTester.addOwner(address).send());
    }

    @Test
    void removeOwnerShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcManagementWithTester.removeOwner(address).send());
    }

    @Test
    void addVerifierShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcManagementWithTester.addVerifier(string, address).send());
    }

    @Test
    void removeVerifierShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcManagementWithTester.removeVerifier(string).send());
    }

    @Test
    void addServiceShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcManagementWithTester.addService(string, address).send());
    }

    @Test
    void removeServiceShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcManagementWithTester.removeService(string).send());
    }

    @Test
    void addLinkShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcManagementWithTester.addLink(btpAddress).send());
    }

    @Test
    void removeLinkShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcManagementWithTester.removeLink(btpAddress).send());
    }

    @Test
    void addRouteShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcManagementWithTester.addRoute(btpAddress, btpAddress).send());
    }

    @Test
    void removeRouteShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcManagementWithTester.removeRoute(btpAddress).send());
    }

    @Test
    void addRelayShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcManagementWithTester.addRelay(btpAddress, address).send());
    }

    @Test
    void removeRelayShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcManagementWithTester.removeRelay(btpAddress, address).send());
    }

    @Test
    void dropMessageShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcManagementWithTester.dropMessage(btpAddress, bigInteger, string, bigInteger, bigInteger, "", new ArrayList<>()).send());

        System.out.println("BMCManagement.setPeripheryShouldRevertUnauthorized");
        assertUnauthorized(() -> bmcManagementWithTester.setBMCPeriphery(address).send());
    }

}

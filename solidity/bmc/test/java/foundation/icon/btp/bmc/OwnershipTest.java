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
    static void beforeAll() throws Exception {
        System.out.println("OwnershipTest:beforeAll start");
        BigInteger balance = w3j.ethGetBalance(tester.getAddress(), DefaultBlockParameterName.LATEST).send().getBalance();
        if (balance.compareTo(BigInteger.ZERO) == 0) {
            Transfer transfer = new Transfer(w3j, tm);
            transfer.sendFunds(tester.getAddress(), BigDecimal.TEN, Convert.Unit.ETHER).send();

            balance = w3j.ethGetBalance(tester.getAddress(), DefaultBlockParameterName.LATEST).send().getBalance();
            System.out.println(tester.getAddress() + ":" + balance);
        }
        System.out.println("OwnershipTest:beforeAll start");
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
        assertUnauthorized(() -> bmcWithTester.addOwner(address).send());
    }

    @Test
    void removeOwnerShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcWithTester.removeOwner(address).send());
    }

    @Test
    void addVerifierShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcWithTester.addVerifier(string, address).send());
    }

    @Test
    void removeVerifierShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcWithTester.removeVerifier(string).send());
    }

    @Test
    void addServiceShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcWithTester.addService(string, address).send());
    }

    @Test
    void removeServiceShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcWithTester.removeService(string).send());
    }

    @Test
    void addLinkShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcWithTester.addLink(btpAddress).send());
    }

    @Test
    void removeLinkShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcWithTester.removeLink(btpAddress).send());
    }

    @Test
    void addRouteShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcWithTester.addRoute(btpAddress, btpAddress).send());
    }

    @Test
    void removeRouteShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcWithTester.removeRoute(btpAddress).send());
    }

    @Test
    void addRelayShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcWithTester.addRelay(btpAddress, address).send());
    }

    @Test
    void removeRelayShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcWithTester.removeRelay(btpAddress, address).send());
    }

    @Test
    void dropMessageShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcWithTester.dropMessage(btpAddress, bigInteger, string, bigInteger).send());
    }

}

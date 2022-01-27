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

package foundation.icon.btp.irc2Tradeable;

import foundation.icon.jsonrpc.Address;
import foundation.icon.score.test.AssertRevertedException;
import foundation.icon.score.test.ScoreIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.function.Executable;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OwnershipTest implements IRC2IntegrationTest {
    static Address address = ScoreIntegrationTest.Faker.address(Address.Type.EOA);
    static String string = "";
    static BigInteger bigInteger = BigInteger.ZERO;
    static BigInteger[] bigIntegerArray = new BigInteger[]{BigInteger.ZERO};

    static boolean isExistsOwner(Address address) {
        return irc2OwnerManager.isOwner(address);
    }

    static void addOwner(Address address) {
        irc2OwnerManager.addOwner(address);
        assertTrue(isExistsOwner(address));
    }

    static void removeOwner(Address address) {
        irc2OwnerManager.removeOwner(address);
        assertFalse(isExistsOwner(address));
    }

    static void clearOwner(Address address) {
        if (isExistsOwner(address)) {
            System.out.println("clear owner address:"+address);
            removeOwner(address);
        }
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

    @SuppressWarnings("ThrowableNotThrown")
    static void assertAlreadyExists(Executable executable) {
        AssertRevertedException.assertUserReverted(0, executable);
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

    @SuppressWarnings("ThrowableNotThrown")
    static void assertNotExists(Executable executable) {
        AssertRevertedException.assertUserReverted(0, executable);
    }

    @Test
    void removeOwnerShouldRevertNotExists() {
        assertNotExists(() -> removeOwner(address));
    }

    @SuppressWarnings("ThrowableNotThrown")
    static void assertUnauthorized(Executable executable) {
        AssertRevertedException.assertUserReverted(0, executable);
    }

    @Test
    void addOwnerShouldRevertUnauthorized() {
        assertUnauthorized(() -> irc2OwnerManagerWithTester.addOwner(address));
    }

    @Test
    void removeOwnerShouldRevertUnauthorized() {
        assertUnauthorized(() -> irc2OwnerManagerWithTester.removeOwner(address));
    }

    @Test
    void mintShouldRevertUnauthorized() {
        assertUnauthorized(() -> irc2SupplierWithTester.mint(address, bigInteger));
    }
}

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

package foundation.icon.btp.nativecoin;

import foundation.icon.jsonrpc.Address;
import foundation.icon.score.test.ScoreIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.function.Executable;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OwnershipTest implements NCSIntegrationTest {
    static Address address = ScoreIntegrationTest.Faker.address(Address.Type.EOA);
    static String string = "";
    static BigInteger bigInteger = BigInteger.ONE;

    static boolean isExistsOwner(Address address) {
        return ncsOwnerManager.isOwner(address);
    }

    static void addOwner(Address address) {
        ncsOwnerManager.addOwner(address);
        assertTrue(isExistsOwner(address));
    }

    static void removeOwner(Address address) {
        ncsOwnerManager.removeOwner(address);
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

    static void assertAlreadyExists(Executable executable) {
        AssertNCSException.assertUnknown(executable);
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
        AssertNCSException.assertUnknown(executable);
    }

    @Test
    void removeOwnerShouldRevertNotExists() {
        assertNotExists(() -> removeOwner(address));
    }

    static void assertUnauthorized(Executable executable) {
        AssertNCSException.assertUnauthorized(executable);
    }

    @Test
    void addOwnerShouldRevertUnauthorized() {
        assertUnauthorized(() -> ncsOwnerManagerWithTester.addOwner(address));
    }

    @Test
    void removeOwnerShouldRevertUnauthorized() {
        assertUnauthorized(() -> ncsOwnerManagerWithTester.removeOwner(address));
    }

    @Test
    void registerShouldRevertUnauthorized() {
        assertUnauthorized(() -> ncsWithTester.register(string));
    }

    @Test
    void setFeeRateShouldRevertUnauthorized() {
        assertUnauthorized(() -> ncsWithTester.setFeeRatio(bigInteger));
    }

}

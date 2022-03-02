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

import foundation.icon.jsonrpc.Address;
import foundation.icon.score.test.ScoreIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.function.Executable;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class OwnershipTest implements BMCIntegrationTest {
    static Address address = ScoreIntegrationTest.Faker.address(Address.Type.EOA);
    static String string = "";
    static String btpAddress = "";
    static int intVal = 0;
    static long longVal = 0;
    static BigInteger bigInteger = BigInteger.ZERO;

    static boolean isExistsOwner(Address address) {
        return ownerManager.isOwner(address);
    }

    static void addOwner(Address address) {
        ownerManager.addOwner(address);
        assertTrue(isExistsOwner(address));
    }

    static void removeOwner(Address address) {
        ownerManager.removeOwner(address);
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
        assertUnauthorized(() -> ownerManagerWithTester.addOwner(address));
    }

    @Test
    void removeOwnerShouldRevertUnauthorized() {
        assertUnauthorized(() -> ownerManagerWithTester.removeOwner(address));
    }

    @Test
    void addVerifierShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcWithTester.addVerifier(string, address));
    }

    @Test
    void removeVerifierShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcWithTester.removeVerifier(string));
    }

    @Test
    void addServiceShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcWithTester.addService(string, address));
    }

    @Test
    void removeServiceShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcWithTester.removeService(string));
    }

    @Test
    void addLinkShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcWithTester.addLink(btpAddress));
    }

    @Test
    void removeLinkShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcWithTester.removeLink(btpAddress));
    }

    @Test
    void setLinkRotateTermShouldRevertUnauthorized() {
        assertUnauthorized(() -> iconSpecificWithTester.setLinkRotateTerm(btpAddress, intVal, intVal));
    }

    @Test
    void setLinkDelayLimitShouldRevertUnauthorized() {
        assertUnauthorized(() -> iconSpecificWithTester.setLinkDelayLimit(btpAddress, intVal));
    }

    @Test
    void setLinkSackTermShouldRevertUnauthorized() {
        assertUnauthorized(() -> iconSpecificWithTester.setLinkSackTerm(btpAddress, intVal));
    }

    @Test
    void addRouteShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcWithTester.addRoute(btpAddress, btpAddress));
    }

    @Test
    void removeRouteShouldRevertUnauthorized() {
        assertUnauthorized(() -> bmcWithTester.removeRoute(btpAddress));
    }

    @Test
    void addRelayShouldRevertUnauthorized() {
        assertUnauthorized(() -> iconSpecificWithTester.addRelay(btpAddress, address));
    }

    @Test
    void removeRelayShouldRevertUnauthorized() {
        assertUnauthorized(() -> iconSpecificWithTester.removeRelay(btpAddress, address));
    }

    @Test
    void setRelayerMinBondShouldRevertUnauthorized() {
        assertUnauthorized(() -> iconSpecificWithTester.setRelayerMinBond(bigInteger));
    }

    @Test
    void setRelayerTermShouldRevertUnauthorized() {
        assertUnauthorized(() -> iconSpecificWithTester.setRelayerTerm(longVal));
    }

    @Test
    void setRelayerRewardRankShouldRevertUnauthorized() {
        assertUnauthorized(() -> iconSpecificWithTester.setRelayerRewardRank(intVal));
    }

    @Test
    void removeRelayerShouldRevertUnauthorized() {
        assertUnauthorized(() -> iconSpecificWithTester.removeRelayer(address, address));
    }

    @Test
    void setFeeGatheringTermShouldRevertUnauthorized() {
        assertUnauthorized(() -> iconSpecificWithTester.setFeeGatheringTerm(intVal));
    }

    @Test
    void setFeeAggregatorShouldRevertUnauthorized() {
        assertUnauthorized(() -> iconSpecificWithTester.setFeeAggregator(address));
    }

    @Test
    void sendFeeGatheringShouldRevertUnauthorized() {
        assertUnauthorized(() -> iconSpecificWithTester.sendFeeGathering());
    }
}

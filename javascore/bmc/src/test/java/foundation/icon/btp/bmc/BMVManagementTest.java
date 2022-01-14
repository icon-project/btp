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
import foundation.icon.jsonrpc.Address;
import foundation.icon.score.test.ScoreIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BMVManagementTest implements BMCIntegrationTest {
    static BTPAddress btpAddress = BTPIntegrationTest.Faker.btpLink();
    static String net = btpAddress.net();
    static Address address = new Address(btpAddress.account());

    static boolean isExistsVerifier(String net, Address address) {
        return ScoreIntegrationTest.contains(
                bmc.getVerifiers(), net, (o) -> address.toString().equals(o));
    }

    static void addVerifier(String net, Address address) {
        bmc.addVerifier(net, address);
        assertTrue(isExistsVerifier(net, address));
    }

    static boolean isExistsVerifier(String net) {
        return bmc.getVerifiers().containsKey(net);
    }

    static void removeVerifier(String net) {
        bmc.removeVerifier(net);
        assertFalse(isExistsVerifier(net));
    }

    static void clearVerifier(String net) {
        if(isExistsVerifier(net)) {
            System.out.println("clear verifier net:"+net);
            removeVerifier(net);
        }
    }

    @Override
    public void clearIfExists(TestInfo testInfo) {
        clearVerifier(net);
    }

    @Test
    void addVerifierShouldSuccess() {
        addVerifier(net, address);
    }

    @Test
    void addVerifierShouldRevertAlreadyExists() {
        addVerifier(net, address);

        AssertBMCException.assertAlreadyExistsBMV(() -> addVerifier(net, address));
    }

    @Test
    void removeVerifierShouldSuccess() {
        addVerifier(net, address);

        removeVerifier(net);
    }

    @Test
    void removeVerifierShouldRevertNotExists() {
        AssertBMCException.assertNotExistsBMV(() -> removeVerifier(net));
    }
}

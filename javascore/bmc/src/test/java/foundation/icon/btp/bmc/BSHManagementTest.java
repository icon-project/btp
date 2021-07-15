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

import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.jsonrpc.Address;
import foundation.icon.score.test.ScoreIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BSHManagementTest implements BMCIntegrationTest {

    static String svc = BTPIntegrationTest.Faker.btpService();
    static Address address = ScoreIntegrationTest.Faker.address(Address.Type.CONTRACT);

    static boolean isExistsServiceCandidate(String svc, Address address, Address owner) {
        Predicate<ServiceCandidate> predicate = (o) -> o.getSvc().equals(svc) &&
                o.getAddress().equals(address) &&
                o.getOwner().equals(owner);
        return ScoreIntegrationTest.indexOf(
                iconSpecific.getServiceCandidates(), predicate) >= 0;
    }

    static void addServiceCandidate(String svc, Address address) {
        iconSpecificWithTester.addServiceCandidate(svc, address);
        assertTrue(isExistsServiceCandidate(svc, address, Address.of(tester)));
    }

    static boolean isExistsServiceCandidate(String svc, Address address) {
        Predicate<ServiceCandidate> predicate = (o) -> o.getSvc().equals(svc) &&
                o.getAddress().equals(address);
        return ScoreIntegrationTest.indexOf(
                iconSpecific.getServiceCandidates(), predicate) >= 0;
    }

    static void removeServiceCandidate(String svc, Address address) {
        iconSpecific.removeServiceCandidate(svc, address);
        assertFalse(isExistsServiceCandidate(svc, address));
    }

    static void clearServiceCandidate(String svc, Address address) {
        if(isExistsServiceCandidate(svc, address)) {
            System.out.println("clear service candidate svc:"+svc);
            removeServiceCandidate(svc, address);
        }
    }

    static boolean isExistsService(String svc, Address address) {
        return ScoreIntegrationTest.contains(bmc.getServices(), svc,
                (o) -> address.toString().equals(o));
    }

    static void addService(String svc, Address address) {
        bmc.addService(svc, address);
        assertTrue(isExistsService(svc, address));
    }

    static boolean isExistsService(String svc) {
        return bmc.getServices().containsKey(svc);
    }

    static void removeService(String svc) {
        bmc.removeService(svc);
        assertFalse(isExistsService(svc));
    }

    static void clearService(String svc) {
        if(isExistsService(svc)) {
            System.out.println("clear service svc:"+svc);
            removeService(svc);
        }
    }

    @Override
    public void clearIfExists(TestInfo testInfo) {
        clearServiceCandidate(svc, address);
        clearService(svc);
    }

    @Test
    void addServiceCandidateShouldSuccess() {
        addServiceCandidate(svc, address);
    }

    @Test
    void addServiceCandidateShouldRevertAlreadyExists() {
        addServiceCandidate(svc, address);

        AssertBMCException.assertUnknown(() -> addServiceCandidate(svc, address));
    }

    @Test
    void removeServiceCandidateShouldSuccess() {
        addServiceCandidate(svc, address);

        removeServiceCandidate(svc, address);
    }

    @Test
    void removeServiceCandidateShouldRevertNotExists() {
        AssertBMCException.assertUnknown(() -> removeServiceCandidate(svc, address));
    }

    @Test
    void addServiceShouldSuccess() {
        addService(svc, address);
    }

    @Test
    void addServiceCandidateAndAddServiceShouldRemoveServiceCandidate() {
        addServiceCandidate(svc, address);
        addService(svc, address);

        assertFalse(isExistsServiceCandidate(svc, address));
    }

    @Test
    void addServiceShouldRevertAlreadyExists() {
        addService(svc, address);

        AssertBMCException.assertAlreadyExistsBSH(() -> addService(svc, address));
    }

    @Test
    void removeServiceShouldSuccess() {
        addService(svc, address);

        removeService(svc);
    }

    @Test
    void removeServiceShouldRevertNotExists() {
        AssertBMCException.assertNotExistsBSH(() -> removeService(svc));
    }
}
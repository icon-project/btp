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

import foundation.icon.btp.test.EVMIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BSHManagementTest implements BMCIntegrationTest {

    static String svc = Faker.btpService();
    static String address = EVMIntegrationTest.Faker.address().toString();

    @SuppressWarnings("unchecked")
    static List<BMCManagement.Service> getServices() {
        try {
            return bmcManagement.getServices().send();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static boolean isExistsService(String svc, String address) {
        return getServices().stream()
                .anyMatch((s) -> s.svc.equals(svc) && s.addr.equals(address));
    }

    static void addService(String svc, String address) {
        try {
            bmcManagement.addService(svc, address).send();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertTrue(isExistsService(svc, address));
    }

    static boolean isExistsService(String svc) {
        return getServices().stream()
                .anyMatch((s) -> s.svc.equals(svc));
    }

    static void removeService(String svc) {
        try {
            bmcManagement.removeService(svc).send();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        clearService(svc);
    }

    @Test
    void addServiceShouldSuccess() {
        addService(svc, address);
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

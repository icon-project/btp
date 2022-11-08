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

package foundation.icon.btp.mock;

import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.EVMIntegrationTest;
import org.junit.jupiter.api.Test;
import org.web3j.tuples.generated.Tuple2;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BTPAddressTest {
    static TestBTPAddress testBTPAddress = EVMIntegrationTest.deploy(TestBTPAddress.class);
    static BTPAddress btpAddress = BTPIntegrationTest.Faker.btpLink();

    @Test
    void parseBTPAddress() throws Exception {
        Tuple2<String, String> tuple2 = testBTPAddress.parseBTPAddress(btpAddress.toString()).send();
        assertEquals(btpAddress.net(), tuple2.component1());
        assertEquals(btpAddress.account(), tuple2.component2());
    }

    @Test
    void networkAddress() throws Exception {
        assertEquals(btpAddress.net(),
                testBTPAddress.networkAddress(btpAddress.toString()).send());
    }

    @Test
    void btpAddress() throws Exception {
        assertEquals(btpAddress.toString(),
                testBTPAddress.btpAddress(
                        btpAddress.net(),
                        btpAddress.account()).send());
    }
}

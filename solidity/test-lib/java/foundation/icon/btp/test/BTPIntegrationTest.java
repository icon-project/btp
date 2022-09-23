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

package foundation.icon.btp.test;

import foundation.icon.btp.lib.BTPAddress;

public interface BTPIntegrationTest extends EVMIntegrationTest {

    interface Faker {
        com.github.javafaker.Faker faker = new com.github.javafaker.Faker();

        static String btpService() {
            return faker.name().firstName();
        }

        String DEFAULT_TYPE = "bsc";

        static String btpNetwork() {
            return btpNetwork(DEFAULT_TYPE);
        }

        static String btpNetwork(String type) {
            return "0x" + faker.crypto().sha256().substring(0, 6) + "." + type;
        }

        static BTPAddress btpLink() {
            return btpLink(DEFAULT_TYPE);
        }

        static BTPAddress btpLink(String type) {
            return new BTPAddress(BTPAddress.PROTOCOL_BTP, btpNetwork(type),
                    EVMIntegrationTest.Faker.address().toString());
        }

        static BTPAddress btpEoa() {
            return btpEoa(DEFAULT_TYPE);
        }

        static BTPAddress btpEoa(String type) {
            return new BTPAddress(BTPAddress.PROTOCOL_BTP, btpNetwork(type),
                    EVMIntegrationTest.Faker.address().toString());
        }
    }

}

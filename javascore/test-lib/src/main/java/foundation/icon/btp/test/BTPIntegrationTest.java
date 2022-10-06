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

package foundation.icon.btp.test;

import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.jsonrpc.Address;
import foundation.icon.score.test.ScoreIntegrationTest;

public interface BTPIntegrationTest extends ScoreIntegrationTest {

    interface Faker extends ScoreIntegrationTest.Faker {

        static String btpService() {
            return faker.name().firstName();
        }

        static String btpNetwork() {
            return "0x" + faker.crypto().sha256().substring(0, 6) + ".icon";
        }

        static BTPAddress btpLink() {
            return new BTPAddress(BTPAddress.PROTOCOL_BTP, btpNetwork(),
                    ScoreIntegrationTest.Faker.address(Address.Type.CONTRACT).toString());
        }

        static BTPAddress btpEoa() {
            return new BTPAddress(BTPAddress.PROTOCOL_BTP, btpNetwork(),
                    ScoreIntegrationTest.Faker.address(Address.Type.EOA).toString());
        }
    }
}

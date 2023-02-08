/*
 * Copyright 2023 ICON Foundation
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

import foundation.icon.jsonrpc.IconStringConverter;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

public interface BTPBlockIntegrationTest extends BTPIntegrationTest {

    static BigInteger nextMessageSN(long networkId, BigInteger height) {
        @SuppressWarnings("rawtypes")
        Map result = client.request(Map.class, "btp_getNetworkInfo",
                Map.of("id", networkId, "height", height));
        return IconStringConverter.toBigInteger((String) result.get("nextMessageSN"));
    }

    static byte[][] messages(long networkId, BigInteger height) {
        return Arrays.stream(client.request(
                        String[].class,
                        "btp_getMessages",
                        Map.of("networkId", networkId, "height", height)))
                .map((s) -> Base64.getDecoder().decode(s))
                .toArray(byte[][]::new);
    }
}

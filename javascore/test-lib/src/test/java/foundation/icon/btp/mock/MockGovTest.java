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

import foundation.icon.btp.test.BTPBlockIntegrationTest;
import foundation.icon.btp.test.MockGovIntegrationTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MockGovTest implements MockGovIntegrationTest {
    static final String NETWORK_TYPE_NAME = "eth";
    static final String NETWORK_NAME = "0x1.icon";

    @BeforeAll
    public static void beforeAll() {
        ((ChainScoreClient) chainScore).setDumpJson(true);
    }

    @Test
    void openBTPNetworkAndSendBTPMessageAndCloseBTPNetwork() {
        long networkId = MockGovIntegrationTest.openBTPNetwork(
                NETWORK_TYPE_NAME, NETWORK_NAME, chainScoreClient._wallet().getAddress());

        @SuppressWarnings("rawtypes")
        Map result = chainScoreClient.request(Map.class, "btp_getNetworkInfo",
                Map.of("id", networkId));
        assertEquals(NETWORK_TYPE_NAME, result.get("networkTypeName"));
        assertEquals(NETWORK_NAME, result.get("networkName"));

        byte[] message = "testMessage".getBytes();
        chainScoreClient.sendBTPMessage(
                (txr) -> {
                    byte[][] messages = BTPBlockIntegrationTest.messages(
                            networkId,
                            txr.getBlockHeight().add(BigInteger.ONE));
                    assertEquals(1, messages.length);
                    assertArrayEquals(message, messages[0]);
                },
                networkId, message);

        MockGovIntegrationTest.closeBTPNetwork(networkId);
    }

}

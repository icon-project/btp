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

import foundation.icon.btp.test.MockGovIntegrationTest;
import foundation.icon.icx.IconService;
import foundation.icon.icx.data.BTPNetworkInfo;
import foundation.icon.icx.data.Base64;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.jsonrpc.Address;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MockGovTest implements MockGovIntegrationTest {
    static final String NETWORK_TYPE_NAME = "icon";
    static final String NETWORK_NAME = "0x1.icon";

    static IconService iconService = new IconService(new HttpProvider(mockGovClient.endpoint()));

    @BeforeAll
    public static void beforeAll() {
        ((ChainScoreClient) chainScore).setDumpJson(true);
    }

    @Test
    void openBTPNetworkAndSendBTPMessageAndCloseBTPNetwork() throws Exception {
        long networkId = MockGovIntegrationTest.openBTPNetwork(
                NETWORK_TYPE_NAME, NETWORK_NAME, Address.of(chainScoreClient._wallet()));
        BTPNetworkInfo btpNetworkInfo = iconService.btpGetNetworkInfo(BigInteger.valueOf(networkId)).execute();
        assertEquals(NETWORK_TYPE_NAME, btpNetworkInfo.getNetworkTypeName());
        assertEquals(NETWORK_NAME, btpNetworkInfo.getNetworkName());

        byte[] message = "testMessage".getBytes();
        chainScoreClient.sendBTPMessage(
                (txr) -> {
                    Base64[] base64Messages = null;
                    try {
                        base64Messages = iconService.btpGetMessages(txr.getBlockHeight().add(BigInteger.ONE),
                                BigInteger.valueOf(networkId)).execute();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    assertEquals(1, base64Messages.length);
                    assertArrayEquals(message, base64Messages[0].decode());
                },
                networkId, message);

        MockGovIntegrationTest.closeBTPNetwork(networkId);
    }

}

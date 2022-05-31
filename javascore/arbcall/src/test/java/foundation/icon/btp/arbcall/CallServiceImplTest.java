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

package foundation.icon.btp.arbcall;

import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.MockBMCIntegrationTest;
import foundation.icon.btp.test.SendMessageEventLog;
import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.model.TransactionResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CallServiceImplTest implements CSIntegrationTest {
    static Address csAddress = csClient._address();
    static Address sampleAddress = sampleClient._address();
    static Address owner = Address.of(csClient._wallet());
    static BTPAddress link = BTPIntegrationTest.Faker.btpLink();
    static String linkNet = BTPIntegrationTest.Faker.btpNetwork();
    static BTPAddress to = new BTPAddress(BTPAddress.PROTOCOL_BTP, linkNet, sampleAddress.toString());

    @BeforeAll
    static void beforeAll() {
    }

    static Consumer<TransactionResult> sendMessageEventLogChecker(CSMessageRequest request) {
        return MockBMCIntegrationTest.eventLogChecker(SendMessageEventLog::eventLogs, (el) -> {
            assertEquals(linkNet, el.getTo());
            assertEquals(CallServiceImpl.SERVICE, el.getSvc());
            CSMessage csMessage = CSMessage.fromBytes(el.getMsg());
            assertEquals(CSMessage.REQUEST, csMessage.getType());
            AssertCallService.assertEqualsCSMessageRequest(request, CSMessageRequest.fromBytes(csMessage.getData()));
        });
    }

    private void sendMessageFromProxy(BTPAddress to, byte[] data) {
        var checker = sendMessageEventLogChecker(
                new CSMessageRequest(sampleAddress.toString(), to.account(), data));
        Map<String, Object> params = new HashMap<>();
        params.put("_to", to.toString());
        params.put("_data", data);
        checker.accept(sampleClient._send("sendMessage", params));
    }

    @Test
    void sendCallMessage() {
        byte[] data = "Hello BTP".getBytes();
        sendMessageFromProxy(to, data);
    }
}

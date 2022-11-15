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

import foundation.icon.btp.mock.MockBSH;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.function.Consumer;

public interface MockBSHIntegrationTest {
    String SERVICE = "mock";

    MockBSH mockBSH = deployMockBSH();
    static MockBSH deployMockBSH() {
        EVMIntegrationTest.replaceContractBinary(MockBSH.class, "mock-bsh.", System.getProperties());
        return EVMIntegrationTest.deploy(MockBSH.class);
    }

    static Consumer<TransactionReceipt> handleBTPMessageEvent(
            Consumer<MockBSH.HandleBTPMessageEventResponse> consumer) {
        return EVMIntegrationTest.eventLogChecker(
                mockBSH.getContractAddress(),
                MockBSH::getHandleBTPMessageEvents,
                consumer);
    }

    static Consumer<TransactionReceipt> handleBTPErrorEvent(
            Consumer<MockBSH.HandleBTPErrorEventResponse> consumer) {
        return EVMIntegrationTest.eventLogChecker(
                mockBSH.getContractAddress(),
                MockBSH::getHandleBTPErrorEvents,
                consumer);
    }
}

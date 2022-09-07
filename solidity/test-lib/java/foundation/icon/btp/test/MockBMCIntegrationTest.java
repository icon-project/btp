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
import foundation.icon.btp.mock.MockBMC;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public interface MockBMCIntegrationTest {
    String NET = "0x"+EVMIntegrationTest.chainId.toString(16)+".bsc";
    MockBMC mockBMC = EVMIntegrationTest.deploy(MockBMC.class, NET);

    static BTPAddress btpAddress() {
        try {
            return BTPAddress.valueOf(mockBMC.getBmcBtpAddress().send());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static Consumer<TransactionReceipt> sendMessageEvent(
            Consumer<MockBMC.MessageEventResponse> consumer) {
        return EVMIntegrationTest.eventLogChecker(
                mockBMC.getContractAddress(),
                MockBMC::getMessageEvents,
                consumer);
    }

    static Consumer<TransactionReceipt> handleRelayMessageEvent(
            Consumer<MockBMC.HandleRelayMessageEventResponse> consumer) {
        return EVMIntegrationTest.eventLogChecker(
                mockBMC.getContractAddress(),
                MockBMC::getHandleRelayMessageEvents,
                consumer);
    }


    static Consumer<TransactionReceipt> shouldSuccessHandleRelayMessage() {
        return EVMIntegrationTest.eventLogsChecker(
                mockBMC.getContractAddress(),
                MockBMC::getErrorHandleRelayMessageEvents, (l) -> {
                    for (MockBMC.ErrorHandleRelayMessageEventResponse el : l) {
                        System.out.println("ErrorHandleRelayMessage:" + el.err);
                    }
                    assertEquals(0, l.size());
                });
    }

    static Consumer<TransactionReceipt> shouldSuccessHandleBTPMessage() {
        return EVMIntegrationTest.eventLogsChecker(
                mockBMC.getContractAddress(),
                MockBMC::getErrorHandleBTPMessageEvents, (l) -> {
                    for (MockBMC.ErrorHandleBTPMessageEventResponse el : l) {
                        System.out.println("ErrorHandleBTPMessage:" + el.err);
                    }
                    assertEquals(0, l.size());
                });
    }

    static Consumer<TransactionReceipt> shouldSuccessHandleBTPError() {
        return EVMIntegrationTest.eventLogsChecker(
                mockBMC.getContractAddress(),
                MockBMC::getErrorHandleBTPErrorEvents, (l) -> {
                    for (MockBMC.ErrorHandleBTPErrorEventResponse el : l) {
                        System.out.println("ErrorHandleBTPError:" + el.err);
                    }
                    assertEquals(0, l.size());
                });
    }
}

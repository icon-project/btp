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

package foundation.icon.btp.xcall;

import foundation.icon.btp.test.MockBMCIntegrationTest;
import foundation.icon.btp.test.BTPIntegrationTest;
import foundation.icon.btp.test.EVMIntegrationTest;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.function.Consumer;

public interface CSIntegrationTest extends BTPIntegrationTest {
    CallService callService = EVMIntegrationTest.deployWithInitialize(CallService.class,
            MockBMCIntegrationTest.mockBMC.getContractAddress());
    DAppProxySample dAppProxySample = EVMIntegrationTest.deployWithInitialize(DAppProxySample.class,
            callService.getContractAddress());

    static Consumer<TransactionReceipt> callMessageEvent(
            Consumer<CallService.CallMessageEventResponse> consumer) {
        return EVMIntegrationTest.eventLogChecker(
                callService.getContractAddress(),
                CallService::getCallMessageEvents,
                consumer);
    }

    static Consumer<TransactionReceipt> rollbackMessageEvent(
            Consumer<CallService.RollbackMessageEventResponse> consumer) {
        return EVMIntegrationTest.eventLogChecker(
                callService.getContractAddress(),
                CallService::getRollbackMessageEvents,
                consumer);
    }

    static Consumer<TransactionReceipt> rollbackMessageEventShouldNotExists() {
        return EVMIntegrationTest.eventLogShouldNotExistsChecker(
                callService.getContractAddress(),
                CallService::getRollbackMessageEvents);
    }

    static Consumer<TransactionReceipt> callRequestClearedEvent(
            Consumer<CallService.CallRequestClearedEventResponse> consumer) {
        return EVMIntegrationTest.eventLogChecker(
                callService.getContractAddress(),
                CallService::getCallRequestClearedEvents,
                consumer);
    }

    static Consumer<TransactionReceipt> callRequestClearedEventShouldNotExists() {
        return EVMIntegrationTest.eventLogShouldNotExistsChecker(
                callService.getContractAddress(),
                CallService::getCallRequestClearedEvents);
    }

    static Consumer<TransactionReceipt> messageReceivedEvent(
            Consumer<DAppProxySample.MessageReceivedEventResponse> consumer) {
        return EVMIntegrationTest.eventLogChecker(
                dAppProxySample.getContractAddress(),
                DAppProxySample::getMessageReceivedEvents,
                consumer);
    }

    static Consumer<TransactionReceipt> fixedFeesUpdatedEvent(
            Consumer<CallService.FixedFeesUpdatedEventResponse> consumer) {
        return EVMIntegrationTest.eventLogChecker(
                callService.getContractAddress(),
                CallService::getFixedFeesUpdatedEvents,
                consumer);
    }
}

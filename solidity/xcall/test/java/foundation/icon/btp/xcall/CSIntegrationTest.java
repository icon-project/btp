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
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.util.function.Consumer;

public interface CSIntegrationTest extends BTPIntegrationTest {
    String NAME = "xcall";
    int MAX_DATA_SIZE = 2048;
    int MAX_ROLLBACK_SIZE = 1024;

    CallService callService = deployCallService();
    static CallService deployCallService() {
        EVMIntegrationTest.replaceContractBinary(CallService.class, "xcall.", System.getProperties());
        return EVMIntegrationTest.deployWithInitialize(CallService.class,
                MockBMCIntegrationTest.mockBMC.getContractAddress());
    }
    IFeeManage feeManager = EVMIntegrationTest.load(IFeeManage.class, callService);

    DAppProxySample dAppProxySample = deployDAppProxySample();
    static DAppProxySample deployDAppProxySample() {
        EVMIntegrationTest.replaceContractBinary(DAppProxySample.class, "sample.", System.getProperties());
        return EVMIntegrationTest.deployWithInitialize(DAppProxySample.class,
                callService.getContractAddress());
    }

    static Consumer<TransactionReceipt> callMessageEvent(
            Consumer<CallService.CallMessageEventResponse> consumer) {
        return eventLogChecker(
                CallService::getCallMessageEvents,
                consumer);
    }

    static Consumer<TransactionReceipt> responseMessageEvent(
            Consumer<CallService.ResponseMessageEventResponse> consumer) {
        return eventLogChecker(
                CallService::getResponseMessageEvents,
                consumer);
    }

    static Consumer<TransactionReceipt> rollbackMessageEvent(
            Consumer<CallService.RollbackMessageEventResponse> consumer) {
        return eventLogChecker(
                CallService::getRollbackMessageEvents,
                consumer);
    }

    static Consumer<TransactionReceipt> callExecutedEvent(
            Consumer<CallService.CallExecutedEventResponse> consumer) {
        return eventLogChecker(
                CallService::getCallExecutedEvents,
                consumer);
    }

    static Consumer<TransactionReceipt> rollbackExecutedEvent(
            Consumer<CallService.RollbackExecutedEventResponse> consumer) {
        return eventLogChecker(
                CallService::getRollbackExecutedEvents,
                consumer);
    }

    static Consumer<TransactionReceipt> responseMessageEventShouldNotExists() {
        return eventLogShouldNotExistsChecker(CallService::getResponseMessageEvents);
    }

    static Consumer<TransactionReceipt> rollbackMessageEventShouldNotExists() {
        return eventLogShouldNotExistsChecker(CallService::getRollbackMessageEvents);
    }

    static Consumer<TransactionReceipt> callRequestClearedEvent(
            Consumer<CallService.CallRequestClearedEventResponse> consumer) {
        return eventLogChecker(
                CallService::getCallRequestClearedEvents,
                consumer);
    }

    static Consumer<TransactionReceipt> callRequestClearedEventShouldNotExists() {
        return eventLogShouldNotExistsChecker(CallService::getCallRequestClearedEvents);
    }

    static <T extends BaseEventResponse> Consumer<TransactionReceipt> eventLogChecker(
            EVMIntegrationTest.EventLogsSupplier<T> supplier, Consumer<T> consumer) {
        return EVMIntegrationTest.eventLogChecker(
                callService.getContractAddress(), supplier, consumer);
    }

    static <T extends BaseEventResponse> Consumer<TransactionReceipt> eventLogShouldNotExistsChecker(
            EVMIntegrationTest.EventLogsSupplier<T> supplier) {
        return EVMIntegrationTest.eventLogShouldNotExistsChecker(
                callService.getContractAddress(), supplier);
    }

    static Consumer<TransactionReceipt> messageReceivedEvent(
            Consumer<DAppProxySample.MessageReceivedEventResponse> consumer) {
        return EVMIntegrationTest.eventLogChecker(
                dAppProxySample.getContractAddress(),
                DAppProxySample::getMessageReceivedEvents,
                consumer);
    }

    static Consumer<TransactionReceipt> rollbackDataReceivedEvent(
            Consumer<DAppProxySample.RollbackDataReceivedEventResponse> consumer) {
        return EVMIntegrationTest.eventLogChecker(
                dAppProxySample.getContractAddress(),
                DAppProxySample::getRollbackDataReceivedEvents,
                consumer);
    }
}

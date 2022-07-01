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

import com.fasterxml.jackson.databind.ObjectMapper;
import foundation.icon.btp.lib.BMCStatus;
import foundation.icon.btp.lib.BMV;
import foundation.icon.btp.lib.BMVStatus;
import foundation.icon.btp.mock.MockBMV;
import foundation.icon.btp.mock.MockBMVScoreClient;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.client.DefaultScoreClient;
import foundation.icon.score.test.ScoreIntegrationTest;

import java.io.IOException;
import java.util.function.Consumer;

public interface MockBMVIntegrationTest {

    DefaultScoreClient mockBMVClient = DefaultScoreClient.of("bmv-mock.", System.getProperties());
    MockBMV mockBMV = new MockBMVScoreClient(mockBMVClient);

    static <T> Consumer<TransactionResult> eventLogChecker(
            ScoreIntegrationTest.EventLogsSupplier<T> supplier, Consumer<T> consumer) {
        return ScoreIntegrationTest.eventLogChecker(
                mockBMVClient._address(), supplier, consumer);
    }

    static BMVStatus getStatus(BMV bmv) {
        ObjectMapper mapper = mockBMVClient.mapper();
        try {
            return mapper.readValue(mapper.writeValueAsString(bmv.getStatus()), BMVStatus.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

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

package foundation.icon.btp.bmc;

import foundation.icon.icx.IconService;
import foundation.icon.icx.data.Base64;
import foundation.icon.icx.transport.http.HttpProvider;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.client.DefaultScoreClient;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface BTPBlockIntegrationTest {
    IconService iconService = new IconService(
            new HttpProvider(DefaultScoreClient.url(System.getProperties())));


    static <T> Consumer<TransactionResult> btpMessageChecker(
            long networkId, Consumer<List<BTPMessage>> consumer) {
        return (txr) -> {
            consumer.accept(
                    btpMessages(txr, networkId)
                            .collect(Collectors.toList()));
        };
    }

    static <T> Consumer<TransactionResult> svcMessageChecker(
            long networkId, String svc, Function<BTPMessage, T> mapperFunc, Consumer<List<T>> consumer) {
        return (txr) -> {
            consumer.accept(
                    btpMessages(txr, networkId)
                            .filter((m) -> m.getSvc().equals(svc))
                            .map(mapperFunc)
                            .collect(Collectors.toList()));
        };
    }

    static <T> Consumer<TransactionResult> bmcMessageChecker(
            long networkId, Consumer<List<BMCMessage>> consumer) {
        return svcMessageChecker(networkId,
                BTPMessageCenter.INTERNAL_SERVICE,
                (m) -> BMCMessage.fromBytes(m.getPayload()),
                consumer);
    }

    static Stream<BTPMessage> btpMessages(TransactionResult txr, long networkId) {
        Base64[] messages = null;
        try {
            messages = iconService.btpGetMessages(
                    txr.getBlockHeight().add(BigInteger.ONE),
                    BigInteger.valueOf(networkId)).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Arrays.stream(messages)
                .map((m) -> BTPMessage.fromBytes(m.decode()));
    }

}

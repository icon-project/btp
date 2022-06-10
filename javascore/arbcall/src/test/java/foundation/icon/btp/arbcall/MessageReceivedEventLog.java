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

import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.IconJsonModule;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.test.ScoreIntegrationTest;

import java.util.List;
import java.util.function.Predicate;

public class MessageReceivedEventLog {
    private static final String SIGNATURE = "MessageReceived(str,bytes)";
    private final String from;
    private final byte[] data;

    public MessageReceivedEventLog(TransactionResult.EventLog el) {
        this.from = el.getData().get(0);
        this.data = IconJsonModule.ByteArrayDeserializer.BYTE_ARRAY.convert(
                el.getData().get(1));
    }

    public String getFrom() {
        return from;
    }

    public byte[] getData() {
        return data;
    }

    public static List<MessageReceivedEventLog> eventLogs(TransactionResult txr,
                                                          Address address,
                                                          Predicate<MessageReceivedEventLog> filter) {
        return ScoreIntegrationTest.eventLogs(
                txr, MessageReceivedEventLog.SIGNATURE, address, MessageReceivedEventLog::new, filter);
    }
}

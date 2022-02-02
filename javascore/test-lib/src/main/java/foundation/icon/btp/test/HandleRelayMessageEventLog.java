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

import foundation.icon.btp.mock.MockRelayMessage;
import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.IconJsonModule;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.test.ScoreIntegrationTest;
import foundation.icon.score.util.StringUtil;

import java.util.List;
import java.util.function.Predicate;

public class HandleRelayMessageEventLog {
    static final String SIGNATURE = "HandleRelayMessage(bytes)";
    private byte[][] ret;

    public HandleRelayMessageEventLog(TransactionResult.EventLog el) {
        this.ret = MockRelayMessage.toBytesArray(
                IconJsonModule.ByteArrayDeserializer.BYTE_ARRAY.convert(
                        el.getData().get(0)));
    }

    public byte[][] getRet() {
        return ret;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HandleRelayMessageEventLog{");
        sb.append("ret=").append(StringUtil.toString(ret));
        sb.append('}');
        return sb.toString();
    }

    public static List<HandleRelayMessageEventLog> eventLogs(
            TransactionResult txr, Address address, Predicate<HandleRelayMessageEventLog> filter) {
        return ScoreIntegrationTest.eventLogs(txr,
                HandleRelayMessageEventLog.SIGNATURE,
                address,
                HandleRelayMessageEventLog::new,
                filter);
    }
}

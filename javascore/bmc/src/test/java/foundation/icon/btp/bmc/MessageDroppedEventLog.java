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

package foundation.icon.btp.bmc;

import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.IconJsonModule;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.test.ScoreIntegrationTest;

import java.math.BigInteger;
import java.util.List;
import java.util.function.Predicate;

public class MessageDroppedEventLog {
    static final String SIGNATURE = "MessageDropped(str,int,bytes)";
    private String link;
    private BigInteger seq;
    private BTPMessage msg;

    public MessageDroppedEventLog(TransactionResult.EventLog el) {
        this.link = el.getIndexed().get(1);
        this.seq = IconJsonModule.NumberDeserializer.BIG_INTEGER.convert(el.getIndexed().get(2));
        this.msg = BTPMessage.fromBytes(IconJsonModule.ByteArrayDeserializer.BYTE_ARRAY.convert(el.getData().get(0)));
    }

    public String getLink() {
        return link;
    }

    public BigInteger getSeq() {
        return seq;
    }

    public BTPMessage getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MessageDroppedEventLog{");
        sb.append("link='").append(link).append('\'');
        sb.append(", seq=").append(seq);
        sb.append(", msg=").append(msg);
        sb.append('}');
        return sb.toString();
    }

    static List<MessageDroppedEventLog> eventLogs(
            TransactionResult txr, Address address, Predicate<MessageDroppedEventLog> filter) {
        return ScoreIntegrationTest.eventLogs(txr,
                MessageDroppedEventLog.SIGNATURE,
                address,
                MessageDroppedEventLog::new,
                filter);
    }
}

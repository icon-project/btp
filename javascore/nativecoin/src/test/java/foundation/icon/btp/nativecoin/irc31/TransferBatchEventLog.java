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

package foundation.icon.btp.nativecoin.irc31;


import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.IconJsonModule;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.test.ScoreIntegrationTest;
import foundation.icon.score.util.ArrayUtil;
import foundation.icon.score.util.StringUtil;
import score.Context;
import score.ObjectReader;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class TransferBatchEventLog {
    static final String SIGNATURE = "TransferBatch(Address,Address,Address,bytes,bytes)";
    private Address operator;
    private Address from;
    private Address to;
    private BigInteger[] ids;
    private BigInteger[] values;

    public TransferBatchEventLog(TransactionResult.EventLog el) {
        this.operator = new Address(el.getIndexed().get(1));
        this.from = new Address(el.getIndexed().get(2));
        this.to = new Address(el.getIndexed().get(3));
        this.ids = toBigIntegerArray(IconJsonModule.ByteArrayDeserializer.BYTE_ARRAY.convert(el.getData().get(0)));
        this.values = toBigIntegerArray(IconJsonModule.ByteArrayDeserializer.BYTE_ARRAY.convert(el.getData().get(1)));
    }

    private static BigInteger[] toBigIntegerArray(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        reader.beginList();
        List<BigInteger> list = new ArrayList<>();
        while(reader.hasNext()) {
            list.add(reader.readBigInteger());
        }
        reader.end();
        return ArrayUtil.toBigIntegerArray(list);
    }

    public Address getOperator() {
        return operator;
    }

    public Address getFrom() {
        return from;
    }

    public Address getTo() {
        return to;
    }

    public BigInteger[] getIds() {
        return ids;
    }

    public BigInteger[] getValues() {
        return values;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TransferBatchEventLog{");
        sb.append("operator=").append(operator);
        sb.append(", from=").append(from);
        sb.append(", to=").append(to);
        sb.append(", ids=").append(StringUtil.toString(ids));
        sb.append(", values=").append(StringUtil.toString(values));
        sb.append('}');
        return sb.toString();
    }

    public static List<TransferBatchEventLog> eventLogs(
            TransactionResult txr, Address address, Predicate<TransferBatchEventLog> filter) {
        return ScoreIntegrationTest.eventLogs(txr,
                TransferBatchEventLog.SIGNATURE,
                address,
                TransferBatchEventLog::new,
                filter);
    }
}

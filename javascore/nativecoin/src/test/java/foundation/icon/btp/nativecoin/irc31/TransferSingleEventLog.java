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

import java.math.BigInteger;
import java.util.List;
import java.util.function.Predicate;

public class TransferSingleEventLog {
    static final String SIGNATURE = "TransferSingle(Address,Address,Address,int,int)";
    private Address operator;
    private Address from;
    private Address to;
    private BigInteger id;
    private BigInteger value;

    public TransferSingleEventLog(TransactionResult.EventLog el) {
        this.operator = new Address(el.getIndexed().get(1));
        this.from = new Address(el.getIndexed().get(2));
        this.to = new Address(el.getIndexed().get(3));
        this.id = IconJsonModule.NumberDeserializer.BIG_INTEGER.convert(el.getData().get(0));
        this.value = IconJsonModule.NumberDeserializer.BIG_INTEGER.convert(el.getData().get(1));
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

    public BigInteger getId() {
        return id;
    }

    public BigInteger getValue() {
        return value;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TransferSingleEventLog{");
        sb.append("operator=").append(operator);
        sb.append(", from=").append(from);
        sb.append(", to=").append(to);
        sb.append(", id=").append(id);
        sb.append(", value=").append(value);
        sb.append('}');
        return sb.toString();
    }

    public static List<TransferSingleEventLog> eventLogs(
            TransactionResult txr, Address address, Predicate<TransferSingleEventLog> filter) {
        return ScoreIntegrationTest.eventLogs(txr,
                TransferSingleEventLog.SIGNATURE,
                address,
                TransferSingleEventLog::new,
                filter);
    }
}

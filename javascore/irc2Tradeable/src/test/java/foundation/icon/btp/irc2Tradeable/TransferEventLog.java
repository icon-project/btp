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

package foundation.icon.btp.irc2Tradeable;

import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.IconJsonModule;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.test.ScoreIntegrationTest;

import java.math.BigInteger;
import java.util.List;
import java.util.function.Predicate;

public class TransferEventLog {
    static final String SIGNATURE = "Transfer(Address,Address,int,bytes)";
    private Address from;
    private Address to;
    private BigInteger value;

    public TransferEventLog(TransactionResult.EventLog el) {
        this.from = new Address(el.getIndexed().get(1));
        this.to = new Address(el.getIndexed().get(2));
        this.value = IconJsonModule.NumberDeserializer.BIG_INTEGER.convert(el.getIndexed().get(3));
    }

    public Address getFrom() {
        return from;
    }

    public Address getTo() {
        return to;
    }

    public BigInteger getValue() {
        return value;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TransferEventLog{");
        sb.append(", from=").append(from);
        sb.append(", to=").append(to);
        sb.append(", value=").append(value);
        sb.append('}');
        return sb.toString();
    }

    public static List<TransferEventLog> eventLogs(
            TransactionResult txr, Address address, Predicate<TransferEventLog> filter) {
        return ScoreIntegrationTest.eventLogs(txr,
                TransferEventLog.SIGNATURE,
                address,
                TransferEventLog::new,
                filter);
    }
}

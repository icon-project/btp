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

public class ApprovalEventLog {
    static final String SIGNATURE = "Approval(Address,Address,int)";
    private Address owner;
    private Address spender;
    private BigInteger value;

    public ApprovalEventLog(TransactionResult.EventLog el) {
        this.owner = new Address(el.getIndexed().get(1));
        this.spender = new Address(el.getIndexed().get(2));
        this.value = IconJsonModule.NumberDeserializer.BIG_INTEGER.convert(el.getData().get(0));
    }

    public Address getOwner() {
        return owner;
    }

    public Address getSpender() {
        return spender;
    }

    public BigInteger getValue() {
        return value;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ApprovalForAllEventLog{");
        sb.append("owner=").append(owner);
        sb.append(", spender=").append(spender);
        sb.append(", value=").append(value);
        sb.append('}');
        return sb.toString();
    }

    public static List<ApprovalEventLog> eventLogs(
            TransactionResult txr, Address address, Predicate<ApprovalEventLog> filter) {
        return ScoreIntegrationTest.eventLogs(txr,
                ApprovalEventLog.SIGNATURE,
                address,
                ApprovalEventLog::new,
                filter);
    }
}

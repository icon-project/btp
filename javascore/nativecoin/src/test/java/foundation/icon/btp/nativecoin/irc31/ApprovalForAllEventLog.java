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

import java.util.List;
import java.util.function.Predicate;

public class ApprovalForAllEventLog {
    static final String SIGNATURE = "ApprovalForAll(Address,Address,bool)";
    private Address owner;
    private Address operator;
    private boolean approved;

    public ApprovalForAllEventLog(TransactionResult.EventLog el) {
        this.owner = new Address(el.getIndexed().get(1));
        this.operator = new Address(el.getIndexed().get(2));
        this.approved = IconJsonModule.BooleanDeserializer.BOOLEAN.convert(el.getData().get(0));
    }

    public Address getOwner() {
        return owner;
    }

    public Address getOperator() {
        return operator;
    }

    public boolean isApproved() {
        return approved;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ApprovalForAllEventLog{");
        sb.append("operator=").append(owner);
        sb.append(", from=").append(operator);
        sb.append(", approved=").append(approved);
        sb.append('}');
        return sb.toString();
    }

    public static List<ApprovalForAllEventLog> eventLogs(
            TransactionResult txr, Address address, Predicate<ApprovalForAllEventLog> filter) {
        return ScoreIntegrationTest.eventLogs(txr,
                ApprovalForAllEventLog.SIGNATURE,
                address,
                ApprovalForAllEventLog::new,
                filter);
    }
}

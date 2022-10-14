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


import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.IconJsonModule;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.test.ScoreIntegrationTest;

import java.math.BigInteger;
import java.util.List;
import java.util.function.Predicate;

public class ClaimRewardResultEventLog {
    static final String SIGNATURE = "ClaimRewardResult(Address,str,int,int)";
    private Address sender;
    private String network;
    private BigInteger nsn;
    private BigInteger result;

    public ClaimRewardResultEventLog(TransactionResult.EventLog el) {
        this.sender = new Address(el.getIndexed().get(1));
        this.network = el.getIndexed().get(2);
        this.nsn = IconJsonModule.NumberDeserializer.BIG_INTEGER.convert(el.getData().get(0));
        this.result = IconJsonModule.NumberDeserializer.BIG_INTEGER.convert(el.getData().get(1));
    }

    public Address getSender() {
        return sender;
    }

    public String getNetwork() {
        return network;
    }

    public BigInteger getNsn() {
        return nsn;
    }

    public BigInteger getResult() {
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ClaimRewardResultEventLog{");
        sb.append("sender=").append(sender);
        sb.append(", network='").append(network).append('\'');
        sb.append(", nsn=").append(nsn);
        sb.append(", result=").append(result);
        sb.append('}');
        return sb.toString();
    }

    public static List<ClaimRewardResultEventLog> eventLogs(
            TransactionResult txr, Address address, Predicate<ClaimRewardResultEventLog> filter) {
        return ScoreIntegrationTest.eventLogs(txr,
                ClaimRewardResultEventLog.SIGNATURE,
                address,
                ClaimRewardResultEventLog::new,
                filter);
    }
}

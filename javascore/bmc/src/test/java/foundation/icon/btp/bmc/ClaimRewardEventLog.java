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

public class ClaimRewardEventLog {
    static final String SIGNATURE = "ClaimReward(str,str,int,int,int)";
    private String network;
    private String receiver;
    private BigInteger amount;
    private BigInteger sn;
    private BigInteger nsn;

    public ClaimRewardEventLog(TransactionResult.EventLog el) {
        this.network = el.getData().get(0);
        this.receiver = el.getData().get(1);
        this.amount = IconJsonModule.NumberDeserializer.BIG_INTEGER.convert(el.getData().get(2));
        this.sn = IconJsonModule.NumberDeserializer.BIG_INTEGER.convert(el.getData().get(3));
        this.nsn = IconJsonModule.NumberDeserializer.BIG_INTEGER.convert(el.getData().get(4));
    }

    public String getNetwork() {
        return network;
    }

    public String getReceiver() {
        return receiver;
    }

    public BigInteger getAmount() {
        return amount;
    }

    public BigInteger getSn() {
        return sn;
    }

    public BigInteger getNsn() {
        return nsn;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ClaimRewardEventLog{");
        sb.append("network='").append(network).append('\'');
        sb.append(", receiver='").append(receiver).append('\'');
        sb.append(", amount=").append(amount);
        sb.append(", sn=").append(sn);
        sb.append(", nsn=").append(nsn);
        sb.append('}');
        return sb.toString();
    }

    public static List<ClaimRewardEventLog> eventLogs(
            TransactionResult txr, Address address, Predicate<ClaimRewardEventLog> filter) {
        return ScoreIntegrationTest.eventLogs(txr,
                ClaimRewardEventLog.SIGNATURE,
                address,
                ClaimRewardEventLog::new,
                filter);
    }
}

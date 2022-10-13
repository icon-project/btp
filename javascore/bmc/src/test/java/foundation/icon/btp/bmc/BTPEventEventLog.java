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

public class BTPEventEventLog {
    static final String SIGNATURE = "BTPEvent(str,int,str,str)";
    private String src;
    private BigInteger nsn;
    private String next;
    private String event;

    public BTPEventEventLog(TransactionResult.EventLog el) {
        this.src = el.getIndexed().get(1);
        this.nsn = IconJsonModule.NumberDeserializer.BIG_INTEGER.convert(el.getIndexed().get(2));
        this.next = el.getData().get(0);
        this.event = el.getData().get(1);
    }

    public String getSrc() {
        return src;
    }

    public BigInteger getNsn() {
        return nsn;
    }

    public String getNext() {
        return next;
    }

    public String getEvent() {
        return event;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BTPEventEventLog{");
        sb.append("src='").append(src).append('\'');
        sb.append(", nsn=").append(nsn);
        sb.append(", next='").append(next).append('\'');
        sb.append(", event='").append(event).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static List<BTPEventEventLog> eventLogs(
            TransactionResult txr, Address address, Predicate<BTPEventEventLog> filter) {
        return ScoreIntegrationTest.eventLogs(txr,
                BTPEventEventLog.SIGNATURE,
                address,
                BTPEventEventLog::new,
                filter);
    }
}

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

package foundation.icon.btp.nativecoin;

import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.IconJsonModule;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.test.ScoreIntegrationTest;
import foundation.icon.score.util.StringUtil;
import score.Context;
import score.ObjectReader;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class UnknownResponseEventLog {
    static final String SIGNATURE = "UnknownResponse(str,int)";
    private String from;
    private BigInteger sn;

    public UnknownResponseEventLog(TransactionResult.EventLog el) {
        from = el.getIndexed().get(1);
        sn = IconJsonModule.NumberDeserializer.BIG_INTEGER.convert(el.getData().get(0));
    }

    public String getFrom() {
        return from;
    }

    public BigInteger getSn() {
        return sn;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("UnknownResponseEventLog{");
        sb.append("from='").append(from).append('\'');
        sb.append(", sn=").append(sn);
        sb.append('}');
        return sb.toString();
    }

    public static List<UnknownResponseEventLog> eventLogs(
            TransactionResult txr, Address address, Predicate<UnknownResponseEventLog> filter) {
        return ScoreIntegrationTest.eventLogs(txr,
                UnknownResponseEventLog.SIGNATURE,
                address,
                UnknownResponseEventLog::new,
                filter);
    }
}

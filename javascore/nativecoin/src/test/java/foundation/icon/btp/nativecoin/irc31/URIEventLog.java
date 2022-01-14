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

public class URIEventLog {
    static final String SIGNATURE = "URI(int,str)";
    private BigInteger id;
    private String value;

    public URIEventLog(TransactionResult.EventLog el) {
        this.id = IconJsonModule.NumberDeserializer.BIG_INTEGER.convert(el.getIndexed().get(1));
        this.value = el.getData().get(0);
    }

    public BigInteger getId() {
        return id;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("URIEventLog{");
        sb.append("id=").append(id);
        sb.append(", value='").append(value).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public static List<URIEventLog> eventLogs(
            TransactionResult txr, Address address, Predicate<URIEventLog> filter) {
        return ScoreIntegrationTest.eventLogs(txr,
                URIEventLog.SIGNATURE,
                address,
                URIEventLog::new,
                filter);
    }
}

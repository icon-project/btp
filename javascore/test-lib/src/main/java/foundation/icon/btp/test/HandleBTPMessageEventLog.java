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

package foundation.icon.btp.test;

import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.IconJsonModule;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.test.ScoreIntegrationTest;
import foundation.icon.score.util.StringUtil;

import java.math.BigInteger;
import java.util.List;
import java.util.function.Predicate;

public class HandleBTPMessageEventLog {
    static final String SIGNATURE = "HandleBTPMessage(str,str,int,bytes)";
    private String from;
    private String svc;
    private BigInteger sn;
    private byte[] msg;

    public HandleBTPMessageEventLog(TransactionResult.EventLog el) {
        from = el.getData().get(0);
        svc = el.getData().get(1);
        sn = IconJsonModule.NumberDeserializer.BIG_INTEGER.convert(el.getData().get(2));
        msg = IconJsonModule.ByteArrayDeserializer.BYTE_ARRAY.convert(el.getData().get(3));
    }

    public String getFrom() {
        return from;
    }

    public String getSvc() {
        return svc;
    }

    public BigInteger getSn() {
        return sn;
    }

    public byte[] getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HandleBTPMessageEventLog{");
        sb.append("from='").append(from).append('\'');
        sb.append(", svc='").append(svc).append('\'');
        sb.append(", sn=").append(sn);
        sb.append(", msg=").append(StringUtil.toString(msg));
        sb.append('}');
        return sb.toString();
    }

    static List<HandleBTPMessageEventLog> eventLogs(
            TransactionResult txr, Address address, Predicate<HandleBTPMessageEventLog> filter) {
        return ScoreIntegrationTest.eventLogs(txr,
                HandleBTPMessageEventLog.SIGNATURE,
                address,
                HandleBTPMessageEventLog::new,
                filter);
    }
}

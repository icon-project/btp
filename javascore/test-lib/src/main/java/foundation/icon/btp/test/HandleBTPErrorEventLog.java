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

import java.math.BigInteger;
import java.util.List;
import java.util.function.Predicate;

public class HandleBTPErrorEventLog {
    static final String SIGNATURE = "HandleBTPError(str,str,int,int,str)";
    private String src;
    private String svc;
    private BigInteger sn;
    private long code;
    private String msg;

    public HandleBTPErrorEventLog(TransactionResult.EventLog el) {
        src = el.getData().get(0);
        svc = el.getData().get(1);
        sn = IconJsonModule.NumberDeserializer.BIG_INTEGER.convert(el.getData().get(2));
        code = IconJsonModule.NumberDeserializer.LONG.convert(el.getData().get(3));
        msg = el.getData().get(4);
    }

    public String getSrc() {
        return src;
    }

    public String getSvc() {
        return svc;
    }

    public BigInteger getSn() {
        return sn;
    }

    public long getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HandleBTPErrorEventLog{");
        sb.append("src='").append(src).append('\'');
        sb.append(", svc='").append(svc).append('\'');
        sb.append(", sn=").append(sn);
        sb.append(", code=").append(code);
        sb.append(", msg='").append(msg).append('\'');
        sb.append('}');
        return sb.toString();
    }

    static List<HandleBTPErrorEventLog> eventLogs(
            TransactionResult txr, Address address, Predicate<HandleBTPErrorEventLog> filter) {
        return ScoreIntegrationTest.eventLogs(txr,
                HandleBTPErrorEventLog.SIGNATURE,
                address,
                HandleBTPErrorEventLog::new,
                filter);
    }
}

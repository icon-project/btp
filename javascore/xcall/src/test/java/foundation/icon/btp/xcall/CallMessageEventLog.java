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

package foundation.icon.btp.xcall;

import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.IconJsonModule;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.test.ScoreIntegrationTest;

import java.math.BigInteger;
import java.util.List;
import java.util.function.Predicate;

public class CallMessageEventLog {
    private static final String SIGNATURE = "CallMessage(str,str,int,int,bytes)";
    private final String from;
    private final String to;
    private final BigInteger sn;
    private final BigInteger reqId;
    private final byte[] data;

    public CallMessageEventLog(TransactionResult.EventLog el) {
        this.from = el.getIndexed().get(1);
        this.to = el.getIndexed().get(2);
        this.sn = IconJsonModule.NumberDeserializer.BIG_INTEGER.convert(
                el.getIndexed().get(3));
        this.reqId = IconJsonModule.NumberDeserializer.BIG_INTEGER.convert(
                el.getData().get(0));
        this.data = IconJsonModule.ByteArrayDeserializer.BYTE_ARRAY.convert(
                el.getData().get(1));
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public BigInteger getSn() {
        return sn;
    }

    public BigInteger getReqId() {
        return reqId;
    }

    public byte[] getData() {
        return data;
    }

    public static List<CallMessageEventLog> eventLogs(TransactionResult txr,
                                                      Address address,
                                                      Predicate<CallMessageEventLog> filter) {
        return ScoreIntegrationTest.eventLogs(
                txr, CallMessageEventLog.SIGNATURE, address, CallMessageEventLog::new, filter);
    }
}

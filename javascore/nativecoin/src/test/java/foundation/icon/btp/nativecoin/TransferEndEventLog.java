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

import java.math.BigInteger;
import java.util.List;
import java.util.function.Predicate;

public class TransferEndEventLog {
    static final String SIGNATURE = "TransferEnd(Address,int,int,bytes)";
    private Address from;
    private BigInteger sn;
    private BigInteger code;
    private byte[] msg;

    public TransferEndEventLog(TransactionResult.EventLog el) {
        from = new Address(el.getIndexed().get(1));
        sn = IconJsonModule.NumberDeserializer.BIG_INTEGER.convert(el.getData().get(0));
        code = IconJsonModule.NumberDeserializer.BIG_INTEGER.convert(el.getData().get(1));
        msg = IconJsonModule.ByteArrayDeserializer.BYTE_ARRAY.convert(el.getData().get(2));
    }

    public Address getFrom() {
        return from;
    }

    public BigInteger getSn() {
        return sn;
    }

    public BigInteger getCode() {
        return code;
    }

    public byte[] getMsg() {
        return msg;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("TransferEndEventLog{");
        sb.append("from=").append(from);
        sb.append(", sn=").append(sn);
        sb.append(", code=").append(code);
        sb.append(", msg=").append(StringUtil.toString(msg));
        sb.append('}');
        return sb.toString();
    }

    public static List<TransferEndEventLog> eventLogs(
            TransactionResult txr, Address address, Predicate<TransferEndEventLog> filter) {
        return ScoreIntegrationTest.eventLogs(txr,
                TransferEndEventLog.SIGNATURE,
                address,
                TransferEndEventLog::new,
                filter);
    }
}

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

package foundation.icon.btp.arbcall;

import foundation.icon.jsonrpc.Address;
import foundation.icon.jsonrpc.IconJsonModule;
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.test.ScoreIntegrationTest;

import java.math.BigInteger;
import java.util.List;
import java.util.function.Predicate;

public class RollbackMessageEventLog {
    private static final String SIGNATURE = "RollbackMessage(int,bytes,str)";
    private final BigInteger sn;
    private final byte[] rollback;
    private final String reason;

    public RollbackMessageEventLog(TransactionResult.EventLog el) {
        this.sn = IconJsonModule.NumberDeserializer.BIG_INTEGER.convert(
                el.getIndexed().get(1));
        this.rollback = IconJsonModule.ByteArrayDeserializer.BYTE_ARRAY.convert(
                el.getData().get(0));
        this.reason = el.getData().get(1);
    }

    public BigInteger getSn() {
        return sn;
    }

    public byte[] getRollback() {
        return rollback;
    }

    public String getReason() {
        return reason;
    }

    public static List<RollbackMessageEventLog> eventLogs(TransactionResult txr,
                                                          Address address,
                                                          Predicate<RollbackMessageEventLog> filter) {
        return ScoreIntegrationTest.eventLogs(
                txr, RollbackMessageEventLog.SIGNATURE, address, RollbackMessageEventLog::new, filter);
    }
}

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

public class RollbackDataReceivedEventLog {
    private static final String SIGNATURE = "RollbackDataReceived(str,int,bytes)";
    private final String from;
    private final BigInteger ssn;
    private final byte[] rollback;

    public RollbackDataReceivedEventLog(TransactionResult.EventLog el) {
        this.from = el.getData().get(0);
        this.ssn = IconJsonModule.NumberDeserializer.BIG_INTEGER.convert(
                el.getData().get(1));
        this.rollback = IconJsonModule.ByteArrayDeserializer.BYTE_ARRAY.convert(
                el.getData().get(2));
    }

    public String getFrom() {
        return from;
    }

    public BigInteger getSsn() {
        return ssn;
    }

    public byte[] getRollback() {
        return rollback;
    }

    public static List<RollbackDataReceivedEventLog> eventLogs(TransactionResult txr,
                                                               Address address,
                                                               Predicate<RollbackDataReceivedEventLog> filter) {
        return ScoreIntegrationTest.eventLogs(
                txr, RollbackDataReceivedEventLog.SIGNATURE, address, RollbackDataReceivedEventLog::new, filter);
    }
}

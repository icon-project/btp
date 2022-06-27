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

public class CallRequestClearedEventLog {
    private static final String SIGNATURE = "CallRequestCleared(int)";
    private final BigInteger sn;

    public CallRequestClearedEventLog(TransactionResult.EventLog el) {
        this.sn = IconJsonModule.NumberDeserializer.BIG_INTEGER.convert(
                el.getIndexed().get(1));
    }

    public BigInteger getSn() {
        return sn;
    }

    public static List<CallRequestClearedEventLog> eventLogs(TransactionResult txr,
                                                             Address address,
                                                             Predicate<CallRequestClearedEventLog> filter) {
        return ScoreIntegrationTest.eventLogs(
                txr, CallRequestClearedEventLog.SIGNATURE, address, CallRequestClearedEventLog::new, filter);
    }
}

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

public class FixedFeeEventLog {
    private static final String SIGNATURE = "FixedFeeUpdated(str,int)";
    private final String net;
    private final BigInteger fee;

    public FixedFeeEventLog(TransactionResult.EventLog el) {
        this.net = el.getIndexed().get(1);
        this.fee = IconJsonModule.NumberDeserializer.BIG_INTEGER.convert(
                el.getData().get(0));
    }

    public String getNet() {
        return net;
    }

    public BigInteger getFee() {
        return fee;
    }

    public static List<FixedFeeEventLog> eventLogs(TransactionResult txr,
                                                   Address address,
                                                   Predicate<FixedFeeEventLog> filter) {
        return ScoreIntegrationTest.eventLogs(
                txr, FixedFeeEventLog.SIGNATURE, address, FixedFeeEventLog::new, filter);
    }
}

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
import foundation.icon.jsonrpc.model.TransactionResult;
import foundation.icon.score.test.ScoreIntegrationTest;

import java.util.List;
import java.util.function.Predicate;

public class HandleFeeGatheringEventLog {
    static final String SIGNATURE = "HandleFeeGathering(str,str)";
    private String fa;
    private String svc;

    public HandleFeeGatheringEventLog(TransactionResult.EventLog el) {
        fa = el.getData().get(0);
        svc = el.getData().get(1);
    }

    public String getFa() {
        return fa;
    }

    public String getSvc() {
        return svc;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("HandleFeeGatheringEventLog{");
        sb.append("fa='").append(fa).append('\'');
        sb.append(", svc='").append(svc).append('\'');
        sb.append('}');
        return sb.toString();
    }

    static List<HandleFeeGatheringEventLog> eventLogs(
            TransactionResult txr, Address address, Predicate<HandleFeeGatheringEventLog> filter) {
        return ScoreIntegrationTest.eventLogs(txr,
                HandleFeeGatheringEventLog.SIGNATURE,
                address,
                HandleFeeGatheringEventLog::new,
                filter);
    }
}

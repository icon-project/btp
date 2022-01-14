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

package foundation.icon.btp.mock;

import foundation.icon.btp.lib.BMCScoreInterface;
import foundation.icon.score.util.Logger;
import score.Address;
import score.annotation.EventLog;
import score.annotation.External;

import java.math.BigInteger;

public class MockBSHImpl implements MockBSH {
    private static final Logger logger = Logger.getLogger(MockBSHImpl.class);

    public MockBSHImpl() {
    }

    @External
    public void handleBTPMessage(String _from, String _svc, BigInteger _sn, byte[] _msg) {
        HandleBTPMessage(_from, _svc, _sn, _msg);
    }

    @External
    public void handleBTPError(String _src, String _svc, BigInteger _sn, long _code, String _msg) {
        HandleBTPError(_src, _svc, _sn, _code, _msg);
    }

    @External
    public void handleFeeGathering(String _fa, String _svc) {
        HandleFeeGathering(_fa, _svc);
    }

    @External
    public void intercallSendMessage(Address _bmc, String _to, String _svc, BigInteger _sn, byte[] _msg) {
        BMCScoreInterface bmc = new BMCScoreInterface(_bmc);
        bmc.sendMessage(_to, _svc, _sn, _msg);
    }

    @EventLog
    public void HandleBTPMessage(String _from, String _svc, BigInteger _sn, byte[] _msg) { }

    @EventLog
    public void HandleBTPError(String _src, String _svc, BigInteger _sn, long _code, String _msg) { }

    @EventLog
    public void HandleFeeGathering(String _fa, String _svc) { }
}

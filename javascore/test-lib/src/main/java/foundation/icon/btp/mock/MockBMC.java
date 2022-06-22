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

import foundation.icon.score.client.ScoreClient;
import score.Address;
import score.annotation.EventLog;
import score.annotation.External;

import java.math.BigInteger;

/**
 * for BMV, BSH
 */
@ScoreClient
public interface MockBMC {
    @External
    void setNet(String _net);

    @External(readonly = true)
    String getNet();

    //for BMV
    @External
    void intercallHandleRelayMessage(Address _addr, String _prev, BigInteger _seq, byte[] _msg);

    @EventLog
    void HandleRelayMessage(byte[] _ret);

    //for BSH
    @External(readonly = true)
    String getBtpAddress();

    @External
    void sendMessage(String _to, String _svc, BigInteger _sn, byte[] _msg);

    @EventLog
    void SendMessage(String _to, String _svc, BigInteger _sn, byte[] _msg);

    @External
    void intercallHandleBTPMessage(Address _addr, String _from, String _svc, BigInteger _sn, byte[] _msg);

    @External
    void intercallHandleBTPError(Address _addr, String _src, String _svc, BigInteger _sn, long _code, String _msg);

    @External
    void intercallHandleFeeGathering(Address _addr, String _fa, String _svc);

}

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

import foundation.icon.btp.lib.*;
import score.Address;
import score.Context;
import score.UserRevertedException;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

import java.math.BigInteger;

public class MockBMCImpl implements MockBMC {

    private VarDB<String> net = Context.newVarDB("net", String.class);
    private String btpAddress;

    public MockBMCImpl(@Optional String _net) {
        if (_net != null) {
            setNet(_net);
        }
    }

    @External
    public void setNet(String _net) {
        net.set(_net);
        btpAddress = new BTPAddress(
                BTPAddress.PROTOCOL_BTP, _net, Context.getAddress().toString()).toString();
    }

    @External(readonly = true)
    public String getNet() {
        return net.get();
    }

    @External
    public void intercallHandleRelayMessage(Address _addr, String _prev, BigInteger _seq, byte[] _msg) {
        BMVScoreInterface bmv = new BMVScoreInterface(_addr);
        try {
            byte[][] ret = bmv.handleRelayMessage(btpAddress, _prev, _seq, _msg);
            HandleRelayMessage(MockRelayMessage.toBytes(ret));
        } catch (UserRevertedException e) {
            throw BTPException.of(e);
        }
    }

    @EventLog
    public void HandleRelayMessage(byte[] _ret) { }

    @External
    public void sendMessage(String _to, String _svc, BigInteger _sn, byte[] _msg) {
        SendMessage(_to, _svc, _sn, _msg);
    }

    @EventLog
    public void SendMessage(String _to, String _svc, BigInteger _sn, byte[] _msg) { }

    @External(readonly = true)
    public String getBtpAddress() {
        return btpAddress;
    }

    @External
    public void intercallHandleBTPMessage(Address _addr, String _from, String _svc, BigInteger _sn, byte[] _msg) {
        BSHScoreInterface bsh = new BSHScoreInterface(_addr);
        bsh.handleBTPMessage(_from, _svc, _sn, _msg);
    }

    @External
    public void intercallHandleBTPError(Address _addr, String _src, String _svc, BigInteger _sn, long _code, String _msg) {
        BSHScoreInterface bsh = new BSHScoreInterface(_addr);
        bsh.handleBTPError(_src, _svc, _sn, _code, _msg);
    }

    @External
    public void intercallHandleFeeGathering(Address _addr, String _fa, String _svc) {
        BSHScoreInterface bsh = new BSHScoreInterface(_addr);
        bsh.handleFeeGathering(_fa, _svc);
    }

    @External(readonly = true)
    public BMVStatus intercallGetStatus(Address _addr) {
        BMVScoreInterface bmv = new BMVScoreInterface(_addr);
        return bmv.getStatus();
    }
}

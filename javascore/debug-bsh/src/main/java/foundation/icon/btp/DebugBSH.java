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
package foundation.icon.btp;

import foundation.icon.btp.lib.BTPException;
import score.Address;
import score.Context;
import score.UserRevertedException;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;

import java.math.BigInteger;

public class DebugBSH {
    private final VarDB<BigInteger> serialNoDB = Context.newVarDB("serialNo", BigInteger.class);
    private final VarDB<Address> bmcDB = Context.newVarDB("bmc", Address.class);
    private final VarDB<String> svcDB = Context.newVarDB("svc", String.class);

    public DebugBSH(String _svc, Address _bmc) {
        serialNoDB.set(BigInteger.ZERO);
        bmcDB.set(_bmc);
        svcDB.set(_svc);
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
    public void sendMessage(Address _bmc, String _to, byte[] _msg) {
        var sn = generateSerialNumber();
        var svc = svcDB.get();
        sendRawMessage(_bmc, _to, svc, sn, _msg);
    }

    @External
    public void sendRawMessage(Address _bmc, String _to, String _svc, BigInteger _sn, byte[] _msg) {
        BMCScore bmc = new BMCScore(_bmc);
        bmc.sendMessage(_to, _svc, _sn, _msg);
    }

    private BigInteger generateSerialNumber() {
        BigInteger newSnNo = serialNoDB.getOrDefault(BigInteger.ZERO).add(BigInteger.ONE);
        serialNoDB.set(newSnNo);
        return newSnNo;
    }

    @EventLog
    public void HandleBTPMessage(String _from, String _svc, BigInteger _sn, byte[] _msg) { }

    @EventLog
    public void HandleBTPError(String _src, String _svc, BigInteger _sn, long _code, String _msg) { }

    @EventLog
    public void HandleFeeGathering(String _fa, String _svc) { }
}

class BMCScore {
    private Address address;

    BMCScore(Address address) {
        this.address = address;
    }

    void sendMessage(String _to, String _svc, BigInteger _sn, byte[] _msg) {
        try {
            Context.call(address, "sendMessage", _to, _svc, _sn, _msg);
        } catch (UserRevertedException e) {
            throw BTPException.of(e);
        }
    }
}
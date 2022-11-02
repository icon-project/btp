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
import score.ArrayDB;
import score.Context;
import score.UserRevertedException;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;

import java.math.BigInteger;

public class MockBMCImpl implements MockBMC {
    public static final String DEFAULT_NET = "0x1.icon";
    private final VarDB<String> net = Context.newVarDB("net", String.class);
    private final VarDB<BigInteger> nsn = Context.newVarDB("nsn", BigInteger.class);
    private final VarDB<BigInteger> forward = Context.newVarDB("forward", BigInteger.class);
    private final VarDB<BigInteger> backward = Context.newVarDB("backward", BigInteger.class);
    private final ArrayDB<String> responseList = Context.newArrayDB("responseList", String.class);

    public MockBMCImpl(@Optional String _net) {
        if (_net != null) {
            setNetworkAddress(_net);
        }
    }

    @External
    public void setNetworkAddress(String _net) {
        net.set(_net);
    }

    @External(readonly = true)
    public String getNetworkAddress() {
        return net.getOrDefault(DEFAULT_NET);
    }

    @External
    public void handleRelayMessage(Address _addr, String _prev, BigInteger _seq, byte[] _msg) {
        BMVScoreInterface bmv = new BMVScoreInterface(_addr);
        try {
            byte[][] ret = bmv.handleRelayMessage(getBtpAddress(), _prev, _seq, _msg);
            HandleRelayMessage(MockRelayMessage.toBytes(ret));
        } catch (UserRevertedException e) {
            throw BTPException.of(e);
        }
    }

    @EventLog
    public void HandleRelayMessage(byte[] _ret) {
    }

    @External(readonly = true)
    public String getBtpAddress() {
        return new BTPAddress(
                BTPAddress.PROTOCOL_BTP, getNetworkAddress(), Context.getAddress().toString()).toString();
    }

    private void require(boolean condition, String message) {
        if (!condition) {
            throw new BTPException.BMC(0, message);
        }
    }

    @Payable
    @External
    public BigInteger sendMessage(String _to, String _svc, BigInteger _sn, byte[] _msg) {
        int snCompare = _sn.compareTo(BigInteger.ZERO);
        BigInteger fee = getFee(_to, snCompare > 0);
        if (snCompare < 0) {
            require(removeResponse(toResponse(_to, _svc, _sn.negate())),
                    "not exists response");
            fee = BigInteger.ZERO;
            //response is one way message;
            _sn = BigInteger.ZERO;

        }
        require(Context.getValue().compareTo(fee) >= 0, "not enough fee");
        BigInteger nextNsn = getNetworkSn().add(BigInteger.ONE);
        nsn.set(nextNsn);
        SendMessage(nextNsn, _to, _svc, _sn, _msg);
        return nextNsn;
    }

    @EventLog(indexed = 1)
    public void SendMessage(BigInteger _nsn, String _to, String _svc, BigInteger _sn, byte[] _msg) {
    }

    @External(readonly = true)
    public BigInteger getNetworkSn() {
        return nsn.getOrDefault(BigInteger.ZERO);
    }

    private String toResponse(String _to, String _svc, BigInteger _sn) {
        return _to + _svc + _sn;
    }

    @External
    public void addResponse(String _to, String _svc, BigInteger _sn) {
        Context.require(_sn.compareTo(BigInteger.ZERO) > 0,
                "_sn should be positive");
        String response = toResponse(_to, _svc, _sn);
        if (getResponseIndex(response) < 0) {
            responseList.add(response);
        }
    }

    private boolean removeResponse(String response) {
        int idx = getResponseIndex(response);
        if (idx >= 0) {
            String last = responseList.pop();
            if (idx < responseList.size()) {
                responseList.set(idx, last);
            }
            return true;
        }
        return false;
    }

    private int getResponseIndex(String response) {
        for (int i = 0; i < responseList.size(); i++) {
            if (responseList.get(i).equals(response)) {
                return i;
            }
        }
        return -1;
    }

    @External(readonly = true)
    public boolean hasResponse(String _to, String _svc, BigInteger _sn) {
        return getResponseIndex(toResponse(_to, _svc, _sn)) >= 0;
    }

    @External
    public void clearResponse() {
        int size = responseList.size();
        for (int i = 0; i < size; i++) {
            responseList.removeLast();
        }
    }

    @External
    public void setFee(BigInteger _forward, BigInteger _backward) {
        forward.set(_forward);
        backward.set(_backward);
    }

    @External(readonly = true)
    public BigInteger getFee(String _to, boolean _response) {
        BigInteger fee = forward.getOrDefault(BigInteger.ZERO);
        if (_response) {
            fee = fee.add(backward.getOrDefault(BigInteger.ZERO));
        }
        return fee;
    }

    @External
    public void handleBTPMessage(Address _addr, String _from, String _svc, BigInteger _sn, byte[] _msg) {
        if (_sn.compareTo(BigInteger.ZERO) > 0) {
            addResponse(_from, _svc, _sn);
        }
        BSHScoreInterface bsh = new BSHScoreInterface(_addr);
        bsh.handleBTPMessage(_from, _svc, _sn, _msg);
    }

    @External
    public void handleBTPError(Address _addr, String _src, String _svc, BigInteger _sn, long _code, String _msg) {
        BSHScoreInterface bsh = new BSHScoreInterface(_addr);
        bsh.handleBTPError(_src, _svc, _sn, _code, _msg);
    }

}

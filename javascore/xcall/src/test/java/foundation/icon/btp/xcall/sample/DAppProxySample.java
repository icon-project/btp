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

package foundation.icon.btp.xcall.sample;

import foundation.icon.btp.xcall.CallServiceReceiver;
import foundation.icon.score.client.ScoreClient;
import score.Address;
import score.Context;
import score.DictDB;
import score.UserRevertedException;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;

import java.math.BigInteger;

@ScoreClient
public class DAppProxySample implements CallServiceReceiver {
    private final Address callSvc;
    private final String callSvcBtpAddr;
    private final VarDB<BigInteger> id = Context.newVarDB("id", BigInteger.class);
    private final DictDB<BigInteger, RollbackData> rollbacks = Context.newDictDB("rollbacks", RollbackData.class);

    public DAppProxySample(Address _callService) {
        this.callSvc = _callService;
        this.callSvcBtpAddr = Context.call(String.class, this.callSvc, "getBtpAddress");
    }

    private void onlyCallService() {
        Context.require(Context.getCaller().equals(this.callSvc), "onlyCallService");
    }

    private BigInteger getNextId() {
        BigInteger _id = this.id.getOrDefault(BigInteger.ZERO);
        _id = _id.add(BigInteger.ONE);
        this.id.set(_id);
        return _id;
    }

    @Payable
    @External
    public void sendMessage(String _to, byte[] _data, @Optional byte[] _rollback) {
        if (_rollback != null) {
            // The code below is not actually necessary because the _rollback data is stored on the xCall side,
            // but in this example, it is needed for testing to compare the _rollback data later.
            var id = getNextId();
            Context.println("DAppProxy: store rollback data with id=" + id);
            RollbackData rbData = new RollbackData(id, _rollback);
            var ssn = _sendCallMessage(Context.getValue(), _to, _data, rbData.toBytes());
            rbData.setSvcSn(ssn);
            rollbacks.set(id, rbData);
        } else {
            // This is for one-way message
            _sendCallMessage(Context.getValue(), _to, _data, null);
        }
    }

    private BigInteger _sendCallMessage(BigInteger value, String to, byte[] data, byte[] rollback) {
        try {
            return Context.call(BigInteger.class, value, this.callSvc, "sendCallMessage", to, data, rollback);
        } catch (UserRevertedException e) {
            // propagate the error code to the caller
            Context.revert(e.getCode(), "UserReverted");
            return BigInteger.ZERO; // call flow does not reach here, but make compiler happy
        }
    }

    @Override
    @External
    public void handleCallMessage(String _from, byte[] _data) {
        onlyCallService();
        Context.println("handleCallMessage: from=" + _from + ", data=" + new String(_data));
        if (callSvcBtpAddr.equals(_from)) {
            // handle rollback data here
            // In this example, just compare it with the stored one.
            RollbackData received = RollbackData.fromBytes(_data);
            var id = received.getId();
            RollbackData stored = rollbacks.get(id);
            Context.require(stored != null, "invalid received id");
            Context.require(received.equals(stored), "rollbackData mismatch");
            rollbacks.set(id, null); // cleanup
            RollbackDataReceived(_from, stored.getSvcSn(), received.getRollback());
        } else {
            // normal message delivery
            MessageReceived(_from, _data);
        }
    }

    @EventLog
    public void MessageReceived(String _from, byte[] _data) {}

    @EventLog
    public void RollbackDataReceived(String _from, BigInteger _ssn, byte[] _rollback) {}
}

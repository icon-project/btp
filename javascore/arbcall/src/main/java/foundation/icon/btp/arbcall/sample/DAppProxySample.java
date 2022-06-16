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

package foundation.icon.btp.arbcall.sample;

import foundation.icon.btp.arbcall.CallRequest;
import foundation.icon.btp.arbcall.CallServiceReceiver;
import foundation.icon.btp.arbcall.ProxyRequest;
import score.Address;
import score.Context;
import score.DictDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;

import java.math.BigInteger;

public class DAppProxySample implements CallServiceReceiver {
    private final Address callSvc;
    private final DictDB<BigInteger, CallRequest> requests = Context.newDictDB("requests", CallRequest.class);
    private final DictDB<BigInteger, ProxyRequest> proxyReqs = Context.newDictDB("proxyReqs", ProxyRequest.class);

    public DAppProxySample(Address _callService) {
        this.callSvc = _callService;
    }

    @Payable
    @External
    public void sendMessage(String _to, byte[] _data, @Optional byte[] _rollback) {
        var sn = _sendCallMessage(Context.getValue(), _to, _data, _rollback);
        CallRequest req = new CallRequest(Context.getCaller(), _to, _rollback);
        ProxyRequest preq = new ProxyRequest("from", _to, sn, _data);
        requests.set(sn, req);
        proxyReqs.set(sn, preq);
    }

    private BigInteger _sendCallMessage(BigInteger value, String to, byte[] data, byte[] rollback) {
        return Context.call(BigInteger.class, value, this.callSvc, "sendCallMessage", to, data, rollback);
    }

    @Override
    @External
    public void handleCallMessage(String _from, byte[] _data) {
        MessageReceived(_from, _data);
        Context.println("handleCallMessage: from=" + _from + ", data=" + new String(_data));
    }

    @EventLog
    public void MessageReceived(String _from, byte[] _data) {}
}

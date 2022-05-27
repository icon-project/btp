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

import foundation.icon.btp.lib.BMCScoreInterface;
import foundation.icon.btp.lib.BSH;
import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.score.util.Logger;
import score.Address;
import score.Context;
import score.DictDB;
import score.RevertedException;
import score.UserRevertedException;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;

import java.math.BigInteger;

public class CallServiceImpl implements BSH, CallService {
    private static final Logger logger = Logger.getLogger(CallServiceImpl.class);
    private static final String SERVICE = "arbcall";

    private final Address bmc;
    private final String net;
    private final VarDB<CSProperties> props = Context.newVarDB("properties", CSProperties.class);
    private final DictDB<BigInteger, CallRequest> requests = Context.newDictDB("requests", CallRequest.class);
    private final DictDB<BigInteger, ProxyRequest> proxyReqs = Context.newDictDB("proxyReqs", ProxyRequest.class);

    public CallServiceImpl(Address _bmc) {
        this.bmc = _bmc;
        BMCScoreInterface bmcInterface = new BMCScoreInterface(this.bmc);
        BTPAddress btpAddress = BTPAddress.valueOf(bmcInterface.getBtpAddress());
        this.net = btpAddress.net();
    }

    public CSProperties getProperties() {
        return props.getOrDefault(new CSProperties());
    }

    public void setProperties(CSProperties props) {
        this.props.set(props);
    }

    private BigInteger getNextSn() {
        CSProperties props = getProperties();
        props.setSn(props.getSn().add(BigInteger.ONE));
        setProperties(props);
        return props.getSn();
    }

    private BigInteger getNextReqId() {
        CSProperties props = getProperties();
        props.setReqId(props.getReqId().add(BigInteger.ONE));
        setProperties(props);
        return props.getReqId();
    }

    @Override
    @External
    public BigInteger sendCallMessage(String _to, byte[] _data, byte[] _rollback) {
        Address caller = Context.getCaller();
        Context.require(caller.isContract(), "SenderNotAContract");
        BTPAddress dst = BTPAddress.valueOf(_to);

        BigInteger sn = getNextSn();
        CallRequest req = new CallRequest(caller.toString(), dst.toString(), _rollback);
        requests.set(sn, req);

        CSMessageRequest msgReq = new CSMessageRequest(caller.toString(), dst.account(), _data);
        sendBTPMessage(dst.net(), CSMessage.REQUEST, sn, msgReq.toBytes());
        return sn;
    }

    @Override
    @External
    public void executeCall(BigInteger _reqId) {
        var req = proxyReqs.get(_reqId);
        Context.require(req != null, "InvalidRequestId");

        BTPAddress from = BTPAddress.valueOf(req.getFrom());
        CSMessageResponse msgRes = null;
        try {
            DAppProxy proxy = new DAppProxy(Address.fromString(req.getTo()));
            proxy.handleCallMessage(req.getFrom(), req.getData());
            msgRes = new CSMessageResponse(req.getSn(), CSMessageResponse.SUCCESS, null);
        } catch (UserRevertedException e) {
            logger.println("executeCall", "code:", e.getCode(), "msg:", e.getMessage());
            msgRes = new CSMessageResponse(req.getSn(), e.getCode(), e.getMessage());
        } catch (IllegalArgumentException | RevertedException e) {
            logger.println("executeCall", "Exception:", e.toString());
            msgRes = new CSMessageResponse(req.getSn(), CSMessageResponse.FAILURE, e.getMessage());
        } finally {
            // cleanup
            proxyReqs.set(_reqId, null);
            // send response
            BigInteger sn = getNextSn();
            sendBTPMessage(from.net(), CSMessage.RESPONSE, sn, msgRes.toBytes());
        }
    }

    @Override
    @External
    public void executeRollback(BigInteger _sn) {
    }

    @Override
    @EventLog(indexed=3)
    public void CallMessage(String _from, String _to, BigInteger _sn, BigInteger _reqId, byte[] _data) {}

    @Override
    @EventLog(indexed=1)
    public void RollbackMessage(BigInteger _sn, byte[] _rollback) {}

    /* ========== Interfaces with BMC ========== */
    @Override
    @External
    public void handleBTPMessage(String _from, String _svc, BigInteger _sn, byte[] _msg) {
        Context.require(Context.getCaller().equals(bmc), "Only BMC");
        Context.require(SERVICE.equals(_svc), "InvalidServiceName");

        CSMessage msg = CSMessage.fromBytes(_msg);
        switch (msg.getType()) {
            case CSMessage.REQUEST:
                handleRequest(_from, _sn, msg.getData());
                break;
            case CSMessage.RESPONSE:
                handleResponse(_from, _sn, msg.getData());
                break;
            default:
                Context.revert("UnknownMsgType(" + msg.getType() + ")");
        }
    }

    @Override
    @External
    public void handleBTPError(String _src, String _svc, BigInteger _sn, long _code, String _msg) {
        Context.require(Context.getCaller().equals(bmc), "Only BMC");
    }

    @Override
    @External
    public void handleFeeGathering(String _fa, String _svc) {
        Context.require(Context.getCaller().equals(bmc), "Only BMC");
    }
    /* ========================================= */

    private void sendBTPMessage(String netTo, int msgType, BigInteger sn, byte[] data) {
        CSMessage msg = new CSMessage(msgType, data);
        BMCScoreInterface bmc = new BMCScoreInterface(this.bmc);
        bmc.sendMessage(netTo, SERVICE, sn, msg.toBytes());
    }

    private void handleRequest(String netFrom, BigInteger sn, byte[] data) {
        CSMessageRequest msgReq = CSMessageRequest.fromBytes(data);
        BTPAddress from = new BTPAddress(netFrom, msgReq.getFrom());
        String to = msgReq.getTo();

        BigInteger reqId = getNextReqId();
        ProxyRequest req = new ProxyRequest(from.toString(), to, sn, msgReq.getData());
        proxyReqs.set(reqId, req);

        // emit event to notify the user
        CallMessage(from.toString(), to, sn, reqId, msgReq.getData());
    }

    private void handleResponse(String netFrom, BigInteger sn, byte[] data) {
        CSMessageResponse msgRes = CSMessageResponse.fromBytes(data);
        var req = requests.get(msgRes.getSn());
        if (req == null) {
            logger.println("handleResponse", "No request for", msgRes.getSn());
            return; // just ignore
        }
        switch (msgRes.getCode()) {
            case CSMessageResponse.SUCCESS:
                // cleanup request state
                requests.set(msgRes.getSn(), null);
                break;
            case CSMessageResponse.FAILURE:
            default:
                logger.println("handleResponse", "code:", msgRes.getCode(), "msg:", msgRes.getMsg());
                // emit rollback event
                RollbackMessage(msgRes.getSn(), req.getRollback());
        }
    }
}

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
import score.annotation.Optional;

import java.math.BigInteger;

public class CallServiceImpl implements BSH, CallService {
    private static final Logger logger = Logger.getLogger(CallServiceImpl.class);
    public static final String SERVICE = "arbcall";

    private final VarDB<Address> bmc = Context.newVarDB("bmc", Address.class);
    private final VarDB<String> net = Context.newVarDB("net", String.class);
    private final VarDB<BigInteger> sn = Context.newVarDB("sn", BigInteger.class);
    private final VarDB<BigInteger> reqId = Context.newVarDB("reqId", BigInteger.class);

    private final DictDB<BigInteger, CallRequest> requests = Context.newDictDB("requests", CallRequest.class);
    private final DictDB<BigInteger, ProxyRequest> proxyReqs = Context.newDictDB("proxyReqs", ProxyRequest.class);

    public CallServiceImpl(Address _bmc) {
        // set bmc address only for the first deploy
        if (bmc.get() == null) {
            bmc.set(_bmc);
            BMCScoreInterface bmcInterface = new BMCScoreInterface(_bmc);
            BTPAddress btpAddress = BTPAddress.valueOf(bmcInterface.getBtpAddress());
            net.set(btpAddress.net());
        }
    }

    private void onlyBMC() {
        Context.require(Context.getCaller().equals(bmc.get()), "Only BMC");
    }

    private void checkService(String _svc) {
        Context.require(SERVICE.equals(_svc), "InvalidServiceName");
    }

    private BigInteger getNextSn() {
        BigInteger _sn = this.sn.getOrDefault(BigInteger.ZERO);
        _sn = _sn.add(BigInteger.ONE);
        this.sn.set(_sn);
        return _sn;
    }

    private BigInteger getNextReqId() {
        BigInteger _reqId = this.reqId.getOrDefault(BigInteger.ZERO);
        _reqId = _reqId.add(BigInteger.ONE);
        this.reqId.set(_reqId);
        return _reqId;
    }

    private void cleanupCallRequest(BigInteger sn) {
        requests.set(sn, null);
        CallRequestCleared(sn);
    }

    @Override
    @External
    public BigInteger sendCallMessage(String _to, byte[] _data, @Optional byte[] _rollback) {
        Address caller = Context.getCaller();
        Context.require(caller.isContract(), "SenderNotAContract");
        BTPAddress dst = BTPAddress.valueOf(_to);

        BigInteger sn = getNextSn();
        CallRequest req = new CallRequest(caller, dst.toString(), _rollback);
        requests.set(sn, req);

        CSMessageRequest msgReq = new CSMessageRequest(caller.toString(), dst.account(), _data);
        sendBTPMessage(dst.net(), CSMessage.REQUEST, sn, msgReq.toBytes());
        return sn;
    }

    @Override
    @External
    public void executeCall(BigInteger _reqId) {
        ProxyRequest req = proxyReqs.get(_reqId);
        Context.require(req != null, "InvalidRequestId");

        BTPAddress from = BTPAddress.valueOf(req.getFrom());
        CSMessageResponse msgRes = null;
        try {
            DAppProxy proxy = new DAppProxy(Address.fromString(req.getTo()));
            proxy.handleCallMessage(req.getFrom(), req.getData());
            msgRes = new CSMessageResponse(req.getSn(), CSMessageResponse.SUCCESS, null);
        } catch (UserRevertedException e) {
            int code = e.getCode() == 0 ? CSMessageResponse.FAILURE : e.getCode();
            String msg = "UserReverted(" + code + ")";
            logger.println("executeCall", "code:", code, "msg:", msg);
            msgRes = new CSMessageResponse(req.getSn(), code, msg);
        } catch (IllegalArgumentException | RevertedException e) {
            logger.println("executeCall", "Exception:", e.toString(), "msg:", e.getMessage());
            msgRes = new CSMessageResponse(req.getSn(), CSMessageResponse.FAILURE, e.toString());
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
        CallRequest req = requests.get(_sn);
        Context.require(req != null, "InvalidSerialNum");
        Context.require(req.enabled(), "RollbackNotEnabled");
        try {
            BTPAddress callSvc = new BTPAddress(net.get(), Context.getAddress().toString());
            DAppProxy proxy = new DAppProxy(req.getFrom());
            proxy.handleCallMessage(callSvc.toString(), req.getRollback());
        } catch (Exception e) {
            logger.println("executeRollback", "Exception:", e.toString());
        } finally {
            cleanupCallRequest(_sn);
        }
    }

    @Override
    @EventLog(indexed=3)
    public void CallMessage(String _from, String _to, BigInteger _sn, BigInteger _reqId, byte[] _data) {}

    @Override
    @EventLog(indexed=1)
    public void RollbackMessage(BigInteger _sn, byte[] _rollback, String _reason) {}

    /* Implementation-specific eventlog */
    @EventLog(indexed=1)
    private void CallRequestCleared(BigInteger _sn) {}

    /* ========== Interfaces with BMC ========== */
    @Override
    @External
    public void handleBTPMessage(String _from, String _svc, BigInteger _sn, byte[] _msg) {
        onlyBMC();
        checkService(_svc);

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
        onlyBMC();
        checkService(_svc);

        String errMsg = "BTPError{code=" + _code + ", msg=" + _msg + "}";
        CSMessageResponse res = new CSMessageResponse(_sn, CSMessageResponse.BTP_ERROR, errMsg);
        handleResponse(_src, _sn, res.toBytes());
    }

    @Override
    @External
    public void handleFeeGathering(String _fa, String _svc) {
        onlyBMC();
        checkService(_svc);
    }
    /* ========================================= */

    private void sendBTPMessage(String netTo, int msgType, BigInteger sn, byte[] data) {
        CSMessage msg = new CSMessage(msgType, data);
        BMCScoreInterface bmc = new BMCScoreInterface(this.bmc.get());
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
        BigInteger resSn = msgRes.getSn();
        CallRequest req = requests.get(resSn);
        if (req == null) {
            logger.println("handleResponse", "No request for", resSn);
            return; // just ignore
        }
        switch (msgRes.getCode()) {
            case CSMessageResponse.SUCCESS:
                cleanupCallRequest(resSn);
                break;
            case CSMessageResponse.FAILURE:
            case CSMessageResponse.BTP_ERROR:
            default:
                logger.println("handleResponse", "code:", msgRes.getCode(), "msg:", msgRes.getMsg());
                // emit rollback event
                if (req.getRollback() != null) {
                    req.setEnabled();
                    requests.set(resSn, req);
                    RollbackMessage(resSn, req.getRollback(), msgRes.getMsg());
                } else {
                    // ignore the failure response since no rollback data
                    cleanupCallRequest(resSn);
                }
        }
    }
}

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

package foundation.icon.btp.xcall;

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
import score.annotation.Payable;

import java.math.BigInteger;

public class CallServiceImpl implements BSH, CallService, FeeManage, CSImplEvent {
    private static final Logger logger = Logger.getLogger(CallServiceImpl.class);
    public static final int MAX_DATA_SIZE = 2048;
    public static final int MAX_ROLLBACK_SIZE = 1024;

    private final VarDB<Address> bmc = Context.newVarDB("bmc", Address.class);
    private final VarDB<BTPAddress> btpAddress = Context.newVarDB("btpAddress", BTPAddress.class);
    private final VarDB<BigInteger> sn = Context.newVarDB("sn", BigInteger.class);
    private final VarDB<BigInteger> reqId = Context.newVarDB("reqId", BigInteger.class);

    private final DictDB<BigInteger, CallRequest> requests = Context.newDictDB("requests", CallRequest.class);
    private final DictDB<BigInteger, CSMessageRequest> proxyReqs = Context.newDictDB("proxyReqs", CSMessageRequest.class);

    // for fee-related operations
    private final VarDB<Address> admin = Context.newVarDB("admin", Address.class);
    private final VarDB<Address> feeHandler = Context.newVarDB("feeHandler", Address.class);
    private final VarDB<BigInteger> protocolFee = Context.newVarDB("protocolFee", BigInteger.class);

    public CallServiceImpl(Address _bmc) {
        // set bmc address only for the first deploy
        if (bmc.get() == null) {
            bmc.set(_bmc);
            BMCScoreInterface bmcInterface = new BMCScoreInterface(_bmc);
            BTPAddress bmcAddress = BTPAddress.valueOf(bmcInterface.getBtpAddress());
            btpAddress.set(new BTPAddress(bmcAddress.net(), Context.getAddress().toString()));
        }
    }

    /* Implementation-specific external */
    @External(readonly=true)
    public String getBtpAddress() {
        return btpAddress.get().toString();
    }

    private void checkCallerOrThrow(Address caller, String errMsg) {
        Context.require(Context.getCaller().equals(caller), errMsg);
    }

    private void onlyOwner() {
        checkCallerOrThrow(Context.getOwner(), "OnlyOwner");
    }

    private void onlyBMC() {
        checkCallerOrThrow(bmc.get(), "OnlyBMC");
    }

    private void checkService(String _svc) {
        Context.require(NAME.equals(_svc), "InvalidServiceName");
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
    @Payable
    @External
    public BigInteger sendCallMessage(String _to, byte[] _data, @Optional byte[] _rollback) {
        Address caller = Context.getCaller();
        // check if caller is a contract or rollback data is null in case of EOA
        Context.require(caller.isContract() || _rollback == null, "RollbackNotPossible");

        // check size of payloads to avoid abusing
        Context.require(_data.length <= MAX_DATA_SIZE, "MaxDataSizeExceeded");
        Context.require(_rollback == null || _rollback.length <= MAX_ROLLBACK_SIZE, "MaxRollbackSizeExceeded");

        boolean needResponse = _rollback != null;
        BTPAddress dst = BTPAddress.valueOf(_to);
        BigInteger value = Context.getValue();
        BigInteger requiredFee = getFee(dst.net(), needResponse);
        Context.require(value.compareTo(requiredFee) >= 0, "InsufficientFee");

        // handle protocol fee
        Address feeHandler = getProtocolFeeHandler();
        BigInteger protocolFee = getProtocolFee();
        if (feeHandler != null && protocolFee.signum() > 0) {
            // we trust fee handler, it should just accept the protocol fee and return
            // assume that no reentrant cases occur here
            Context.transfer(feeHandler, protocolFee);
        }

        BigInteger relayFee = value.subtract(protocolFee);
        BigInteger sn = getNextSn();
        if (needResponse) {
            CallRequest req = new CallRequest(caller, dst.toString(), _rollback);
            requests.set(sn, req);
        }
        CSMessageRequest msgReq = new CSMessageRequest(caller.toString(), dst.account(), sn, needResponse, _data);
        BigInteger nsn = sendBTPMessage(relayFee, dst.net(), CSMessage.REQUEST,
                needResponse ? sn : BigInteger.ZERO, msgReq.toBytes());
        CallMessageSent(caller, dst.toString(), sn, nsn, _data);
        return sn;
    }

    @Override
    @External
    public void executeCall(BigInteger _reqId) {
        CSMessageRequest req = proxyReqs.get(_reqId);
        Context.require(req != null, "InvalidRequestId");
        // cleanup
        proxyReqs.set(_reqId, null);

        BTPAddress from = BTPAddress.valueOf(req.getFrom());
        CSMessageResponse msgRes = null;
        try {
            DAppProxy proxy = new DAppProxy(Address.fromString(req.getTo()));
            proxy.handleCallMessage(req.getFrom(), req.getData());
            msgRes = new CSMessageResponse(req.getSn(), CSMessageResponse.SUCCESS, "");
        } catch (UserRevertedException e) {
            int code = e.getCode();
            String msg = "UserReverted(" + code + ")";
            logger.println("executeCall", "code:", code, "msg:", msg);
            msgRes = new CSMessageResponse(req.getSn(), code == 0 ? CSMessageResponse.FAILURE : code, msg);
        } catch (IllegalArgumentException | RevertedException e) {
            logger.println("executeCall", "Exception:", e.toString(), "msg:", e.getMessage());
            msgRes = new CSMessageResponse(req.getSn(), CSMessageResponse.FAILURE, e.toString());
        } finally {
            if (msgRes == null) {
                msgRes = new CSMessageResponse(req.getSn(), CSMessageResponse.FAILURE, "UnknownFailure");
            }
            CallExecuted(_reqId, msgRes.getCode(), msgRes.getMsg());
            // send response only when there was a rollback
            if (req.needRollback()) {
                BigInteger sn = req.getSn().negate();
                sendBTPMessage(BigInteger.ZERO, from.net(), CSMessage.RESPONSE, sn, msgRes.toBytes());
            }
        }
    }

    @Override
    @External
    public void executeRollback(BigInteger _sn) {
        CallRequest req = requests.get(_sn);
        Context.require(req != null, "InvalidSerialNum");
        Context.require(req.enabled(), "RollbackNotEnabled");
        cleanupCallRequest(_sn);

        CSMessageResponse msgRes = null;
        try {
            DAppProxy proxy = new DAppProxy(req.getFrom());
            proxy.handleCallMessage(btpAddress.get().toString(), req.getRollback());
            msgRes = new CSMessageResponse(_sn, CSMessageResponse.SUCCESS, "");
        } catch (UserRevertedException e) {
            int code = e.getCode();
            String msg = "UserReverted(" + code + ")";
            msgRes = new CSMessageResponse(_sn, code == 0 ? CSMessageResponse.FAILURE : code, msg);
        } catch (IllegalArgumentException | RevertedException e) {
            msgRes = new CSMessageResponse(_sn, CSMessageResponse.FAILURE, e.toString());
        } finally {
            if (msgRes == null) {
                msgRes = new CSMessageResponse(_sn, CSMessageResponse.FAILURE, "UnknownFailure");
            }
            RollbackExecuted(_sn, msgRes.getCode(), msgRes.getMsg());
        }
    }

    @Override
    @EventLog(indexed=3)
    public void CallMessage(String _from, String _to, BigInteger _sn, BigInteger _reqId, byte[] _data) {}

    @Override
    @EventLog(indexed=1)
    public void CallExecuted(BigInteger _reqId, int _code, String _msg) {}

    @Override
    @EventLog(indexed=1)
    public void ResponseMessage(BigInteger _sn, int _code, String _msg) {}

    @Override
    @EventLog(indexed=1)
    public void RollbackMessage(BigInteger _sn) {}

    @Override
    @EventLog(indexed=1)
    public void RollbackExecuted(BigInteger _sn, int _code, String _msg) {}

    /* Implementation-specific eventlog */
    @EventLog(indexed=3)
    public void CallMessageSent(Address _from, String _to, BigInteger _sn, BigInteger _nsn, byte[] _data) {}

    /* Implementation-specific eventlog */
    @EventLog(indexed=1)
    public void CallRequestCleared(BigInteger _sn) {}

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
    /* ========================================= */

    private BigInteger sendBTPMessage(BigInteger value, String netTo, int msgType, BigInteger sn, byte[] data) {
        CSMessage msg = new CSMessage(msgType, data);
        BMCScoreInterface bmc = new BMCScoreInterface(this.bmc.get());
        return bmc.sendMessage(value, netTo, NAME, sn, msg.toBytes());
    }

    private void handleRequest(String netFrom, BigInteger sn, byte[] data) {
        CSMessageRequest msgReq = CSMessageRequest.fromBytes(data);
        BTPAddress from = new BTPAddress(netFrom, msgReq.getFrom());
        String to = msgReq.getTo();

        BigInteger reqId = getNextReqId();
        CSMessageRequest req = new CSMessageRequest(from.toString(), to, msgReq.getSn(), msgReq.needRollback(), msgReq.getData());
        proxyReqs.set(reqId, req);

        // emit event to notify the user
        CallMessage(from.toString(), to, msgReq.getSn(), reqId, msgReq.getData());
    }

    private void handleResponse(String netFrom, BigInteger sn, byte[] data) {
        CSMessageResponse msgRes = CSMessageResponse.fromBytes(data);
        BigInteger resSn = msgRes.getSn();
        CallRequest req = requests.get(resSn);
        if (req == null) {
            logger.println("handleResponse", "No request for", resSn);
            return; // just ignore
        }
        String errMsg = msgRes.getMsg();
        ResponseMessage(resSn, msgRes.getCode(), errMsg != null ? errMsg : "");
        switch (msgRes.getCode()) {
            case CSMessageResponse.SUCCESS:
                cleanupCallRequest(resSn);
                break;
            case CSMessageResponse.FAILURE:
            case CSMessageResponse.BTP_ERROR:
            default:
                logger.println("handleResponse", "code:", msgRes.getCode(), "msg:", msgRes.getMsg());
                // emit rollback event
                Context.require(req.getRollback() != null, "NoRollbackData");
                req.setEnabled();
                requests.set(resSn, req);
                RollbackMessage(resSn);
        }
    }

    @External(readonly=true)
    public Address admin() {
        return admin.getOrDefault(Context.getOwner());
    }

    @External
    public void setAdmin(Address _address) {
        onlyOwner();
        admin.set(_address);
    }

    @External
    public void setProtocolFeeHandler(@Optional Address _addr) {
        checkCallerOrThrow(admin(), "OnlyAdmin");
        feeHandler.set(_addr);
        if (_addr != null) {
            var accruedFees = Context.getBalance(Context.getAddress());
            if (accruedFees.signum() > 0) {
                Context.transfer(_addr, accruedFees);
            }
        }
    }

    @External(readonly=true)
    public Address getProtocolFeeHandler() {
        return feeHandler.get();
    }

    @External
    public void setProtocolFee(BigInteger _value) {
        checkCallerOrThrow(admin(), "OnlyAdmin");
        Context.require(_value.signum() >= 0, "ValueShouldBePositive");
        protocolFee.set(_value);
    }

    @External(readonly=true)
    public BigInteger getProtocolFee() {
        return protocolFee.getOrDefault(BigInteger.ZERO);
    }

    @External(readonly=true)
    public BigInteger getFee(String _net, boolean _rollback) {
        if (_net.isEmpty() || _net.indexOf('/') != -1 || _net.indexOf(':') != -1) {
            Context.revert("InvalidNetworkAddress");
        }
        BMCScoreInterface bmc = new BMCScoreInterface(this.bmc.get());
        var relayFee = bmc.getFee(_net, _rollback);
        return getProtocolFee().add(relayFee);
    }
}

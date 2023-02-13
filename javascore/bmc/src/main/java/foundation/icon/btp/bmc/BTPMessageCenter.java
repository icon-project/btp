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

package foundation.icon.btp.bmc;

import foundation.icon.btp.lib.BMC;
import foundation.icon.btp.lib.BMCStatus;
import foundation.icon.btp.lib.BMVScoreInterface;
import foundation.icon.btp.lib.BSHScoreInterface;
import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.btp.lib.BTPException;
import foundation.icon.btp.lib.OwnerManager;
import foundation.icon.btp.lib.OwnerManagerImpl;
import foundation.icon.score.util.ArrayUtil;
import foundation.icon.score.util.Logger;
import foundation.icon.score.util.StringUtil;
import score.Address;
import score.ArrayDB;
import score.BranchDB;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Payable;
import scorex.util.ArrayList;
import scorex.util.Base64;
import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class BTPMessageCenter implements BMC, ICONSpecific, OwnerManager {
    private static final Logger logger = Logger.getLogger(BTPMessageCenter.class);

    public static final String INTERNAL_SERVICE = "bmc";
    public static final Address CHAIN_SCORE = Address.fromString("cx0000000000000000000000000000000000000000");

    public enum Internal {
        Init, Link, Unlink, Claim, Response;

        public static Internal of(String s) {
            for (Internal internal : values()) {
                if (internal.name().equals(s)) {
                    return internal;
                }
            }
            throw new IllegalArgumentException();
        }
    }

    public enum Event {
        SEND, DROP, ROUTE, ERROR, RECEIVE, REPLY
    }

    //
    private final BTPAddress btpAddr;

    //
    private final OwnerManager ownerManager = new OwnerManagerImpl("owners");
    private final BranchDB<String, BranchDB<Address, ArrayDB<byte[]>>> fragments = Context.newBranchDB("fragments", byte[].class);

    //
    private final Verifiers verifiers = new Verifiers("verifiers");
    private final Services services = new Services("services");
    private final Routes routes = new Routes("routes");
    private final Links links = new Links("links");

    private final BranchDB<String, ArrayDB<Address>> relays = Context.newBranchDB("relays", Address.class);
    private final DictDB<String, BigInteger> btpLinkNetworkIds = Context.newDictDB("btpLinkNetworkIds", BigInteger.class);
    private final DictDB<BigInteger, BigInteger> btpLinkOffset = Context.newDictDB("btpLinkOffset", BigInteger.class);
    private final VarDB<BigInteger> networkSn = Context.newVarDB("networkSn", BigInteger.class);

    private final Fees fees = new Fees("fees");
    //Map<SourceNetwork, Map<Service, Map<SerialNumber, ResponseInfo>>>
    private final BranchDB<String, BranchDB<String, DictDB<BigInteger, ResponseInfo>>> responseInfos
            = Context.newBranchDB("responseInfos", ResponseInfo.class);
    private final BranchDB<Address, DictDB<String, BigInteger>> rewards
            = Context.newBranchDB("rewards", BigInteger.class);
    private final VarDB<Address> feeHandler = Context.newVarDB("feeHandler", Address.class);
    //Map<NetworkSn, BMCRequest>
    private final DictDB<BigInteger, BMCRequest> requests = Context.newDictDB("requests", BMCRequest.class);

    public BTPMessageCenter(String _net) {
        this.btpAddr = new BTPAddress(BTPAddress.PROTOCOL_BTP, _net, Context.getAddress().toString());
    }

    @External(readonly = true)
    public String getBtpAddress() {
        return btpAddr.toString();
    }

    @External(readonly = true)
    public String getNetworkAddress() {
        return btpAddr.net();
    }

    @External
    public void addVerifier(String _net, Address _addr) {
        requireOwnerAccess();
        if (btpAddr.net().equals(_net)) {
            throw BMCException.unknown("invalid _net");
        }
        if (verifiers.containsKey(_net)) {
            throw BMCException.alreadyExistsBMV();
        }
        verifiers.put(_net, _addr);
    }

    @External
    public void removeVerifier(String _net) {
        requireOwnerAccess();
        if (!verifiers.containsKey(_net)) {
            throw BMCException.notExistsBMV();
        }
        verifiers.remove(_net);
    }

    @External(readonly = true)
    public Map getVerifiers() {
        return verifiers.toMap();
    }

    private BMVScoreInterface getVerifier(String _net) {
        if (!verifiers.containsKey(_net)) {
            throw BMCException.notExistsBMV();
        }
        Address address = verifiers.get(_net);
        return new BMVScoreInterface(address);
    }

    @External
    public void addService(String _svc, Address _addr) {
        requireOwnerAccess();
        if (!StringUtil.isAlphaNumeric(_svc)) {
            throw BMCException.unknown("invalid service name");
        }
        if (services.containsKey(_svc) || INTERNAL_SERVICE.equals(_svc)) {
            throw BMCException.alreadyExistsBSH();
        }
        services.put(_svc, _addr);
    }

    @External
    public void removeService(String _svc) {
        requireOwnerAccess();
        if (!services.containsKey(_svc)) {
            throw BMCException.notExistsBSH();
        }
        services.remove(_svc);
    }

    @External(readonly = true)
    public Map getServices() {
        return services.toMap();
    }

    private BSHScoreInterface getService(String _svc) {
        Address address = services.get(_svc);
        if (address == null) {
            throw BMCException.notExistsBSH();
        }
        return new BSHScoreInterface(address);
    }

    private void requireLink(String net) {
        if (!links.containsKey(net)) {
            throw BMCException.notExistsLink();
        }
    }

    private void requireLink(BTPAddress address) {
        Link link = links.get(address.net());
        if (link == null || !link.getAddr().equals(address)) {
            throw BMCException.notExistsLink();
        }
    }

    private Link getLink(String net) {
        Link link = links.get(net);
        if (link == null) {
            throw BMCException.notExistsLink();
        } else {
            return link;
        }
    }

    private Link getLink(BTPAddress address) {
        Link link = links.get(address.net());
        if (link == null || !link.getAddr().equals(address)) {
            throw BMCException.notExistsLink();
        }
        return link;
    }

    private void putLink(Link link) {
        links.put(link.getAddr().net(), link);
    }

    @External
    public void addLink(String _link) {
        requireOwnerAccess();
        BTPAddress target = BTPAddress.valueOf(_link);
        String net = target.net();
        if (!verifiers.containsKey(net)) {
            throw BMCException.notExistsBMV();
        }
        if (links.containsKey(net)) {
            throw BMCException.alreadyExistsLink();
        }

        BTPAddress[] prevLinks = propagateInternal(new BMCMessage(
                Internal.Link.name(),
                new LinkMessage(target).toBytes()).toBytes());

        Link link = new Link();
        link.setAddr(target);
        link.setRxSeq(BigInteger.ZERO);
        link.setTxSeq(BigInteger.ZERO);
        link.setReachable(new ArrayList<>());
        putLink(link);

        sendInternal(target, new BMCMessage(Internal.Init.name(),
                new InitMessage(prevLinks).toBytes()).toBytes());
    }

    @External
    public void removeLink(String _link) {
        requireOwnerAccess();
        BTPAddress target = BTPAddress.valueOf(_link);
        String net = target.net();
        Link link = links.remove(net);
        if (link == null || !link.getAddr().equals(target)) {
            throw BMCException.notExistsLink();
        }
        if (routes.containsValue(net)) {
            throw BMCException.unknown("could not remove, referred by route");
        }
        ArrayDB<Address> arrayDB = relays.at(_link);
        for(int i = 0; i < arrayDB.size(); i++ ) {
            arrayDB.removeLast();
        }
        BigInteger networkId = btpLinkNetworkIds.get(_link);
        if (networkId != null) {
            btpLinkNetworkIds.set(_link, null);
            btpLinkOffset.set(networkId, null);
        }
        fees.remove(net);

        propagateInternal(new BMCMessage(
                Internal.Unlink.name(),
                new UnlinkMessage(target).toBytes()).toBytes());
    }

    @External(readonly = true)
    public BMCStatus getStatus(String _link) {
        BTPAddress target = BTPAddress.valueOf(_link);
        Link link = getLink(target.net());
        BMCStatus status = new BMCStatus();
        status.setTx_seq(link.getTxSeq());
        status.setRx_seq(link.getRxSeq());
        BMVScoreInterface verifier = getVerifier(link.getAddr().net());
        status.setVerifier(verifier.getStatus());
        status.setCur_height(Context.getBlockHeight());
        return status;
    }

    @External(readonly = true)
    public String[] getLinks() {
        List<Link> values = links.values();
        int len = values.size();
        String[] links = new String[len];
        for (int i = 0; i < len; i++) {
            links[i] = values.get(i).getAddr().toString();
        }
        return links;
    }

    @External
    public void addRoute(String _dst, String _link) {
        requireOwnerAccess();
        if (_dst.equals(_link)) {
            throw BMCException.unknown("invalid _dst");
        }
        if (routes.containsKey(_dst)) {
            throw BMCException.unknown("already exists route");
        }

        requireLink(_link);
        routes.put(_dst, _link);
    }

    @External
    public void removeRoute(String _dst) {
        requireOwnerAccess();
        if (routes.remove(_dst) == null) {
            throw BMCException.unknown("not exists route");
        }
        fees.remove(_dst);
    }

    @External(readonly = true)
    public Map getRoutes() {
        Map<String, String> map = new HashMap<>();
        for (Link link : links.values()) {
            for(BTPAddress reachable : link.getReachable()) {
                if (!map.containsKey(reachable.net())) {
                    map.put(reachable.net(), link.getAddr().net());
                }
            }
        }
        map.putAll(routes.toMap());
        return map;
    }

    @External
    public void setFeeTable(String[] _dst, BigInteger[][] _value) {
        requireOwnerAccess();
        if (_dst.length != _value.length) {
            throw BMCException.unknown("invalid array length");
        }
        for (int i = 0; i < _dst.length; i++) {
            String dstNet = _dst[i];
            BigInteger[] values = _value[i];
            if (values != null && values.length > 0) {
                if ((values.length % 2) != 0) {
                    throw BMCException.unknown("length of _value[" + i + "] must be even");
                }
                for (int j = 0; j < values.length; j++) {
                    if (values[j].compareTo(BigInteger.ZERO) < 0) {
                        throw BMCException.unknown("_value[" + i + "][" + j + "] must be positive");
                    }
                }
                int hop = values.length / 2;
                if (hop == 1) {
                    requireLink(dstNet);
                } else {
                    resolveNext(dstNet);
                }
                fees.put(dstNet, new FeeInfo(dstNet, values));
            } else {
                fees.remove(dstNet);
            }
        }
    }

    private BigInteger[] getFeeList(String net, boolean includeBackward) {
        FeeInfo fee = fees.get(net);
        if (fee == null) {
            return new BigInteger[]{};
        }
        return includeBackward ? fee.getValues() :
                ArrayUtil.copyOf(fee.getValues(), fee.getValues().length / 2);
    }

    @External(readonly = true)
    public BigInteger getFee(String _to, boolean _response) {
        resolveNext(_to);
        return ArrayUtil.sum(getFeeList(_to, _response));
    }

    @External(readonly = true)
    public BigInteger[][] getFeeTable(String[] _dst) {
        BigInteger[][] ret = new BigInteger[_dst.length][];
        for (int i = 0; i < _dst.length; i++) {
            resolveNext(_dst[i]);
            ret[i] = getFeeList(_dst[i], true);
        }
        return ret;
    }

    private void accumulateFee(Address address, FeeInfo feeInfo) {
        if (feeInfo != null && feeInfo.getValues().length > 0) {
            BigInteger[] feeList = feeInfo.getValues();
            BigInteger[] nextFeeList = new BigInteger[feeList.length - 1];
            System.arraycopy(feeList, 1, nextFeeList, 0, feeList.length - 1);
            feeInfo.setValues(nextFeeList);
            addReward(address, feeInfo.getNetwork(), feeList[0]);
        }
    }

    private void collectRemainFee(FeeInfo feeInfo) {
        if (feeInfo != null) {
            BigInteger[] feeList = feeInfo.getValues();
            if (feeList.length > 0) {
                BigInteger sum = BigInteger.ZERO;
                for (BigInteger v : feeList) {
                    sum = sum.add(v);
                }
                collectRemainFee(feeInfo.getNetwork(), sum);
            }
        }
    }

    private void collectRemainFee(String net, BigInteger amount) {
        addReward(Context.getAddress(), net, amount);
    }

    private void addReward(Address addr, String net, BigInteger amount) {
        if (amount != null && amount.compareTo(BigInteger.ZERO) > 0) {
            DictDB<String, BigInteger> rewardDictDB = rewards.at(addr);
            rewardDictDB.set(net,
                    amount.add(rewardDictDB.getOrDefault(net, BigInteger.ZERO)));
        }
    }

    static Address toAddress(String s) {
        try {
            return Address.fromString(s);
        } catch (IllegalArgumentException e) {
            throw BMCException.unknown("invalid address format");
        }
    }

    @Payable
    @External
    public void claimReward(String _network, String _receiver) {
        Address caller = Context.getCaller();
        Address fh = getFeeHandler();
        if (fh != null && fh.equals(caller)) {
            caller = Context.getAddress();
        }
        BigInteger reward = getReward(_network, caller);
        if (BigInteger.ZERO.compareTo(reward) >= 0) {
            throw BMCException.unknown("not exists claimable reward");
        }
        rewards.at(caller).set(_network, BigInteger.ZERO);
        if (_network.equals(btpAddr.net())) {
            Context.transfer(toAddress(_receiver), reward);
            ClaimReward(caller, _network, _receiver, reward, BigInteger.ZERO);
        } else {
            BMCMessage bmcMessage = new BMCMessage(Internal.Claim.name(),
                    new ClaimMessage(reward, _receiver).toBytes());
            BigInteger nsn = sendMessageWithFee(_network, INTERNAL_SERVICE, null, bmcMessage.toBytes(), false, true);
            requests.set(nsn, new BMCRequest(_network, bmcMessage, caller));
            ClaimReward(caller, _network, _receiver, reward, nsn);
        }
    }

    @EventLog(indexed = 2)
    public void ClaimReward(Address _sender, String _network, String _receiver, BigInteger _amount, BigInteger _nsn) {
    }

    @EventLog(indexed = 2)
    public void ClaimRewardResult(Address _sender, String _network, BigInteger _nsn, BigInteger _result) {
    }

    @External(readonly = true)
    public BigInteger getReward(String _network, Address _addr) {
        return rewards.at(_addr).getOrDefault(_network, BigInteger.ZERO);
    }

    @External
    public void setFeeHandler(Address _addr) {
        requireOwnerAccess();
        feeHandler.set(_addr);
    }

    @External(readonly = true)
    public Address getFeeHandler() {
        return feeHandler.get();
    }

    private BTPAddress resolveNextFromReachable(String _net) {
        int size = links.size();
        for (int i = 0; i < size; i++) {
            Link link = links.getValue(i);
            for (BTPAddress reachable : link.getReachable()) {
                if (_net.equals(reachable.net())) {
                    return link.getAddr();
                }
            }
        }
        return null;
    }

    private BTPAddress resolveNext(String _net) {
        if (links.containsKey(_net)) {
            return links.get(_net).getAddr();
        }

        String nextNet = routes.get(_net);
        if (nextNet != null) {
            return getLink(nextNet).getAddr();
        }

        BTPAddress next = resolveNextFromReachable(_net);
        if (next != null) {
            return next;
        }
        throw BMCException.unreachable();
    }

    @External
    public void handleRelayMessage(String _prev, String _msg) {
        byte[] msgBytes = Base64.getUrlDecoder().decode(_msg.getBytes());
        handleRelayMessage(_prev, msgBytes);
    }

    private void handleRelayMessage(String _prev, byte[] msgBytes) {
        BTPAddress prev = BTPAddress.valueOf(_prev);
        Link link = getLink(prev);
        BigInteger rxSeq = link.getRxSeq();

        BMVScoreInterface verifier = getVerifier(link.getAddr().net());
        // decode and verify relay message
        byte[][] serializedMsgs;
        try {
            serializedMsgs = verifier.handleRelayMessage(btpAddr.toString(), _prev, rxSeq, msgBytes);
        } catch (Exception e) {
            logger.println("handleRelayMessage", "fail to verify", e.toString());
            throw BTPException.of(e);
        }
        long msgCount = serializedMsgs.length;

        Address caller = Context.getCaller();
        if (getRelayIndex(_prev, caller) < 0) {
            throw BMCException.unauthorized("not registered relay");
        }
        if (msgCount > 0) {
            link.setRxSeq(rxSeq.add(BigInteger.valueOf(msgCount)));
        }
        putLink(link);

        // dispatch BTPMessages
        for (byte[] serializedMsg : serializedMsgs) {
            rxSeq = rxSeq.add(BigInteger.ONE);
            BTPMessage msg;
            try {
                msg = BTPMessage.fromBytes(serializedMsg);
            } catch (Exception e) {
                logger.println("handleRelayMessage",
                        "fail to parse BTPMessage rxSeq:",rxSeq,
                        ", msg:", serializedMsg,
                        ", err:", e.toString());
                throw BMCException.unknown("fail to parse BTPMessage");
            }
            //TODO [TBD] needs nsn validation?
//                int snCompare = msg.getSn().compareTo(BigInteger.ZERO);
//                if (isInvalidSn(snCompare, msg.getNsn().compareTo(BigInteger.ZERO))) {
//                    throw BMCException.invalidSn();
//                }

            accumulateFee(caller, msg.getFeeInfo());
            try {
                if (btpAddr.net().equals(msg.getDst())) {
                    handleMessage(msg);
                    emitBTPEvent(msg, null, Event.RECEIVE);
                } else {
                    BTPAddress next = resolveNext(msg.getDst());
                    sendMessage(next, msg.toBytes());
                    emitBTPEvent(msg, next, Event.ROUTE);
                }
            } catch (BTPException e) {
                if (msg.getSn().compareTo(BigInteger.ZERO) > 0) {
                    try {
                        sendError(prev, msg, e);
                    } catch (BTPException e2) {
                        //abnormal case, if ChainScore.sendBTPMessage revert
                        collectRemainFee(msg.getFeeInfo());
                        emitMessageDropped(prev, rxSeq, msg, e2);
                    }
                } else {
                    collectRemainFee(msg.getFeeInfo());
                    emitMessageDropped(prev, rxSeq, msg, e);
                }
            }
        }
    }

    private void handleMessage(BTPMessage msg) {
        String src = msg.getSrc();
        String svc = msg.getSvc();
        BigInteger sn = msg.getSn();
        byte[] payload = msg.getPayload();
        FeeInfo feeInfo = msg.getFeeInfo();
        int snCompare = sn.compareTo(BigInteger.ZERO);
        if (snCompare >= 0) {
            DictDB<BigInteger, ResponseInfo> responseInfoDictDb = null;
            if (feeInfo != null) {
                if (snCompare > 0) {
                    responseInfoDictDb = responseInfos.at(src).at(msg.getSvc());
                    ResponseInfo oldInfo = responseInfoDictDb.get(sn);
                    if (oldInfo != null) {
                        collectRemainFee(oldInfo.getFeeInfo());
                    }
                    responseInfoDictDb.set(sn, new ResponseInfo(msg.getNsn(), msg.getFeeInfo()));
                } else {
                    collectRemainFee(feeInfo);
                }
            }

            try {
                if (svc.equals(INTERNAL_SERVICE)) {
                    internalHandleBTPMessage(src, msg.getNsn(), payload);
                } else {
                    BSHScoreInterface service = getService(svc);
                    service.handleBTPMessage(src, svc, sn, payload);
                }
            } catch (Exception e) {
                if (responseInfoDictDb != null) {
                    responseInfoDictDb.set(sn, null);
                }
                throw BTPException.of(e, BTPException.Type.BSH);
            }
        } else {
            sn = sn.negate();
            collectRemainFee(feeInfo);
            try {
                ResponseMessage responseMessage = ResponseMessage.fromBytes(payload);
                long eCode = responseMessage.getCode();
                String eMsg = responseMessage.getMsg() == null ? "" : responseMessage.getMsg();
                if (svc.equals(INTERNAL_SERVICE)) {
                    internalHandleBTPError(src, msg.getNsn(), eCode, eMsg);
                } else {
                    BSHScoreInterface service = getService(svc);
                    service.handleBTPError(src, svc, sn, eCode, eMsg);
                }
            } catch (Exception e) {
                throw BTPException.of(e, BTPException.Type.BSH);
            }
        }
    }

    private void internalHandleBTPMessage(String src, BigInteger nsn, byte[] msg) {
        BMCMessage bmcMsg = BMCMessage.fromBytes(msg);
        Internal internal = Internal.of(bmcMsg.getType());
        byte[] payload = bmcMsg.getPayload();
        switch (internal) {
            case Claim:
                ClaimMessage claimMsg = ClaimMessage.fromBytes(payload);
                BigInteger reward = claimMsg.getAmount();
                Address receiver = toAddress(claimMsg.getReceiver());
                addReward(receiver, btpAddr.net(), reward);
                try {
                    sendInternalResponse(src, nsn);
                } catch (Exception e) {
                    addReward(receiver, btpAddr.net(), reward.negate());
                    throw e;
                }
                break;
            case Response:
                ResponseMessage responseMsg = ResponseMessage.fromBytes(payload);
                handleResponse(nsn.negate(), responseMsg.getCode());
                break;
            case Link:
                LinkMessage linkMsg = LinkMessage.fromBytes(payload);
                addReachable(src, linkMsg.getLink());
                break;
            case Unlink:
                UnlinkMessage unlinkMsg = UnlinkMessage.fromBytes(payload);
                removeReachable(src, unlinkMsg.getLink());
                break;
            case Init:
                InitMessage initMsg = InitMessage.fromBytes(payload);
                addReachable(src, initMsg.getLinks());
                break;
            default:
                throw BMCException.unknown("not exists internal handler");
        }
    }

    private void addReachable(String net, BTPAddress... reachable) {
        Link link = getLink(net);
        List<BTPAddress> list = link.getReachable();
        for (BTPAddress address : reachable) {
            if (!list.contains(address)) {
                list.add(address);
            }
        }
        putLink(link);
    }

    private void removeReachable(String net, BTPAddress address) {
        Link link = getLink(net);
        link.getReachable().remove(address);
        putLink(link);
    }

    private void handleResponse(BigInteger nsn, long result) {
        BMCRequest request = requests.get(nsn);
        if (request != null) {
            requests.set(nsn, null);
            BMCMessage bmcMessage = request.getMsg();
            if (Internal.Claim.name().equals(bmcMessage.getType())) {
                ClaimMessage claimMsg =
                        ClaimMessage.fromBytes(bmcMessage.getPayload());
                if (ResponseMessage.CODE_SUCCESS != result) {
                    addReward(request.getCaller(), request.getDst(), claimMsg.getAmount());
                }
                ClaimRewardResult(request.getCaller(), request.getDst(), nsn, BigInteger.valueOf(result));
            }
        } else {
            throw BMCException.unknown("not exists request");
        }
    }

    private void internalHandleBTPError(String src, BigInteger nsn, long code, String msg) {
        logger.println("internalHandleBTPError",
                "src:", src, "nsn:", nsn, "code:", code, "msg:", msg);
        handleResponse(nsn.negate(), code);
    }

    @External(readonly = true)
    public BigInteger getNetworkSn() {
        return networkSn.getOrDefault(BigInteger.ZERO);
    }

    private BigInteger nextNetworkSn() {
        BigInteger sn = getNetworkSn();
        sn = sn.add(BigInteger.ONE);
        networkSn.set(sn);
        return sn;
    }

    @Payable
    @External
    public BigInteger sendMessage(String _to, String _svc, BigInteger _sn, byte[] _msg) {
        Address addr = services.get(_svc);
        if (addr == null) {
            throw BMCException.notExistsBSH();
        }
        if (!Context.getCaller().equals(addr)) {
            throw BMCException.unauthorized();
        }
        boolean isResponse = false;
        if (_sn.compareTo(BigInteger.ZERO) < 0) {
            isResponse = true;
            _sn = _sn.negate();
        }
        return sendMessageWithFee(_to, _svc, _sn, _msg, isResponse);
    }

    private BigInteger sendMessageWithFee(String _to, String _svc, BigInteger _sn, byte[] msg, boolean isResponse) {
        return sendMessageWithFee(_to, _svc, _sn, msg, isResponse, false);
    }

    private BigInteger sendMessageWithFee(String _to, String _svc, BigInteger _sn, byte[] msg, boolean isResponse, boolean fillSnByNsn) {
        BTPAddress next = resolveNext(_to);
        BTPMessage btpMsg = new BTPMessage();
        btpMsg.setSrc(btpAddr.net());
        btpMsg.setDst(_to);
        btpMsg.setSvc(_svc);
        btpMsg.setSn(isResponse ? BigInteger.ZERO : _sn);
        btpMsg.setPayload(msg);

        Event event;
        if (isResponse) {
            DictDB<BigInteger, ResponseInfo> responseInfoDictDb = responseInfos.at(_to).at(_svc);
            ResponseInfo responseInfo = responseInfoDictDb.get(_sn);
            if (responseInfo == null) {
                throw BMCException.unknown("not exists response");
            }
            responseInfoDictDb.set(_sn, null);
            collectRemainFee(btpAddr.net(), Context.getValue());
            btpMsg.setNsn(responseInfo.getNsn().negate());
            btpMsg.setFeeInfo(responseInfo.getFeeInfo());
            event = Event.REPLY;
        } else {
            btpMsg.setNsn(nextNetworkSn());
            if (fillSnByNsn) {
                _sn = btpMsg.getNsn();
                btpMsg.setSn(_sn);
            }
            BigInteger[] values = getFeeList(_to, _sn.compareTo(BigInteger.ZERO) > 0);
            BigInteger remain = Context.getValue().subtract(ArrayUtil.sum(values));
            if (remain.compareTo(BigInteger.ZERO) < 0) {
                logger.println("sendMessage", "not enough fee", remain);
                throw BMCException.unknown("not enough fee");
            }
            collectRemainFee(btpAddr.net(), remain);
            btpMsg.setFeeInfo(new FeeInfo(btpAddr.net(), values));
            event = Event.SEND;
        }
        sendMessage(next, btpMsg.toBytes());
        emitBTPEvent(btpMsg, next, event);
        return btpMsg.getNsn();
    }

    static ResponseMessage toResponseMessage(BTPException exception) {
        if (exception == null) {
            return new ResponseMessage(ResponseMessage.CODE_SUCCESS, "");
        }
        long code = ResponseMessage.CODE_UNKNOWN;
        if (BMCException.Code.Unreachable.code == exception.getCodeOfType()) {
            code = ResponseMessage.CODE_NO_ROUTE;
        } else if (BMCException.Code.NotExistsBSH.code == exception.getCodeOfType()) {
            code = ResponseMessage.CODE_NO_BSH;
        } else if (BTPException.Type.BSH.equals(exception.getType())) {
            code = ResponseMessage.CODE_BSH_REVERT;
        }
        return new ResponseMessage(code, exception.getMessage());
    }

    private void sendError(BTPAddress prev, BTPMessage msg, BTPException e) {
        FeeInfo feeInfo = msg.getFeeInfo();
        if (feeInfo != null) {
            String feeNet = feeInfo.getNetwork();
            BigInteger[] feeList = feeInfo.getValues();
            if (feeNet != null && feeList != null && feeList.length > 0) {
                int hop = getFeeList(msg.getSrc(), false).length;
                if (hop > 0) {
                    if (feeList.length > hop) {
                        //swap not-consumed and to-be-consumed
                        int remainLen = feeList.length - hop;
                        BigInteger[] nextFeeList = new BigInteger[feeList.length];
                        System.arraycopy(feeList, remainLen, nextFeeList, 0, hop);
                        System.arraycopy(feeList, 0, nextFeeList, hop, remainLen);
                        feeInfo.setValues(nextFeeList);
                    }
                }
            }
        }
        BTPMessage btpMsg = new BTPMessage();
        btpMsg.setSrc(btpAddr.net());
        btpMsg.setDst(msg.getSrc());
        btpMsg.setSvc(msg.getSvc());
        btpMsg.setSn(msg.getSn().negate());
        btpMsg.setPayload(toResponseMessage(e).toBytes());
        btpMsg.setNsn(msg.getNsn().negate());
        btpMsg.setFeeInfo(feeInfo);
        sendMessage(prev, btpMsg.toBytes());
        emitBTPEvent(msg, prev, Event.ERROR);
    }

    private void sendMessage(BTPAddress next, byte[] serializedMsg) {
        Link link = getLink(next);
        link.setTxSeq(link.getTxSeq().add(BigInteger.ONE));
        putLink(link);
        BigInteger networkId = btpLinkNetworkIds.get(next.toString());
        if (networkId == null) {
            Message(next.toString(), link.getTxSeq(), serializedMsg);
        } else {
            try {
                Context.call(CHAIN_SCORE, "sendBTPMessage", networkId, serializedMsg);
            } catch (Exception e) {
                link.setTxSeq(link.getTxSeq().subtract(BigInteger.ONE));
                putLink(link);
                throw BMCException.unknown("fail to sendBTPMessage :" + e);
            }
        }
    }

    private void sendInternalResponse(String net, BigInteger nsn) {
        sendMessageWithFee(net, INTERNAL_SERVICE, nsn,
                new BMCMessage(Internal.Response.name(),
                        toResponseMessage(null).toBytes()).toBytes(),
                true);
    }

    private void sendInternal(BTPAddress next, byte[] payload) {
        BTPMessage btpMsg = new BTPMessage();
        btpMsg.setSrc(btpAddr.net());
        btpMsg.setDst(next.net());
        btpMsg.setSvc(INTERNAL_SERVICE);
        btpMsg.setSn(BigInteger.ZERO);
        btpMsg.setPayload(payload);
        btpMsg.setNsn(nextNetworkSn());
        emitBTPEvent(btpMsg, next, Event.SEND);
        sendMessage(next, btpMsg.toBytes());
    }

    private BTPAddress[] propagateInternal(byte[] payload) {
        BTPAddress[] addrs = new BTPAddress[links.size()];
        int i = 0;
        for (Link link : links.values()) {
            BTPAddress next = link.getAddr();
            addrs[i++] = next;
            sendInternal(next, payload);
        }
        return addrs;
    }

    @EventLog(indexed = 2)
    public void Message(String _next, BigInteger _seq, byte[] _msg) {
    }

    private void emitBTPEvent(BTPMessage msg, BTPAddress next, Event event) {
        BigInteger nsn = msg.getNsn();
        String _next = next == null ? "" : next.toString();
        if (nsn.compareTo(BigInteger.ZERO) < 0) {
            BTPEvent(msg.getDst(), nsn.negate(), _next, event.name());
        } else {
            BTPEvent(msg.getSrc(), nsn, _next, event.name());
        }
    }

    @EventLog(indexed = 2)
    public void BTPEvent(String _src, BigInteger _nsn, String _next, String _event) {
    }

    @External
    public void handleFragment(String _prev, String _msg, int _idx) {
        logger.println("handleFragment", "_prev", _prev, "_idx:", _idx, "len(_msg):" + _msg.length());
        requireLink(BTPAddress.valueOf(_prev));
        Address caller = Context.getCaller();
        if (getRelayIndex(_prev, caller) < 0) {
            throw BMCException.unauthorized("not registered relay");
        }
        byte[] fragmentBytes = Base64.getUrlDecoder().decode(_msg.getBytes());
        final int INDEX_LAST = 0;
        final int INDEX_NEXT = 1;
        final int INDEX_OFFSET = 2;
        ArrayDB<byte[]> fragments = this.fragments.at(_prev).at(caller);
        if (_idx < 0) {
            int last = _idx * -1;
            if (fragments.size() == 0) {
                fragments.add(Integer.toString(last).getBytes());
                fragments.add(Integer.toString(last - 1).getBytes());
                fragments.add(fragmentBytes);
            } else {
                fragments.set(INDEX_LAST, Integer.toString(last).getBytes());
                fragments.set(INDEX_NEXT, Integer.toString(last - 1).getBytes());
                fragments.set(INDEX_OFFSET, fragmentBytes);
            }
        } else {
            int next = Integer.parseInt(new String(fragments.get(INDEX_NEXT)));
            if (next != _idx) {
                throw BMCException.unknown("invalid _idx");
            }
            int last = Integer.parseInt(new String(fragments.get(INDEX_LAST)));
            if (_idx == 0) {
                int total = 0;
                byte[][] bytesArr = new byte[last + 1][];
                for (int i = 0; i < last; i++) {
                    bytesArr[i] = fragments.get(i + INDEX_OFFSET);
                    total += bytesArr[i].length;
                }
                bytesArr[last] = fragmentBytes;
                total += fragmentBytes.length;
                byte[] msgBytes = new byte[total];
                int pos = 0;
                for (byte[] bytes : bytesArr) {
                    System.arraycopy(bytes, 0, msgBytes, pos, bytes.length);
                    pos += bytes.length;
                }
                logger.println("handleFragment", "handleRelayMessage", "fragments:", last + 1, "len:" + total);
                handleRelayMessage(_prev, msgBytes);
            } else {
                fragments.set(INDEX_NEXT, Integer.toString(_idx - 1).getBytes());
                int INDEX_MSG = last - _idx + INDEX_OFFSET;
                if (INDEX_MSG < fragments.size()) {
                    fragments.set(INDEX_MSG, fragmentBytes);
                } else {
                    fragments.add(fragmentBytes);
                }
            }
        }
    }

    static boolean isInvalidSn(int snCompare, int nsnCompare) {
        return (nsnCompare == 0 ||
                (nsnCompare > 0 && snCompare < 0) ||
                (nsnCompare < 0 && snCompare > 0));
    }

    @External
    public void dropMessage(
            String _src, BigInteger _seq, String _svc, BigInteger _sn, BigInteger _nsn,
            String _feeNetwork, BigInteger[] _feeValues) {
        requireOwnerAccess();
        BTPAddress prev = resolveNext(_src);
        Link link = links.get(prev.net());
        if (link.getRxSeq().add(BigInteger.ONE).compareTo(_seq) != 0) {
            throw BMCException.unknown("invalid _seq");
        }
        if (!services.containsKey(_svc)) {
            throw BMCException.notExistsBSH();
        }
        int snCompare = _sn.compareTo(BigInteger.ZERO);
        if (isInvalidSn(snCompare, _nsn.compareTo(BigInteger.ZERO))) {
            throw BMCException.invalidSn();
        }
        link.setRxSeq(_seq);
        putLink(link);

        BTPMessage assumeMsg = new BTPMessage();
        assumeMsg.setSrc(_src);
        assumeMsg.setDst("");
        assumeMsg.setSvc(_svc);
        assumeMsg.setSn(_sn);
        assumeMsg.setPayload(new byte[0]);
        assumeMsg.setNsn(_nsn);
        if (!_feeNetwork.isEmpty()) {
            assumeMsg.setFeeInfo(new FeeInfo(_feeNetwork, _feeValues));
            accumulateFee(Context.getAddress(), assumeMsg.getFeeInfo());
        }
        BMCException e = BMCException.drop();
        if (snCompare > 0) {
            sendError(prev, assumeMsg, e);
        } else {
            emitMessageDropped(prev, _seq, assumeMsg, e);
        }
    }

    private void emitMessageDropped(BTPAddress prev, BigInteger seq, BTPMessage msg, BTPException e) {
        emitBTPEvent(msg, null, Event.DROP);
        String emsg = e.getMessage();
        if (emsg == null) {
            emsg = e.toString();
        }
        MessageDropped(prev.toString(), seq, msg.toBytes(), e.getCode(), emsg);
    }

    @EventLog(indexed = 2)
    public void MessageDropped(String _prev, BigInteger _seq, byte[] _msg, long _ecode, String _emsg) {
    }

    private int getRelayIndex(String _link, Address _addr) {
        ArrayDB<Address> arrayDB = relays.at(_link);
        for (int i = 0 ; i < arrayDB.size(); i++) {
            if (arrayDB.get(i).equals(_addr)) {
                return i;
            }
        }
        return -1;
    }

    @External
    public void addRelay(String _link, Address _addr) {
        requireOwnerAccess();
        requireLink(BTPAddress.valueOf(_link));
        if (getRelayIndex(_link, _addr) >= 0) {
            throw BMCException.alreadyExistsBMR();
        }
        relays.at(_link).add(_addr);
    }

    @External
    public void removeRelay(String _link, Address _addr) {
        requireOwnerAccess();
        requireLink(BTPAddress.valueOf(_link));
        ArrayDB<Address> arrayDB = relays.at(_link);
        if (arrayDB.size() == 0) {
            throw BMCException.notExistsBMR();
        }
        Address last = arrayDB.pop();
        if (!last.equals(_addr)) {
            for (int i = 0 ; i < arrayDB.size(); i++) {
                if (arrayDB.get(i).equals(_addr)) {
                    arrayDB.set(i, last);
                    return;
                }
            }
            throw BMCException.notExistsBMR();
        }
    }

    @External(readonly = true)
    public Address[] getRelays(String _link) {
        requireLink(BTPAddress.valueOf(_link));
        ArrayDB<Address> arrayDB = relays.at(_link);
        Address[] arr = new Address[arrayDB.size()];
        for (int i = 0 ; i < arrayDB.size(); i++) {
            arr[i] = arrayDB.get(i);
        }
        return arr;
    }

    /* Delegate OwnerManager */
    private void requireOwnerAccess() {
        if (!ownerManager.isOwner(Context.getCaller())) {
            throw BMCException.unauthorized("require owner access");
        }
    }

    @External
    public void addOwner(Address _addr) {
        try {
            ownerManager.addOwner(_addr);
        } catch (IllegalStateException e) {
            throw BMCException.unauthorized(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw BMCException.unknown(e.getMessage());
        }
    }

    @External
    public void removeOwner(Address _addr) {
        try {
            ownerManager.removeOwner(_addr);
        } catch (IllegalStateException e) {
            throw BMCException.unauthorized(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw BMCException.unknown(e.getMessage());
        }
    }

    @External(readonly = true)
    public Address[] getOwners() {
        return ownerManager.getOwners();
    }

    @External(readonly = true)
    public boolean isOwner(Address _addr) {
        return ownerManager.isOwner(_addr);
    }

    @Payable
    public void fallback() {
        logger.println("fallback", "value:", Context.getValue());
    }

    @External
    public void addBTPLink(String _link, long _networkId) {
        requireOwnerAccess();

        setBTPLink(_link, _networkId, BigInteger.ZERO);
        addLink(_link);
    }

    @External
    public void setBTPLinkNetworkId(String _link, long _networkId) {
        requireOwnerAccess();

        Link link = getLink(BTPAddress.valueOf(_link));
        setBTPLink(_link, _networkId, link.getTxSeq());
    }

    private void setBTPLink(String _link, long _networkId, BigInteger offset) {
        if (_networkId < 1) {
            throw BMCException.unknown("_networkId should be greater than zero");
        }
        BigInteger networkId = BigInteger.valueOf(_networkId);
        if (btpLinkOffset.get(networkId) != null) {
            throw BMCException.unknown("already exists networkId");
        }
        btpLinkNetworkIds.set(_link, networkId);
        btpLinkOffset.set(networkId, offset);
    }

    @External(readonly = true)
    public long getBTPLinkNetworkId(String _link) {
        requireLink(BTPAddress.valueOf(_link));
        return btpLinkNetworkIds.getOrDefault(_link, BigInteger.ZERO).longValue();
    }

    @External(readonly = true)
    public long getBTPLinkOffset(String _link) {
        requireLink(BTPAddress.valueOf(_link));
        BigInteger networkId = btpLinkNetworkIds.get(_link);
        if (networkId == null) {
            throw BMCException.unknown("not exists networkId");
        }
        return btpLinkOffset.get(networkId).longValue();
    }

}

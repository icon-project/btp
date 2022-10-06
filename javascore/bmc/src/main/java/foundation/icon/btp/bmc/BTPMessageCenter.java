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
import foundation.icon.btp.lib.BMCEvent;
import foundation.icon.btp.lib.BMRStatus;
import foundation.icon.btp.lib.BMVScoreInterface;
import foundation.icon.btp.lib.BMVStatus;
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
import score.UserRevertedException;
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

public class BTPMessageCenter implements BMC, BMCEvent, ICONSpecific, OwnerManager {
    private static final Logger logger = Logger.getLogger(BTPMessageCenter.class);

    public static final String INTERNAL_SERVICE = "bmc";
    public static final Address CHAIN_SCORE = Address.fromString("cx0000000000000000000000000000000000000000");
    public static final Address BURN_ADDRESS = Address.fromString("hx1000000000000000000000000000000000000000");

    public enum Internal {
        Init, Link, Unlink, ClaimReward, Response;

        public static Internal of(String s) {
            for (Internal internal : values()) {
                if (internal.name().equals(s)) {
                    return internal;
                }
            }
            throw new IllegalArgumentException();
        }
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
    private final DictDB<String, BigInteger> btpLinkNetworkIds = Context.newDictDB("btpLinkNetworkIds", BigInteger.class);
    private final DictDB<BigInteger, BigInteger> btpLinkOffset = Context.newDictDB("btpLinkOffset", BigInteger.class);
    private final VarDB<BigInteger> networkSn = Context.newVarDB("networkSn", BigInteger.class);

    private final Fees fees = new Fees("fees");
    //Map<SourceNetwork, Map<Service, Map<SerialNumber, FeeInfo>>>
    private final BranchDB<String,BranchDB<String,DictDB<BigInteger, FeeInfo>>> responseFeeInfos
            = Context.newBranchDB("responseFeeInfos", FeeInfo.class);
    private final BranchDB<Address, DictDB<String, BigInteger>> rewards
            = Context.newBranchDB("rewards", BigInteger.class);
    private final VarDB<Address> feeHandler = Context.newVarDB("feeHandler", Address.class);
    private final VarDB<BigInteger> internalSn = Context.newVarDB("internalSn", BigInteger.class);
    private final DictDB<BigInteger, BMCRequest> requests = Context.newDictDB("requests", BMCRequest.class);

    public BTPMessageCenter(String _net) {
        this.btpAddr = new BTPAddress(BTPAddress.PROTOCOL_BTP, _net, Context.getAddress().toString());
    }

    @External(readonly = true)
    public String getBtpAddress() {
        return btpAddr.toString();
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

    private void requireLink(BTPAddress link) {
        if (!links.containsKey(link)) {
            throw BMCException.notExistsLink();
        }
    }

    private Link getLink(BTPAddress link) {
        requireLink(link);
        return links.get(link);
    }

    private void putLink(Link link) {
        links.put(link.getAddr(), link);
    }

    @External
    public void addLink(String _link) {
        requireOwnerAccess();
        BTPAddress target = BTPAddress.valueOf(_link);
        if (!verifiers.containsKey(target.net())) {
            throw BMCException.notExistsBMV();
        }
        if (links.containsKey(target)) {
            throw BMCException.alreadyExistsLink();
        }

        BTPAddress[] prevLinks = new BTPAddress[links.size()];
        int i = 0;
        for (BTPAddress link : links.keySet()) {
            if (link.net().equals(target.net())) {
                throw BMCException.alreadyExistsLink();
            }
            prevLinks[i++] = link;
        }
        InitMessage initMsg = new InitMessage();
        initMsg.setLinks(prevLinks);

        LinkMessage linkMsg = new LinkMessage();
        linkMsg.setLink(target);
        propagateInternal(Internal.Link, linkMsg.toBytes());

        Link link = new Link();
        link.setAddr(target);
        link.setRxSeq(BigInteger.ZERO);
        link.setTxSeq(BigInteger.ZERO);
        link.setReachable(new ArrayList<>());
        putLink(link);

        sendInternal(target, Internal.Init, initMsg.toBytes());
    }

    @External
    public void removeLink(String _link) {
        requireOwnerAccess();
        BTPAddress target = BTPAddress.valueOf(_link);
        if (!links.containsKey(target)) {
            throw BMCException.notExistsLink();
        }
        if (routes.containsNext(target)) {
            throw BMCException.unknown("could not remove, referred by route");
        }
        UnlinkMessage unlinkMsg = new UnlinkMessage();
        unlinkMsg.setLink(target);
        propagateInternal(Internal.Unlink, unlinkMsg.toBytes());
        Link link = links.remove(target);
        link.getRelays().clear();

        BigInteger networkId = btpLinkNetworkIds.get(_link);
        if (networkId != null) {
            btpLinkNetworkIds.set(_link, null);
            btpLinkOffset.set(networkId, null);
        }

        fees.remove(target.net());
    }

    @External(readonly = true)
    public Map getStatus(String _link) {
        BTPAddress target = BTPAddress.valueOf(_link);
        Link link = getLink(target);
        Map<String, Object> map = new HashMap<>();
        map.put("tx_seq", link.getTxSeq());
        map.put("rx_seq", link.getRxSeq());
        BMVScoreInterface verifier = getVerifier(link.getAddr().net());
        map.put("verifier", verifier.getStatus());
        map.put("cur_height", Context.getBlockHeight());
        return map;
    }

    @External(readonly = true)
    public String[] getLinks() {
        List<BTPAddress> keySet = links.keySet();
        int len = keySet.size();
        String[] links = new String[len];
        for (int i = 0; i < len; i++) {
            links[i] = keySet.get(i).toString();
        }
        return links;
    }

    @External
    public void addRoute(String _dst, String _link) {
        requireOwnerAccess();
        if (_dst.equals(_link)) {
            throw BMCException.unknown("invalid _dst");
        }
        BTPAddress dst = BTPAddress.valueOf(_dst);
        if (routes.containsKey(dst.net())) {
            throw BMCException.unknown("already exists route");
        }
        //TODO [TBD] check reachable
        BTPAddress target = BTPAddress.valueOf(_link);
        requireLink(target);
        routes.put(dst.net(), new Route(dst, target));
    }

    @External
    public void removeRoute(String _dst) {
        requireOwnerAccess();
        BTPAddress dst = BTPAddress.valueOf(_dst);
        Route old = routes.get(dst.net());
        if (old == null || !old.getDestination().equals(dst)){
            throw BMCException.unknown("not exists route");
        }
        routes.remove(dst.net());
        fees.remove(dst.net());
    }

    static Map<String, String> toMap(Route route) {
        Map<String, String> map = new HashMap<>();
        map.put("destination", route.getDestination().toString());
        map.put("next", route.getNext().toString());
        return map;
    }

    @External(readonly = true)
    public Map getRoutes() {
        Map<String, Map<String, String>> routeMap = new HashMap<>();
        for(Link link : links.values()) {
            routeMap.put(link.getAddr().net(), toMap(new Route(link.getAddr(),link.getAddr())));
            for (BTPAddress reachable : link.getReachable()) {
                if (!routeMap.containsKey(reachable.net())) {
                    routeMap.put(reachable.net(), toMap(new Route(reachable,link.getAddr())));
                }
            }
        }
        for(Route route : routes.values()) {
            routeMap.put(route.getDestination().net(), toMap(route));
        }
        return routeMap;
    }

    @External
    public void setFeeTable(String[] _dst, BigInteger[][] _value) {
        requireOwnerAccess();
        if (_dst.length != _value.length) {
            throw BMCException.unknown("invalid array length");
        }
        for (int i=0; i<_dst.length; i++){
            String dstNet = _dst[i];
            BigInteger[] values = _value[i];
            if (values != null && values.length > 0) {
                if ((values.length % 2) != 0) {
                    throw BMCException.unknown("length of _value["+i+"] must be even");
                }
                for (int j=0; j<values.length; j++){
                    if (values[j].compareTo(BigInteger.ZERO) < 0) {
                        throw BMCException.unknown("_value["+i+"]["+j+"] must be positive");
                    }
                }
                int hop = values.length / 2;
                if (hop == 1) {
                    if (resolveRouteFromLinks(dstNet) == null) {
                        throw BMCException.notExistsLink();
                    }
                } else {
                    if (routes.get(dstNet) == null && resolveRouteFromReachable(dstNet) == null) {
                        throw BMCException.unreachable();
                    }
                }
                fees.put(dstNet, new Fee(dstNet, values));
            } else {
                fees.remove(dstNet);
            }
        }
    }

    private BigInteger[] getFeeList(String net, boolean response) {
        Fee fee = fees.get(net);
        if (fee == null) {
            return new BigInteger[]{};
        }
        return response ? fee.getValues() :
                ArrayUtil.copyOf(fee.getValues(), fee.getValues().length / 2);
    }

    @External(readonly = true)
    public BigInteger getFee(String _to, boolean _response) {
        resolveRoute(_to);
        return ArrayUtil.sum(getFeeList(_to, _response));
    }

    @External(readonly = true)
    public BigInteger[][] getFeeTable(String[] _dst) {
        BigInteger[][] ret = new BigInteger[_dst.length][];
        for (int i=0; i<_dst.length; i++) {
            resolveRoute(_dst[i]);
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
                for(BigInteger v : feeList) {
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

    private BigInteger nextInternalSn() {
        BigInteger sn = internalSn.getOrDefault(BigInteger.ZERO);
        sn = sn.add(BigInteger.ONE);
        internalSn.set(sn);
        return sn;
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
        //FIXME [TBD] To transfer remainFee, '_receiver.isEmpty()' condition should be removed
        //If _receiver is empty, caller's reward will be burned.
        //specially if caller is feeHandler, the reward is remained fee.
        //If feeHandler has own reward and want burn it, BURN_ADDRESS could be used at _receiver
        if (fh != null && fh.equals(caller) && _receiver.isEmpty()) {
            caller = Context.getAddress();
        }
        BigInteger reward = getReward(_network, caller);
        if (BigInteger.ZERO.compareTo(reward) >= 0) {
            throw BMCException.unknown("not exists claimable reward");
        }
        rewards.at(caller).set(_network, BigInteger.ZERO);
        if (_network.equals(btpAddr.net())) {
            if (_receiver.isEmpty()) {
                burnReward(reward);
            } else {
                Context.transfer(toAddress(_receiver), reward);
            }
        } else {
            ClaimRewardMessage msg = new ClaimRewardMessage(reward, _receiver);
            BMCMessage bmcMessage = new BMCMessage(Internal.ClaimReward.name(), msg.toBytes());
            BigInteger sn = nextInternalSn();
            requests.set(sn, new BMCRequest(_network, bmcMessage, caller));
            BigInteger nsn = sendMessageWithFee(_network, INTERNAL_SERVICE, sn, bmcMessage.toBytes(), false);
            ClaimReward(_network, _receiver, reward, sn, nsn);
        }
    }

    @EventLog
    public void ClaimReward(String _network, String _receiver, BigInteger _amount, BigInteger _sn, BigInteger _nsn) {}

    private void burnReward(BigInteger reward) {
        //FIXME How to burn
        Context.transfer(BURN_ADDRESS, reward);
    }

    @External(readonly = true)
    public BigInteger getReward(String _network, Address _addr) {
        return rewards.at(_addr).getOrDefault(_network, BigInteger.ZERO);
    }

    @External
    public void setFeeHandler(Address _addr) {
        feeHandler.set(_addr);
    }

    @External(readonly = true)
    public Address getFeeHandler() {
        return feeHandler.get();
    }

    private Route resolveRouteFromLinks(String _net) {
        int size = links.size();
        for (int i = 0; i < size; i++) {
            BTPAddress key = links.getKey(i);
            if (_net.equals(key.net())) {
                return new Route(key, key);
            }
        }
        return null;
    }

    private Route resolveRouteFromReachable(String _net) {
        int size = links.size();
        for (int i = 0; i < size; i++) {
            Link link = links.getValue(i);
            for (BTPAddress reachable : link.getReachable()) {
                if (_net.equals(reachable.net())) {
                    return new Route(reachable, link.getAddr());
                }
            }
        }
        return null;
    }

    private Route resolveRoute(String _net) {
        Route route = resolveRouteFromLinks(_net);
        if (route != null) {
            return route;
        }

        route = routes.get(_net);
        if (route != null) {
            return route;
        }

        route = resolveRouteFromReachable(_net);
        if (route == null) {
            throw BMCException.unreachable();
        }
        return route;
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
        BMVStatus prevStatus = verifier.getStatus();

        // decode and verify relay message
        byte[][] serializedMsgs = null;
        try {
            serializedMsgs = verifier.handleRelayMessage(btpAddr.toString(), _prev, rxSeq, msgBytes);
        } catch (UserRevertedException e) {
            logger.println("handleRelayMessage",
                    "fail to verifier.handleRelayMessage",
                    "code:", e.getCode(), "msg:", e.getMessage());
            throw BTPException.of(e);
        }
        long msgCount = serializedMsgs.length;

        Address caller = Context.getCaller();
        Relays relays = link.getRelays();
        Relay relay = relays.get(caller);
        if (relay == null) {
            throw BMCException.unauthorized("not registered relay");
        }
        BMVStatus status = verifier.getStatus();
        relay.setBlockCount(relay.getBlockCount() + status.getHeight() - prevStatus.getHeight());
        relay.setMsgCount(relay.getMsgCount().add(BigInteger.valueOf(msgCount)));
        relays.put(relay.getAddress(), relay);
        if (msgCount > 0) {
            link.setRxSeq(rxSeq.add(BigInteger.valueOf(msgCount)));
        }
        putLink(link);

        // dispatch BTPMessages
        for (byte[] serializedMsg : serializedMsgs) {
            BTPMessage msg = null;
            try {
                msg = BTPMessage.fromBytes(serializedMsg);
            } catch (Exception e) {
                //TODO [TBD] ignore BTPMessage parse failure?
                logger.println("handleRelayMessage","fail to parse BTPMessage err:", e.getMessage());
            }
            logger.println("handleRelayMessage", "BTPMessage = ", msg);
            if (msg != null) {
                logger.println("handleRelayMessage", "btpAddr = ", btpAddr.net(), ", to = ", msg.getDst().net());
                accumulateFee(caller, msg.getFeeInfo());
                if (btpAddr.net().equals(msg.getDst().net())) {
                    handleMessage(prev, msg);
                } else {
                    try {
                        Route route = resolveRoute(msg.getDst().net());
                        sendMessage(route.getNext(), msg.toBytes());
                    } catch (BTPException e) {
                        sendError(prev, msg, e);
                    }
                }
            }
        }
    }

    private void handleMessage(BTPAddress prev, BTPMessage msg) {
        BTPAddress src = msg.getSrc();
        String svc = msg.getSvc();
        BigInteger sn = msg.getSn();
        byte[] payload = msg.getPayload();
        FeeInfo feeInfo = msg.getFeeInfo();
        int snCompare = sn.compareTo(BigInteger.ZERO);
        if (snCompare > -1) {
            DictDB<BigInteger, FeeInfo> responseFeeInfosDictDb = null;
            if (feeInfo != null) {
                if (snCompare > 0) {
                    responseFeeInfosDictDb = responseFeeInfos.at(src.net()).at(msg.getSvc());
                    FeeInfo oldFeeInfo = responseFeeInfosDictDb.get(sn);
                    if (oldFeeInfo != null) {
                        collectRemainFee(oldFeeInfo);
                    }
                    responseFeeInfosDictDb.set(sn, msg.getFeeInfo());
                } else {
                    collectRemainFee(feeInfo);
                }
            }

            try {
                if (svc.equals(INTERNAL_SERVICE)) {
                    internalHandleBTPMessage(src, sn, payload);
                } else {
                    BSHScoreInterface service = getService(svc);
                    service.handleBTPMessage(src.net(), svc, sn, payload);
                }
            } catch (Exception e) {
                if (responseFeeInfosDictDb != null) {
                    responseFeeInfosDictDb.set(sn, null);
                }
                sendError(prev, msg, toBTPException(e));
            }
        } else {
            sn = sn.negate();
            collectRemainFee(feeInfo);
            long eCode = -1;
            String eMsg = null;
            try {
                ErrorMessage errorMsg = ErrorMessage.fromBytes(payload);
                eCode = errorMsg.getCode();
                eMsg = errorMsg.getMsg() == null ? "" : errorMsg.getMsg();
                if (svc.equals(INTERNAL_SERVICE)) {
                    internalHandleBTPError(src, sn, eCode, eMsg);
                } else {
                    BSHScoreInterface service = getService(svc);
                    service.handleBTPError(src.toString(), svc, sn, eCode, eMsg);
                }
            } catch (Exception e) {
                BTPException be = toBTPException(e);
                logger.println("handleMessage", "fail to handleBTPError",
                        "code:", be.getCode(), "msg:", be.getMessage());
                emitErrorOnBTPError(svc, sn, eCode, eMsg, be.getCode(), be.getMessage());
            }
        }
    }

    private BTPException toBTPException(Exception e) {
        if (e instanceof BTPException) {
            return (BTPException)e;
        } else if (e instanceof UserRevertedException) {
            return BTPException.of((UserRevertedException) e);
        } else {
            return BTPException.unknown(e.toString());
        }
    }

    private void internalHandleBTPMessage(BTPAddress src, BigInteger sn, byte[] msg) {
        BMCMessage bmcMsg = BMCMessage.fromBytes(msg);
        Internal internal = Internal.of(bmcMsg.getType());
        byte[] payload = bmcMsg.getPayload();
        switch (internal) {
            case Init:
                InitMessage initMsg = InitMessage.fromBytes(payload);
                addReachable(src, initMsg.getLinks());
                break;
            case Link:
                LinkMessage linkMsg = LinkMessage.fromBytes(payload);
                addReachable(src, linkMsg.getLink());
                break;
            case Unlink:
                UnlinkMessage unlinkMsg = UnlinkMessage.fromBytes(payload);
                removeReachable(src, unlinkMsg.getLink());
                break;
            case ClaimReward:
                ClaimRewardMessage claimMsg = ClaimRewardMessage.fromBytes(payload);
                BigInteger reward = claimMsg.getAmount();
                if (reward.compareTo(BigInteger.ZERO) > 0) {
                    String receiver = claimMsg.getReceiver();
                    if (receiver == null || receiver.isEmpty()) {
                        burnReward(reward);
                    } else {
                        addReward(toAddress(receiver), btpAddr.net(), reward);
                    }
                }
                sendInternalResponse(src.net(), sn, ResponseMessage.CODE_SUCCESS);
                break;
            case Response:
                ResponseMessage responseMsg = ResponseMessage.fromBytes(payload);
                handleResponse(responseMsg.getRequestSn(),
                        ResponseMessage.CODE_SUCCESS == responseMsg.getCode());
                break;
        }
    }

    private void addReachable(BTPAddress _link, BTPAddress ... reachable) {
        Link link = getLink(_link);
        List<BTPAddress> list = link.getReachable();
        for (BTPAddress address : reachable) {
            if (!list.contains(address)) {
                list.add(address);
            }
        }
        putLink(link);
    }

    private void removeReachable(BTPAddress _link, BTPAddress address) {
        Link link = getLink(_link);
        link.getReachable().remove(address);
        putLink(link);
    }

    private void handleResponse(BigInteger sn, boolean success) {
        BMCRequest request = requests.get(sn);
        if (request != null) {
            requests.set(sn, null);
            BMCMessage requestBmcMsg = request.getMsg();
            if (Internal.ClaimReward.name().equals(requestBmcMsg.getType())) {
                if (!success) {
                    ClaimRewardMessage requestClaimMsg =
                            ClaimRewardMessage.fromBytes(requestBmcMsg.getPayload());
                    addReward(request.getCaller(), request.getDst(), requestClaimMsg.getAmount());
                }
            }
        }
    }

    private void internalHandleBTPError(BTPAddress src, BigInteger sn, long code, String msg) {
        logger.println("internalHandleBTPError",
                "src:", src, "src:", sn, "code:", code, "msg:", msg);
        handleResponse(sn, false);
    }

    private void emitErrorOnBTPError(String _svc, BigInteger _sn, long _code, String _msg, long _ecode, String _emsg) {
        ErrorOnBTPError(
                _svc,
                _sn,
                BigInteger.valueOf(_code),
                _msg == null ? "" : _msg,
                BigInteger.valueOf(_ecode),
                _emsg == null ? "" : _emsg);
    }

    private BigInteger nextNetworkSn() {
        BigInteger sn = networkSn.getOrDefault(BigInteger.ZERO);
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
        Route route = resolveRoute(_to);
        BTPMessage btpMsg = new BTPMessage();
        btpMsg.setSrc(btpAddr);
        btpMsg.setDst(route.getDestination());
        btpMsg.setSvc(_svc);
        btpMsg.setSn(isResponse ? BigInteger.ZERO : _sn);
        btpMsg.setPayload(msg);

        BigInteger nsn = nextNetworkSn();
        FeeInfo feeInfo;
        if (isResponse) {
            DictDB<BigInteger, FeeInfo> responseFeeInfosDictDb = responseFeeInfos.at(_to).at(_svc);
            feeInfo = responseFeeInfosDictDb.get(_sn);
            if (feeInfo == null) {
                throw BMCException.unknown("not exists response");
            }
            responseFeeInfosDictDb.set(_sn, null);
            collectRemainFee(btpAddr.net(), Context.getValue());
        } else {
            BigInteger[] values = getFeeList(_to, _sn.compareTo(BigInteger.ZERO) > 0);
            BigInteger remain = Context.getValue().subtract(ArrayUtil.sum(values));
            if (remain.compareTo(BigInteger.ZERO) < 0 ) {
                logger.println("sendMessage", "not enough fee", remain);
                throw BMCException.unknown("not enough fee");
            }
            feeInfo = new FeeInfo(btpAddr.net(), values);
            collectRemainFee(btpAddr.net(), remain);
        }

        btpMsg.setFeeInfo(feeInfo);
        sendMessage(route.getNext(), btpMsg);
        return nsn;
    }

    private void sendMessage(BTPAddress next, BTPMessage msg) {
        sendMessage(next, msg.toBytes());
    }

    private void sendError(BTPAddress prev, BTPMessage msg, BTPException e) {
        FeeInfo feeInfo = msg.getFeeInfo();
        if (msg.getSn().compareTo(BigInteger.ZERO) > 0) {
            if (feeInfo != null) {
                String feeNet = feeInfo.getNetwork();
                BigInteger[] feeList = feeInfo.getValues();
                if (feeNet != null && feeList != null && feeList.length > 0) {
                    int hop = getFeeList(msg.getSrc().net(), false).length;
                    if (hop > 0) {
                        if (feeList.length > hop) {
                            //swap not-consumed and to-be-consumed
                            int remainLen = feeList.length-hop;
                            BigInteger[] nextFeeList = new BigInteger[feeList.length];
                            System.arraycopy(feeList, remainLen, nextFeeList, 0, hop);
                            System.arraycopy(feeList, 0, nextFeeList, hop, remainLen);
                            feeInfo.setValues(nextFeeList);
                        }
                    }
                }
            }
            ErrorMessage errMsg = new ErrorMessage();
            errMsg.setCode(e.getCode());
            errMsg.setMsg(e.getMessage());
            BTPMessage btpMsg = new BTPMessage();
            btpMsg.setSrc(btpAddr);
            btpMsg.setDst(msg.getSrc());
            btpMsg.setSvc(msg.getSvc());
            btpMsg.setSn(msg.getSn().negate());
            btpMsg.setPayload(errMsg.toBytes());
            btpMsg.setFeeInfo(feeInfo);
            sendMessage(prev, btpMsg);
        } else {
            collectRemainFee(feeInfo);
            //TODO emit MessageDropped(ErrorOnUnidirectionalMessage)
        }
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
                throw BMCException.unknown(e.toString());
            }
        }
    }

    private void sendInternalResponse(String net, BigInteger sn, long code) {
        ResponseMessage responseMessage = new ResponseMessage();
        responseMessage.setRequestSn(sn);
        responseMessage.setCode(code);
        BMCMessage bmcMessage = new BMCMessage(Internal.Response.name(), responseMessage.toBytes());
        sendMessageWithFee(net, INTERNAL_SERVICE, sn, bmcMessage.toBytes(), true);
    }

    private void sendInternal(BTPAddress link, Internal internal, byte[] payload) {
        BMCMessage bmcMsg = new BMCMessage();
        bmcMsg.setType(internal.name());
        bmcMsg.setPayload(payload);

        BTPMessage btpMsg = new BTPMessage();
        btpMsg.setSrc(btpAddr);
        btpMsg.setDst(link);
        btpMsg.setSvc(INTERNAL_SERVICE);
        btpMsg.setSn(BigInteger.ZERO);
        btpMsg.setPayload(bmcMsg.toBytes());
        sendMessage(link, btpMsg);
    }

    private void propagateInternal(Internal internal, byte[] payload) {
        for (BTPAddress link : links.keySet()) {
            sendInternal(link, internal, payload);
        }
    }

    @EventLog(indexed = 2)
    public void Message(String _next, BigInteger _seq, byte[] _msg) {
    }

    @EventLog(indexed = 2)
    public void ErrorOnBTPError(String _svc, BigInteger _sn, BigInteger _code, String _msg, BigInteger _ecode, String _emsg) {
    }

    @External
    public void handleFragment(String _prev, String _msg, int _idx) {
        logger.println("handleFragment", "_prev",_prev,"_idx:",_idx, "len(_msg):"+_msg.length());
        BTPAddress prev = BTPAddress.valueOf(_prev);
        Link link = getLink(prev);
        Relays relays = link.getRelays();
        if (!relays.containsKey(Context.getOrigin())) {
            throw BMCException.unauthorized("not registered relay");
        }
        byte[] fragmentBytes = Base64.getUrlDecoder().decode(_msg.getBytes());
        final int INDEX_LAST = 0;
        final int INDEX_NEXT = 1;
        final int INDEX_OFFSET = 2;
        ArrayDB<byte[]> fragments = this.fragments.at(_prev).at(Context.getOrigin());
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
                byte[][] bytesArr = new byte[last+1][];
                for(int i = 0; i < last; i++){
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
                logger.println("handleFragment", "handleRelayMessage","fragments:",last+1, "len:"+total);
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

    @External
    public void dropMessage(String _src, BigInteger _seq, String _svc, BigInteger _sn, String _feeNetwork, BigInteger[] _feeValues) {
        requireOwnerAccess();
        BTPAddress src = BTPAddress.valueOf(_src);
        BTPAddress next = resolveRoute(src.net()).getNext();
        Link link = getLink(next);
        if(link.getRxSeq().add(BigInteger.ONE).compareTo(_seq) != 0) {
            throw BMCException.unknown("invalid _seq");
        }
        if(!services.containsKey(_svc)) {
            throw BMCException.notExistsBSH();
        }
        if(_sn.compareTo(BigInteger.ZERO) < 0) {
            throw BMCException.invalidSn();
        }
        link.setRxSeq(_seq);
        putLink(link);

        BTPMessage assumeMsg = new BTPMessage();
        assumeMsg.setSrc(src);
        assumeMsg.setDst(BTPAddress.parse(""));
        assumeMsg.setSvc(_svc);
        assumeMsg.setSn(_sn);
        assumeMsg.setPayload(new byte[0]);
        if (!_feeNetwork.isEmpty()) {
            assumeMsg.setFeeInfo(new FeeInfo(_feeNetwork, _feeValues));
            accumulateFee(Context.getAddress(), assumeMsg.getFeeInfo());
        }
        MessageDropped(next.toString(), _seq, assumeMsg.toBytes());
        sendError(next, assumeMsg, BMCException.drop());
    }

    //FIXME add reason to MessageDropped
    @EventLog(indexed = 2)
    public void MessageDropped(String _link, BigInteger _seq, byte[] _msg) {}

    @External
    public void addRelay(String _link, Address _addr) {
        requireOwnerAccess();

        BTPAddress target = BTPAddress.valueOf(_link);
        Relays relays = getLink(target).getRelays();
        if (relays.containsKey(_addr)) {
            throw BMCException.alreadyExistsBMR();
        }
        Relay relay = new Relay();
        relay.setAddress(_addr);
        relay.setMsgCount(BigInteger.ZERO);
        relays.put(_addr, relay);
    }

    @External
    public void removeRelay(String _link, Address _addr) {
        requireOwnerAccess();

        BTPAddress target = BTPAddress.valueOf(_link);
        Relays relays = getLink(target).getRelays();
        if (!relays.containsKey(_addr)) {
            throw BMCException.notExistsBMR();
        }
        relays.remove(_addr);
    }

    @External(readonly = true)
    public BMRStatus[] getRelays(String _link) {
        BTPAddress target = BTPAddress.valueOf(_link);
        Relays relays = getLink(target).getRelays();
        BMRStatus[] arr = new BMRStatus[relays.size()];
        int i = 0;
        for (Relay relay : relays.values()) {
            BMRStatus s = new BMRStatus();
            s.setAddress(relay.getAddress());
            s.setBlock_count(relay.getBlockCount());
            s.setMsg_count(relay.getMsgCount());
            arr[i++] = s;
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

    //FIXME fallback is required?
    @Payable
    public void fallback() {
        logger.println("fallback","value:", Context.getValue());
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

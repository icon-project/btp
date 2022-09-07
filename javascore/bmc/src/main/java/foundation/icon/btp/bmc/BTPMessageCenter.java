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

import foundation.icon.btp.lib.*;
import foundation.icon.score.util.ArrayUtil;
import foundation.icon.score.util.Logger;
import foundation.icon.score.util.StringUtil;
import score.UserRevertedException;
import score.VarDB;
import score.ArrayDB;
import score.BranchDB;
import score.DictDB;
import score.Address;
import score.Context;
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
    private static final Address CHAIN_SCORE = Address.fromString("cx0000000000000000000000000000000000000000");

    public enum Internal {
        Init, Link, Unlink, Sack;

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
    private final DictDB<String, DropSequences> drops = Context.newDictDB("drops", DropSequences.class);

    //
    private final Verifiers verifiers = new Verifiers("verifiers");
    private final Services services = new Services("services");
    private final Routes routes = new Routes("routes");
    private final Links links = new Links("links");
    private final DictDB<String, BigInteger> btpLinkNetworkIds = Context.newDictDB("btpLinkNetworkIds", BigInteger.class);
    private final DictDB<BigInteger, BigInteger> btpLinkOffset = Context.newDictDB("btpLinkOffset", BigInteger.class);

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
        if (!services.containsKey(_svc)) {
            throw BMCException.notExistsBSH();
        }
        Address address = services.get(_svc);
        return new BSHScoreInterface(address);
    }

    private void requireLink(BTPAddress link) {
        if (!links.containsKey(link)) {
            throw BMCException.notExistsLink();
        }
    }

    //TODO flushable
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
        link.setSackSeq(BigInteger.ZERO);
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
        if (routes.values().contains(target)) {
            throw BMCException.unknown("could not remove, referred by route");
        }
        UnlinkMessage unlinkMsg = new UnlinkMessage();
        unlinkMsg.setLink(target);
        propagateInternal(Internal.Unlink, unlinkMsg.toBytes());
        Link link = links.remove(target);
        link.getRelays().clear();

        drops.set(_link, null);
        BigInteger networkId = btpLinkNetworkIds.get(_link);
        if (networkId != null) {
            btpLinkNetworkIds.set(_link, null);
            btpLinkOffset.set(networkId, null);
        }
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
        map.put("sack_term", link.getSackTerm());
        map.put("sack_next", link.getSackNext());
        map.put("sack_height", link.getSackHeight());
        map.put("sack_seq", link.getSackSeq());
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
        BTPAddress dst = BTPAddress.valueOf(_dst);
        if (routes.containsKey(dst)) {
            throw BMCException.unknown("already exists route");
        }
        BTPAddress target = BTPAddress.valueOf(_link);
        requireLink(target);
        routes.put(dst, target);
    }

    @External
    public void removeRoute(String _dst) {
        requireOwnerAccess();
        BTPAddress dst = BTPAddress.valueOf(_dst);
        if (!routes.containsKey(dst)) {
            throw BMCException.unknown("not exists route");
        }
        routes.remove(dst);
    }

    @External(readonly = true)
    public Map getRoutes() {
        Map<String, String> stringMap = new HashMap<>();
        for (Map.Entry<BTPAddress, BTPAddress> entry : routes.toMap().entrySet()) {
            stringMap.put(entry.getKey().toString(), entry.getValue().toString());
        }
        return stringMap;
    }

    private Map.Entry<BTPAddress, BTPAddress> resolveRoutePath(String _net) throws BMCException {
        for (BTPAddress key : routes.keySet()) {
            if (_net.equals(key.net())) {
                return Map.entry(key, routes.get(key));
            }
        }
        for (BTPAddress key : links.keySet()) {
            if (_net.equals(key.net())) {
                return Map.entry(key, key);
            }
        }
        for (BTPAddress key : links.keySet()) {
            Link link = links.get(key);
            for (BTPAddress reachable : link.getReachable()) {
                if (_net.equals(reachable.net())) {
                    return Map.entry(reachable, key);
                }
            }
        }
        throw BMCException.unreachable();
    }

    private BTPAddress resolveNext(BTPAddress dst) throws BMCException {
        if (routes.containsKey(dst)) {
            return routes.get(dst);
        }
        if (links.containsKey(dst)) {
            return dst;
        }
        for (BTPAddress key : links.keySet()) {
            Link link = links.get(key);
            for (BTPAddress reachable : link.getReachable()) {
                if (dst.equals(reachable)) {
                    return key;
                }
            }
        }
        throw BMCException.unreachable();
    }

    @External
    public void handleRelayMessage(String _prev, String _msg) {
        //FIXME Throws INVALID_RELAY_MSG=25?
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

        Relays relays = link.getRelays();
        long currentHeight = Context.getBlockHeight();
        Relay relay = relays.get(Context.getOrigin());
        if (relay == null) {
            throw BMCException.unauthorized("not registered relay");
        }
        BMVStatus status = verifier.getStatus();
        relay.setBlockCount(relay.getBlockCount() + status.getHeight() - prevStatus.getHeight());
        relay.setMsgCount(relay.getMsgCount().add(BigInteger.valueOf(msgCount)));
        relays.put(relay.getAddress(), relay);

        int oldDropSequencesLen = 0;
        DropSequences dropSequences = null;
        if (msgCount > 0) {
            link.setRxSeq(rxSeq.add(BigInteger.valueOf(msgCount)));
            dropSequences = drops.get(_prev);
            oldDropSequencesLen = dropSequences == null ? 0 : dropSequences.size();
        }
        putLink(link);

        // dispatch BTPMessages
        for (byte[] serializedMsg : serializedMsgs) {
            BTPMessage msg = null;
            try {
                //TODO [TBD] how to catch exception while processing in ByteArrayObjectReader?
                msg = BTPMessage.fromBytes(serializedMsg);
            } catch (Exception e) {
                //TODO [TBD] ignore BTPMessage parse failure?
                logger.println("handleRelayMessage","fail to parse BTPMessage err:", e.getMessage());
            }
            logger.println("handleRelayMessage", "BTPMessage = ", msg);
            if (msg != null) {
                rxSeq = rxSeq.add(BigInteger.ONE);
                if (dropSequences != null && dropSequences.remove(rxSeq)) {
                    if (msg.getSn().compareTo(BigInteger.ZERO) > 0) {
                        sendError(prev, msg, BMCException.drop());
                    }
                    MessageDropped(_prev, rxSeq, serializedMsg);
                } else {
                    logger.println("handleRelayMessage", "btpAddr = ", btpAddr.net(), ", to = ", msg.getDst().net());
                    if (btpAddr.net().equals(msg.getDst().net())) {
                        handleMessage(prev, msg);
                    } else {
                        try {
                            BTPAddress next = resolveNext(msg.getDst());
                            sendMessage(next, msg);
                        } catch (BTPException e) {
                            sendError(prev, msg, e);
                        }
                    }
                }
            }
        }

        if (dropSequences != null && oldDropSequencesLen != dropSequences.size()) {
            drops.set(_prev, dropSequences);
        }

        //sack
        link = getLink(prev);
        long sackTerm = link.getSackTerm();
        long sackNext = link.getSackNext();
        if (sackTerm > 0 && sackNext <= currentHeight) {
            sendSack(prev, status.getHeight(), link.getRxSeq());
            while(sackNext <= currentHeight) {
                sackNext += sackTerm;
            }
            link.setSackNext(sackNext);
            putLink(link);
        }
    }

    private void handleMessage(BTPAddress prev, BTPMessage msg) {
        if (msg.getSvc().equals(INTERNAL_SERVICE)) {
            handleInternal(prev, msg);
        } else {
            handleService(prev, msg);
        }
    }

    private void handleInternal(BTPAddress prev, BTPMessage msg) {
        BMCMessage bmcMsg = BMCMessage.fromBytes(msg.getPayload());
        byte[] payload = bmcMsg.getPayload();
        Internal internal = null;
        try {
            internal = Internal.of(bmcMsg.getType());
        } catch (IllegalArgumentException e) {
            //TODO exception handling
            logger.println("handleInternal", "not supported internal type", e.getMessage());
            return;
        }

        if (!prev.equals(msg.getSrc())) {
            throw BMCException.unknown("internal message not allowed from " + msg.getSrc().toString());
        }

        try {
            switch (internal) {
                case Init:
                    InitMessage initMsg = InitMessage.fromBytes(payload);
                    handleInit(prev, initMsg);
                    break;
                case Link:
                    LinkMessage linkMsg = LinkMessage.fromBytes(payload);
                    handleLink(prev, linkMsg);
                    break;
                case Unlink:
                    UnlinkMessage unlinkMsg = UnlinkMessage.fromBytes(payload);
                    handleUnlink(prev, unlinkMsg);
                    break;
                case Sack:
                    SackMessage sackMsg = SackMessage.fromBytes(payload);
                    handleSack(prev, sackMsg);
                    break;
            }
        } catch (BTPException e) {
            //TODO exception handling
            logger.println("handleInternal", internal, e);
        }
    }

    private void handleInit(BTPAddress prev, InitMessage msg) {
        logger.println("handleInit", "prev:", prev, "msg:", msg.toString());
        try {
            Link link = getLink(prev);
            for (BTPAddress reachable : msg.getLinks()) {
                link.getReachable().add(reachable);
            }
            putLink(link);
        } catch (BMCException e) {
            //TODO exception handling
            if (!BMCException.Code.NotExistsLink.equals(e)) {
                throw e;
            }
        }
    }

    private void handleLink(BTPAddress prev, LinkMessage msg) {
        logger.println("handleLink", "prev:", prev, "msg:", msg.toString());
        try {
            Link link = getLink(prev);
            BTPAddress reachable = msg.getLink();
            if (!link.getReachable().contains(reachable)) {
                link.getReachable().add(reachable);
                putLink(link);
            }
        } catch (BMCException e) {
            //TODO exception handling
            if (!BMCException.Code.NotExistsLink.equals(e)) {
                throw e;
            }
        }
    }

    private void handleUnlink(BTPAddress prev, UnlinkMessage msg) {
        logger.println("handleUnlink", "prev:", prev, "msg:", msg.toString());
        try {
            Link link = getLink(prev);
            BTPAddress reachable = msg.getLink();
            if (link.getReachable().contains(reachable)) {
                link.getReachable().remove(reachable);
                putLink(link);
            }
        } catch (BMCException e) {
            //TODO exception handling
            if (!BMCException.Code.NotExistsLink.equals(e)) {
                throw e;
            }
        }
    }

    private void handleSack(BTPAddress prev, SackMessage msg) {
        logger.println("handleSack", "prev:", prev, "msg:", msg.toString());
        Link link = getLink(prev);
        link.setSackHeight(msg.getHeight());
        link.setSackSeq(msg.getSeq());
        putLink(link);
    }

    private void handleService(BTPAddress prev, BTPMessage msg) {
        //TODO throttling in a tx, EOA_LIMIT, handleService_LIMIT each Link
        //  limit in block
        String svc = msg.getSvc();
        BigInteger sn = msg.getSn();
        if (sn.compareTo(BigInteger.ZERO) > -1) {
            try {
                BSHScoreInterface service = getService(svc);
                service.handleBTPMessage(msg.getSrc().net(), svc, msg.getSn(), msg.getPayload());
            } catch (BTPException e) {
                logger.println("handleService","fail to getService",
                        "code:", e.getCode(), "msg:", e.getMessage());
                sendError(prev, msg, e);
            } catch (UserRevertedException e) {
                logger.println("handleService", "fail to service.handleBTPMessage",
                        "code:", e.getCode(), "msg:", e.getMessage());
                sendError(prev, msg, BTPException.of(e));
            } catch (Exception e) {
                //TODO handle uncatchable exception?
                logger.println("handleService", "fail to service.handleBTPMessage",
                        "Exception:", e.toString());
                sendError(prev, msg, BTPException.unknown(e.getMessage()));
            }
        } else {
            sn = sn.negate();
            ErrorMessage errorMsg = null;
            try {
                errorMsg = ErrorMessage.fromBytes(msg.getPayload());
            } catch (Exception e) {
                logger.println("handleService", "fail to ErrorMessage.fromBytes",
                        "Exception:", e.toString());
                emitErrorOnBTPError(svc, sn, -1, null, -1, e.getMessage());
                return;
            }
            try {
                BSHScoreInterface service = getService(svc);
                service.handleBTPError(msg.getSrc().toString(), svc, sn, errorMsg.getCode(), errorMsg.getMsg() == null ? "" : errorMsg.getMsg());
            } catch (BTPException e) {
                logger.println("handleService","fail to getService",
                        "code:", e.getCode(), "msg:", e.getMessage());
                emitErrorOnBTPError(svc, sn, errorMsg.getCode(), errorMsg.getMsg(), e.getCode(), e.getMessage());
            } catch (UserRevertedException e) {
                logger.println("handleService", "fail to service.handleBTPError",
                        "code:", e.getCode(), "msg:", e.getMessage());
                emitErrorOnBTPError(svc, sn, errorMsg.getCode(), errorMsg.getMsg(), e.getCode(), e.getMessage());
            } catch (Exception e) {
                logger.println("handleService", "fail to service.handleBTPError",
                        "Exception:", e.toString());
                emitErrorOnBTPError(svc, sn, errorMsg.getCode(), errorMsg.getMsg(), -1, e.getMessage());
            }
        }
    }

    private void emitErrorOnBTPError(String _svc, BigInteger _seq, long _code, String _msg, long _ecode, String _emsg) {
        ErrorOnBTPError(
                _svc,
                _seq,
                BigInteger.valueOf(_code),
                _msg == null ? "" : _msg,
                BigInteger.valueOf(_ecode),
                _emsg == null ? "" : _emsg);
    }

    @External
    public void sendMessage(String _to, String _svc, BigInteger _sn, byte[] _msg) {
        Address addr = services.get(_svc);
        if (addr == null) {
            throw BMCException.notExistsBSH();
        }
        if (!Context.getCaller().equals(addr)) {
            throw BMCException.unauthorized();
        }
        if (_sn.compareTo(BigInteger.ZERO) < 1) {
            throw BMCException.invalidSn();
        }

        Map.Entry<BTPAddress, BTPAddress> routePath = resolveRoutePath(_to);

        //TODO (txSeq > sackSeq && (currentHeight - sackHeight) > THRESHOLD) ? revert
        //  THRESHOLD = (delayLimit * NUM_OF_ROTATION)
        BTPMessage btpMsg = new BTPMessage();
        btpMsg.setSrc(btpAddr);
        btpMsg.setDst(routePath.getKey());
        btpMsg.setSvc(_svc);
        btpMsg.setSn(_sn);
        btpMsg.setPayload(_msg);
        logger.println("sendMessage", "to = ", routePath.getValue(), ", btpMsg = ", btpMsg);
        sendMessage(routePath.getValue(), btpMsg);
    }

    private void sendMessage(BTPAddress to, BTPMessage msg) {
        sendMessage(to, msg.toBytes());
    }

    private void sendMessage(BTPAddress to, byte[] serializedMsg) {
        Link link = getLink(to);
        link.setTxSeq(link.getTxSeq().add(BigInteger.ONE));
        putLink(link);
        BigInteger networkId = btpLinkNetworkIds.get(to.toString());
        if (networkId == null) {
            Message(to.toString(), link.getTxSeq(), serializedMsg);
        } else {
            Context.call(CHAIN_SCORE, "sendBTPMessage", networkId, serializedMsg);
        }
    }

    private void sendError(BTPAddress prev, BTPMessage msg, BTPException e) {
        if (msg.getSn().compareTo(BigInteger.ZERO) > 0) {
            ErrorMessage errMsg = new ErrorMessage();
            errMsg.setCode(e.getCode());
            errMsg.setMsg(e.getMessage());
            BTPMessage btpMsg = new BTPMessage();
            btpMsg.setSrc(btpAddr);
            btpMsg.setDst(msg.getSrc());
            btpMsg.setSvc(msg.getSvc());
            btpMsg.setSn(msg.getSn().negate());
            btpMsg.setPayload(errMsg.toBytes());
            sendMessage(prev, btpMsg);
        }
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

    private void sendSack(BTPAddress link, long height, BigInteger seq) {
        logger.println("sendSack", "link:", link, "height:", height, "seq:", seq);
        SackMessage sackMsg = new SackMessage();
        sackMsg.setHeight(height);
        sackMsg.setSeq(seq);
        sendInternal(link, Internal.Sack, sackMsg.toBytes());
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
    public void ErrorOnBTPError(String _svc, BigInteger _seq, BigInteger _code, String _msg, BigInteger _ecode, String _emsg) {
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
    public void dropMessage(String _src, BigInteger _seq, String _svc, BigInteger _sn) {
        requireOwnerAccess();
        BTPAddress src = BTPAddress.valueOf(_src);
        BTPAddress next = resolveNext(src);
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
        sendError(next, assumeMsg, BMCException.drop());
        MessageDropped(next.toString(), _seq, assumeMsg.toBytes());
    }

    @External
    public void scheduleDropMessage(String _link, BigInteger _seq) {
        requireOwnerAccess();
        BTPAddress target = BTPAddress.valueOf(_link);
        Link link = getLink(target);
        if(link.getRxSeq().compareTo(_seq) >= 0) {
            throw BMCException.unknown("invalid _seq");
        }
        DropSequences dropSequences = drops.getOrDefault(_link, new DropSequences());
        dropSequences.add(_seq);
        drops.set(_link, dropSequences);
    }

    @External
    public void cancelDropMessage(String _link, BigInteger _seq) {
        requireOwnerAccess();
        requireLink(BTPAddress.valueOf(_link));
        DropSequences dropSequences = drops.get(_link);
        if (dropSequences == null || !dropSequences.remove(_seq)) {
            throw BMCException.unknown("not exists");
        }
        drops.set(_link, dropSequences);
    }

    @External(readonly = true)
    public BigInteger[] getScheduledDropMessages(String _link) {
        requireLink(BTPAddress.valueOf(_link));
        DropSequences dropSequences = drops.getOrDefault(_link, new DropSequences());
        return dropSequences.getSequences() == null ? new BigInteger[]{} : dropSequences.getSequences();
    }

    @EventLog(indexed = 2)
    public void MessageDropped(String _link, BigInteger _seq, byte[] _msg) {}

    @External
    public void setLinkSackTerm(String _link, int _value) {
        requireOwnerAccess();
        BTPAddress target = BTPAddress.valueOf(_link);
        Link link = getLink(target);
        if (_value < 0) {
            throw BMCException.unknown("invalid param");
        }
        link.setSackTerm(_value);
        link.setSackNext(Context.getBlockHeight()+_value);
        putLink(link);
    }

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
        for (int i = 0; i < relays.size(); i++) {
            Relay relay = relays.getByIndex(i);
            BMRStatus s = new BMRStatus();
            s.setAddress(relay.getAddress());
            s.setBlock_count(relay.getBlockCount());
            s.setMsg_count(relay.getMsgCount());
            arr[i] = s;
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
        setBTPLink(_link, _networkId, BigInteger.ZERO);
        addLink(_link);
    }

    @External
    public void setBTPLinkNetworkId(String _link, long _networkId) {
        Link link = getLink(BTPAddress.valueOf(_link));
        setBTPLink(_link, _networkId, link.getTxSeq());
    }

    private void setBTPLink(String _link, long _networkId, BigInteger offset) {
        requireOwnerAccess();
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

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
import foundation.icon.score.util.*;
import foundation.icon.btp.lib.*;
import foundation.icon.score.util.ArrayUtil;
import foundation.icon.score.util.Logger;
import foundation.icon.score.util.StringUtil;
import score.*;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Payable;
import scorex.util.ArrayList;
import scorex.util.HashMap;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

public class BTPMessageCenter implements BMC, BMCEvent, ICONSpecific, OwnerManager, RelayerManager {
    private static final Logger logger = Logger.getLogger(BTPMessageCenter.class);
    public static final int BLOCK_INTERVAL_MSEC = 2000;
    public static final String INTERNAL_SERVICE = "bmc";
    public enum Internal { Init, Link, Unlink, FeeGathering, Sack }

    //
    private final BTPAddress btpAddr;
    private final VarDB<BMCProperties> properties = Context.newVarDB("properties", BMCProperties.class);

    //
    private final OwnerManager ownerManager = new OwnerManagerImpl("owners");
    private final RelayerManager relayerManager = new RelayerManagerImpl("relayers");
    private final ArrayDB<ServiceCandidate> serviceCandidates = Context.newArrayDB("serviceCandidates", ServiceCandidate.class);
    private final BranchDB<String, BranchDB<Address, ArrayDB<String>>> fragments = Context.newBranchDB("fragments", String.class);

    //
    private final Verifiers verifiers = new Verifiers("verifiers");
    private final Services services = new Services("services");
    private final Routes routes = new Routes("routes");
    private final Links links = new Links("links");

    public BTPMessageCenter(String _net) {
        this.btpAddr = new BTPAddress(BTPAddress.PROTOCOL_BTP, _net, Context.getAddress().toString());
    }

    public BMCProperties getProperties() {
        return properties.getOrDefault(BMCProperties.DEFAULT);
    }

    public void setProperties(BMCProperties properties) {
        this.properties.set(properties);
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
        if (getServiceCandidateIndex(_svc, _addr) >= 0) {
            removeServiceCandidate(_svc, _addr);
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
        LinkMessage linkMsg = new LinkMessage();
        linkMsg.setLink(target);
        propagateInternal(Internal.Link, linkMsg.toBytes());

        List<BTPAddress> list = this.links.keySet();
        int size = list.size();
        BTPAddress[] links = new BTPAddress[size];
        for (int i = 0; i < size; i++) {
            links[i] = list.get(i);
        }
        InitMessage initMsg = new InitMessage();
        initMsg.setLinks(links);

        Link link = new Link();
        link.setAddr(target);
        link.setRxSeq(BigInteger.ZERO);
        link.setTxSeq(BigInteger.ZERO);
        link.setBlockIntervalSrc(BLOCK_INTERVAL_MSEC);
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
    }

    @External(readonly = true)
    public BMCStatus getStatus(String _link) {
        BTPAddress target = BTPAddress.valueOf(_link);
        BMCStatus status = new BMCStatus();

        Link link = getLink(target);
        status.setTx_seq(link.getTxSeq());
        status.setRx_seq(link.getRxSeq());
        status.setRelay_idx(link.getRelayIdx());
        status.setRotate_height(link.getRotateHeight());
        status.setRotate_term(link.rotateTerm());
        status.setDelay_limit(link.getDelayLimit());
        status.setMax_agg(link.getMaxAggregation());
        status.setRx_height(link.getRxHeight());
        status.setRx_height_src(link.getRxHeightSrc());
        status.setBlock_interval_dst(link.getBlockIntervalDst());
        status.setBlock_interval_src(link.getBlockIntervalSrc());
        status.setSack_term(link.getSackTerm());
        status.setSack_next(link.getSackNext());
        status.setSack_height(link.getSackHeight());
        status.setSack_seq(link.getSackSeq());
        status.setCur_height(Context.getBlockHeight());

        Map<Address, Relay> relayMap = link.getRelays().toMap();
        BMRStatus[] relays = new BMRStatus[relayMap.size()];
        int i = 0;
        for (Map.Entry<Address, Relay> entry : relayMap.entrySet()) {
            Relay relay = entry.getValue();
            BMRStatus bmrStatus = new BMRStatus();
            bmrStatus.setAddress(relay.getAddress());
            bmrStatus.setBlock_count(relay.getBlockCount());
            bmrStatus.setMsg_count(relay.getMsgCount());
            relays[i++] = bmrStatus;
        }
        status.setRelays(relays);

        BMVScoreInterface bmv = getVerifier(link.getAddr().net());
        BMVStatus bmvStatus = bmv.getStatus();
        //FIXME currently javaee-rt eliminate not-called functions include default constructor
        //  and @Keep has not ElementType.CONSTRUCTOR in @Target
        status.setVerifier(new BMVStatus());
        status.setVerifier(bmvStatus);
        return status;
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
        if (routes.containsKey(_dst)) {
            throw BMCException.unknown("already exists route");
        }
        BTPAddress target = BTPAddress.valueOf(_link);
        requireLink(target);
        routes.put(_dst, target);
    }

    @External
    public void removeRoute(String _dst) {
        requireOwnerAccess();
        if (!routes.containsKey(_dst)) {
            throw BMCException.unknown("not exists route");
        }
        routes.remove(_dst);
    }

    @External(readonly = true)
    public Map getRoutes() {
        Map<String, String> stringMap = new HashMap<>();
        for (Map.Entry<String, BTPAddress> entry : routes.toMap().entrySet()) {
            stringMap.put(entry.getKey(), entry.getValue().toString());
        }
        return stringMap;
    }

    private BTPAddress resolveRoute(String _net) {
        if (routes.containsKey(_net)) {
            return routes.get(_net);
        } else {
            for (String key : routes.keySet()) {
                if (_net.equals(BTPAddress.parse(key).net())) {
                    return routes.get(key);
                }
            }
        }
        return null;
    }

    private Link resolveNext(String _net) throws BMCException {
        BTPAddress next = resolveRoute(_net);
        if (next == null) {
            for (BTPAddress key : links.keySet()) {
                if (_net.equals(key.net())) {
                    return links.get(key);
                }
            }
            // TODO [TBD] reachable enabled
            for (BTPAddress key : links.keySet()) {
                Link link = links.get(key);
                for (BTPAddress reachable : link.getReachable()) {
                    if (_net.equals(reachable.net())) {
                        return link;
                    }
                }
            }
            throw BMCException.unreachable();
        } else {
            return getLink(next);
        }
    }

    @External
    public void handleRelayMessage(String _prev, String _msg) {
        BTPAddress prev = BTPAddress.valueOf(_prev);
        Link link = getLink(prev);
        BMVScoreInterface verifier = getVerifier(link.getAddr().net());
        BMVStatus prevStatus = verifier.getStatus();
        // decode and verify relay message
        byte[][] serializedMsgs = null;
        try {
            serializedMsgs = verifier.handleRelayMessage(btpAddr.toString(), _prev, link.getRxSeq(), _msg);
        } catch (UserRevertedException e) {
            logger.println("handleRelayMessage",
                    "fail to verifier.handleRelayMessage",
                    "code:", e.getCode(), "msg:", e.getMessage());
            throw BTPException.of(e);
        }
        long msgCount = serializedMsgs.length;

        // rotate and check valid relay
        BMVStatus status = verifier.getStatus();
        Relays relays = link.getRelays();
        Relay relay = link.rotate(Context.getBlockHeight(), status.getLast_height(), msgCount > 0);
        if (relay == null) {
            //rotateRelay is disabled.
            relay = relays.get(Context.getOrigin());
            if (relay == null) {
                throw BMCException.unauthorized("not registered relay");
            }
        } else if (!relay.getAddress().equals(Context.getOrigin())) {
            throw BMCException.unauthorized("invalid relay");
        }
        relay.setBlockCount(relay.getBlockCount() + status.getHeight() - prevStatus.getHeight());
        relay.setMsgCount(relay.getMsgCount().add(BigInteger.valueOf(msgCount)));
        relays.put(relay.getAddress(), relay);

        link.setRxSeq(link.getRxSeq().add(BigInteger.valueOf(msgCount)));
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
            if (msg != null) {
                if (btpAddr.equals(msg.getDst())) {
                    handleMessage(prev, msg);
                } else {
                    try {
                        Link next = resolveNext(msg.getDst().net());
                        sendMessage(next.getAddr(), msg);
                    } catch (BTPException e) {
                        sendError(prev, msg, e);
                    }
                }
            }
        }

        //sack
        link = getLink(prev);
        if (link.getSackTerm() > 0 && link.getSackNext() <= Context.getBlockHeight()) {
            sendSack(prev, status.getHeight(), link.getRxSeq());
            link.setSackNext(link.getSackNext() + link.getSackTerm());
            putLink(link);
        }

        //gatherFee
        BMCProperties properties = getProperties();
        Address feeAggregator = properties.getFeeAggregator();
        long gatherFeeTerm = properties.getFeeGatheringTerm();
        long gatherFeeNext = properties.getFeeGatheringNext();
        if (services.size() > 0 && feeAggregator != null &&
                gatherFeeTerm > 0 &&
                gatherFeeNext <= Context.getBlockHeight()) {
            String[] svcs = ArrayUtil.toStringArray(services.keySet());
            sendFeeGathering(feeAggregator, svcs);
            gatherFeeNext = gatherFeeNext + gatherFeeTerm;
            properties.setFeeGatheringNext(gatherFeeNext);
            setProperties(properties);
        }

        distributeRelayerReward();
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
            internal = Internal.valueOf(bmcMsg.getType());
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
                case FeeGathering:
                    FeeGatheringMessage feeGatheringMsg = FeeGatheringMessage.fromBytes(payload);
                    handleFeeGathering(prev, feeGatheringMsg);
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

    private void handleFeeGathering(BTPAddress prev, FeeGatheringMessage msg) {
        logger.println("handleFeeGathering", "prev:", prev, "msg:", msg.toString());
        if (!prev.net().equals(msg.getFa().net())) {
            throw BMCException.unknown("not allowed GatherFeeMessage from:"+prev.net());
        }
        String[] svcs = msg.getSvcs();
        if (svcs.length < 1) {
            throw BMCException.unknown("requires svcs.length > 1");
        }
        String fa = msg.getFa().toString();
        for (String svc : svcs) {
            try {
                BSHScoreInterface service = getService(svc);
                service.handleFeeGathering(fa, svc);
            } catch (BTPException e) {
                if (!BMCException.Code.NotExistsBSH.equals(e)) {
                    //TODO exception handling
                    logger.println("handleGatherFee", svc, e);
                }
            } catch (UserRevertedException e) {
                //TODO exception handling
                logger.println("handleGatherFee", "fail to service.handleFeeGathering",
                        "code:", e.getCode(), "msg:", e.getMessage());
            } catch (Exception e) {
                //TODO handle uncatchable exception?
                logger.println("handleGatherFee", "fail to service.handleFeeGathering",
                        "Exception:", e.toString());
            }
        }
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
            try {
                ErrorMessage errorMsg = ErrorMessage.fromBytes(msg.getPayload());
                try {
                    BSHScoreInterface service = getService(svc);
                    service.handleBTPError(msg.getSrc().toString(), svc, sn, errorMsg.getCode(), errorMsg.getMsg() == null ? "" : errorMsg.getMsg());
                } catch (BTPException e) {
                    logger.println("handleService","fail to getService",
                            "code:", e.getCode(), "msg:", e.getMessage());
                    ErrorOnBTPError(svc, sn, errorMsg.getCode(), errorMsg.getMsg(), e.getCode(), e.getMessage());
                } catch (UserRevertedException e) {
                    logger.println("handleService", "fail to service.handleBTPError",
                            "code:", e.getCode(), "msg:", e.getMessage());
                    ErrorOnBTPError(svc, sn, errorMsg.getCode(), errorMsg.getMsg(), e.getCode(), e.getMessage());
                } catch (Exception e) {
                    logger.println("handleService", "fail to service.handleBTPError",
                            "Exception:", e.toString());
                    ErrorOnBTPError(svc, sn, -1, null, -1, e.getMessage());
                }
            } catch (Exception e) {
                logger.println("handleService", "fail to ErrorMessage.fromBytes",
                        "Exception:", e.toString());
                ErrorOnBTPError(svc, sn, -1, null, -1, e.getMessage());
            }
        }
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
        Link link = resolveNext(_to);

        //TODO (txSeq > sackSeq && (currentHeight - sackHeight) > THRESHOLD) ? revert
        //  THRESHOLD = (delayLimit * NUM_OF_ROTATION)
        BTPMessage btpMsg = new BTPMessage();
        btpMsg.setSrc(btpAddr);
        btpMsg.setDst(link.getAddr());
        btpMsg.setSvc(_svc);
        btpMsg.setSn(_sn);
        btpMsg.setPayload(_msg);
        sendMessage(link.getAddr(), btpMsg);
    }

    private void sendMessage(BTPAddress to, BTPMessage msg) {
        sendMessage(to, msg.toBytes());
    }

    private void sendMessage(BTPAddress to, byte[] serializedMsg) {
        Link link = getLink(to);
        link.setTxSeq(link.getTxSeq().add(BigInteger.ONE));
        putLink(link);
        Message(to.toString(), link.getTxSeq(), serializedMsg);
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

    private void sendFeeGathering(Address addr, String[] svcs) {
        logger.println("sendFeeGathering", "addr:", addr, "svcs:", StringUtil.toString(svcs));
        BTPAddress fa = new BTPAddress(BTPAddress.PROTOCOL_BTP, btpAddr.net(), addr.toString());
        FeeGatheringMessage feeGatheringMsg = new FeeGatheringMessage();
        feeGatheringMsg.setFa(fa);
        feeGatheringMsg.setSvcs(svcs);
        handleFeeGathering(btpAddr, feeGatheringMsg);
        propagateInternal(Internal.FeeGathering, feeGatheringMsg.toBytes());
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
    public void ErrorOnBTPError(String _svc, BigInteger _seq, long _code, String _msg, long _ecode, String _emsg) {
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
        final int INDEX_LAST = 0;
        final int INDEX_NEXT = 1;
        final int INDEX_OFFSET = 2;
        ArrayDB<String> fragments = this.fragments.at(_prev).at(Context.getOrigin());
        if (_idx < 0) {
            int last = _idx * -1;
            if (fragments.size() == 0) {
                fragments.add(Integer.toString(last));
                fragments.add(Integer.toString(last - 1));
                fragments.add(_msg);
            } else {
                fragments.set(INDEX_LAST, Integer.toString(last));
                fragments.set(INDEX_NEXT, Integer.toString(last - 1));
                fragments.set(INDEX_OFFSET, _msg);
            }
        } else {
            int next = Integer.parseInt(fragments.get(INDEX_NEXT));
            if (next != _idx) {
                throw BMCException.unknown("invalid _idx");
            }
            int last = Integer.parseInt(fragments.get(INDEX_LAST));
            if (_idx == 0) {
                StringBuilder msg = new StringBuilder();
                for(int i = 0; i < last; i++){
                    msg.append(fragments.get(i + INDEX_OFFSET));
                }
                msg.append(_msg);
                logger.println("handleFragment", "handleRelayMessage","fragments:",last+1, "len:"+msg.length());
                handleRelayMessage(_prev, msg.toString());;
            } else {
                fragments.set(INDEX_NEXT, Integer.toString(_idx - 1));
                int INDEX_MSG = last - _idx + INDEX_OFFSET;
                if (INDEX_MSG < fragments.size()) {
                    fragments.set(INDEX_MSG, _msg);
                } else {
                    fragments.add(_msg);
                }
            }
        }
    }

    @External
    public void setLinkRotateTerm(String _link, int _block_interval, int _max_agg) {
        requireOwnerAccess();
        BTPAddress target = BTPAddress.valueOf(_link);
        Link link = getLink(target);
        if (_block_interval < 0 || _max_agg < 1) {
            throw BMCException.unknown("invalid param");
        }
        int oldRotateTerm = link.rotateTerm();
        link.setBlockIntervalDst(_block_interval);
        link.setMaxAggregation(_max_agg);
        int rotateTerm = link.rotateTerm();
        if (oldRotateTerm == 0 && rotateTerm > 0) {
            long currentHeight = Context.getBlockHeight();
            link.setRotateHeight(currentHeight + rotateTerm);
            link.setRxHeight(currentHeight);
            BMVScoreInterface verifier = getVerifier(target.net());
            link.setRxHeightSrc(verifier.getStatus().getHeight());
        }
        putLink(link);
    }

    @External
    public void setLinkDelayLimit(String _link, int _value) {
        requireOwnerAccess();
        BTPAddress target = BTPAddress.valueOf(_link);
        Link link = getLink(target);
        if (_value < 1) {
            throw BMCException.unknown("invalid param");
        }
        link.setDelayLimit(_value);
        putLink(link);
    }

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

    private int getServiceCandidateIndex(String svc, Address addr) {
        for (int i = 0; i < serviceCandidates.size(); i++) {
            ServiceCandidate sc = serviceCandidates.get(i);
            if (sc.getSvc().equals(svc) && sc.getAddress().equals(addr)) {
                return i;
            }
        }
        return -1;
    }

    @External
    public void addServiceCandidate(String _svc, Address _addr) {
        if (getServiceCandidateIndex(_svc, _addr) >= 0) {
            throw BMCException.unknown("already exists ServiceCandidate");
        }
        ServiceCandidate sc = new ServiceCandidate();
        sc.setSvc(_svc);
        sc.setAddress(_addr);
        sc.setOwner(Context.getOrigin());
        serviceCandidates.add(sc);
    }

    @External
    public void removeServiceCandidate(String _svc, Address _addr) {
        requireOwnerAccess();
        int idx = getServiceCandidateIndex(_svc, _addr);
        if (idx < 0) {
            throw BMCException.unknown("not exists ServiceCandidate");
        }
        ServiceCandidate last = serviceCandidates.pop();
        if (idx != serviceCandidates.size()) {
            serviceCandidates.set(idx, last);
        }
    }

    @External(readonly = true)
    public ServiceCandidate[] getServiceCandidates() {
        int size = this.serviceCandidates.size();
        ServiceCandidate[] serviceCandidates = new ServiceCandidate[size];
        for (int i = 0; i < size; i++) {
            serviceCandidates[i] = this.serviceCandidates.get(i);
        }
        return serviceCandidates;
    }

    @External(readonly = true)
    public Address[] getRelays(String _link) {
        BTPAddress target = BTPAddress.valueOf(_link);
        Relays relays = getLink(target).getRelays();
        return ArrayUtil.toAddressArray(relays.keySet());
    }

    @External
    public void sendFeeGathering() {
        requireOwnerAccess();
        if (services.size() == 0) {
            throw BMCException.unknown("services is empty");
        }
        Address feeAggregator = getFeeAggregator();
        if (feeAggregator == null) {
            throw BMCException.unknown("feeAggregator is null");
        }
        String[] svcs = ArrayUtil.toStringArray(services.keySet());
        sendFeeGathering(feeAggregator, svcs);
    }

    @External(readonly = true)
    public long getFeeGatheringTerm() {
        BMCProperties properties = getProperties();
        return properties.getFeeGatheringTerm();
    }

    @External
    public void setFeeGatheringTerm(long _value) {
        requireOwnerAccess();
        BMCProperties properties = getProperties();
        if (_value < 0) {
            throw BMCException.unknown("invalid param");
        }
        properties.setFeeGatheringTerm(_value);
        properties.setFeeGatheringNext(Context.getBlockHeight()+_value);
        setProperties(properties);
    }

    @External(readonly = true)
    public Address getFeeAggregator() {
        BMCProperties properties = getProperties();
        return properties.getFeeAggregator();
    }

    @External
    public void setFeeAggregator(Address _addr) {
        requireOwnerAccess();
        BMCProperties properties = getProperties();
        properties.setFeeAggregator(_addr);
        setProperties(properties);
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

    /* Delegate RelayerManager */
    @Payable
    @External
    public void registerRelayer(String _desc) {
        relayerManager.registerRelayer(_desc);
    }

    @External
    public void unregisterRelayer() {
        relayerManager.unregisterRelayer();
    }

    @External
    public void removeRelayer(Address _addr, Address _refund) {
        requireOwnerAccess();
        relayerManager.removeRelayer(_addr, _refund);
    }

    @External(readonly = true)
    public Map<String, Relayer> getRelayers() {
        return relayerManager.getRelayers();
    }

    @External
    public void distributeRelayerReward() {
        relayerManager.distributeRelayerReward();
    }

    //FIXME fallback is required?
    @Payable
    public void fallback() {
        logger.println("fallback","value:", Context.getValue());
    }

    @External
    public void claimRelayerReward() {
        relayerManager.claimRelayerReward();
    }

    @External(readonly = true)
    public BigInteger getRelayerMinBond() {
        return relayerManager.getRelayerMinBond();
    }

    @External
    public void setRelayerMinBond(BigInteger _value) {
        requireOwnerAccess();
        relayerManager.setRelayerMinBond(_value);
    }

    @External(readonly = true)
    public long getRelayerTerm() {
        return relayerManager.getRelayerTerm();
    }

    @External
    public void setRelayerTerm(long _value) {
        requireOwnerAccess();
        relayerManager.setRelayerTerm(_value);
    }

    @External
    public void setNextRewardDistribution(long _height) {
        requireOwnerAccess();
        relayerManager.setNextRewardDistribution(_height);
    }

    @External(readonly = true)
    public int getRelayerRewardRank() {
        return relayerManager.getRelayerRewardRank();
    }

    @External(readonly = true)
    public RelayerManagerProperties getRelayerManagerProperties() {
        return relayerManager.getRelayerManagerProperties();
    }

    @External
    public void setRelayerRewardRank(int _value) {
        requireOwnerAccess();
        relayerManager.setRelayerRewardRank(_value);
    }

}

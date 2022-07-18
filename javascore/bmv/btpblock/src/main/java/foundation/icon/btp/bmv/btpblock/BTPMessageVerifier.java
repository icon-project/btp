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

package foundation.icon.btp.bmv.btpblock;

import foundation.icon.btp.lib.BMV;
import foundation.icon.btp.lib.BTPAddress;
import foundation.icon.score.util.Logger;
import foundation.icon.score.util.StringUtil;
import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.External;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class BTPMessageVerifier implements BMV {
    private static final Logger logger = Logger.getLogger(BTPMessageVerifier.class);
    private static String HASH = "sha3-256";
    private static String SIGNATURE_ALG = "ecdsa-secp256k1";
    private final VarDB<BMVProperties> propertiesDB = Context.newVarDB("properties", BMVProperties.class);

    public BTPMessageVerifier(byte[] srcNetworkID, int networkTypeID, Address bmc, byte[] blockHeader, BigInteger seqOffset) {
        BMVProperties bmvProperties = getProperties();
        bmvProperties.setSrcNetworkID(srcNetworkID);
        bmvProperties.setNetworkTypeID(networkTypeID);
        bmvProperties.setBmc(bmc);
        bmvProperties.setSequenceOffset(seqOffset);
        handleFirstBlockHeader(BlockHeader.fromBytes(blockHeader), bmvProperties);
    }

    public BMVProperties getProperties() {
        return propertiesDB.getOrDefault(BMVProperties.DEFAULT);
    }

    @External
    public byte[][] handleRelayMessage(String _bmc, String _prev, BigInteger _seq, byte[] _msg) {
        BTPAddress curAddr = BTPAddress.valueOf(_bmc);
        BTPAddress prevAddr = BTPAddress.valueOf(_prev);
        checkAccessible(curAddr, prevAddr);
        var bmvProperties = getProperties();
        var lastSeq = bmvProperties.getLastSequence();
        var seq = bmvProperties.getSequenceOffset().add(lastSeq);
        if (seq.compareTo(_seq) != 0) throw BMVException.unknown("invalid sequence");
        RelayMessage relayMessages = RelayMessage.fromBytes(_msg);
        RelayMessage.TypePrefixedMessage[] typePrefixedMessages = relayMessages.getMessages();
        BlockUpdate blockUpdate = null;
        List<byte[]> msgList = new ArrayList<>();
        for (RelayMessage.TypePrefixedMessage message : typePrefixedMessages) {
            Object msg = message.getMessage();
            if (msg instanceof BlockUpdate) {
                blockUpdate = (BlockUpdate) msg;
                handleBlockUpdateMessage(blockUpdate);
            } else if (msg instanceof MessageProof) {
                var msgs = handleMessageProof((MessageProof) msg, blockUpdate);
                for(byte[] m : msgs) {
                    msgList.add(m);
                }
            }
        }
        var retSize = msgList.size();
        var ret = new byte[retSize][];
        if (retSize > 0) {
            for (int i = 0; i < retSize; i ++) {
                ret[i] = msgList.get(i);
            }
        }
        return ret;
    }

    @External(readonly = true)
    public Map getStatus() {
        var properties = getProperties();
        return Map.of(
                "height", properties.getHeight().longValue(),
                "sequence_offset", properties.getSequenceOffset().longValue(),
                "first_message_sn", properties.getLastFirstMessageSN().longValueExact(),
                "message_count", properties.getLastMessageCount().longValue()
        );
    }

    private void handleFirstBlockHeader(BlockHeader blockHeader, BMVProperties bmvProperties) {
        var prev = blockHeader.getPrev();
        if (prev != null) throw BMVException.invalidBlockUpdate("not first blockUpdate");
        var updateNumber = blockHeader.getUpdateNumber();
        var blockUpdateNid = blockHeader.getNid();
        var msgCnt = blockHeader.getMessageCount();
        var msgRoot = blockHeader.getMessageRoot();
        NetworkSection ns = new NetworkSection(
                blockUpdateNid,
                updateNumber,
                null,
                msgCnt,
                msgRoot
        );
        var nsHash = ns.hash();
        var nextProofContextHash = blockHeader.getNextProofContextHash();
        var nextProofContext = blockHeader.getNextProofContext();
        if (!Arrays.equals(hash(nextProofContext), nextProofContextHash))
            throw BMVException.invalidBlockUpdate("mismatch Hash of proofContext");
        bmvProperties.setNetworkID(blockUpdateNid);
        bmvProperties.setProofContextHash(nextProofContextHash);
        bmvProperties.setProofContext(nextProofContext);
        bmvProperties.setLastNetworkSectionHash(nsHash);
        bmvProperties.setLastSequence(BigInteger.ZERO);
        bmvProperties.setLastMessagesRoot(msgRoot);
        bmvProperties.setLastMessageCount(msgCnt);
        bmvProperties.setLastFirstMessageSN(blockHeader.getFirstMessageSn());
        bmvProperties.setHeight(blockHeader.getMainHeight());
        propertiesDB.set(bmvProperties);
    }

    private void handleBlockUpdateMessage(BlockUpdate blockUpdate) {
        var bmvProperties = propertiesDB.get();
        var networkID = bmvProperties.getNetworkID();
        var blockHeader = blockUpdate.getBlockHeader();
        var updateNumber = blockHeader.getUpdateNumber();
        var blockUpdateNid = blockHeader.getNid();
        var prev = blockHeader.getPrev();
        if (bmvProperties.getRemainMessageCount().compareTo(BigInteger.ZERO) != 0) throw BMVException.invalidBlockUpdate("remain must be zero");
        if (networkID.compareTo(blockUpdateNid) != 0) throw BMVException.invalidBlockUpdate("invalid network id");
        if (!Arrays.equals(bmvProperties.getLastNetworkSectionHash(), prev)) throw BMVException.invalidBlockUpdate("mismatch networkSectionHash");
        if (bmvProperties.getLastSequence().compareTo(blockHeader.getFirstMessageSn()) != 0) throw BMVException.invalidBlockUpdate("invalid first message sequence of blockUpdate");
        NetworkSection ns = new NetworkSection(
                blockUpdateNid,
                updateNumber,
                prev,
                blockHeader.getMessageCount(),
                blockHeader.getMessageRoot()
        );
        var nsHash = ns.hash();
        var nsRoot = blockHeader.getNetworkSectionsRoot(nsHash);
        var nextProofContextHash = blockHeader.getNextProofContextHash();
        NetworkTypeSection nts = new NetworkTypeSection(nextProofContextHash, nsRoot);
        var srcNetworkID = bmvProperties.getSrcNetworkID();
        var networkTypeID = bmvProperties.getNetworkTypeID();
        var height = blockHeader.getMainHeight();
        var round = blockHeader.getRound();
        var ntsHash = nts.hash();
        NetworkTypeSectionDecision decision = new NetworkTypeSectionDecision(
                srcNetworkID, networkTypeID, height.longValue(), round.intValue(), ntsHash);
        Proofs proofs = Proofs.fromBytes(blockUpdate.getBlockProof());
        var isUpdate = updateNumber.and(BigInteger.ONE).compareTo(BigInteger.ONE) == 0;
        verifyProof(decision, proofs);
        if (isUpdate) {
            var nextProofContext = blockHeader.getNextProofContext();
            verifyProofContextData(nextProofContextHash, nextProofContext, bmvProperties.getProofContextHash());
            bmvProperties.setProofContextHash(nextProofContextHash);
            bmvProperties.setProofContext(nextProofContext);
        }
        bmvProperties.setLastMessagesRoot(blockHeader.getMessageRoot());
        bmvProperties.setLastMessageCount(blockHeader.getMessageCount());
        bmvProperties.setLastFirstMessageSN(blockHeader.getFirstMessageSn());
        bmvProperties.setLastNetworkSectionHash(nsHash);
        bmvProperties.setHeight(blockHeader.getMainHeight());
        propertiesDB.set(bmvProperties);
    }

    private void verifyProofContextData(byte[] proofContextHash, byte[] proofContext, byte[] currentProofContextHash) {
        if (Arrays.equals(currentProofContextHash, proofContextHash)) throw BMVException.invalidBlockUpdate("mismatch UpdateFlag");
        if (!Arrays.equals(hash(proofContext), proofContextHash)) throw BMVException.invalidBlockUpdate("mismatch Hash of NextProofContext");
    }

    private void verifyProof(NetworkTypeSectionDecision decision, Proofs proofs) {
        byte[] decisionHash = decision.hash();
        byte[][] sigs = proofs.getProofs();
        List<Address> verifiedValidator = new ArrayList<>();
        var bmvProperties = propertiesDB.get();
        var proofContextBytes = bmvProperties.getProofContext();
        var proofContext = ProofContext.fromBytes(proofContextBytes);
        for (byte[] sig : sigs) {
            Address address = recoverAddress(decisionHash, sig);
            if (!proofContext.isValidator(address)) throw BMVException.invalidProofs("invalid validator : " + address);
            if (verifiedValidator.contains(address)) throw BMVException.invalidProofs("duplicated validator : " + address);
            verifiedValidator.add(address);
        }
        var verified = verifiedValidator.size();
        var validatorsCnt = proofContext.getValidators().length;
        //quorum = validator * 2/3
        if (verified * 3 <= validatorsCnt * 2)
                throw BMVException.invalidProofs("not enough proof parts num of validator : " + validatorsCnt + ", num of proof parts : " + verified);
    }

    private byte[][] handleMessageProof(MessageProof messageProof, BlockUpdate blockUpdate) {
        byte[] expectedMessageRoot;
        BigInteger expectedMessageCnt;
        var bmvProperties = propertiesDB.get();
        if (bmvProperties.getRemainMessageCount().compareTo(BigInteger.ZERO) <= 0)
            throw BMVException.invalidMessageProof("remaining message count must greater than zero");
        MessageProof.ProveResult result = messageProof.proveMessage();
        if (bmvProperties.getProcessedMessageCount().compareTo(BigInteger.valueOf(result.offset)) != 0)
            throw BMVException.invalidMessageProof("invalid ProofInLeft.NumberOfLeaf");
        if (blockUpdate != null ) {
            var blockHeader = blockUpdate.getBlockHeader();
            expectedMessageRoot = blockHeader.getMessageRoot();
            expectedMessageCnt = blockHeader.getMessageCount();
            if (0 < result.offset) {
                throw BMVException.invalidMessageProof("ProofInLeft should be empty");
            }
        } else {
            expectedMessageRoot = bmvProperties.getLastMessagesRoot();
            expectedMessageCnt = bmvProperties.getLastMessageCount();
            if (bmvProperties.getLastSequence().subtract(bmvProperties.getLastFirstMessageSN()).intValue() != result.offset) {
                throw BMVException.invalidMessageProof("mismatch ProofInLeft");
            }
        }
        if (expectedMessageCnt.intValue() != result.total) {
            var rightProofNodes = messageProof.getRightProofNodes();
            for (int i = 0; i < rightProofNodes.length; i++) {
                logger.println("ProofInRight["+i+"] : " + "NumOfLeaf:"+rightProofNodes[i].getNumOfLeaf()
                + "value:" + StringUtil.bytesToHex(rightProofNodes[i].getValue()));
            }
            throw BMVException.invalidMessageProof(
                    "mismatch MessageCount offset:" + result.offset +
                            ", expected:" + expectedMessageCnt +
                            ", count :" + result.total);
        }
        if (!Arrays.equals(result.hash, expectedMessageRoot)) throw BMVException.invalidMessageProof("mismatch MessagesRoot");
        var msgCnt = messageProof.getMessages().length;
        var remainCnt = result.total - result.offset - msgCnt;
        if (remainCnt == 0) {
            bmvProperties.setLastMessagesRoot(null);
        }
        bmvProperties.setLastSequence(bmvProperties.getLastSequence().add(BigInteger.valueOf(msgCnt)));
        propertiesDB.set(bmvProperties);
        return messageProof.getMessages();
    }

    static byte[] hash(byte[] msg) {
        return Context.hash(HASH, msg);
    }

    static Address recoverAddress(byte[] msg, byte[] sig) {
        byte[] publicKey = Context.recoverKey(SIGNATURE_ALG, msg, sig, true);
        return Context.getAddressFromKey(publicKey);
    }

    private void checkAccessible(BTPAddress curAddr, BTPAddress fromAddress) {
        BMVProperties properties = getProperties();
        if (!properties.getNetwork().equals(fromAddress.net())) {
            throw BMVException.permissionDenied("invalid prev bmc");
        } else if (!Context.getCaller().equals(properties.getBmc())) {
            throw BMVException.permissionDenied("invalid caller bmc");
        } else if (!Address.fromString(curAddr.account()).equals(properties.getBmc())) {
            throw BMVException.permissionDenied("invalid current bmc");
        }
    }
}

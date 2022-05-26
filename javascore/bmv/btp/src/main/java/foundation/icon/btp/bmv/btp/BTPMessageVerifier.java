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

package foundation.icon.btp.bmv.btp;

import foundation.icon.btp.lib.BMV;
import foundation.icon.btp.lib.BMVStatus;
import foundation.icon.score.util.Logger;
import foundation.icon.score.util.StringUtil;
import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.External;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class BTPMessageVerifier implements BMV {
    private static final Logger logger = Logger.getLogger(BTPMessageVerifier.class);
    private static String HASH = "keccak-256";
    private static String SIGNATURE_ALG = "ecdsa-secp256k1";
    private final VarDB<BMVProperties> propertiesDB = Context.newVarDB("properties", BMVProperties.class);

    public BTPMessageVerifier(byte[] srcNetworkID, int networkTypeID, Address bmc) {
        BMVProperties bmvProperties = getProperties();
        bmvProperties.setSrcNetworkID(srcNetworkID);
        bmvProperties.setNetworkID(networkTypeID);
        bmvProperties.setBmc(bmc);
        propertiesDB.set(bmvProperties);
    }

    public BMVProperties getProperties() {
        return propertiesDB.getOrDefault(BMVProperties.DEFAULT);
    }

    @External
    public byte[][] handleRelayMessage(String _bmc, String _prev, BigInteger _seq, String _msg) {
        RelayMessage relayMessages = RelayMessage.fromBytes(_msg.getBytes(StandardCharsets.UTF_8));
        RelayMessage.TypePrefixedMessage[] typePrefixedMessages = relayMessages.getMessages();
        BlockUpdate blockUpdate = null;
        for (RelayMessage.TypePrefixedMessage message : typePrefixedMessages) {
            Object msg = message.getMessage();
            if (msg instanceof BlockUpdate) {
                blockUpdate = (BlockUpdate) msg;
                handleBlockUpdateMessage(blockUpdate);
            } else if (msg instanceof MessageProof) {
                handleMessageProof((MessageProof) msg, blockUpdate);
            }
        }
        return new byte[0][];
    }

    @External(readonly = true)
    public BMVStatus getStatus() {
        return null;
    }

    private void handleBlockUpdateMessage(BlockUpdate blockUpdate) {
        var bmvProperties = propertiesDB.get();
        var networkID = bmvProperties.getNetworkID();
        var updateNumber = blockUpdate.getUpdateNumber();
        var blockUpdateNid = blockUpdate.getNid();
        var prev = blockUpdate.getPrev();
        Context.require(networkID == blockUpdateNid, "invalid network id");
        Context.require(Arrays.equals(bmvProperties.getLastNetworkSectionHash(), prev), "mismatch networkSectionHash");
        Context.require(bmvProperties.getLastSequence() == updateNumber >> 1, "invalid first message sequence of blockUpdate");

        NetworkSection ns = new NetworkSection(
                blockUpdateNid,
                updateNumber,
                prev,
                blockUpdate.getMessageCount(),
                blockUpdate.getMessageRoot()
        );
        var nsHash = ns.hash();
        var nsRoot = blockUpdate.getNetworkSectionsRoot(nsHash);
        var nextProofContextHash = blockUpdate.getNextProofContextHash();
        NetworkTypeSection nts = new NetworkTypeSection(nextProofContextHash, nsRoot);
        var srcNetworkID = bmvProperties.getSrcNetworkID();
        var networkTypeID = bmvProperties.getNetworkTypeID();
        var height = blockUpdate.getMainHeight();
        var round = blockUpdate.getRound();
        var ntsHash = nts.hash();
        NetworkTypeSectionDecision decision = new NetworkTypeSectionDecision(
                srcNetworkID, networkTypeID, height.longValue(), round.intValue(), ntsHash);
        Proofs proofs = Proofs.fromBytes(blockUpdate.getProof());
        verifyProof(decision, proofs);

        var isUpdate = (updateNumber & 1) == 1;
        if (isUpdate) {
            var nextProofContext = blockUpdate.getNextProofContext();
            verifyProofContextData(nextProofContextHash, nextProofContext, bmvProperties.getProofContextHash());
            bmvProperties.setProofContextHash(nextProofContextHash);
            bmvProperties.setProofContext(nextProofContext);
        }
        bmvProperties.setLastNetworkSectionHash(nsHash);
        propertiesDB.set(bmvProperties);
    }

    private void verifyProofContextData(byte[] proofContextHash, byte[] proofContext, byte[] currentProofContextHash) {
        Context.require(!Arrays.equals(currentProofContextHash, proofContextHash), "mismatch UpdateFlag");
        Context.require(Arrays.equals(hash(proofContext), proofContextHash), "mismatch Hash of NextProofContext");
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
            Context.require(proofContext.isValidator(address), "invalid validator : " + address);
            Context.require(!verifiedValidator.contains(address), "duplicated validator : " + address);
            verifiedValidator.add(address);
        }
        var verified = verifiedValidator.size();
        var validatorsCnt = proofContext.getValidators().length;
        //quorum = validator * 2/3
        Context.require(verified * 3 >= validatorsCnt * 2,
                "not enough proof parts num of validator : " + validatorsCnt + ", num of proof parts : " + verified);
    }

    private void handleMessageProof(MessageProof messageProof, BlockUpdate blockUpdate) {
    }

    static byte[] hash(byte[] msg) {
        return Context.hash(HASH, msg);
    }

    static Address recoverAddress(byte[] msg, byte[] sig) {
        byte[] publicKey = Context.recoverKey(SIGNATURE_ALG, msg, sig, true);
        int sliceLen = publicKey.length - 1;
        byte[] sliced = new byte[sliceLen];
        System.arraycopy(publicKey, 1, sliced, 0, sliceLen);
        byte[] hashed = hash(sliced);
        byte[] addr = new byte[20];
        System.arraycopy(hashed, 12, addr, 0, 20);
        return new Address(addr);
    }
}

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

package foundation.icon.btp.bmv.icon;

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

    private final VarDB<BMVProperties> properties = Context.newVarDB("properties", BMVProperties.class);

    public BTPMessageVerifier(Address _bmc, String _net, String _validators, long _offset) {
        BMVProperties properties = getProperties();
        properties.setBmc(_bmc);
        properties.setNet(_net);
        Validators validators = Validators.fromString(_validators);
        properties.setValidators(validators);
        if (properties.getLastHeight() == 0) {
            properties.setLastHeight(_offset);
        }
        if (properties.getMta() == null) {
            MerkleTreeAccumulator mta = new MerkleTreeAccumulator();
            mta.setHeight(_offset);
            mta.setOffset(_offset);
            properties.setMta(mta);
        }
        setProperties(properties);
    }

    static byte[] hash(byte[] bytes) {
        return Context.hash("sha3-256",bytes);
    }

    static Address recoverAddress(byte[] msg, byte[] sig, boolean compressed) {
        byte[] publicKey = Context.recoverKey("ecdsa-secp256k1", msg, sig, compressed);
        return Context.getAddressFromKey(publicKey);
    }

    public BMVProperties getProperties() {
        return properties.getOrDefault(BMVProperties.DEFAULT);
    }

    public void setProperties(BMVProperties properties) {
        this.properties.set(properties);
    }

    @External
    public byte[][] handleRelayMessage(String _bmc, String _prev, BigInteger _seq, byte[] _msg) {
        BTPAddress curAddr = BTPAddress.valueOf(_bmc);
        BTPAddress prevAddr = BTPAddress.valueOf(_prev);
        checkAccessible(curAddr, prevAddr);

        RelayMessage relayMessage = RelayMessage.fromBytes(_msg);
        BlockUpdate[] blockUpdates = relayMessage.getBlockUpdates();
        BlockProof blockProof = relayMessage.getBlockProof();
        ReceiptProof[] receiptProofs = relayMessage.getReceiptProofs();
        BlockHeader lastBlockHeader;
        BMVProperties properties = getProperties();
        MerkleTreeAccumulator mta = properties.getMta();
        if (blockUpdates != null && blockUpdates.length > 0) {
            Validators validators = verifyBlockUpdates(blockUpdates, mta, properties.getValidators());
            properties.setMta(mta);
            if (validators != null) {
                properties.setValidators(validators);
            }
            lastBlockHeader = blockUpdates[blockUpdates.length - 1].getBlockHeader();
        } else if (blockProof != null) {
            verifyBlockProof(blockProof, mta);
            lastBlockHeader = blockProof.getBlockHeader();
        } else {
            throw BMVException.unknown("invalid RelayMessage, not includes BlockUpdate or BlockProof");
        }

        byte[][] ret = new byte[0][];
        if (receiptProofs != null && receiptProofs.length > 0) {
            BigInteger next_seq = _seq.add(BigInteger.ONE);
            List<byte[]> msgs = new ArrayList<>();
            if (lastBlockHeader.getResult() == null) {
                throw BMVException.unknown("invalid RelayMessage, BlockHeader has not receiptHash");
            }
            byte[] receiptHash = lastBlockHeader.getResult().getReceiptHash();
            for(ReceiptProof receiptProof : receiptProofs) {
                Receipt receipt = proveReceiptProof(receiptProof, receiptHash);
                for(EventLog eventLog : receipt.getEventLogs()) {
                    if(!(prevAddr.account().equals(eventLog.getAddress().toString()))) {
                        continue;
                    }
                    MessageEvent msgEvent = eventLog.toMessageEvent();
                    if (msgEvent != null && msgEvent.getNext().equals(_bmc)) {
                        int compare = msgEvent.getSeq().compareTo(next_seq);
                        if (compare > 0) {
                            throw BMVException.invalidSequenceHigher(
                                    "invalid sequence "+msgEvent.getSeq() + " expected:"+next_seq);
                        } else if (compare < 0) {
                            throw BMVException.invalidSequence(
                                    "invalid sequence "+msgEvent.getSeq() + " expected:"+next_seq);
                        } else {
                            msgs.add(msgEvent.getMsg());
                            next_seq = next_seq.add(BigInteger.ONE);
                        }
                    }
                }
            }
            if (msgs.size() > 0) {
                properties.setLastHeight(lastBlockHeader.getHeight());
                ret = new byte[msgs.size()][];
                int i = 0;
                for (byte[] msg : msgs) {
                    ret[i] = msg;
                }
            }
        }

        if ((blockUpdates != null && blockUpdates.length > 0) || ret.length > 0) {
            setProperties(properties);
        }
        return ret;
    }

    private Receipt proveReceiptProof(ReceiptProof receiptProof, byte[] receiptHash) {
        try {
            byte[] serializedReceipt = MerklePatriciaTree.prove(
                    receiptHash,
                    MerklePatriciaTree.encodeKey(receiptProof.getIndex()),
                    receiptProof.getProofs().getProofs());
            Receipt receipt = Receipt.fromBytes(serializedReceipt);
            byte[] eventLogsHash = receipt.getEventLogsHash();
            MPTProof[] eventProofs = receiptProof.getEventProofs();
            if (eventProofs != null) {
                EventLog[] eventLogs = new EventLog[eventProofs.length];
                int i=0;
                for(MPTProof eventProof : eventProofs){
                    byte[] serializedEventLog = MerklePatriciaTree.prove(
                            eventLogsHash,
                            MerklePatriciaTree.encodeKey(eventProof.getIndex()),
                            eventProof.getProofs().getProofs());
                    EventLog eventLog = EventLog.fromBytes(serializedEventLog);
                    eventLogs[i++] = eventLog;
                }
                receipt.setEventLogs(eventLogs);
            }
            return receipt;
        } catch (MerklePatriciaTree.MPTException e) {
            throw BMVException.invalidMPT(e.getMessage());
        }
    }

    private Validators verifyBlockUpdates(BlockUpdate[] blockUpdates, MerkleTreeAccumulator mta, Validators validators) {
        boolean isValidatorsUpdate = false;
        byte[] validatorHash = hash(validators.toBytes());
        for(BlockUpdate blockUpdate : blockUpdates) {
            BlockHeader blockHeader = blockUpdate.getBlockHeader();
            long blockHeight = blockHeader.getHeight();
            long nextHeight = mta.getHeight() + 1;
            if (nextHeight == blockHeight) {
                byte[] blockHash = hash(blockHeader.toBytes());
                verifyVotes(blockUpdate.getVotes(), blockHeight, blockHash, validators);
                byte[] nextValidatorHash = blockHeader.getNextValidatorHash();
                if (!(Arrays.equals(validatorHash, nextValidatorHash))) {
                    Validators nextValidators = blockUpdate.getNextValidators();
                    if (nextValidators == null) {
                        throw BMVException.invalidBlockUpdate("not exists next validator");
                    }
                    if (!(Arrays.equals(hash(nextValidators.toBytes()), nextValidatorHash))) {
                        throw BMVException.invalidBlockUpdate("invalid next validator hash");
                    }
                    validators = nextValidators;
                    validatorHash = nextValidatorHash;
                    isValidatorsUpdate = true;
                }
                mta.add(blockHash);
            } else if (nextHeight < blockHeight) {
                throw BMVException.invalidBlockUpdateHeightHigher(
                        "invalid blockUpdate height "+blockHeight+" expected:"+nextHeight);
            } else {
                throw BMVException.invalidBlockUpdateHeightLower(
                        "invalid blockUpdate height "+blockHeight+" expected:"+nextHeight);
            }
        }
        return isValidatorsUpdate ? validators : null;
    }

    private void verifyBlockProof(BlockProof blockProof, MerkleTreeAccumulator mta) {
        BlockWitness blockWitness = blockProof.getBlockWitness();
        if (blockWitness == null) {
            throw BMVException.invalidBlockProof("not exists witness");
        }
        BlockHeader blockHeader = blockProof.getBlockHeader();
        long blockHeight = blockHeader.getHeight();
        if (mta.getHeight() < blockHeight) {
            throw BMVException.invalidBlockProofHeightHigher(
                    "given block height is newer "+blockHeight+" expected:"+mta.getHeight());
        }
        byte[] blockHash = hash(blockHeader.toBytes());
        try {
            mta.verify(blockWitness.getWitness(), blockHash, blockHeight, blockWitness.getHeight());
        } catch (MTAException.InvalidWitnessOldException e) {
            throw BMVException.invalidBlockWitnessOld(e.getMessage());
        } catch (MTAException e) {
            logger.println("verifyBlockProof","MTAException", e.getMessage());
            throw BMVException.invalidBlockWitness(e.getMessage());
        }
    }

    private void verifyVotes(Votes votes, long blockHeight, byte[] blockHash, Validators validators) {
        if (votes == null) {
            logger.println("verifyVotes","invalidBlockUpdate", "not exists votes");
            throw BMVException.invalidBlockUpdate("not exists votes");
        }
        VoteMessage voteMessage = new VoteMessage();
        voteMessage.setHeight(blockHeight);
        voteMessage.setRound(votes.getRound());
        voteMessage.setVoteType(VoteMessage.VOTE_TYPE_PRECOMMIT);
        voteMessage.setBlockId(blockHash);
        voteMessage.setPartSetId(votes.getPartSetId());
        List<Address> addresses = new ArrayList<>();
        for(Vote vote : votes.getItems()) {
            voteMessage.setTimestamp(vote.getTimestamp());
            byte[] voteMessageHash = hash(voteMessage.toBytes());
            Address address = recoverAddress(voteMessageHash, vote.getSignature(), true);
            if (!validators.contains(address)) {
                logger.println("verifyVotes","invalidVotes", "invalid signature",
                        "messageHash:", StringUtil.toString(voteMessageHash),
                        "signature:", StringUtil.toString(vote.getSignature()),
                        "address:",  StringUtil.toString(address.toString()));
                throw BMVException.invalidVotes("invalid signature");
            }
            if (addresses.contains(address)) {
                logger.println("verifyVotes","invalidVotes", "duplicated vote");
                throw BMVException.invalidVotes("duplicated vote");
            } else {
                addresses.add(address);
            }
        }

        if (addresses.size() <= (validators.getAddresses().length * 2 / 3)) {
            logger.println("verifyVotes","invalidVotes", "require votes +2/3");
            throw BMVException.invalidVotes("require votes +2/3");
        }
    }

    private void checkAccessible(BTPAddress curAddr, BTPAddress fromAddr) {
        BMVProperties properties = getProperties();
        if (!properties.getNet().equals(fromAddr.net())) {
            throw BMVException.unknown("not acceptable from");
        } else if (!Context.getCaller().equals(properties.getBmc())) {
            throw BMVException.unknown("not acceptable bmc");
        } else if (!Address.fromString(curAddr.account()).equals(properties.getBmc())) {
            throw BMVException.unknown("not acceptable bmc");
        }
    }

    @External(readonly = true)
    public Map getStatus() {
        BMVProperties properties = getProperties();
        MerkleTreeAccumulator mta = properties.getMta();
        return Map.of("height", mta.getHeight(),
                "offset", mta.getOffset(),
                "last_height", properties.getLastHeight());
    }
}

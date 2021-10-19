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
package foundation.icon.btp.bmv;


import foundation.icon.btp.bmv.lib.HexConverter;
import foundation.icon.btp.bmv.lib.mta.MerkleTreeAccumulator;
import foundation.icon.btp.bmv.types.*;
import score.Address;
import score.Context;
import score.VarDB;
import score.annotation.External;
import scorex.util.ArrayList;
import scorex.util.Base64;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * A relay message is composed of both BTP Messages and proof of existence of these BTP Message
 * Merkle Accumulator can be used for verifying old hashes. BMV sustains roots of Merkle Tree Accumulator,
 * and relay will sustain all elements of Merkle Tree Accumulator.
 * <p>
 * The relay may make the proof of any one of old hashes. So, even if byzantine relay updated the trust information
 * with the proof of new block, normal relay can send BTP Messages in the past block with the proof.
 */
public class BTPMessageVerifier {

    final static String RLPn = "RLPn";

    private final VarDB<Address> bmcScoreAddress = Context.newVarDB("bmc", Address.class);
    private final VarDB<String> network = Context.newVarDB("network", String.class);
    private final VarDB<BigInteger> lastHeight = Context.newVarDB("lastHeight", BigInteger.class);
    private final VarDB<MerkleTreeAccumulator> mta = Context.newVarDB("mta", MerkleTreeAccumulator.class);
    private final VarDB<Boolean> mtaUpdated = Context.newVarDB("mtaUpdated", Boolean.class);

    public BTPMessageVerifier(
            String bmc,
            String network,
            int offset,
            int rootSize,
            int cacheSize,
            boolean isAllowNewerWitness) {
        this.network.set(network);
        this.bmcScoreAddress.set(Address.fromString(bmc));
        this.lastHeight.set(BigInteger.valueOf(offset));
        this.mta.set(new MerkleTreeAccumulator(0, offset, rootSize, cacheSize, isAllowNewerWitness, null, null));
    }

    @External
    public List<byte[]> handleRelayMessage(String bmc, String prev, BigInteger seq, String msg) {
        byte[] messageEventSignature = HexConverter.hexStringToByteArray("37be353f216cf7e33639101fd610c542e6a0c0109173fa1c1d8b04d34edb7c1b"); // kekak256("Message(string,uint256,bytes)");
        List<byte[]> msgList = new ArrayList<>();
        BTPAddress currBMCAddress = BTPAddress.fromString(bmc);
        BTPAddress prevBMCAddress = BTPAddress.fromString(prev);
        canBMCAccess(currBMCAddress, prevBMCAddress);
        byte[] _msg = null;
        try {
            _msg = Base64.getUrlDecoder().decode(msg.getBytes());
        } catch (Exception e) {
            Context.revert(BMVErrorCodes.INVALID_RELAY_MSG, "Failed to decode relay message");
        }
        RelayMessage relayMessage = RelayMessage.fromBytes(_msg);
        if (relayMessage.getBlockUpdates().length == 0
                && (relayMessage.getBlockProof() == null
                || (relayMessage.getBlockProof() != null && relayMessage.getBlockProof().getBlockHeader() == null))) {
            Context.revert(BMVErrorCodes.FAILED_TO_DECODE, "Invalid relay message");
        }
        List<Object> result = lastReceiptRootHash(relayMessage);
        byte[] receiptRootHash = (byte[]) result.get(0);
        BigInteger lastHeight = (BigInteger) result.get(1);
        BigInteger nextSeq = seq.add(BigInteger.ONE);// nextSeq= seq + 1
        for (ReceiptProof receiptProof : relayMessage.getReceiptProofs()) {
            Receipt receipt = receiptProof.prove(receiptRootHash);
            for (ReceiptEventLog eventLog : receipt.getLogs()) {
                //TODO: check better way, now the event log address doesnt have the prefix
                if (!prevBMCAddress.getContract().equalsIgnoreCase("0x" + HexConverter.bytesToHex(eventLog.getAddress()))) {
                    continue;
                }
                //skip : if the 0th of the topic(which has the method signature) doesnt match the signature of keccak(Message(string,uint256,bytes))
                if (!Arrays.equals(messageEventSignature, eventLog.getTopics().get(0))) {
                    continue;
                }
                //TODO: check why _next value is indexed? and remove later
                EventDataBTPMessage messageEvent = EventDataBTPMessage.fromBytes(eventLog.getData());
                //TODO: remove seq comment , just for testing e2e
                if (messageEvent.getSeq().compareTo(nextSeq) != 0) {
                    Context.revert(BMVErrorCodes.INVALID_SEQ_NUMBER, "Invalid sequence No:" + messageEvent.getSeq() + ", Expected: " + nextSeq);
                } else {
                    msgList.add(messageEvent.getMsg());
                    //BTPMessage.fromBytes(messageEvent.getMsg());
                    nextSeq = nextSeq.add(BigInteger.ONE);
                }
            }
        }
        if (msgList.size() > 0) {
            this.lastHeight.set(lastHeight);
        }
        return msgList;
    }

    /**
     * get status of BMV
     */
    @External(readonly = true)
    public BMVStatus getStatus() {
        MerkleTreeAccumulator mta = this.mta.get();
        BMVStatus status = new BMVStatus();
        long lastHeight = mta.getHeight();
        status.setHeight(lastHeight);
        status.setLast_height(this.lastHeight.get().longValue());
        status.setOffset(mta.getOffset());
        return status;
    }

    private void canBMCAccess(BTPAddress currBMCAddress, BTPAddress prevAddress) {
        if (!(Context.getCaller().equals(this.bmcScoreAddress.get()) || Context.getCaller().equals(Context.getOwner()))) {
            Context.revert(BMVErrorCodes.INVALID_BMC_SENDER, "Invalid message sender from BMC");
        }

        if (!this.network.get().equals(prevAddress.getNet())) {
            Context.revert(BMVErrorCodes.INVALID_BMC_PREV, "Invalid previous BMC " + prevAddress.getNet());
        }

        if (!currBMCAddress.getContract().equals(this.bmcScoreAddress.get().toString())) {
            Context.revert(BMVErrorCodes.INVALID_BMC_CURR, "Invalid Current BMC " + currBMCAddress.getContract());
        }
    }

    private List<Object> lastReceiptRootHash(RelayMessage relayMessage) {
        byte[] receiptRoot = null;
        BigInteger lastHeight = BigInteger.ZERO;
        MerkleTreeAccumulator mta = this.mta.get();
        for (BlockUpdate blockUpdate : relayMessage.getBlockUpdates()) {
            int nextHeight = (int) (mta.getHeight() + 1);
            if (BigInteger.valueOf(nextHeight).compareTo(blockUpdate.getBlockHeader().getNumber()) == 0) {
                if (!BlockHeader.verifyValidatorSignature(blockUpdate.getBlockHeader(),blockUpdate.getEvmHeader())) {
                    Context.revert(BMVErrorCodes.INVALID_COINBASE_SIGNATURE, "Invalid validator signature");
                }
                mta.add(blockUpdate.getBlockHeader().getHash());
                this.mtaUpdated.set(true);
                receiptRoot = blockUpdate.getBlockHeader().getReceiptsRoot();
                lastHeight = blockUpdate.getBlockHeader().getNumber();
            } else if (nextHeight < blockUpdate.getBlockHeader().getNumber().intValue()) {
                Context.revert(BMVErrorCodes.INVALID_BLOCK_UPDATE_HEIGHT_HIGH, "Invalid block update due to higher height");
            } else {
                Context.revert(BMVErrorCodes.INVALID_BLOCK_UPDATE_HEIGHT_LOW, "Invalid block update due to lower height");
            }
        }
        BlockProof blockProof = relayMessage.getBlockProof();
        if (blockProof != null) {
            blockProof.verify(mta);
            receiptRoot = blockProof.getBlockHeader().getReceiptsRoot();
            lastHeight = blockProof.getBlockHeader().getNumber();
        }
        if (receiptRoot == null) {
            Context.revert(BMVErrorCodes.INVALID_RECEIPT_PROOFS, "Invalid receipt proofs with wrong sequence");
        }
        List<Object> result = new ArrayList<>();
        result.add(receiptRoot);
        result.add(lastHeight);
        this.mta.set(mta);
        return result;
    }
/*
    @External(readonly = true)
    public BigInteger getLastHeight() {
        return this.lastHeight.get();
    }*/
}


/**
 * @param bmc String address of BMC handling the message
 * @param prev String BTP address of previous BMC
 * @param seq int next sequence number to get a message
 * @param msg serialised bytes of relay Message
 * @return List of serialised bytes of BTP Message's
 * <p>
 * 1 - Decodes Relay Messages and process BTP Messages
 * 2 - If there is an error, then it sends a BTP Message containing the Error Message
 * 3 - BTP Messages with old sequence numbers are ignored. A BTP Message contains future sequence number will fail.
 */

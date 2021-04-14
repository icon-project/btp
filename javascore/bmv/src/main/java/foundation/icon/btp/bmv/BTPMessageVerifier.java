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

import foundation.icon.icx.data.TransactionResult;
import org.bouncycastle.util.encoders.Hex;
import score.Address;
import score.ByteArrayObjectWriter;
import score.Context;
import score.annotation.EventLog;
import score.annotation.External;

import scorex.util.ArrayList;

import java.util.List;

/**
 * A relay message is composed of both BTP Messages and proof of existence of these BTP Message
 * Merkle Accumulator can be used for verifying old hashes. BMV sustains roots of Merkle Tree Accumulator,
 * and relay will sustain all elements of Merkle Tree Accumulator.
 *
 * The relay may make the proof of any one of old hashes. So, even if byzantine relay updated the trust information
 * with the proof of new block, normal relay can send BTP Messages in the past block with the proof.
 */
public class BTPMessageVerifier {

    final static String RLPn = "RLPn";

    private long lastHeight;

    private String bmcScoreAddress;
    private String netAddress;
    private String network;

    private MerkleTreeAccumulator mta;
    private ValidatorList validatorList;
    private Votes votes;
    private boolean mtaUpdated = false;

    public BTPMessageVerifier(String network,
                              String bmcScoreAddress,
                              byte[] validators,
                              byte[] encMTA,
                              int offset) {

        this.network         = network;
        this.bmcScoreAddress = bmcScoreAddress;
        this.validatorList   = new ValidatorList(new Address[]{});
        this.mta             = new MerkleTreeAccumulator(null);
        this.mta.setOffset(offset);
        this.lastHeight      = offset;
        //this.bmcScore = bmcScoreAddress
        //this.bmcScore.addVerifier(network, Context.getAddress())
    }

    /**
     * @param bmc String address of BMC handling the message
     * @param prev String BTP address of previous BMC
     * @param seq int next sequence number to get a message
     * @param msg serialised bytes of relay Message
     * @return List of serialised bytes of BTP Message's
     *
     * 1 - Decodes Relay Messages and process BTP Messages
     * 2 - If there is an error, then it sends a BTP Message containing the Error Message
     * 3 - BTP Messages with old sequence numbers are ignored. A BTP Message contains future sequence number will fail.
     */
    @External
    public byte[] handleRelayMessage(String bmc, String prev, int seq, byte[] msg) {

        BTPAddress currBMCAddress = BTPAddress.fromString(bmc);
        BTPAddress prevBMCAddress = BTPAddress.fromString(prev);

        RelayMessage relayMessage = RelayMessage.fromBytes(msg);

        Context.require(relayMessage.getBlockUpdates().length > 0 || relayMessage.getBlockProof() != null);

        byte[] receiptHash = lastReceiptHash(relayMessage);

        int nextSeq = seq + 1;
        List<byte[]> msgList = new ArrayList<>();
        for(ReceiptProof receiptProof : relayMessage.getReceiptProofs()) {
           Receipt receipt = receiptProof.prove(receiptHash);
        }

        return new byte[0];
    }

    private byte[] lastReceiptHash(RelayMessage relayMessage) {
        lastHeight = 0;
        byte[] receiptHash = null;
        for(BlockUpdate blockUpdate : relayMessage.getBlockUpdates()) {
            int nextHeight = this.mta.getHeight() + 1;
            log("Receipt Hash -> " + nextHeight + " " + blockUpdate.getBlockHeader().getHeight());
            if (nextHeight == blockUpdate.getBlockHeader().getHeight()) {
                if (blockUpdate.verify(this.validatorList))
                    this.validatorList = ValidatorList.fromAddressBytes(blockUpdate.getNextValidators());
                this.mta.add(blockUpdate.getBlockHeader().getHash());
                this.mtaUpdated = true;
                receiptHash = blockUpdate.getBlockHeader().getNormalReceiptHash();
                lastHeight = blockUpdate.getBlockHeader().getHeight();
            } else
                Context.println("invalid blockUpdate height "
                        + blockUpdate.getBlockHeader().getHeight() + ", expected " + nextHeight);
        }
        BlockProof blockProof = relayMessage.getBlockProof();
        if (blockProof != null) {
            blockProof.verify(this.mta);
            receiptHash = blockProof.getBlockHeader().getHash();
            lastHeight = blockProof.getBlockHeader().getHeight();
        }
        return receiptHash;
    }

    @EventLog(indexed = 1)
    public void log(String msg){}

    @External(readonly = true)
    public int getVotesCount() {
        return votes.getVoteItems().length;
    }

    @External(readonly = true)
    public long getLastHeight() {
        return this.lastHeight;
    }
}

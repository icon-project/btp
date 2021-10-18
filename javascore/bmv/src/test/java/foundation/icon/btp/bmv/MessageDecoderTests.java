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
import foundation.icon.btp.bmv.types.*;
import foundation.icon.ee.io.DataWriter;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import score.ByteArrayObjectWriter;
import score.Context;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MessageDecoderTests {

    private static final byte[] parentHash = {88, -8, -97, 118, 100, 116, 42, 24, 62, 48, -108, 22, -99, -6, -55, -40, 47, -87, -26, 95, -32, -51, -60, -15, 12, 126, 42, 76, -77, -75, -54, -8};
    private static final byte[] uncleHash = {29, -52, 77, -24, -34, -57, 93, 122, -85, -123, -75, 103, -74, -52, -44, 26, -45, 18, 69, 27, -108, -118, 116, 19, -16, -95, 66, -3, 64, -44, -109, 71};
    private static final byte[] coinBase = {72, -108, -126, -105, -61, 35, 110, -61, -22, 108, -107, -12, -18, -62, 47, -37, 24, 37, 94, 85};
    private static final byte[] stateRoot = {-81, 125, -40, -117, -97, 55, 37, -114, -40, 63, 71, 81, -95, -94, -82, -24, 108, -23, -88, 48, -76, 99, -121, 91, 88, -1, -37, -71, -39, -56, -89, -48};
    private static final byte[] transactionsRoot = {-12, 75, -34, 51, -105, -82, 127, 61, 51, -5, 39, -23, -86, 10, -23, -68, -22, 125, -56, -119, -29, -57, -13, 100, 41, -16, 119, 29, 31, 65, -2, -75};
    private static final byte[] receiptsRoot = {-31, -73, 95, -102, -84, -9, -51, 46, -62, -111, 13, 56, -96, -48, 69, -48, -111, 74, 22, -21, 97, 56, 37, 53, 54, 72, 5, 15, 69, 16, -127, 94};
    private static final byte[] logsBloom = {0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 64, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 16, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 0, 0, 0, 0, 0, -128, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 0, 0, 64, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 33, 0, 0, 1, 0, 16, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 8, 0, 32};
    private static final BigInteger difficulty = BigInteger.TWO;
    private static final BigInteger number = BigInteger.valueOf(181);
    private static final long gasLimit = 19697141;
    private static final long gasUsed = 3175734;
    private static final long timestamp = 1632813244;
    private static final byte[] extraData = {-40, -125, 1, 0, 6, -124, 103, 101, 116, 104, -120, 103, 111, 49, 46, 49, 54, 46, 54, -123, 108, 105, 110, 117, 120, 0, 0, 0, 17, -64, -86, -98, -104, 104, -2, -41, -87, -103, -66, -120, 122, 108, -23, -20, -106, 42, 124, -34, -49, -39, 22, -108, 100, -71, -111, 77, -3, 6, -95, 3, -4, 96, -113, 107, 65, 50, 51, 92, -41, 107, -89, -29, 2, -117, 28, 60, 107, -119, -16, 79, -57, -82, -108, -58, -101, -117, 38, 107, -112, -14, 75, -126, -37, -99, -55, 13, 0};
    private static final byte[] mixHash = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static final byte[] nonce = {0, 0, 0, 0, 0, 0, 0, 0};

    private static final int height = 70;
    private static final byte[] w1 = {4, -88, 23, -56, 0};
    private static final byte[] w2 = {102, -111, -73};
    private static final byte[] w3 = {99, -41, -4, -79, -9, -101, 95, -123, 71, 5, -87, 78, 62, 6, 91, -24, 32, 76, 63, -26};
    private static final byte[] w4 = {};
    private static final byte[] w5 = {56, 66, -120, -116, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 64, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -96, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 52};
    private static final byte[] w6 = {-65, -110, -77, -89, 56, -117, 121, 68, -120, 22, -126, 28, -28, 26, -7, -20, -37, -18, 103, -85, -125, 86, -110, 90, -51, -63, -99, -22, 104, 26, -60, -68};
    private static final byte[] w7 = {38};
    private static final byte[] w8 = {71, -117, -107, 66, 59, -85, 37, -88, -51, -60, -127, 124, -84, -12, 121, 88, -84, 116, -45, 14, -108, 8, 127, 24, 100, -44, -107, -24, -74, 45, -81, -123};

    private static final int index = 0;
    private static final byte[] mptKey = {-128};
    private static final byte[] address = {72, -108, -126, -105, -61, 35, 110, -61, -22, 108, -107, -12, -18, -62, 47, -37, 24, 37, 94, 85};
    private static final byte[] topic = {88, -8, -97, 118, 100, 116, 42, 24, 62, 48, -108, 22, -99, -6, -55, -40, 47, -87, -26, 95, -32, -51, -60, -15, 12, 126, 42, 76, -77, -75, -54, -8};
    private static final byte[] data = {-40, -125, 1, 0, 6, -124, 103, 101, 116, 104, -120, 103, 111, 49, 46, 49, 54, 46, 54, -123, 108, 105, 110, 117, 120, 0, 0, 0, 17, -64, -86, -98, -104, 104, -2, -41, -87, -103, -66, -120, 122, 108, -23, -20, -106, 42, 124, -34, -49, -39, 22, -108, 100, -71};
    private static final byte[] mptProof1 = {0};
    private static final byte[] mptProof2 = {-7, 1, -75, -71, 1, -78, -7, 1, -81, -126, 32, -128, -71, 1, -87, -7, 1, -90, 1, -126, -84, -87, -71, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
    private static final int eventProofIndex = 0;

    /*  BlockHeader
        BlockUpdate
        BlockProof
        BlockWitness
        ReceiptProof
        RelayMessage
     */

    @Test
    public void testBlockHeader() {
        DataWriter w = foundation.icon.test.common.Codec.rlp.newWriter();
        w.writeListHeader(15);
        w.write(parentHash);
        w.write(uncleHash);
        w.write(coinBase);
        w.write(stateRoot);
        w.write(transactionsRoot);
        w.write(receiptsRoot);
        w.write(logsBloom);
        w.write(difficulty);
        w.write(number);
        w.write(gasLimit);
        w.write(gasUsed);
        w.write(timestamp);
        w.write(extraData);
        w.write(mixHash);
        w.write(nonce);
        w.writeFooter();

        BlockHeader bh = BlockHeader.fromBytes(w.toByteArray());

        assertEquals(BigInteger.valueOf(97), bh.getNetwork());
        assertEquals(Hex.toHexString(parentHash), Hex.toHexString(bh.getParentHash()));
        assertEquals(Hex.toHexString(uncleHash), Hex.toHexString(bh.getUncleHash()));
        assertEquals(Hex.toHexString(coinBase), Hex.toHexString(bh.getCoinBase()));
        assertEquals(Hex.toHexString(stateRoot), Hex.toHexString(bh.getStateRoot()));
        assertEquals(Hex.toHexString(transactionsRoot), Hex.toHexString(bh.getTransactionsRoot()));
        assertEquals(Hex.toHexString(receiptsRoot), Hex.toHexString(bh.getReceiptsRoot()));
        assertEquals(Hex.toHexString(logsBloom), Hex.toHexString(bh.getLogsBloom()));
        assertEquals(difficulty, bh.getDifficulty());
        assertEquals(number, bh.getNumber());
        assertEquals(gasLimit, bh.getGasLimit());
        assertEquals(gasUsed, bh.getGasUsed());
        assertEquals(timestamp, bh.getTimestamp());
        assertEquals(Hex.toHexString(extraData), Hex.toHexString(bh.getExtraData()));
        assertEquals(Hex.toHexString(mixHash), Hex.toHexString(bh.getMixHash()));
        assertEquals(Hex.toHexString(nonce), Hex.toHexString(bh.getNonce()));
    }

    @Test
    public void testBlockUpdate() {
        DataWriter w = foundation.icon.test.common.Codec.rlp.newWriter();
        w.writeListHeader(15);
        w.write(parentHash);
        w.write(uncleHash);
        w.write(coinBase);
        w.write(stateRoot);
        w.write(transactionsRoot);
        w.write(receiptsRoot);
        w.write(logsBloom);
        w.write(difficulty);
        w.write(number);
        w.write(gasLimit);
        w.write(gasUsed);
        w.write(timestamp);
        w.write(extraData);
        w.write(mixHash);
        w.write(nonce);
        w.writeFooter();

        BlockHeader header = BlockHeader.fromBytes(w.toByteArray());

        DataWriter wr = foundation.icon.test.common.Codec.rlp.newWriter();
        wr.writeListHeader(16);
        wr.write(BigInteger.valueOf(97));
        wr.write(parentHash);
        wr.write(uncleHash);
        wr.write(coinBase);
        wr.write(stateRoot);
        wr.write(transactionsRoot);
        wr.write(receiptsRoot);
        wr.write(logsBloom);
        wr.write(difficulty);
        wr.write(number);
        wr.write(gasLimit);
        wr.write(gasUsed);
        wr.write(timestamp);
        wr.write(extraData);
        wr.write(mixHash);
        wr.write(nonce);
        wr.writeFooter();

        BlockUpdate bu = new BlockUpdate(header, null, new byte[0][0], null);;
        assertBlockUpdate(wr, bu);
    }

    @Test
    public void testBlockProof() {
        DataWriter bhWriter = foundation.icon.test.common.Codec.rlp.newWriter();
        bhWriter.writeListHeader(15);
        bhWriter.write(parentHash);
        bhWriter.write(uncleHash);
        bhWriter.write(coinBase);
        bhWriter.write(stateRoot);
        bhWriter.write(transactionsRoot);
        bhWriter.write(receiptsRoot);
        bhWriter.write(logsBloom);
        bhWriter.write(difficulty);
        bhWriter.write(number);
        bhWriter.write(gasLimit);
        bhWriter.write(gasUsed);
        bhWriter.write(timestamp);
        bhWriter.write(extraData);
        bhWriter.write(mixHash);
        bhWriter.write(nonce);
        bhWriter.writeFooter();

        DataWriter bwWriter = foundation.icon.test.common.Codec.rlp.newWriter();
        bwWriter.writeListHeader(2);
        bwWriter.write(height);
        bwWriter.write(w1);
        bwWriter.write(w2);
        bwWriter.write(w3);
        bwWriter.write(w4);
        bwWriter.write(w5);
        bwWriter.write(w6);
        bwWriter.write(w7);
        bwWriter.write(w8);
        bwWriter.writeFooter();

        DataWriter bpWriter = foundation.icon.test.common.Codec.rlp.newWriter();
        bpWriter.writeListHeader(2);
        bpWriter.write(bhWriter.toByteArray());
        bpWriter.write(bwWriter.toByteArray());
        bpWriter.writeFooter();

        BlockProof bp = BlockProof.fromBytes(bpWriter.toByteArray());
        assertBlockProof(bp);
    }

    @Test
    public void testBlockWitness() {
        int height = 70;
        byte[] w1 = {4, -88, 23, -56, 0};
        byte[] w2 = {102, -111, -73};
        byte[] w3 = {99, -41, -4, -79, -9, -101, 95, -123, 71, 5, -87, 78, 62, 6, 91, -24, 32, 76, 63, -26};
        byte[] w4 = {};
        byte[] w5 = {56, 66, -120, -116, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 64, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -96, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 52};
        byte[] w6 = {-65, -110, -77, -89, 56, -117, 121, 68, -120, 22, -126, 28, -28, 26, -7, -20, -37, -18, 103, -85, -125, 86, -110, 90, -51, -63, -99, -22, 104, 26, -60, -68};
        byte[] w7 = {38};
        byte[] w8 = {71, -117, -107, 66, 59, -85, 37, -88, -51, -60, -127, 124, -84, -12, 121, 88, -84, 116, -45, 14, -108, 8, 127, 24, 100, -44, -107, -24, -74, 45, -81, -123};

        DataWriter writer = foundation.icon.test.common.Codec.rlp.newWriter();
        writer.writeListHeader(2);
        writer.write(height);
        writer.write(w1);
        writer.write(w2);
        writer.write(w3);
        writer.write(w4);
        writer.write(w5);
        writer.write(w6);
        writer.write(w7);
        writer.write(w8);
        writer.writeFooter();

        BlockWitness bw = BlockWitness.fromBytes(writer.toByteArray());
        assertEquals(height, bw.getHeight());
        assertEquals(Hex.toHexString(w1), Hex.toHexString(bw.getWitness().get(0)));
        assertEquals(Hex.toHexString(w2), Hex.toHexString(bw.getWitness().get(1)));
        assertEquals(Hex.toHexString(w3), Hex.toHexString(bw.getWitness().get(2)));
        assertEquals(Hex.toHexString(w4), Hex.toHexString(bw.getWitness().get(3)));
        assertEquals(Hex.toHexString(w5), Hex.toHexString(bw.getWitness().get(4)));
        assertEquals(Hex.toHexString(w6), Hex.toHexString(bw.getWitness().get(5)));
        assertEquals(Hex.toHexString(w7), Hex.toHexString(bw.getWitness().get(6)));
        assertEquals(Hex.toHexString(w8), Hex.toHexString(bw.getWitness().get(7)));
    }

    @Test
    public void testReceiptProof() {
        DataWriter eventWriter = foundation.icon.test.common.Codec.rlp.newWriter();
        eventWriter.writeListHeader(3);
        eventWriter.write(address);
        eventWriter.writeListHeader(1);
        eventWriter.write(topic);
        eventWriter.writeFooter();
        eventWriter.write(data);
        eventWriter.writeFooter();

        byte[] proof = eventWriter.toByteArray();
        int eventProofIndex = 0;
        DataWriter eventProofWriter = foundation.icon.test.common.Codec.rlp.newWriter();
        eventProofWriter.writeListHeader(2);
        eventProofWriter.write(eventProofIndex);
        eventProofWriter.write(proof);
        eventProofWriter.writeFooter();

        DataWriter mptProofWriter = foundation.icon.test.common.Codec.rlp.newWriter();
        mptProofWriter.writeListHeader(2);
        mptProofWriter.write(mptProof1);
        mptProofWriter.write(mptProof2);
        mptProofWriter.writeFooter();

        DataWriter rpWriter = foundation.icon.test.common.Codec.rlp.newWriter();
        rpWriter.writeListHeader(4);
        rpWriter.write(index);
        rpWriter.write(mptProofWriter.toByteArray());

        rpWriter.writeListHeader(1);
        rpWriter.writeListHeader(2);
        rpWriter.write(eventProofIndex);
        rpWriter.write(proof);
        rpWriter.writeFooter();
        rpWriter.writeFooter();
        rpWriter.writeFooter();

        ReceiptProof rp = ReceiptProof.fromBytes(rpWriter.toByteArray());
        assertReceiptProof(proof, rp);
    }

    @Test
    public void testRelayMessage() {
        // block header
        DataWriter bhWriter = foundation.icon.test.common.Codec.rlp.newWriter();
        bhWriter.writeListHeader(15);
        bhWriter.write(parentHash);
        bhWriter.write(uncleHash);
        bhWriter.write(coinBase);
        bhWriter.write(stateRoot);
        bhWriter.write(transactionsRoot);
        bhWriter.write(receiptsRoot);
        bhWriter.write(logsBloom);
        bhWriter.write(difficulty);
        bhWriter.write(number);
        bhWriter.write(gasLimit);
        bhWriter.write(gasUsed);
        bhWriter.write(timestamp);
        bhWriter.write(extraData);
        bhWriter.write(mixHash);
        bhWriter.write(nonce);
        bhWriter.writeFooter();

        DataWriter wr = foundation.icon.test.common.Codec.rlp.newWriter();
        wr.writeListHeader(16);
        wr.write(BigInteger.valueOf(97));
        wr.write(parentHash);
        wr.write(uncleHash);
        wr.write(coinBase);
        wr.write(stateRoot);
        wr.write(transactionsRoot);
        wr.write(receiptsRoot);
        wr.write(logsBloom);
        wr.write(difficulty);
        wr.write(number);
        wr.write(gasLimit);
        wr.write(gasUsed);
        wr.write(timestamp);
        wr.write(extraData);
        wr.write(mixHash);
        wr.write(nonce);
        wr.writeFooter();

        // block update
        BlockHeader bh = BlockHeader.fromBytes(bhWriter.toByteArray());
        ByteArrayObjectWriter buWriter = Context.newByteArrayObjectWriter("RLPn");
        BlockUpdate bu = new BlockUpdate(bh, null, new byte[0][0], null);
        BlockUpdate.writeObject(buWriter, bu, bhWriter.toByteArray());

        // block proof
        DataWriter bwWriter = foundation.icon.test.common.Codec.rlp.newWriter();
        bwWriter.writeListHeader(2);
        bwWriter.write(height);
        bwWriter.write(w1);
        bwWriter.write(w2);
        bwWriter.write(w3);
        bwWriter.write(w4);
        bwWriter.write(w5);
        bwWriter.write(w6);
        bwWriter.write(w7);
        bwWriter.write(w8);
        bwWriter.writeFooter();

        DataWriter bpWriter = foundation.icon.test.common.Codec.rlp.newWriter();
        bpWriter.writeListHeader(2);
        bpWriter.write(bhWriter.toByteArray());
        bpWriter.write(bwWriter.toByteArray());
        bpWriter.writeFooter();

        // receipt proof

        DataWriter eventWriter = foundation.icon.test.common.Codec.rlp.newWriter();
        eventWriter.writeListHeader(3);
        eventWriter.write(address);
        eventWriter.writeListHeader(1);
        eventWriter.write(topic);
        eventWriter.writeFooter();
        eventWriter.write(data);
        eventWriter.writeFooter();

        byte[] proof = eventWriter.toByteArray();
        DataWriter eventProofWriter = foundation.icon.test.common.Codec.rlp.newWriter();
        eventProofWriter.writeListHeader(2);
        eventProofWriter.write(eventProofIndex);
        eventProofWriter.write(proof);
        eventProofWriter.writeFooter();

        byte[] mptProof1 = {0};
        byte[] mptProof2 = {-7, 1, -75, -71, 1, -78, -7, 1, -81, -126, 32, -128, -71, 1, -87, -7, 1, -90, 1, -126, -84, -87, -71, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        DataWriter mptProofWriter = foundation.icon.test.common.Codec.rlp.newWriter();
        mptProofWriter.writeListHeader(2);
        mptProofWriter.write(mptProof1);
        mptProofWriter.write(mptProof2);
        mptProofWriter.writeFooter();

        DataWriter rpWriter = foundation.icon.test.common.Codec.rlp.newWriter();
        rpWriter.writeListHeader(4);
        rpWriter.write(index);
        rpWriter.write(mptProofWriter.toByteArray());

        rpWriter.writeListHeader(1);
        rpWriter.writeListHeader(2);
        rpWriter.write(eventProofIndex);
        rpWriter.write(proof);
        rpWriter.writeFooter();
        rpWriter.writeFooter();
        rpWriter.writeFooter();

        DataWriter relayMsgWriter = foundation.icon.test.common.Codec.rlp.newWriter();
        relayMsgWriter.writeListHeader(3);
        relayMsgWriter.writeListHeader(1);
        relayMsgWriter.write(buWriter.toByteArray());
        relayMsgWriter.writeFooter();
        relayMsgWriter.write(bpWriter.toByteArray());
        relayMsgWriter.writeListHeader(1);
        relayMsgWriter.write(rpWriter.toByteArray());
        relayMsgWriter.writeFooter();
        relayMsgWriter.writeFooter();

        RelayMessage relayMessage = RelayMessage.fromBytes(relayMsgWriter.toByteArray());

        assertBlockUpdate(wr, relayMessage.getBlockUpdates()[0]);
        assertBlockProof(relayMessage.getBlockProof());
        assertReceiptProof(proof, relayMessage.getReceiptProofs()[0]);
    }

    private void assertBlockUpdate(DataWriter bhWriter, BlockUpdate bu) {
        String blockHeaderEncoded = HexConverter.bytesToHex(bhWriter.toByteArray());

        assertEquals(blockHeaderEncoded, HexConverter.bytesToHex(BlockHeader.toBytes(bu.getBlockHeader())));
        assertEquals(bu.getVotes(), null);
        assertEquals(null, bu.getVotes());
        assertEquals(0, bu.getNextValidators().length);
        assertEquals(null, bu.getEvmHeader());
    }

    private void assertBlockProof(BlockProof bp) {
        assertEquals(height, bp.getBlockWitness().getHeight());
        assertEquals(Hex.toHexString(w1), Hex.toHexString(bp.getBlockWitness().getWitness().get(0)));
        assertEquals(Hex.toHexString(w2), Hex.toHexString(bp.getBlockWitness().getWitness().get(1)));
        assertEquals(Hex.toHexString(w3), Hex.toHexString(bp.getBlockWitness().getWitness().get(2)));
        assertEquals(Hex.toHexString(w4), Hex.toHexString(bp.getBlockWitness().getWitness().get(3)));
        assertEquals(Hex.toHexString(w5), Hex.toHexString(bp.getBlockWitness().getWitness().get(4)));
        assertEquals(Hex.toHexString(w6), Hex.toHexString(bp.getBlockWitness().getWitness().get(5)));
        assertEquals(Hex.toHexString(w7), Hex.toHexString(bp.getBlockWitness().getWitness().get(6)));
        assertEquals(Hex.toHexString(w8), Hex.toHexString(bp.getBlockWitness().getWitness().get(7)));
    }
    private void assertReceiptProof(byte[] proof, ReceiptProof rp) {
        assertEquals(index, rp.getIndex());
        assertEquals(Hex.toHexString(mptKey), Hex.toHexString(rp.getMptKey()));
        // check event proofs
        assertEquals(1, rp.getEventProofs().size());
        assertEquals(eventProofIndex, rp.getEventProofs().get(0).getIndex());
        assertEquals(Hex.toHexString(proof), Hex.toHexString(rp.getEventProofs().get(0).getProof()));
        // check event logs
        assertEquals(1, rp.getEvents().size());
        assertEquals(Hex.toHexString(address), Hex.toHexString(rp.getEvents().get(0).getAddress()));
        assertEquals(1, rp.getEvents().get(0).getTopics().size());
        assertEquals(Hex.toHexString(topic), Hex.toHexString(rp.getEvents().get(0).getTopics().get(0)));
        assertEquals(Hex.toHexString(data), Hex.toHexString(rp.getEvents().get(0).getData()));
        // check mpt proofs
        assertEquals(2, rp.getMptProofs().size());
        assertEquals(Hex.toHexString(mptProof1), Hex.toHexString(rp.getMptProofs().get(0)));
        assertEquals(Hex.toHexString(mptProof2), Hex.toHexString(rp.getMptProofs().get(1)));
    }

    @Test
    public void testBTPAddress() {
        BTPAddress bmcAddress = BTPAddress.fromString("btp://0x1.iconee/cxa18bf891d029d836ace51cbe41aaad2fdd5f9c65");
        assertEquals("btp", bmcAddress.getProtocol());
        assertEquals("0x1.iconee", bmcAddress.getNet());
        assertEquals("cxa18bf891d029d836ace51cbe41aaad2fdd5f9c65", bmcAddress.getContract());
    }

  /*  @Test
    public void testReceiptProof() {
        var bytes = Hex.decode("f902f400b90142f9013fb9013cf90139822000b90133f901300095010312fb60e64860c24f1de245a5ff7aa68ad4d88a000000b8ef2100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000200000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000000000000002000020000000000000000000000000000000000000000000001000000000000000004000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000000000000000000001000000000000000000f800f800a0eb095869650536d97c85e0753aa39ceb87eba6e70337236ecb2b020d843154b8f901abf901a800b901a4f901a1b9019ef9019b822000b90195f9019295010312fb60e64860c24f1de245a5ff7aa68ad4d88af85a964d657373616765287374722c696e742c627974657329b8406274703a2f2f30786435306166322e69636f6e65652f63786531393437613363616339303736656233613135333832396637396435353431366135353933613301f9011db9011af90117b83e6274703a2f2f3078653931662e69636f6e65652f637830333132666236306536343836306332346631646532343561356666376161363861643464383861b8406274703a2f2f30786435306166322e69636f6e65652f637865313934376133636163393037366562336131353338323966373964353534313661353539336133865f6576656e7400b88bf889844c696e6bf882b83e6274703a2f2f3078653931662e69636f6e65652f637830333132666236306536343836306332346631646532343561356666376161363861643464383861b8406274703a2f2f30786435306166322e69636f6e65652f637865313934376133636163393037366562336131353338323966373964353534313661353539336133");
        ReceiptProof receiptProof = ClassDecoderUtil.decodeReceiptProof(bytes);
        assertEquals(0, receiptProof.getIndex());
        assertEquals("f90139822000b90133f901300095010312fb60e64860c24f1de245a5ff7aa68ad4d88a000000b8ef2100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000200000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000000000000002000020000000000000000000000000000000000000000000001000000000000000004000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000000000000000000001000000000000000000f800f800a0eb095869650536d97c85e0753aa39ceb87eba6e70337236ecb2b020d843154b8",
                Hex.toHexString(receiptProof.getMptProofs().get(0)));
        //assertEquals(0, receiptProof.getEventProofs()[0].getMptProofs());
    }

    @Test
    public void testEventProof() {
        var bytes = Hex.decode("f901a800b901a4f901a1b9019ef9019b822000b90195f9019295010312fb60e64860c24f1de245a5ff7aa68ad4d88af85a964d657373616765287374722c696e742c627974657329b8406274703a2f2f30786435306166322e69636f6e65652f63786531393437613363616339303736656233613135333832396637396435353431366135353933613301f9011db9011af90117b83e6274703a2f2f3078653931662e69636f6e65652f637830333132666236306536343836306332346631646532343561356666376161363861643464383861b8406274703a2f2f30786435306166322e69636f6e65652f637865313934376133636163393037366562336131353338323966373964353534313661353539336133865f6576656e7400b88bf889844c696e6bf882b83e6274703a2f2f3078653931662e69636f6e65652f637830333132666236306536343836306332346631646532343561356666376161363861643464383861b8406274703a2f2f30786435306166322e69636f6e65652f637865313934376133636163393037366562336131353338323966373964353534313661353539336133");
        EventProof eventProof = ClassDecoderUtil.decodeEventProof(bytes);
        assertEquals(0, eventProof.getIndex());
//        assertEquals(414, eventProof.getMptProofs()[0].length);
       // assertTrue(Hex.toHexString(eventProof.getMptProofs()[0]).contains("f9019b822000b90195f9019295010312fb60e64860c24f1de245a5ff7"));
    }

    @Test
    public void testValidators(){
        var bytes = Hex.decode("f85494a93af7e0abfdcd9b962d17af65e47d0d7607460e944d3edde4e6b5863f5a220c56f843268f5b6afea1948c7799051f5f4c98936feb4aba83f633def77bf694a46817c03260f6d625984a3ec5fec8e03f6a5f0e");
        byte[][] validators = ClassDecoderUtil.decodeValidatorList(bytes, Codec.rlp);

        String[] expected = new String[] {
          "cxa93af7e0abfdcd9b962d17af65e47d0d7607460e",
          "cx4d3edde4e6b5863f5a220c56f843268f5b6afea1",
          "cx8c7799051f5f4c98936feb4aba83f633def77bf6",
          "cxa46817c03260f6d625984a3ec5fec8e03f6a5f0e"
        };

        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], new Address(formatAddress(validators[i])).toString());
        }
    }

    public static byte[] formatAddress(byte[] addr){
        if(addr.length == 21)
            return addr;
        var ba2 = new byte[addr.length+1];
        System.arraycopy(addr, 0, ba2, 1, addr.length);
        ba2[0] = 1;
        return ba2;
    }*/

   /* @Test
    public void testBlockHeader() {
        byte[] bytes = Hex.decode("ef800100808080a0b138facf1061259a735d016af65fa840b46ccc1cd028ebca4e313ae307fa0c2180808084c3808080");
        BlockHeader blockHeader = ClassDecoderUtil.decodeBlockHeader(bytes, Codec.rlp);
        assertEquals(0, blockHeader.getVersion());
        assertEquals(1, blockHeader.getHeight());
        assertEquals(0, blockHeader.getTimestamp());
        assertEquals("", Hex.toHexString(blockHeader.getPrevID()));
        assertEquals("b138facf1061259a735d016af65fa840b46ccc1cd028ebca4e313ae307fa0c21",
                Hex.toHexString(blockHeader.getNextValidatorHash()));
        //assertEquals("", Hex.toHexString(blockHeader.getPatchTxHash())); - skipped
        //assertEquals("", blockHeader.getTxHash()); - skipped
        //assertEquals("", blockHeader.getLogsBloom()); - skipped
        //skip patch receipt hash, result hash
        assertEquals("", Hex.toHexString(blockHeader.getVotesHash()));
        assertEquals("", Hex.toHexString(blockHeader.getNormalReceiptHash()));
    }*/

  /*  public byte[] packMessages(byte[][] blockUpdates, byte[] blockProof, byte[][] receipts){
        DataWriter writer = Codec.rlp.newWriter();
        writer.writeListHeader(3);
        writer.writeListHeader(blockUpdates.length);
        for (byte[] blockUpdate : blockUpdates) {
            writer.write(blockUpdate);
        }
        writer.writeFooter();
        if (blockProof == null)
            writer.writeNullity(true);
        else
            writer.write(blockProof);
        writer.writeListHeader(receipts.length);
        for (byte[] receipt : receipts) {
            writer.write(receipt);
        }
        writer.writeFooter();
        writer.writeFooter();
        return writer.toByteArray();
    }*/

/*
    public void testCreateRelayMessage() {
        byte[] bytes = Hex.decode("ef800100808080a0b138facf1061259a735d016af65fa840b46ccc1cd028ebca4e313ae307fa0c2180808084c3808080");
        BlockHeader blockHeader =  ClassDecoderUtil.decodeBlockHeader(bytes, Codec.rlp);
        byte[] blockSetId = Hex.decode("77267e69401942b1d0e5955bb8a936940b0f439a0f7649c8bba2ba5e6deb04a3");

        byte[] blk = Hex.decode("f90194b0ef800100808080a02d99e5f5b78b4f63b43dbbf93ce17657dca5697a499d00066fb49a362d8d9c0480808084c3808080b9015ef9015b00e201a077267e69401942b1d0e5955bb8a936940b0f439a0f7649c8bba2ba5e6deb04a3f90134f84b870598c2d9aaf5deb84115058471a91fc3a9fd0d959e4cbe459ef470528602d750fe5231e2c3a1cc749376ca09d5545f362f4d08d786ae166665ebe58934df5972cf300dc1772197b40a01f84b870598c2d9aaf5deb8419402330f93c4bc2f688f7af8bb2c80d3da47240da347bdf5e15ab6bdb3160d7b11905d4ef889deac57dfbbb5b3530d02fd411a1e6baafb8e7a805a3937bf923001f84b870598c2d9aaf5deb841674d5c584b2f3b56cb3df1e023a2049f68eb5bb7b9a024c2faf7c2309d1031ff2686e0307b30e8e00b90bd2e98ed37d2a9a995e02bdcad50e6292a16a0dc83b800f84b870598c2d9aaf5deb841facbb629ab0ddf3c1d33f95d78192c30aaff836db60b810cf177cb9b8aee0a9c0f2c13bcbe4a4f2a05c2e9c9e7b03076335d4609fea267b1cc944cd2888cb34a00f800");
        BlockUpdate blockUpdate = ClassDecoderUtil.decodeBlockUpdate(blk);

        assertEquals("2d99e5f5b78b4f63b43dbbf93ce17657dca5697a499d00066fb49a362d8d9c04",
                Hex.toHexString(blockUpdate.getBlockHeader().getNextValidatorHash()));

        byte[][]  proofReceipts = new byte[][]{};
        byte[] messages = packMessages(new byte[][]{blk}, null, proofReceipts);
        assertEquals("f901a0f9019ab90197f90194b0ef800100808080a02d99e5f5b78b4f63b43dbbf93ce17657dca5697a499d00066fb49a362d8d9c0480808084c3808080b9015ef9015b00e201a077267e69401942b1d0e5955bb8a936940b0f439a0f7649c8bba2ba5e6deb04a3f90134f84b870598c2d9aaf5deb84115058471a91fc3a9fd0d959e4cbe459ef470528602d750fe5231e2c3a1cc749376ca09d5545f362f4d08d786ae166665ebe58934df5972cf300dc1772197b40a01f84b870598c2d9aaf5deb8419402330f93c4bc2f688f7af8bb2c80d3da47240da347bdf5e15ab6bdb3160d7b11905d4ef889deac57dfbbb5b3530d02fd411a1e6baafb8e7a805a3937bf923001f84b870598c2d9aaf5deb841674d5c584b2f3b56cb3df1e023a2049f68eb5bb7b9a024c2faf7c2309d1031ff2686e0307b30e8e00b90bd2e98ed37d2a9a995e02bdcad50e6292a16a0dc83b800f84b870598c2d9aaf5deb841facbb629ab0ddf3c1d33f95d78192c30aaff836db60b810cf177cb9b8aee0a9c0f2c13bcbe4a4f2a05c2e9c9e7b03076335d4609fea267b1cc944cd2888cb34a00f800f800c0",
        Hex.toHexString(messages));
        //System.out.println(Hex.toHexString(messages));
    }*/

  /*  @Test
    public void testVotes() {
        byte[] bytes = Hex
                .decode("f9015b00e201a077267e69401942b1d0e5955bb8a936940b0f439a0f7649c8bba2ba5e6deb04a3f90134f84b870598c2d9aaf5deb84115058471a91fc3a9fd0d959e4cbe459ef470528602d750fe5231e2c3a1cc749376ca09d5545f362f4d08d786ae166665ebe58934df5972cf300dc1772197b40a01f84b870598c2d9aaf5deb8419402330f93c4bc2f688f7af8bb2c80d3da47240da347bdf5e15ab6bdb3160d7b11905d4ef889deac57dfbbb5b3530d02fd411a1e6baafb8e7a805a3937bf923001f84b870598c2d9aaf5deb841674d5c584b2f3b56cb3df1e023a2049f68eb5bb7b9a024c2faf7c2309d1031ff2686e0307b30e8e00b90bd2e98ed37d2a9a995e02bdcad50e6292a16a0dc83b800f84b870598c2d9aaf5deb841facbb629ab0ddf3c1d33f95d78192c30aaff836db60b810cf177cb9b8aee0a9c0f2c13bcbe4a4f2a05c2e9c9e7b03076335d4609fea267b1cc944cd2888cb34a00");
        Votes2 votes = new Votes2(bytes, Codec.rlp);

        String[] expected = new String[]
                {"15058471a91fc3a9fd0d959e4cbe459ef470528602d750fe5231e2c3a1cc749376ca09d5545f362f4d08d786ae166665ebe58934df5972cf300dc1772197b40a01",
                "9402330f93c4bc2f688f7af8bb2c80d3da47240da347bdf5e15ab6bdb3160d7b11905d4ef889deac57dfbbb5b3530d02fd411a1e6baafb8e7a805a3937bf923001",
                "674d5c584b2f3b56cb3df1e023a2049f68eb5bb7b9a024c2faf7c2309d1031ff2686e0307b30e8e00b90bd2e98ed37d2a9a995e02bdcad50e6292a16a0dc83b800",
                "facbb629ab0ddf3c1d33f95d78192c30aaff836db60b810cf177cb9b8aee0a9c0f2c13bcbe4a4f2a05c2e9c9e7b03076335d4609fea267b1cc944cd2888cb34a00"};

        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], Hex.toHexString(votes.getVoteItems()[i].getSignature()));
        }
    }*/
/*
    public void testRelayMessage() {
        var msg = Hex.decode("f916bbf913b9b9023af90237b8d2f8d00282369f8705b7a7bb61ebac9500f9ec5bf9c18c3b2587ed35ca50aaae197b50ffa2a0f5f80bacdc4d17a9a86a32d9dfac64e8834a9565b47fa9e8d56149d2550cdcc6a0c97e3857bc5c25d450047e5ed01b5541ec3b83bfc9cabf5394b5e07456883a5fa0a99f6171d39494c3a4f3bf8fcce62d155fe95c014164c1ff766628ccb28e05faf800a0057ae8d9c472a1c6af897b7f6bf09c7b1ff2fe3e8196af377096f4d578d0f17080a6e5a0100b662fd45e2a71e4f214f67058f03b66b4a7179d4a8cdf48b407af49b42149f800f800b9015ef9015b00e201a0502da52a3aff1906e866bc1d7dd0b9dbb30bd57a04fb6bede6d8de1a353f774df90134f84b8705b7a7bb71834bb841c2c35e7ea5a019eedcf24bb662d918a7715e1487afd86fee37b0d7e536a93a5707acb25bb0fc7473328abf0dfcf86bb18f5dfced5e08b54c803e73005d3b153801f84b8705b7a7bb72007db8411819064c71dd704d9114adcdb1e30b9834c09b09171078f16082d50f6586242f3482b6f7ee43e51f3bed0263690e37242493a300969827e7f4e9834903a101d900f84b8705b7a7bb71abceb8413a83c0d0953a7aaed9fbc3f351f62573d97cf41fa5f58737f610a1786cb8cb236dbce380338fd42d0dd18a13bf69b000b90df923a4f98917c211f477201f26cf01f84b8705b7a7bb71dd51b841cad0086f94c7e650c3bcb6e320f1d29ea5e7c81b23c4b9263e5cb682407111e63769171007b41f97038b82765186811e07a6f0ef0d9d5f4c9249e49d5119a81301f800b9023cf90239b8d4f8d2028236a08705b7a7bb71c48f950011858429081490da2a24abb4156d5709839a27a1a07373dc1c60f6e52f4adaf69d1e6cfd8422c6bb65f5c0651b7efe879a14120ed1a0e4d042989b04dfa7f5ae62db8ca990edf2b9c55e2d477682db4a0b17ccb0b4b2a0a99f6171d39494c3a4f3bf8fcce62d155fe95c014164c1ff766628ccb28e05faf800f80080b846f844a0a75987ed2c2a8afacf0ff2c4c9f5668309fc3a315bad1b378f9ca6031aebfe0bf800a090b497636a85b33a6fb74386a81bbbb9b648e1decc0d662ce37a6fbc543a8ae8b9015ef9015b00e201a03a36ee375a41bfd3c2a39859790039886f62b6b5a85e0855365a37ccdbe26461f90134f84b8705b7a7bb805539b841ed5e36d8d66802990bce292253ca52511885a24704f0889e4203d4112b1db7f94d8f00164ac3931398837486a3697bda191729df25b515ea40f42d7dd011195c00f84b8705b7a7bb806b34b84143f97739289fc128a01db17e1f63b59ba7f1a1361ab6aa2524acbaacd395a61e62bde2e2fa1c69c53630faab4efd3ee9f306cfabddb2f42d4be1b34d6885ed4201f84b8705b7a7bb80554cb841c7d93f858c90385caadacdf4ed2b9714c448852ee83ea565169be0d8083ddab21b3150fd62ad1d8e1c8b73b84c457e0be170ad0e0e8fcd3f7925077d5c2c11cf01f84b8705b7a7bb80554db841cdb937b0e4d5abd1696cacbfb9b46d4f1b4dba4b790b308c548e91b865263d501fff427434382d91ffdfb29dafae29032ff4d0632c54eb3b42f003a8115d859101f800b9021bf90218b8b3f8b1028236a18705b7a7bb80554c9500ab4527f57a5b9afe15c2671e54eae8e2b1224dcea084cd00bd2ec7822c09bf47d07e291fc179239212d8e0902496e1dfbb1790c6aca0c2f55f9a986f10c0044c9750d9e25f2c948097dd52f3d23224ab7b206c830a6ea0a99f6171d39494c3a4f3bf8fcce62d155fe95c014164c1ff766628ccb28e05faf800f80080a6e5a0a75987ed2c2a8afacf0ff2c4c9f5668309fc3a315bad1b378f9ca6031aebfe0bf800f800b9015ef9015b00e201a03a6526a5bfbdbcb4c7ad0578a9eafc26004f9bf33e99018eadc596fed6f58f11f90134f84b8705b7a7bb8f9011b8416b22a1f7a209567e2d8ab1c4b513cf7e18bfa0471dd1a2020f17deafbae986d95094a544131631b4700e5b4eade275f891f0b415d42188468a50d51102162eaf01f84b8705b7a7bb8f9063b8416e4a6b92185a5eb6170a985711252e4a9c1a35cdfbac3bf6d42fbc12dedf23d969fbbec238a4877aa1a829f3ced54204ca1e72cb7f7c8cc98b3d6f5710f8f35700f84b8705b7a7bb8f9011b84130da7b49a574d8619eac9e5e6aca16ef10af11b39774ea8ddcc40e1a6cccd85554116b4dd0d5ad496775447aafad61a3b171918fc40d221b9572bbbbb93f6fe101f84b8705b7a7bb8fa5b9b84108536774c95d79498a73857d1c5473940139d4e7cbd0db771d046c307103ec2a4468ce6b792ee79bc66e5a1aaf7ea303c70f71c776ffab6ed86429dd4176ac3e00f800b9021bf90218b8b3f8b1028236a28705b7a7bb8f903a95002777a469a2eed69bb45a30f457b5133e10f11018a0e9d3435dc85efb8bcf2e6bcb3788632e213e194f949108b5ab5ab8f28cb6f4d1a03e03de6a2c4c5ad81c21e7657a03331a22c55012afa88761763c7a97ddd464bfa0a99f6171d39494c3a4f3bf8fcce62d155fe95c014164c1ff766628ccb28e05faf800f80080a6e5a0a75987ed2c2a8afacf0ff2c4c9f5668309fc3a315bad1b378f9ca6031aebfe0bf800f800b9015ef9015b00e201a0a4d1c4a994a3bc929b7d1db610c2d9cd3b8a45d58c1b802f8d9990416da0b8d7f90134f84b8705b7a7bb9f0875b84104fac671bf2e3ae0af5982226bf514926fd212f3416d0f734c3df42f8a314e830a5a39bc74413c3b437e4284bc22a06856908d0113a00a4a05a80a097ea7e37901f84b8705b7a7bb9ee098b841fd763606ddcdf36c258dcf0dfcfa2f95e4b2a5c6fbb3a7d078be5ced8387ca69476d33bd34052fb269a448af9d9990cea67620d990ccf482da66f809a119249701f84b8705b7a7bb9ee0bbb8413a46ec6c991755023b16c765413d276f9de4e0d48930b7fcfc99ab63aa691cb3118c8d3b9520027136f450460cf8b1b23d8635e373fec21be8adbd978b78a94900f84b8705b7a7bb9ec28ab841afefe11d2fdc65d2d31d6daa05054c3d5c34bc10679342b1223aa9d12d19b6d55a7aad896ad1e5c63fbfc45f8f04e037b423fd8fe5c1eef0394056e82a16476700f800b9021bf90218b8b3f8b1028236a38705b7a7bb9ee0a99500f9ec5bf9c18c3b2587ed35ca50aaae197b50ffa2a04454759dadf9cbc2212316f69314b21db65d5d790eb14c67588af5a8d80a11c6a013a56d65129967b3ccac0ffefd2cb91e7f04ce51457baa92ee79fa3d625aaa64a0a99f6171d39494c3a4f3bf8fcce62d155fe95c014164c1ff766628ccb28e05faf800f80080a6e5a0a75987ed2c2a8afacf0ff2c4c9f5668309fc3a315bad1b378f9ca6031aebfe0bf800f800b9015ef9015b00e201a0a78e6f96c2be0181a9b1caba2ef0d74a2c7e79d811e2c2d5162d9290728fb0a2f90134f84b8705b7a7bbae17d8b841f6dab60d9cb91cfb4d574b335cb4f7a172414f9384a0fa222b2f08585306384d31021bc4925b46aca4b539a6505fa242525211a7fc13517f67fdc626d52eaaae01f84b8705b7a7bbae366cb8416fe0cae5ec2c88e993edfe98e2b11acccaabd7a624ee3a247e27815e9fed154520a74a92f4b8a73a8fcb404bd1bd661b13d3caa4cfdcfb36a83db298ef07d2b600f84b8705b7a7bbae668ab8419206cc09876af4dbe32de0268ebf5d35e23a296fb68135eff05f6c164e50dd2036419db53cc766ff91a9f4baff8e603f8a7466f9cf49fe9109cb0718c032d9ea01f84b8705b7a7bbae366ab841a5bfdfc2f9fa91c4938294a32ef12dcba7b7656f0faa419925b0c9cc4c98794970e28ad0d6796961c334b1c1bc55ce2d97345e1c68ba50aeb81a60ea296b1fb300f800b9021bf90218b8b3f8b1028236a48705b7a7bbae366b950011858429081490da2a24abb4156d5709839a27a1a0ff99b38aa6ff9e662e60d823322ffa2248b7036129bc028b455feb9bf97c5a4ca0ec4ca003ec80cc3d993458936f99f266c52623b3e4b567f02210cb3a7458c3e6a0a99f6171d39494c3a4f3bf8fcce62d155fe95c014164c1ff766628ccb28e05faf800f80080a6e5a0a75987ed2c2a8afacf0ff2c4c9f5668309fc3a315bad1b378f9ca6031aebfe0bf800f800b9015ef9015b00e201a072c7cb57cd5b83551d5938fbe830ae4c682910bece2061686b4ed8941558f9cbf90134f84b8705b7a7bbbd727db8416ecb895707a5120845d5c3043558ac20d39797e416dabb838c92a0dc125a74705e880e5900ba7a6370f2e966f42ef990fec81e529c11a2cdd1384ce4269d30eb00f84b8705b7a7bbbd97aab8413b96567a9c55632fbde9b3e2d2e2f573c5eb19b9cdc56e4c22d3885efd6465be2a81663a4ead4ff1f779a626dea37e1e130211fe36e2a6086a1bfed2f93ef25101f84b8705b7a7bbbdadc7b841c61de19fe81ef769687ab195109e61e3378bd9bf12e24a5afb39451136f93b013a12244b44bf9aa49d6bd69a4aba4ec9efd1721337956d193715d028f7499ed400f84b8705b7a7bbbd9790b841268e7265b49126deeb9964f3586a7e27b3a1b6268d3282ce711183b09c8dd28c23a12981151642345b03327203247ba24fcd6e062b081e68a2b33594c3b7fdc701f800b9021bf90218b8b3f8b1028236a58705b7a7bbbd979d9500ab4527f57a5b9afe15c2671e54eae8e2b1224dcea0a792113338ceb371a55bed4fe33a851e16888f02a1274f504159cfb3d4c83947a0ab2b1eca4699560610744cc3758a0016a91974cbdf9736682430e62931118b04a0a99f6171d39494c3a4f3bf8fcce62d155fe95c014164c1ff766628ccb28e05faf800f80080a6e5a0a75987ed2c2a8afacf0ff2c4c9f5668309fc3a315bad1b378f9ca6031aebfe0bf800f800b9015ef9015b00e201a04c3ef744d259614ecc97cf89c4b9b04afbc0312065e8c8e8e5efb5cf88874babf90134f84b8705b7a7bbccd62cb841cc8f1fa1f153b9bf77ad13a2764957c6b47adb15ea054b8584f72d34f008cd552be17e882b4f2f16d7e663db5859814bc075cf41cadf9d8c2378f06bb281c56300f84b8705b7a7bbccbaa0b841b34fb10f58e0e6f8d2f701668faa301898a98f3dc2a1fe7ca00a0befdd9ec8b53ee04ce010a0b86c4fb2b0930cf3aa509f351b82d9c4eed688092d288a52a0b401f84b8705b7a7bbccbaa0b8411e908f4510445699c3a1a2c957f6f36f6919e56223a380b1da8741aa45dddc836ab475d437d4cc3d349cb4fb4f8241ad07ae35fda6ac04e475df719b5083a69101f84b8705b7a7bbccba87b8411eb24153972a54289f4e6c3943810fcbc1fbf9af5724cc2e2dc048e0dec6aeaf57540a7ab07189756ab7148b9f90931adbaea84690db8fac8870cfd4973bc87001f800b9023af90237b8d2f8d0028236a68705b7a7bbccbaa095002777a469a2eed69bb45a30f457b5133e10f11018a022cf5ee004d67d3aeddbcaa58a3de87606296238b866ab7ab89eb786597fbe6ea0cf44215d42e2fb885cb86712614584c4d979cd8d88b7416153dfaf47743fa866a0a99f6171d39494c3a4f3bf8fcce62d155fe95c014164c1ff766628ccb28e05faf800a08732ea4f381a0cf56a55b6de84b008a9559836f02595131b38fbad921dddd15680a6e5a0a75987ed2c2a8afacf0ff2c4c9f5668309fc3a315bad1b378f9ca6031aebfe0bf800f800b9015ef9015b00e201a005edf1e9b2ed6888683686762fecc7e95dc1abebd19764e43a970d4d725c5db3f90134f84b8705b7a7bbdc3d4cb841033253ed597a348ec6b6418aa5e63c22e8bf4cefa98591eba0b377e75dd22c0c3c27a93673094cbec64607188a217bf005e773a57eb5281cbe319d9557f2daae00f84b8705b7a7bbdc549cb8414eea75c2e73254b63df4281839a8536bfa38e0650e60c577ceb9ea0358b6d2bb01b0833a715bb762f247b8710a572cbf65c71af96b69621abc364cefbe372d0300f84b8705b7a7bbdc25b4b8415656e08611c175b5122b9d8c2977814dd275f907a133f4053da16ce4f25c33dc281fcdea96e48b8cbde7ea298163bc55e91c278ab46326f9893b1408c74fecf601f84b8705b7a7bbdc0f4db841742be392bafe163a7c66adebf7ded04cc1e62b6059d7951dabcd471ca434dc8729eefbe2c636d19e1bd925faf9a2e226c5ae669f95f48d5ec5b14954b5ce379301f800b90267f90264b8fff8fd028236a78705b7a7bbdc31809500f9ec5bf9c18c3b2587ed35ca50aaae197b50ffa2a0a5eea935725ca4b18b138d2f417f34ae89b6ab7149f097fca2ef22e429412c28a005743e3f46fce08dc34de4de3553dabd01c426c9e861ae3d1c5d4803b5f7a1bca0a99f6171d39494c3a4f3bf8fcce62d155fe95c014164c1ff766628ccb28e05faf800f800ab10802070482c1a0f0884c2a060285c1c410e852022314874421b158400612048cc7800068fc8a091b84c04b846f844a02bc4a8e966310a84c9eba6ad32232f5c34b66dad78369b2720040254fd85ec72f800a08f072e44f1cb06d331dff43c3f3e2f4cb70e315deb24e538608b5b72ab6bbbe0b9015ef9015b00e201a00fd3a1993e06537db1747708d07d086db76f7d92fcb92aeec219600b6d3d0d56f90134f84b8705b7a7bbeb44b3b8414e9a504d4fbcd22362b39d623cc16130d3d11b2bf73f8763507cfb45e21c1a5467201bea428b8ebaf26af29d7b31ed78d9b40668f8185269558b53aa5c03fa0401f84b8705b7a7bbeb8a25b841df2336921702a53aadf8a4b5fbcff56bcb84879a5a61815307a2d351860d66ef33b3ee23deac1cc3d358c5c32902df326aed6b8c10d98779af5a3ee67651115201f84b8705b7a7bbeb61cbb8418b79c6ac66bbdf2971bfa17a885bf7d3b2b6453aceff6b21da0451421fafe0ba056532f640ef35d309df10496c79e1ba9c40e3d067188a574f09e5f6b2e6e3d500f84b8705b7a7bbeb61dbb841cdc2091a59c777b66d08927efe206ddc074eb25188c65efbacc3538e08ff07bc6e197eeb8176babacea22edb6f77ec8e8782dce61b9c545cca9bbab81802ed4401f800f800f902fab902f7f902f400b90142f9013fb9013cf90139822000b90133f901300095010312fb60e64860c24f1de245a5ff7aa68ad4d88a000000b8ef2100000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000200000000000000000000000000000000000000000800000000000000000000000000000000000000000000000000000000000000000000000002000020000000000000000000000000000000000000000000001000000000000000004000000000000000000000000000000000000000000000000000000000000000006000000000000000000000000000000000000000000000000000000000000000000000000000001000000000000000000f800f800a0eb095869650536d97c85e0753aa39ceb87eba6e70337236ecb2b020d843154b8f901abf901a800b901a4f901a1b9019ef9019b822000b90195f9019295010312fb60e64860c24f1de245a5ff7aa68ad4d88af85a964d657373616765287374722c696e742c627974657329b8406274703a2f2f30786435306166322e69636f6e65652f63786531393437613363616339303736656233613135333832396637396435353431366135353933613301f9011db9011af90117b83e6274703a2f2f3078653931662e69636f6e65652f637830333132666236306536343836306332346631646532343561356666376161363861643464383861b8406274703a2f2f30786435306166322e69636f6e65652f637865313934376133636163393037366562336131353338323966373964353534313661353539336133865f6576656e7400b88bf889844c696e6bf882b83e6274703a2f2f3078653931662e69636f6e65652f637830333132666236306536343836306332346631646532343561356666376161363861643464383861b8406274703a2f2f30786435306166322e69636f6e65652f637865313934376133636163393037366562336131353338323966373964353534313661353539336133");
        RelayMessage relayMessage = ClassDecoderUtil.decodeMessageRelay(msg);
        assertEquals(9, relayMessage.getBlockUpdates().length);
        assertEquals(13983, relayMessage.getBlockUpdates()[0].getBlockHeader().getHeight());
        assertEquals(1, relayMessage.getReceiptProofs().length);
        //System.out.println(Hex.toHexString(relayMessage.getReceiptProofs()[0].getMptProofs()[0]));
    }*/

}

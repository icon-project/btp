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
package foundation.icon.btp.bmv.types;

import foundation.icon.btp.bmv.lib.mpt.MerklePatriciaTree;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;

import java.util.List;

public class ReceiptProof {

    final static String RLPn = "RLPn";

    private final int index;
    private final byte[] mptKey;
    private final List<EventProof> eventProofs;
    private final List<ReceiptEventLog> events;
    private final List<byte[]> mptProofs;

    public ReceiptProof(int index, byte[] mptKey, List<byte[]> mptProofs, List<EventProof> eventProofs, List<ReceiptEventLog> events) {
        this.index = index;
        this.mptKey = mptKey;
        this.mptProofs = mptProofs;
        this.eventProofs = eventProofs;
        this.events = events;
    }

    public static ReceiptProof fromBytes(byte[] serialized) {
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, serialized);
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter(RLPn);
        reader.beginList();
        //Index
        int index = reader.readInt();

        //TODO: change later: see how to accommodate '0x0' from not failing the prove
        //mptKey
        byte[] mptKey = new byte[0];
        if (index != 0) {
            writer.beginList(1);
            writer.write(index);
            writer.end();
            mptKey = writer.toByteArray();
        } else {
            writer.beginList(1);
            writer.write(new byte[0]);
            writer.end();
            byte[] src = writer.toByteArray();
            mptKey = new byte[1];
            System.arraycopy(src, 1, mptKey, 0, 1);

        }

        //proofs
        List<byte[]> mptProofs = readByteArrayListFromRLP(reader.readNullable(byte[].class));
        //EventProofs
        List<EventProof> eventProofs = readEventProof(reader.readNullable(byte[].class));
        //Event Logs
        List<ReceiptEventLog> eventLogs = readEventLog(reader.readNullable(byte[].class));
        return new ReceiptProof(index, mptKey, mptProofs, eventProofs, eventLogs);
    }

    public static List<byte[]> readByteArrayListFromRLP(byte[] serialized) {
        if (serialized == null)
            return null;
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, serialized);
        reader.beginList();
        List<byte[]> lists = new ArrayList<>();
        if (!reader.hasNext())
            return lists;

        while (reader.hasNext()) {
            lists.add(reader.readByteArray());
        }
        reader.end();

        return lists;
    }

    public static List<EventProof> readEventProof(byte[] serialized) {
        if (serialized == null)
            return null;
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, serialized);
        reader.beginList();

        List<EventProof> lists = new ArrayList<>();
        if (!reader.hasNext())
            return lists;
        while (reader.hasNext()) {
            lists.add(EventProof.fromBytes(reader.readByteArray()));
        }
        reader.end();

        return lists;
    }

    public static List<ReceiptEventLog> readEventLog(byte[] serialized) {
        if (serialized == null)
            return null;
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, serialized);
        reader.beginList();

        List<ReceiptEventLog> lists = new ArrayList<>();
        if (!reader.hasNext())
            return lists;
        while (reader.hasNext()) {
            lists.add(ReceiptEventLog.fromBytes(reader.readByteArray()));
        }
        reader.end();

        return lists;
    }

    public static String getRLPn() {
        return RLPn;
    }

    public Receipt prove(byte[] receiptRootHash) {
        try {
            byte[] leaf = MerklePatriciaTree.prove(receiptRootHash, this.mptKey, this.mptProofs);
            Receipt receipt = Receipt.fromBytes(leaf);
            //receipt.setEventLogsWithProofs(eventProofs);//TODO: check this
            return receipt;
        } catch (Exception e) {
            Context.revert(BMVErrorCodes.INVALID_RECEIPT_PROOFS, "Invalid receipt proofs with wrong sequence");
            return null;
        }
    }

    public int getIndex() {
        return index;
    }

    public byte[] getMptKey() {
        return mptKey;
    }

    public List<byte[]> getMptProofs() {
        return mptProofs;
    }

    public List<EventProof> getEventProofs() {
        return eventProofs;
    }

    public List<ReceiptEventLog> getEvents() {
        return events;
    }
}

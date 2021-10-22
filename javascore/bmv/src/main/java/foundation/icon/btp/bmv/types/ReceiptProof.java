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

import foundation.icon.btp.bmv.lib.HexConverter;
import foundation.icon.btp.bmv.lib.mpt.MPTException;
import foundation.icon.btp.bmv.lib.mpt.Trie;
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
        reader.beginList();
        //Index
        int index = reader.readInt();

        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter(RLPn);
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

        List<byte[]> mptProofs = new ArrayList<>();
        //mptProofs.add(reader.readNullable(byte[].class));
        ObjectReader mptProofReader = Context.newByteArrayObjectReader(RLPn, reader.readNullable(byte[].class));
        try {
            mptProofReader.beginList();
            while (reader.hasNext()) {
                mptProofs.add(mptProofReader.readByteArray());
            }
            mptProofReader.end();
        } catch (Exception e) {
            //TODO: check why last reader.hasNext = true, even where there is no data, hence the exception
        }

        //EventProofs
        List<EventProof> eventProofs = readEventProofs(reader);

        //Event Logs
        List<ReceiptEventLog> eventLogs = new ArrayList<>();
        for (EventProof ef : eventProofs) {
            eventLogs.add(ReceiptEventLog.fromBytes(ef.getProof()));
        }

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

    public static List<EventProof> readEventProofs(ObjectReader reader) {
        List<EventProof> eventProofs = new ArrayList<>();

        reader.beginList();
        while (reader.hasNext()) {
            reader.beginList();
            int index = reader.readInt();
            byte[] proof = reader.readNullable(byte[].class);
            eventProofs.add(new EventProof(index, proof));
            reader.end();
        }
        reader.end();

        return eventProofs;
    }

    public Receipt prove(byte[] receiptRootHash) throws MPTException {
        //byte[] leaf = MerklePatriciaTree.prove(receiptRootHash, this.mptKey, this.mptProofs);
        byte[] leaf = Trie.verifyProof(receiptRootHash, this.mptKey, this.mptProofs);
        //receipt.setEventLogsWithProofs(eventProofs);//TODO: check this
        return Receipt.fromBytes(leaf);
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

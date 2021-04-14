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

import score.Context;
import score.ObjectReader;

public class ReceiptProof {

    final static String RLPn = "RLPn";

    private final int index;
    private final byte[] mptKey;
    private final byte[][] mptProofs;
    private final EventProof[] eventProofs;

    public ReceiptProof(int index, byte[] mptKey, byte[][] mptProofs, EventProof[] eventProofs) {
        this.index = index;
        this.mptKey = mptKey;
        this.mptProofs = mptProofs;
        this.eventProofs = eventProofs;
    }

    public static ReceiptProof fromBytes(byte[] serialized) {
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, serialized);
        reader.beginList();

        int index = reader.readInt();
        byte[] mptKey = new byte[]{};
        byte[][] mptProofs = readMPTProofs(reader.readByteArray());

        var eventProofs = Context.newArrayDB("eventProofs", EventProof.class);
        reader.beginList();
        reader.beginList();

        while(reader.hasNext())
            EventProof.fromBytes(reader.readByteArray());

        var evp = new EventProof[eventProofs.size()];
        for (int i = 0; i < evp.length; i++)
            evp[i] = eventProofs.get(i);

        return new ReceiptProof(index, mptKey, mptProofs, evp);
    }

    public static byte[][] readMPTProofs(byte[] serialized) {
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, serialized);
        reader.beginList();

        //FIXME - use scorex ArrayList or ArrayDB
        byte[][] tmp = new byte[100][];
        int length = 0;
        while (reader.hasNext()) {
            tmp[length] = reader.readByteArray();
            length = length + 1;
        }

        reader.end();

        byte[][] mptProofs = new byte[length][];
        System.arraycopy(tmp, 0, mptProofs, 0, length);

        return mptProofs;
    }

    public Receipt prove(byte[] receiptHash){
        byte[] leaf = MerklePatriciaTree.prove(receiptHash, this.mptKey, this.mptProofs);
        return Receipt.fromBytes(leaf);
    }

    public int getIndex() {
        return index;
    }

    public byte[] getMptKey() {
        return mptKey;
    }

    public byte[][] getMptProofs() {
        return mptProofs;
    }

    public EventProof[] getEventProofs() {
        return eventProofs;
    }

}

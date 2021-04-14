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

public class EventProof {

    final static String RLPn = "RLPn";

    private int index;
    private byte[] mptKey;
    private byte[][] mptProofs;

    public EventProof(int index, byte[] mptKey, byte[][] mptProofs) {
        this.index = index;
        this.mptKey = mptKey;
        this.mptProofs = mptProofs;
    }

    public static EventProof fromBytes(byte[] serialized) {
        if(serialized == null)
            return null;
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, serialized);
        reader.beginList();

        if (!reader.hasNext())
            return new EventProof(0, new byte[]{0}, new byte[][]{});

        int index = reader.readInt();

        byte[][] mptProofs = null;

        if(reader.hasNext())
          mptProofs = ReceiptProof.readMPTProofs(reader.readByteArray());

        EventProof eventProof = new EventProof(index, new byte[0], mptProofs);

        reader.end();
        return eventProof;
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

}
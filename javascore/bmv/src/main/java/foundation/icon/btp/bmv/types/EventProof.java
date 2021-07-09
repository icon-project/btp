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

import score.Context;
import score.ObjectReader;

//TODO: Change all the field here: write prove function and return eventLog function from leaf ref: message.py: 477
//event proof receiver.py:157
public class EventProof {

    final static String RLPn = "RLPn";

    private final int index;
    private final byte[] proof;

    public EventProof(int index, byte[] proof) {
        this.index = index;
        this.proof = proof;
    }

    public static EventProof fromBytes(byte[] serialized) {
        if (serialized == null)
            return null;
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, serialized);
        reader.beginList();

        if (!reader.hasNext())
            return new EventProof(0, new byte[]{0});

        int index = reader.readInt();
        byte[] proof = reader.readByteArray();
        EventProof eventProof = new EventProof(index, proof);

        reader.end();
        return eventProof;
    }

    public static String getRLPn() {
        return RLPn;
    }

    public int getIndex() {
        return index;
    }

    public byte[] getProof() {
        return proof;
    }
}
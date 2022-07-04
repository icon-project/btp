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

package foundation.icon.btp.bmv.btpblock;

import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;

import java.util.List;

public class Proofs {
    private byte[][] proofs;

    public byte[][] getProofs() {
        return proofs;
    }

    public void setProofs(byte[][] proofs) {
        this.proofs = proofs;
    }

    public static Proofs readObject(ObjectReader r) {
        r.beginList();
        Proofs obj = new Proofs();
        r.beginList();
        byte[][] proofs;
        List<byte[]> proofList = new ArrayList<>();
        while(r.hasNext()) {
            proofList.add(r.readByteArray());
        }
        int proofsLength = proofList.size();
        proofs = new byte[proofsLength][];
        for (int i = 0; i < proofsLength; i++) {
            proofs[i] = proofList.get(i);
        }
        r.end();
        obj.setProofs(proofs);
        r.end();
        return obj;
    }

    public static Proofs fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return Proofs.readObject(reader);
    }
}

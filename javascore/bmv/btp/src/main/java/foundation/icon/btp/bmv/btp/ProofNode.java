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

package foundation.icon.btp.bmv.btp;

import score.ObjectReader;

import java.math.BigInteger;

public class ProofNode {
    private int numOfLeaf;
    private byte[] value;

    public ProofNode(int level, byte[] value) {
        this.numOfLeaf = level;
        this.value = value;
    }

    public int getNumOfLeaf() {
        return numOfLeaf;
    }

    public byte[] getValue() {
        return value;
    }

    public static ProofNode readObject(ObjectReader r) {
        r.beginList();
        ProofNode obj = new ProofNode(r.readInt(), r.readByteArray());
        r.end();
        return obj;
    }

}

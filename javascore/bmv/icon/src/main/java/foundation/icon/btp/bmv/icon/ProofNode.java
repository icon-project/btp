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

package foundation.icon.btp.bmv.icon;

import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class ProofNode {
    private BigInteger level;
    private byte[] value;

    public ProofNode(BigInteger level, byte[] value) {
        this.level = level;
        this.value = value;
    }

    public static ProofNode readObject(ObjectReader r) {
        r.beginList();
        ProofNode obj = new ProofNode(
                r.readBigInteger(),
                r.readByteArray()
        );
        r.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(2);
        writer.write(level);
        writer.write(value);
        writer.end();
    }
}

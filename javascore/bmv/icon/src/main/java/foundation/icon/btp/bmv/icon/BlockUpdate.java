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

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class BlockUpdate {
    private BigInteger mainHeight;
    private BigInteger round;
    private byte[] nextProofContextHash;
    private byte[][] networkSectionToRoot;
    private BigInteger nid;
    private BigInteger updateNumber;
    private byte[] prev;
    private BigInteger messageCount;
    private byte[] messageRoot;
    private byte[] proof;

    public BlockUpdate(
            BigInteger mainHeight,
            BigInteger round,
            byte[] nextProofContextHash,
            byte[][] networkSectionToRoot,
            BigInteger nid,
            BigInteger updateNumber,
            byte[] prev,
            BigInteger messageCount,
            byte[] messageRoot,
            byte[] proof
    ) {
        this.mainHeight = mainHeight;
        this.round = round;
        this.nextProofContextHash = nextProofContextHash;
        this.networkSectionToRoot = networkSectionToRoot;
        this.nid = nid;
        this.updateNumber = updateNumber;
        this.prev = prev;
        this.messageCount = messageCount;
        this.messageRoot = messageRoot;
        this.proof = proof;
    }

    public static BlockUpdate readObject(ObjectReader r) {
        r.beginList();
        BlockUpdate obj = new BlockUpdate(
                r.readNullable(BigInteger.class),
                r.readNullable(BigInteger.class),
                r.readNullable(byte[].class),
                r.readNullable(byte[][].class),
                r.readBigInteger(),
                r.readBigInteger(),
                r.readNullable(byte[].class),
                r.readBigInteger(),
                r.readNullable(byte[].class),
                r.readNullable(byte[].class)
        );
        r.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(10);
        writer.writeNullable(mainHeight);
        writer.writeNullable(round);
        writer.writeNullable(nextProofContextHash);
        writer.writeNullable((Object) networkSectionToRoot);
        writer.write(nid);
        writer.write(updateNumber);
        writer.writeNullable(prev);
        writer.write(messageCount);
        writer.writeNullable(messageRoot);
        writer.writeNullable(proof);
        writer.end();
    }

    public static BlockUpdate fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return BlockUpdate.readObject(reader);
    }
}

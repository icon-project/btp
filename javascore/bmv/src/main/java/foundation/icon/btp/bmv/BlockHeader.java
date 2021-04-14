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

public class BlockHeader {

    final static String RLPn = "RLPn";

    private final int version;
    private final long height;
    private final byte[] proposer;
    private final byte[] prevID;
    private final long timestamp;
    private final byte[] votesHash;
    private final byte[] nextValidatorHash;
    private final byte[] normalReceiptHash;
    private final byte[] hash;

    public BlockHeader(int version,
                       long height,
                       byte[] proposer,
                       byte[] prevID,
                       long timestamp,
                       byte[] votesHash,
                       byte[] nextValidatorHash,
                       byte[] normalReceiptHash,
                       byte[] hash) {

        this.version = version;
        this.height = height;
        this.proposer = proposer;
        this.prevID = prevID;
        this.timestamp = timestamp;
        this.votesHash = votesHash;
        this.nextValidatorHash = nextValidatorHash;
        this.normalReceiptHash = normalReceiptHash;
        this.hash = hash;
    }

    public static BlockHeader fromBytes(byte[] bytes) {
        byte[] hash = Context.sha3_256(bytes);
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, bytes);
        reader.beginList();

        int version = reader.readInt();
        long height = reader.readLong();
        long timestamp = reader.readLong();

        byte[] proposer = reader.readByteArray();
        byte[] prevID = reader.readNullable(byte[].class);;
        byte[] votesHash = reader.readNullable(byte[].class);
        byte[] nextValidatorHash = reader.readByteArray();
        reader.skip(3); // PatchTransactionsHash, NormalTransactionHash, LogBloom

        var rr = Context.newByteArrayObjectReader(RLPn, reader.readByteArray());
        rr.beginList();
        rr.skip(2); // StateHash, PatchReceiptsHash

        byte[] normalReceiptHash = rr.readNullable(byte[].class);

        while (rr.hasNext())
            rr.skip(1);

        rr.end();

        // clean-up remains
        while (reader.hasNext())
            reader.skip(1);

        reader.end();

        return new BlockHeader(version,
                height,
                proposer,
                prevID,
                timestamp,
                votesHash,
                nextValidatorHash,
                normalReceiptHash,
                hash);
    }

    public int getVersion() {
        return version;
    }

    public long getHeight() {
        return height;
    }

    public byte[] getVotesHash() {
        return votesHash;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getNextValidatorHash() {
        return nextValidatorHash;
    }

    public byte[] getPrevID() {
        return prevID;
    }

    public byte[] getNormalReceiptHash() {
        return normalReceiptHash;
    }

    public byte[] getHash() {
        return hash;
    }
}

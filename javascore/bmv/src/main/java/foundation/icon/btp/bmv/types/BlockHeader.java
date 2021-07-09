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

import java.math.BigInteger;

public class BlockHeader {

    final static String RLPn = "RLPn";
    private final byte[] parentHash;
    private final byte[] stateRoot;
    private final byte[] transactionsRoot;
    private final byte[] receiptsRoot;
    private final byte[] logsBloom;
    private final BigInteger difficulty;
    private final BigInteger number;
    private final long timestamp;
    private final byte[] hash;

    public BlockHeader(byte[] parentHash, byte[] stateRoot, byte[] transactionsRoot, byte[] receiptsRoot, byte[] logsBloom, BigInteger difficulty, BigInteger number, long timestamp, byte[] hash) {
        this.parentHash = parentHash;
        this.stateRoot = stateRoot;
        this.transactionsRoot = transactionsRoot;
        this.receiptsRoot = receiptsRoot;
        this.logsBloom = logsBloom;
        this.difficulty = difficulty;
        this.number = number;
        this.timestamp = timestamp;
        this.hash = hash;
    }

    public static BlockHeader fromBytes(byte[] bytes) {
        if (bytes == null)
            return null;
        byte[] hash = Context.hash("keccak-256", bytes);
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, bytes);
        reader.beginList();

        byte[] parentHash = reader.readByteArray();
        reader.skip(2);
        byte[] stateRoot = reader.readByteArray();
        byte[] transactionsRoot = reader.readByteArray();
        byte[] receiptsRoot = reader.readByteArray();
        byte[] logsBloom = reader.readByteArray();
        BigInteger difficulty = reader.readBigInteger();
        BigInteger number = reader.readBigInteger();
        reader.skip(2);
        long timestamp = reader.readLong();
        // clean-up remains
        while (reader.hasNext())
            reader.skip(1);

        reader.end();

        return new BlockHeader(parentHash,
                stateRoot,
                transactionsRoot,
                receiptsRoot,
                logsBloom,
                difficulty,
                number,
                timestamp,
                hash);
    }

    public static String getRLPn() {
        return RLPn;
    }

    public byte[] getParentHash() {
        return parentHash;
    }

    public byte[] getStateRoot() {
        return stateRoot;
    }

    public byte[] getTransactionsRoot() {
        return transactionsRoot;
    }

    public byte[] getReceiptsRoot() {
        return receiptsRoot;
    }

    public BigInteger getDifficulty() {
        return difficulty;
    }

    public BigInteger getNumber() {
        return number;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public byte[] getLogsBloom() {
        return logsBloom;
    }

    public byte[] getHash() {
        return hash;
    }
}

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

public class BlockProof {

    final static String RLPn = "RLPn";

    private BlockHeader blockHeader;
    private BlockWitness blockWitness;

    public BlockProof(BlockHeader blockHeader, BlockWitness blockWitness) {
        this.blockHeader = blockHeader;
        this.blockWitness = blockWitness;
    }

    public static BlockProof fromBytes(byte[] serialized) {
        if (serialized == null)
            return null;
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, serialized);
        reader.beginList();

        BlockProof bp = new BlockProof(
                 BlockHeader.fromBytes(reader.readByteArray()),
                 BlockWitness.fromBytes(reader.readByteArray()));

        reader.end();
        return bp;
    }

    public BlockHeader getBlockHeader() {
        return blockHeader;
    }

    public BlockWitness getBlockWitness() {
        return blockWitness;
    }

    /**
     * Verifies a given block hash if the following conditions are met:
     *
     * 1. Valid witness exists - none fails / throws invalid wintness
     * 2. Given block update height is not above MTA height
     *
     */
    public boolean verify(MerkleTreeAccumulator mta) {
        Context.require(this.blockWitness != null);
        Context.require(blockHeader.getHeight() <= mta.getHeight()); // Block update height should've been previously added to MTA
        return blockWitness.verify(mta, blockHeader.getHash(), (int)blockHeader.getHeight());
    }
}

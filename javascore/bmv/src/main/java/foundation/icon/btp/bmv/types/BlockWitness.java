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

import foundation.icon.btp.bmv.lib.mta.MTAException;
import foundation.icon.btp.bmv.lib.mta.MerkleTreeAccumulator;
import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;

/**
 * Block witness
 */
public class BlockWitness {

    final static String RLPn = "RLPn";

    private final int height;
    private List<byte[]> witness;

    public BlockWitness(byte[] enc) {
        BlockWitness bw = BlockWitness.fromBytes(enc);
        this.height = bw.height;
        this.witness = bw.witness;
    }

    private BlockWitness(int height, List<byte[]> witness) {
        this.height = height;
        this.witness = witness;
    }

    public static BlockWitness fromBytes(byte[] serialized) {
        if (serialized == null)
            return null;
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, serialized);
        assert reader != null;
        reader.beginList();
        int height = reader.readInt();
        //reader.beginList();

        List<byte[]> witness = new ArrayList<>();
        while (reader.hasNext())
            witness.add(reader.readNullable(byte[].class));

        return new BlockWitness(height, witness);
    }


    public int getHeight() {
        return this.height;
    }


    public List<byte[]> getWitness() {
        return witness;
    }

    public void verify(MerkleTreeAccumulator mta, byte[] hash, BigInteger givenHeight) {
        //TODO: remove try catch later
        try {
            mta.verify(witness, hash, givenHeight.longValue(), this.height);
        } catch (IllegalStateException | MTAException e) {
            Context.revert(BMVErrorCodes.INVALID_BLOCK_WITNESS, "Invalid Block Witness" + e.toString());
        }
    }
}

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

import score.Address;
import score.ArrayDB;
import score.Context;
import score.ObjectReader;
import score.annotation.External;
import scorex.util.ArrayList;

import java.util.List;

/**
 * Block witness
 */
public class BlockWitness {

    final static String RLPn = "RLPn";

    private final int height;
    private byte[][] witness;

    public BlockWitness(byte[] enc) {
        BlockWitness bw = BlockWitness.fromBytes(enc);
        this.height = bw.height;
        this.witness = bw.witness;
    }

    private BlockWitness(int height, byte[][] witness) {
        this.height = height;
        this.witness = witness;
    }

    public static BlockWitness fromBytes(byte[] serialized){
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, serialized);
        assert reader != null;
        reader.beginList();
        int height = reader.readInt();
        reader.beginList();

        List<byte[]> witness = new ArrayList<>();
        while(reader.hasNext())
            witness.add(reader.readNullable(byte[].class));

        byte[][] arr = new byte[witness.size()][];
        for (int i = 0; i < witness.size(); i++)
            arr[i] = witness.get(i);

        return new BlockWitness(height, arr);
    }

    @External(readonly = true)
    public int getHeight() {
        return this.height;
    }

    @External(readonly = true)
    public byte[][] getWitness() {
        return witness;
    }

    /**
     * Verification is called from MerkelTreeAccumalor
     *
     * @param mtaScore // TODO: Pass address instead lookup from the context
     * @param hash
     * @param givenHeight
     */

    @External
    public void verify(Address mtaScore, byte[] hash, int givenHeight) {
        //mta.verify(this.getWitness(), hash, givenHeight, this.height);
    }

    public boolean verify(MerkleTreeAccumulator mta, byte[] hash, int givenHeight) {
        try {
            mta.verify(witness, hash, givenHeight, this.height);
        }catch (IllegalStateException e){
            return false;
        }
        return true;
    }
}

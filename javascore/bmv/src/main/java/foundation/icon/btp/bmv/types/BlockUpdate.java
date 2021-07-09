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
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.util.List;

public class BlockUpdate {

    final static String RLPn = "RLPn";

    private final BlockHeader blockHeader;
    private final Votes votes;
    private final byte[][] nextValidators;
    private ValidatorList nextValidatorList;

    public BlockUpdate(BlockHeader header,
                       Votes votes,
                       byte[][] nextValidators) {
        this.blockHeader = header;
        this.votes = votes;
        this.nextValidators = nextValidators;
        this.nextValidatorList = ValidatorList.fromAddressBytes(nextValidators);
    }

    public static BlockUpdate fromBytes(byte[] serialized) {
        if (serialized == null)
            return null;
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, serialized);
        reader.beginList();

        BlockHeader blockHeader = BlockHeader.fromBytes(reader.readNullable(byte[].class));
        Votes votes = Votes.fromBytes(reader.readNullable(byte[].class));

        byte[][] nextValidators = null;
        if (reader.hasNext())
            nextValidators = readValidators(reader.readNullable(byte[].class));

        reader.end();
        return new BlockUpdate(blockHeader, votes, nextValidators);
    }


    public static void writeObject(ObjectWriter w, BlockUpdate v, byte[] headerBytes) {
        w.beginList(3);
        w.write(headerBytes);
        w.writeNull();
        w.writeNull();
        w.end();
    }


    private static byte[][] readValidators(byte[] bytes) {
        if (bytes == null)
            return new byte[][]{};
        List<byte[]> arr = new ArrayList<byte[]>();
        ObjectReader reader = Context.newByteArrayObjectReader(RLPn, bytes);
        reader.beginList();
        while (reader.hasNext())
            arr.add(reader.readByteArray());
        reader.end();

        byte[][] res = new byte[arr.size()][];
        for (int i = 0; i < res.length; i++)
            res[i] = arr.get(i);
        return res;
    }

    public BlockHeader getBlockHeader() {
        return blockHeader;
    }

    public Votes getVotes() {
        return votes;
    }

    public byte[][] getNextValidators() {
        return nextValidators;
    }

    public boolean verify(ValidatorList validatorList) {
       /*
       if(this.votes == null){
        Context.revert("Votes doesn't exist");
       }
       boolean verified = votes.verify(blockHeader.getHeight(), this.blockHeader.getHash(), validatorList);
        if(blockHeader.getNextValidatorHash() != validatorList.getHash()){
            if(nextValidatorList.getHash() == blockHeader.getNextValidatorHash())
                return true;
        }*/
        return false;
    }

}

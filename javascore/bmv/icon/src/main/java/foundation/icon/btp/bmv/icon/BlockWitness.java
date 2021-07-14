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

package foundation.icon.btp.bmv.icon;

import foundation.icon.score.util.StringUtil;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.util.List;

public class BlockWitness {
    private long height;
    private byte[][] witness;

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    public byte[][] getWitness() {
        return witness;
    }

    public void setWitness(byte[][] witness) {
        this.witness = witness;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BlockWitness{");
        sb.append("height=").append(height);
        sb.append(", witness=").append(StringUtil.toString(witness));
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, BlockWitness obj) {
        obj.writeObject(writer);
    }

    public static BlockWitness readObject(ObjectReader reader) {
        BlockWitness obj = new BlockWitness();
        reader.beginList();
        obj.setHeight(reader.readLong());
        if (reader.beginNullableList()) {
            byte[][] witness = null;
            List<byte[]> witnessList = new ArrayList<>();
            while(reader.hasNext()) {
                witnessList.add(reader.readNullable(byte[].class));
            }
            witness = new byte[witnessList.size()][];
            for(int i=0; i<witnessList.size(); i++) {
                witness[i] = (byte[])witnessList.get(i);
            }
            obj.setWitness(witness);
            reader.end();
        }
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(2);
        writer.write(this.getHeight());
        byte[][] witness = this.getWitness();
        if (witness != null) {
            writer.beginNullableList(witness.length);
            for(byte[] v : witness) {
                writer.write(v);
            }
            writer.end();
        } else {
            writer.writeNull();
        }
        writer.end();
    }

    public static BlockWitness fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return BlockWitness.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        BlockWitness.writeObject(writer, this);
        return writer.toByteArray();
    }

}

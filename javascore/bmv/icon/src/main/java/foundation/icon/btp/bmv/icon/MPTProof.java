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

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

public class MPTProof {
    private long index;
    private Proofs proofs;

    public long getIndex() {
        return index;
    }

    public void setIndex(long index) {
        this.index = index;
    }

    public Proofs getProofs() {
        return proofs;
    }

    public void setProofs(Proofs proofs) {
        this.proofs = proofs;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MPTProof{");
        sb.append("index=").append(index);
        sb.append(", proofs=").append(proofs);
        sb.append('}');
        return sb.toString();
    }


    public static void writeObject(ObjectWriter writer, MPTProof obj) {
        obj.writeObject(writer);
    }

    public static MPTProof readObject(ObjectReader reader) {
        MPTProof obj = new MPTProof();
        reader.beginList();
        obj.setIndex(reader.readLong());
        byte[] proofsBytes = reader.readNullable(byte[].class);
        if (proofsBytes != null) {
            ObjectReader proofsReader = Context.newByteArrayObjectReader("RLPn",proofsBytes);
            obj.setProofs(proofsReader.read(Proofs.class));
        }
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(2);
        writer.write(this.getIndex());
        Proofs proofs = this.getProofs();
        if (proofs != null) {
            ByteArrayObjectWriter proofsWriter = Context.newByteArrayObjectWriter("RLPn");
            proofsWriter.write(proofs);
            writer.writeNullable(proofsWriter.toByteArray());
        } else {
            writer.writeNull();
        }
        writer.end();
    }

    public static MPTProof fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return MPTProof.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        MPTProof.writeObject(writer, this);
        return writer.toByteArray();
    }

}

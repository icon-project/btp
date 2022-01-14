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

package foundation.icon.btp.bmc;

import foundation.icon.score.util.StringUtil;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;

public class DropSequences {

    private BigInteger[] sequences;

    public BigInteger[] getSequences() {
        return sequences;
    }

    public void setSequences(BigInteger[] sequences) {
        this.sequences = sequences;
    }

    public int indexOf(BigInteger sequence) {
        if (sequences == null || sequences.length == 0) return -1;
        for (int i = 0; i < sequences.length; i++) {
            if (sequences[i].equals(sequence)){
                return i;
            }
        }
        return -1;
    }

    static BigInteger[] copyOf(BigInteger[] src, int newLength) {
        BigInteger[] copy = new BigInteger[newLength];
        System.arraycopy(src, 0, copy, 0,
                StrictMath.min(src.length, newLength));
        return copy;
    }

    public void add(BigInteger sequence) {
        if (indexOf(sequence) >= 0) throw BMCException.unknown("already exists _seq");
        if (sequences == null || sequences.length == 0) {
            sequences = new BigInteger[]{sequence};
        } else {
            int len = sequences.length;
            sequences = copyOf(sequences, len+1);
            sequences[len] = sequence;
        }
    }

    public boolean remove(BigInteger sequence) {
        int idx = indexOf(sequence);
        if (idx < 0) return false;
        int newLen = sequences.length - 1;
        BigInteger[] copy = new BigInteger[newLen];
        if (newLen > 0) {
            if (idx == 0) {
                System.arraycopy(sequences, 1, copy, 0,newLen);
            } else {
                System.arraycopy(sequences, 0, copy, 0,idx);
                System.arraycopy(sequences, idx+1, copy, 0,newLen-idx);
            }
        }
        sequences = copy;
        return true;
    }

    public int size() {
        return sequences == null ? 0 : sequences.length;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DropSequences{");
        sb.append("sequences=").append(StringUtil.toString(sequences));
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, DropSequences obj) {
        obj.writeObject(writer);
    }

    public static DropSequences readObject(ObjectReader reader) {
        DropSequences obj = new DropSequences();
        reader.beginList();
        List<BigInteger> sequencesList = new ArrayList<>();
        while(reader.hasNext()) {
            sequencesList.add(reader.readNullable(BigInteger.class));
        }
        BigInteger[] sequences = new BigInteger[sequencesList.size()];
        for(int i=0; i<sequencesList.size(); i++) {
            sequences[i] = sequencesList.get(i);
        }
        obj.setSequences(sequences);
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        BigInteger[] sequences = this.getSequences();
        writer.beginList(sequences.length);
        for(BigInteger v : sequences) {
            writer.write(v);
        }
        writer.end();
    }

    public static DropSequences fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return DropSequences.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        DropSequences.writeObject(writer, this);
        return writer.toByteArray();
    }

}

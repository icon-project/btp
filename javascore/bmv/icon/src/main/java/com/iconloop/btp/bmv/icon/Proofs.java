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

package com.iconloop.btp.bmv.icon;

import com.iconloop.score.util.StringUtil;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.util.List;

public class Proofs {
    private byte[][] proofs;

    public byte[][] getProofs() {
        return proofs;
    }

    public void setProofs(byte[][] proofs) {
        this.proofs = proofs;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Proofs{");
        sb.append("proofs=").append(StringUtil.toString(proofs));
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, Proofs obj) {
        obj.writeObject(writer);
    }

    public static Proofs readObject(ObjectReader reader) {
        Proofs obj = new Proofs();
        if (reader.beginNullableList()) {
            byte[][] proofs = null;
            List<byte[]> proofsList = new ArrayList<>();
            while(reader.hasNext()) {
                proofsList.add(reader.readNullable(byte[].class));
            }
            proofs = new byte[proofsList.size()][];
            for(int i=0; i<proofsList.size(); i++) {
                proofs[i] = (byte[])proofsList.get(i);
            }
            obj.setProofs(proofs);
            reader.end();
        }
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        byte[][] proofs = this.getProofs();
        if (proofs != null) {
            writer.beginNullableList(proofs.length);
            for(byte[] v : proofs) {
                writer.write(v);
            }
            writer.end();
        } else {
            writer.writeNull();
        }
    }

    public static Proofs fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return Proofs.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        Proofs.writeObject(writer, this);
        return writer.toByteArray();
    }
}

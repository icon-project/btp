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

public class ReceiptProof extends MPTProof {
    private MPTProof[] eventProofs;

    public MPTProof[] getEventProofs() {
        return eventProofs;
    }

    public void setEventProofs(MPTProof[] eventProofs) {
        this.eventProofs = eventProofs;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ReceiptProof{");
        sb.append("eventProofs=").append(StringUtil.toString(eventProofs));
        sb.append('}');
        sb.append(super.toString());
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, ReceiptProof obj) {
        obj.writeObject(writer);
    }

    public static ReceiptProof readObject(ObjectReader reader) {
        ReceiptProof obj = new ReceiptProof();
        reader.beginList();
        obj.setIndex(reader.readLong());
        byte[] proofsBytes = reader.readNullable(byte[].class);
        if (proofsBytes != null) {
            ObjectReader proofsReader = Context.newByteArrayObjectReader("RLPn",proofsBytes);
            obj.setProofs(proofsReader.read(Proofs.class));
        }
        if (reader.beginNullableList()) {
            MPTProof[] eventProofs = null;
            List<MPTProof> eventProofsList = new ArrayList<>();
            while(reader.hasNext()) {
                eventProofsList.add(reader.readNullable(MPTProof.class));
            }
            eventProofs = new MPTProof[eventProofsList.size()];
            for(int i=0; i<eventProofsList.size(); i++) {
                eventProofs[i] = (MPTProof)eventProofsList.get(i);
            }
            obj.setEventProofs(eventProofs);
            reader.end();
        }
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(3);
        writer.write(this.getIndex());
        Proofs proofs = this.getProofs();
        if (proofs != null) {
            ByteArrayObjectWriter proofsWriter = Context.newByteArrayObjectWriter("RLPn");
            proofsWriter.write(proofs);
            writer.writeNullable(proofsWriter.toByteArray());
        } else {
            writer.writeNull();
        }
        MPTProof[] eventProofs = this.getEventProofs();
        if (eventProofs != null) {
            writer.beginNullableList(eventProofs.length);
            for(MPTProof v : eventProofs) {
                writer.writeNullable(v);
            }
            writer.end();
        } else {
            writer.writeNull();
        }
        writer.end();
    }

    public static ReceiptProof fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return ReceiptProof.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        ReceiptProof.writeObject(writer, this);
        return writer.toByteArray();
    }
}

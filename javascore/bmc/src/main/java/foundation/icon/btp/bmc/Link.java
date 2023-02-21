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

import foundation.icon.btp.lib.BTPAddress;
import score.Address;
import score.ArrayDB;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;

public class Link {
    private BTPAddress addr; //primary key
    private BigInteger rxSeq;
    private BigInteger txSeq;

    //with suffix("reachable") ArrayDB<String>
    private List<BTPAddress> reachable;

    public BTPAddress getAddr() {
        return addr;
    }

    public void setAddr(BTPAddress addr) {
        this.addr = addr;
    }

    public BigInteger getRxSeq() {
        return rxSeq;
    }

    public void setRxSeq(BigInteger rxSeq) {
        this.rxSeq = rxSeq;
    }

    public BigInteger getTxSeq() {
        return txSeq;
    }

    public void setTxSeq(BigInteger txSeq) {
        this.txSeq = txSeq;
    }

    public List<BTPAddress> getReachable() {
        return reachable;
    }

    public void setReachable(List<BTPAddress> reachable) {
        this.reachable = reachable;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Link{");
        sb.append("addr=").append(addr);
        sb.append(", rxSeq=").append(rxSeq);
        sb.append(", txSeq=").append(txSeq);
        sb.append(", reachable=").append(reachable);
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, Link obj) {
        obj.writeObject(writer);
    }

    public static Link readObject(ObjectReader reader) {
        Link obj = new Link();
        reader.beginList();
        obj.setAddr(reader.readNullable(BTPAddress.class));
        obj.setRxSeq(reader.readNullable(BigInteger.class));
        obj.setTxSeq(reader.readNullable(BigInteger.class));
        if (reader.beginNullableList()) {
            List<BTPAddress> reachable = new ArrayList<>();
            while(reader.hasNext()) {
                reachable.add(reader.readNullable(BTPAddress.class));
            }
            obj.setReachable(reachable);
            reader.end();
        }
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(4);
        writer.writeNullable(this.getAddr());
        writer.writeNullable(this.getRxSeq());
        writer.writeNullable(this.getTxSeq());
        List<BTPAddress> reachable = this.getReachable();
        if (reachable != null) {
            writer.beginNullableList(reachable.size());
            for(BTPAddress v : reachable) {
                writer.write(v);
            }
            writer.end();
        } else {
            writer.writeNull();
        }
        writer.end();
    }

    public static Link fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return Link.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        Link.writeObject(writer, this);
        return writer.toByteArray();
    }
}

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

package foundation.icon.btp.nativecoin;

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class Balance {
    private BigInteger locked;
    private BigInteger refundable;

    public BigInteger getLocked() {
        return locked;
    }

    public void setLocked(BigInteger locked) {
        this.locked = locked;
    }

    public BigInteger getRefundable() {
        return refundable;
    }

    public void setRefundable(BigInteger refundable) {
        this.refundable = refundable;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Balance{");
        sb.append("locked=").append(locked);
        sb.append(", refundable=").append(refundable);
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, Balance obj) {
        obj.writeObject(writer);
    }

    public static Balance readObject(ObjectReader reader) {
        Balance obj = new Balance();
        reader.beginList();
        obj.setLocked(reader.readNullable(BigInteger.class));
        obj.setRefundable(reader.readNullable(BigInteger.class));
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(2);
        writer.writeNullable(this.getLocked());
        writer.writeNullable(this.getRefundable());
        writer.end();
    }

    public static Balance fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return Balance.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        Balance.writeObject(writer, this);
        return writer.toByteArray();
    }

}
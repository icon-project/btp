/*
 * Copyright 2022 ICON Foundation
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

package foundation.icon.btp.xcall.sample;

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class RollbackData {
    private final BigInteger id;
    private final byte[] rollback;
    private BigInteger ssn;

    public RollbackData(BigInteger id, byte[] rollback) {
        this.id = id;
        this.rollback = rollback;
    }

    public BigInteger getId() {
        return id;
    }

    public byte[] getRollback() {
        return rollback;
    }

    public BigInteger getSvcSn() {
        return ssn;
    }

    public void setSvcSn(BigInteger ssn) {
        this.ssn = ssn;
    }

    public static void writeObject(ObjectWriter w, RollbackData data) {
        w.beginList(3);
        w.write(data.id);
        w.write(data.rollback);
        w.writeNullable(data.ssn);
        w.end();
    }

    public static RollbackData readObject(ObjectReader r) {
        r.beginList();
        RollbackData rbData = new RollbackData(
                r.readBigInteger(),
                r.readByteArray()
        );
        rbData.setSvcSn(r.readNullable(BigInteger.class));
        r.end();
        return rbData;
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        writeObject(writer, this);
        return writer.toByteArray();
    }

    public static RollbackData fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return readObject(reader);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof RollbackData)) {
            return false;
        } else {
            RollbackData other = (RollbackData) obj;
            if (this.rollback == null || other.rollback == null) {
                return false;
            }
            if (this.rollback.length != other.rollback.length) {
                return false;
            }
            for (int i = 0; i < this.rollback.length; i++) {
                if (this.rollback[i] != other.rollback[i]) {
                    return false;
                }
            }
            return true;
        }
    }
}

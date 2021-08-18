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

import score.Address;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class Relay {
    private Address address; //primary key
    private long blockCount;
    private BigInteger msgCount;

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public long getBlockCount() {
        return blockCount;
    }

    public void setBlockCount(long blockCount) {
        this.blockCount = blockCount;
    }

    public BigInteger getMsgCount() {
        return msgCount;
    }

    public void setMsgCount(BigInteger msgCount) {
        this.msgCount = msgCount;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Relay{");
        sb.append("address=").append(address);
        sb.append(", blockCount=").append(blockCount);
        sb.append(", msgCount=").append(msgCount);
        sb.append('}');
        return sb.toString();
    }

    public static Relay readObject(ObjectReader reader) {
        Relay obj = new Relay();
        reader.beginList();
        obj.setAddress(reader.readNullable(Address.class));
        obj.setBlockCount(reader.readLong());
        obj.setMsgCount(reader.readNullable(BigInteger.class));
        reader.end();
        return obj;
    }

    public static void writeObject(ObjectWriter writer, Relay obj) {
        obj.writeObject(writer);
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(3);
        writer.writeNullable(this.getAddress());
        writer.write(this.getBlockCount());
        writer.writeNullable(this.getMsgCount());
        writer.end();
    }
}

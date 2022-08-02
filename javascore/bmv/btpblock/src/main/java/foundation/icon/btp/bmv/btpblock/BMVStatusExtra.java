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

package foundation.icon.btp.bmv.btpblock;

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

import java.math.BigInteger;

public class BMVStatusExtra {
    private BigInteger sequenceOffset;
    private BigInteger firstMessageSN;
    private BigInteger messageCount;

    public BMVStatusExtra() {
    }

    public BMVStatusExtra(BigInteger sequenceOffset, BigInteger firstMessageSN, BigInteger messageCount) {
        this.sequenceOffset = sequenceOffset;
        this.firstMessageSN = firstMessageSN;
        this.messageCount = messageCount;
    }

    public BigInteger getSequenceOffset() {
        return sequenceOffset;
    }

    public void setSequenceOffset(BigInteger sequenceOffset) {
        this.sequenceOffset = sequenceOffset;
    }

    public BigInteger getFirstMessageSN() {
        return firstMessageSN;
    }

    public void setFirstMessageSN(BigInteger firstMessageSN) {
        this.firstMessageSN = firstMessageSN;
    }

    public BigInteger getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(BigInteger messageCount) {
        this.messageCount = messageCount;
    }

    public static void writeObject(ObjectWriter writer, BMVStatusExtra obj) {
        obj.writeObject(writer);
    }

    public static BMVStatusExtra readObject(ObjectReader reader) {
        BMVStatusExtra obj = new BMVStatusExtra();
        reader.beginList();
        obj.setSequenceOffset(reader.readBigInteger());
        obj.setFirstMessageSN(reader.readBigInteger());
        obj.setMessageCount(reader.readBigInteger());
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(3);
        writer.write(this.getSequenceOffset());
        writer.write(this.getFirstMessageSN());
        writer.write(this.getMessageCount());
        writer.end();
    }

    public static BMVStatusExtra fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return BMVStatusExtra.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        BMVStatusExtra.writeObject(writer, this);
        return writer.toByteArray();
    }

}

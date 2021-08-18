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
import score.*;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.List;

public class EventLog {
    private Address address;
    private byte[][] indexed;
    private byte[][] data;

    public Address getAddress() {
        return address;
    }

    public void setAddress(Address address) {
        this.address = address;
    }

    public byte[][] getIndexed() {
        return indexed;
    }

    public void setIndexed(byte[][] indexed) {
        this.indexed = indexed;
    }

    public byte[][] getData() {
        return data;
    }

    public void setData(byte[][] data) {
        this.data = data;
    }

    public String methodSignature() {
        String s = new String(indexed[0]);
        return s.substring(0, s.indexOf("("));
    }

    public MessageEvent toMessageEvent() {
        if (methodSignature().equals("Message")) {
            MessageEvent evt = new MessageEvent();
            evt.setNext(new String(indexed[1]));
            evt.setSeq(new BigInteger(indexed[2]));
            evt.setMsg(data[0]);
            return evt;
        }
        return null;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("EventLog{");
        sb.append("address=").append(address);
        sb.append(", indexed=").append(StringUtil.toString(indexed));
        sb.append(", data=").append(StringUtil.toString(data));
        sb.append('}');
        return sb.toString();
    }


    public static void writeObject(ObjectWriter writer, EventLog obj) {
        obj.writeObject(writer);
    }

    public static EventLog readObject(ObjectReader reader) {
        EventLog obj = new EventLog();
        reader.beginList();
        obj.setAddress(reader.readNullable(Address.class));
        reader.beginList();
        byte[][] indexed = null;
        List<byte[]> indexedList = new ArrayList<>();
        while(reader.hasNext()) {
            indexedList.add(reader.readByteArray());
        }
        indexed = new byte[indexedList.size()][];
        for(int i=0; i<indexedList.size(); i++) {
            indexed[i] = (byte[])indexedList.get(i);
        }
        obj.setIndexed(indexed);
        reader.end();
        reader.beginList();
        byte[][] data = null;
        List<byte[]> dataList = new ArrayList<>();
        while(reader.hasNext()) {
            dataList.add(reader.readByteArray());
        }
        data = new byte[dataList.size()][];
        for(int i=0; i<dataList.size(); i++) {
            data[i] = (byte[])dataList.get(i);
        }
        obj.setData(data);
        reader.end();
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(3);
        writer.writeNullable(this.getAddress());
        byte[][] indexed = this.getIndexed();
        writer.beginList(indexed.length);
        for(byte[] v : indexed) {
            writer.write(v);
        }
        writer.end();
        byte[][] data = this.getData();
        writer.beginList(data.length);
        for(byte[] v : data) {
            writer.write(v);
        }
        writer.end();
        writer.end();
    }

    public static EventLog fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return EventLog.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        EventLog.writeObject(writer, this);
        return writer.toByteArray();
    }

}

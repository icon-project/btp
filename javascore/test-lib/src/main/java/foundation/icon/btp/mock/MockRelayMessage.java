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

package foundation.icon.btp.mock;

import foundation.icon.score.util.StringUtil;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;
import scorex.util.Base64;

import java.util.List;

public class MockRelayMessage {
    private Long offset;
    private Long height;
    private Long lastHeight;
    private byte[][] btpMessages;
    private Integer revertCode;
    private String revertMessage;

    public Long getOffset() {
        return offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    public Long getHeight() {
        return height;
    }

    public void setHeight(Long height) {
        this.height = height;
    }

    public Long getLastHeight() {
        return lastHeight;
    }

    public void setLastHeight(Long lastHeight) {
        this.lastHeight = lastHeight;
    }

    public byte[][] getBtpMessages() {
        return btpMessages;
    }

    public void setBtpMessages(byte[][] btpMessages) {
        this.btpMessages = btpMessages;
    }

    public Integer getRevertCode() {
        return revertCode;
    }

    public void setRevertCode(Integer revertCode) {
        this.revertCode = revertCode;
    }

    public String getRevertMessage() {
        return revertMessage;
    }

    public void setRevertMessage(String revertMessage) {
        this.revertMessage = revertMessage;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MockRelayMessage{");
        sb.append("offset=").append(offset);
        sb.append(", height=").append(height);
        sb.append(", lastHeight=").append(lastHeight);
        sb.append(", btpMessages=").append(StringUtil.toString(btpMessages));
        sb.append(", revertCode=").append(revertCode);
        sb.append(", revertMessage='").append(revertMessage).append('\'');
        sb.append('}');
        return sb.toString();
    }

    public String toBase64String() {
        return new String(Base64.getUrlEncoder().encode(toBytes()));
    }

    public static MockRelayMessage fromBase64String(String str) {
        return fromBytes(Base64.getUrlDecoder().decode(str.getBytes()));
    }

    public static void writeObject(ObjectWriter writer, MockRelayMessage obj) {
        obj.writeObject(writer);
    }

    public static MockRelayMessage readObject(ObjectReader reader) {
        MockRelayMessage obj = new MockRelayMessage();
        reader.beginList();
        obj.setOffset(reader.readNullable(Long.class));
        obj.setHeight(reader.readNullable(Long.class));
        obj.setLastHeight(reader.readNullable(Long.class));
        if (reader.beginNullableList()) {
            byte[][] btpMessages = null;
            List<byte[]> btpMessagesList = new ArrayList<>();
            while(reader.hasNext()) {
                btpMessagesList.add(reader.readNullable(byte[].class));
            }
            btpMessages = new byte[btpMessagesList.size()][];
            for(int i=0; i<btpMessagesList.size(); i++) {
                btpMessages[i] = (byte[])btpMessagesList.get(i);
            }
            obj.setBtpMessages(btpMessages);
            reader.end();
        }
        obj.setRevertCode(reader.readNullable(Integer.class));
        obj.setRevertMessage(reader.readNullable(String.class));
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(5);
        writer.writeNullable(this.getOffset());
        writer.writeNullable(this.getHeight());
        writer.writeNullable(this.getLastHeight());
        byte[][] btpMessages = this.getBtpMessages();
        if (btpMessages != null) {
            writer.beginNullableList(btpMessages.length);
            for(byte[] v : btpMessages) {
                writer.writeNullable(v);
            }
            writer.end();
        } else {
            writer.writeNull();
        }
        writer.writeNullable(this.getRevertCode());
        writer.writeNullable(this.getRevertMessage());
        writer.end();
    }

    public static MockRelayMessage fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return MockRelayMessage.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        MockRelayMessage.writeObject(writer, this);
        return writer.toByteArray();
    }

    public static byte[] toBytes(byte[][] bytesArray) {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        writer.beginList(bytesArray.length);
        for(byte[] v : bytesArray) {
            writer.writeNullable(v);
        }
        writer.end();
        return writer.toByteArray();
    }

    public static byte[][] toBytesArray(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        reader.beginList();
        List<byte[]> bytesArrayList = new ArrayList<>();
        while(reader.hasNext()) {
            bytesArrayList.add(reader.readNullable(byte[].class));
        }
        byte[][]  bytesArray = new byte[bytesArrayList.size()][];
        for(int i=0; i<bytesArrayList.size(); i++) {
            bytesArray[i] = bytesArrayList.get(i);
        }
        reader.end();
        return bytesArray;
    }
}

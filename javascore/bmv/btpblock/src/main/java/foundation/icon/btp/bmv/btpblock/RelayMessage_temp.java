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

import foundation.icon.score.util.StringUtil;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.util.Arrays;
import java.util.List;

public class RelayMessage_temp {
    private TypePrefixedMessage[] messages;

    public RelayMessage_temp() {}

    public TypePrefixedMessage[] getMessages() {
        return messages;
    }

    public void setMessages(TypePrefixedMessage[] messages) {
        this.messages = messages;
    }

    public static RelayMessage_temp readObject(ObjectReader reader) {
        reader.beginList();
        RelayMessage_temp relayMessage = new RelayMessage_temp();
        List<TypePrefixedMessage> typePrefixedMessages = new ArrayList<>();
        reader.beginList();
        while(reader.hasNext()) {
            typePrefixedMessages.add(reader.read(TypePrefixedMessage.class));
        }
        reader.end();
        int msgLength = typePrefixedMessages.size();
        TypePrefixedMessage[] messageArray = new TypePrefixedMessage[msgLength];
        for (int i = 0; i < msgLength; i++) {
            messageArray[i] = typePrefixedMessages.get(i);
        }
        relayMessage.setMessages(messageArray);
        reader.end();
        return relayMessage;
    }

    public static void writeObject(ObjectWriter writer, RelayMessage_temp message) {
        writer.beginList(1);
        writer.beginList(message.messages.length);
        for (TypePrefixedMessage typedMessage : message.messages)
            writer.write(typedMessage);
        writer.end();
        writer.end();
    }

    public static RelayMessage_temp fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter w = Context.newByteArrayObjectWriter("RLPn");
        writeObject(w, this);
        return w.toByteArray();
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for(int i = 0; i < messages.length; i++) {
            s.append(messages[i].toString());
        }

        return "RelayMessage{" +
                "messages=" + s +
                '}';
    }

    public static class TypePrefixedMessage {
        public static final int BLOCK_UPDATE = 1;
        public static final int MESSAGE_PROOF = 2;
        private final int type;
        private final byte[] payload;

        public TypePrefixedMessage(int type, byte[] payload) {
            this.type = type;
            this.payload = payload;
        }

        public Object getMessage() {
            if (type == BLOCK_UPDATE) {
                Context.println("BLOCK_UPDATE : " + payload.toString());
                return BlockUpdate.fromBytes(payload);
            } else if (type == MESSAGE_PROOF) {
                Context.println("MESSAGE_PROOF : " + payload.toString());
                return MessageProof.fromBytes(payload);
            }
            throw BMVException.unknown("invalid type : " + type);
        }

        public static TypePrefixedMessage readObject(ObjectReader reader) {
            reader.beginList();
            TypePrefixedMessage typePrefixedMessage = new TypePrefixedMessage(reader.readInt(), reader.readByteArray());
            reader.end();
            return typePrefixedMessage;
        }

        public static TypePrefixedMessage fromBytes(byte[] bytes) {
            ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
            return readObject(reader);
        }

        public static void writeObject(ObjectWriter writer, TypePrefixedMessage message) {
            writer.beginList(2);
            writer.write(message.type);
            writer.write(message.payload);
            writer.end();
        }

        public byte[] toBytes() {
            ByteArrayObjectWriter w = Context.newByteArrayObjectWriter("RLPn");
            writeObject(w, this);
            return w.toByteArray();
        }

        @Override
        public String toString() {
            return "TypePrefixedMessage{" +
                    "type=" + type +
                    ", payload=" + StringUtil.bytesToHex(payload) +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TypePrefixedMessage that = (TypePrefixedMessage) o;
            return type == that.type && Arrays.equals(payload, that.payload);
        }
    }
}
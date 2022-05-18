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

package foundation.icon.btp.bmv.btp;

import score.Context;
import score.ObjectReader;
import scorex.util.ArrayList;

import java.util.List;

public class RelayMessage {
    private TypePrefixedMessage[] messages;

    public RelayMessage() {}

    public TypePrefixedMessage[] getMessages() {
        return messages;
    }

    public void setMessages(TypePrefixedMessage[] messages) {
        this.messages = messages;
    }

    public static RelayMessage readObject(ObjectReader reader) {
        reader.beginList();
        RelayMessage relayMessage = new RelayMessage();
        List<TypePrefixedMessage> typePrefixedMessages = new ArrayList<>();
        while(reader.hasNext()) {
            byte[] elementBytes = reader.readByteArray();
            ObjectReader typePrefixedReader = Context.newByteArrayObjectReader("RLPn", elementBytes);
            typePrefixedMessages.add(typePrefixedReader.read(TypePrefixedMessage.class));
        }
        int msgLength = typePrefixedMessages.size();
        TypePrefixedMessage[] messageArray = new TypePrefixedMessage[msgLength];
        for (int i = 0; i < msgLength; i++) {
            messageArray[i] = typePrefixedMessages.get(i);
        }
        reader.end();
        relayMessage.setMessages(messageArray);
        return relayMessage;
    }

    public static RelayMessage fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return readObject(reader);
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
                return BlockUpdate.fromBytes(payload);
            } else if (type == MESSAGE_PROOF) {
                return MessageProof.fromBytes(payload);
            }
            return null;
        }

        public static TypePrefixedMessage ReadObject(ObjectReader reader) {
            reader.beginList();
            TypePrefixedMessage typePrefixedMessage = new TypePrefixedMessage(reader.readInt(), reader.readByteArray());
            reader.end();
            return typePrefixedMessage;
        }
    }
}
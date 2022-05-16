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

package foundation.icon.btp.bmv.icon;

import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

public class MessageProof {
    private ProofNode[] leftProofNodes;
    private byte[][] messages;
    private ProofNode[] rightProofNodes;

    public MessageProof(ProofNode[] leftProofNodes, byte[][] messages, ProofNode[] rightProofNodes) {
        this.leftProofNodes = leftProofNodes;
        this.messages = messages;
        this.rightProofNodes = rightProofNodes;
    }

    public static MessageProof readObject(ObjectReader r) {
        r.beginList();
        MessageProof obj = new MessageProof(
                r.readNullable(ProofNode[].class),
                r.read(byte[][].class),
                r.readNullable(ProofNode[].class)
        );
        r.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(3);
        writer.writeNullable(leftProofNodes);
        writer.write(messages);
        writer.writeNullable(rightProofNodes);
        writer.end();
    }

    public static MessageProof fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return MessageProof.readObject(reader);
    }
}

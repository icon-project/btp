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

public class MessageProof {
    private ProofNode[] leftProofNodes;
    private byte[][] messages;
    private ProofNode[] rightProofNodes;

    public MessageProof(ProofNode[] leftProofNodes, byte[][] messages, ProofNode[] rightProofNodes) {
        this.leftProofNodes = leftProofNodes;
        this.messages = messages;
        this.rightProofNodes = rightProofNodes;
    }

    public ProofNode[] getLeftProofNodes() {
        return leftProofNodes;
    }

    public byte[][] getMessages() {
        return messages;
    }

    public ProofNode[] getRightProofNodes() {
        return rightProofNodes;
    }

    public static MessageProof readObject(ObjectReader r) {
        r.beginList();
        List<ProofNode> lNodes = new ArrayList<>();
        r.beginList();
        while(r.hasNext()) {
            lNodes.add(r.read(ProofNode.class));
        }
        r.end();
        var lSize = lNodes.size();
        ProofNode[] leftProofNodes = new ProofNode[lSize];
        for (int i = 0; i < lSize; i++){
            leftProofNodes[i] = lNodes.get(i);
        }
        byte[][] messages;
        List<byte[]> messageList = new ArrayList<>();
        r.beginList();
        while(r.hasNext()) {
            messageList.add(r.readByteArray());
        }
        int messagesLength = messageList.size();
        messages = new byte[messagesLength][];
        for(int i = 0; i < messagesLength; i++) {
            messages[i] = messageList.get(i);
        }
        r.end();
        List<ProofNode> rNodes = new ArrayList<>();
        r.beginList();
        while(r.hasNext()) {
            rNodes.add(r.read(ProofNode.class));
        }
        r.end();
        var rSize = rNodes.size();
        ProofNode[] rightProofNodes = new ProofNode[rSize];
        for (int i = 0; i < rSize; i++){
            rightProofNodes[i] = rNodes.get(i);
        }
        r.end();
        return new MessageProof(leftProofNodes, messages, rightProofNodes);
    }

    public static MessageProof fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return MessageProof.readObject(reader);
    }

    public void proveMessage() {
        Node node = new Node();
        int total = 0;
        for (ProofNode pn : leftProofNodes) {
            var num = pn.getNumOfLeaf();
            node.add(num, pn.getValue());
            total += num;
        }

        for (byte[] message : messages) {
            node.add(1, BTPMessageVerifier.hash(message));
            total++;
        }

        for (ProofNode pn : rightProofNodes) {
            var num = pn.getNumOfLeaf();
            node.add(num, pn.getValue());
            total += num;
        }
        node.ensureHash(false);

        var rootNumOfLeaf = node.getNumOfLeaf();
        if (total != rootNumOfLeaf)
            throw BMVException.unknown("total doesn't match total : " + total + ", node : " + rootNumOfLeaf);
        node.verify();
    }
}

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

import foundation.icon.score.util.StringUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class MessageProofTest {
    @Test
    void messageProofTest() {
        ProofNode[] left = {
                new ProofNode(0, StringUtil.hexToBytes("0x01")),
                new ProofNode(1, StringUtil.hexToBytes("0x11"))
        };
        byte[][] messages = {StringUtil.hexToBytes("0x11"), StringUtil.hexToBytes("0x12")};
        ProofNode[] right = {
                new ProofNode(0, StringUtil.hexToBytes("0x01")),
                new ProofNode(1, StringUtil.hexToBytes("0x11")),
                new ProofNode(0, StringUtil.hexToBytes("0x21"))
        };
        MessageProof messageProof = new MessageProof(left, messages, right);
        byte[] bytes = messageProof.toBytes();
        MessageProof fromBytes = MessageProof.fromBytes(bytes);
        assertArrayEquals(messageProof.getLeftProofNodes(), fromBytes.getLeftProofNodes());
        assertArrayEquals(messageProof.getMessages(), fromBytes.getMessages());
        assertArrayEquals(messageProof.getRightProofNodes(), fromBytes.getRightProofNodes());
    }
}

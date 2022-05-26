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

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectWriter;

import java.math.BigInteger;

public class NetworkSection {
    private int nid;
    private int updateNumber;
    private byte[] prev;
    private int messageCnt;
    private byte[] messagesRoot;

    public NetworkSection(int nid, int updateNumber, byte[] prev, int messageCnt, byte[] messagesRoot) {
        this.nid = nid;
        this.updateNumber = updateNumber;
        this.prev = prev;
        this.messageCnt = messageCnt;
        this.messagesRoot = messagesRoot;
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter w = Context.newByteArrayObjectWriter("RLPn");
        writeObject(w);
        return w.toByteArray();
    }

    public void writeObject(ObjectWriter w) {
        w.beginList(5);
        w.write(nid);
        w.write(updateNumber);
        w.write(prev);
        w.write(messageCnt);
        w.write(messagesRoot);
        w.end();
    }

    public byte[] hash() {
        byte[] bytes = toBytes();
        return BTPMessageVerifier.hash(bytes);
    }
}

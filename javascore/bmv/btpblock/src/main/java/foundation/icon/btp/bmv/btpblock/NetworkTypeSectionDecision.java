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
import score.ObjectWriter;

public class NetworkTypeSectionDecision {
    private byte[] srcNetworkId;
    private int networkTypeID;
    private long mainHeight;
    private int round;
    private byte[] networkTypeSectionHash;

    public NetworkTypeSectionDecision(byte[] srcNetworkId, int networkTypeID, long mainHeight, int round, byte[] networkTypeSectionHash) {
        this.srcNetworkId = srcNetworkId;
        this.networkTypeID = networkTypeID;
        this.mainHeight = mainHeight;
        this.round = round;
        this.networkTypeSectionHash = networkTypeSectionHash;
    }

    public byte[] getSrcNetworkId() {
        return srcNetworkId;
    }

    public int getNetworkTypeID() {
        return networkTypeID;
    }

    public long getMainHeight() {
        return mainHeight;
    }

    public int getRound() {
        return round;
    }

    public byte[] getNetworkTypeSectionHash() {
        return networkTypeSectionHash;
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter w = Context.newByteArrayObjectWriter("RLPn");
        writeObject(w);
        return w.toByteArray();
    }

    public void writeObject(ObjectWriter w) {
        w.beginList(5);
        w.write(srcNetworkId);
        w.write(networkTypeID);
        w.write(mainHeight);
        w.write(round);
        w.write(networkTypeSectionHash);
        w.end();
    }

    public byte[] hash() {
        byte[] bytes = toBytes();
        return BTPMessageVerifier.hash(bytes);
    }
}

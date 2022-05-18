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

public class NetworkTypeSectionDecision {
    private byte[] srcNetworkId;
    private int networkTypeID;
    private long mainHeight;
    private int round;
    private byte[] networkTypeSectionHash;

    public byte[] getSrcNetworkId() {
        return srcNetworkId;
    }

    public void setSrcNetworkId(byte[] srcNetworkId) {
        this.srcNetworkId = srcNetworkId;
    }

    public int getNetworkTypeID() {
        return networkTypeID;
    }

    public void setNetworkTypeID(int networkTypeID) {
        this.networkTypeID = networkTypeID;
    }

    public long getMainHeight() {
        return mainHeight;
    }

    public void setMainHeight(long mainHeight) {
        this.mainHeight = mainHeight;
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public byte[] getNetworkTypeSectionHash() {
        return networkTypeSectionHash;
    }

    public void setNetworkTypeSectionHash(byte[] networkTypeSectionHash) {
        this.networkTypeSectionHash = networkTypeSectionHash;
    }
}

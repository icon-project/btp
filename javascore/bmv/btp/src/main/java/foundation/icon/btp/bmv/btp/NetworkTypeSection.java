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

public class NetworkTypeSection {
    private byte[] nextProofContextHash;
    private byte[] networkSectionsRoot;

    public byte[] getNextProofContextHash() {
        return nextProofContextHash;
    }

    public void setNextProofContextHash(byte[] nextProofContextHash) {
        this.nextProofContextHash = nextProofContextHash;
    }

    public byte[] getNetworkSectionsRoot() {
        return networkSectionsRoot;
    }

    public void setNetworkSectionsRoot(byte[] networkSectionsRoot) {
        this.networkSectionsRoot = networkSectionsRoot;
    }

}

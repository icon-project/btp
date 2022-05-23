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

public class NetworkTypeSection {
    private byte[] nextProofContextHash;
    private byte[] networkSectionsRoot;

    public NetworkTypeSection(byte[] nextProofContextHash, byte[] networkSectionsRoot) {
        this.nextProofContextHash = nextProofContextHash;
        this.networkSectionsRoot = networkSectionsRoot;
    }

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

    public byte[] toBytes() {
        ByteArrayObjectWriter w = Context.newByteArrayObjectWriter("RLPn");
        writeObject(w);
        return w.toByteArray();
    }

    public void writeObject(ObjectWriter w) {
        w.beginList(2);
        w.writeNullable(nextProofContextHash);
        w.write(networkSectionsRoot);
        w.end();
    }

    public byte[] hash() {
        byte[] bytes = toBytes();
        return BTPMessageVerifier.hash(bytes);
    }
}

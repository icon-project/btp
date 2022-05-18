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

import java.math.BigInteger;
import java.util.List;

public class BlockUpdate {
    private BigInteger mainHeight;
    private BigInteger round;
    private byte[] nextProofContextHash;
    private byte[][] networkSectionToRoot;
    private BigInteger nid;
    private BigInteger updateNumber;
    private byte[] prev;
    private BigInteger messageCount;
    private byte[] messageRoot;
    private byte[] proof;

    public BigInteger getMainHeight() {
        return mainHeight;
    }

    public BigInteger getRound() {
        return round;
    }

    public byte[] getNextProofContextHash() {
        return nextProofContextHash;
    }

    public byte[][] getNetworkSectionToRoot() {
        return networkSectionToRoot;
    }

    public BigInteger getNid() {
        return nid;
    }

    public BigInteger getUpdateNumber() {
        return updateNumber;
    }

    public byte[] getPrev() {
        return prev;
    }

    public BigInteger getMessageCount() {
        return messageCount;
    }

    public byte[] getMessageRoot() {
        return messageRoot;
    }

    public byte[] getProof() {
        return proof;
    }

    public BlockUpdate(
            BigInteger mainHeight,
            BigInteger round,
            byte[] nextProofContextHash,
            byte[][] networkSectionToRoot,
            BigInteger nid,
            BigInteger updateNumber,
            byte[] prev,
            BigInteger messageCount,
            byte[] messageRoot,
            byte[] proof
    ) {
        this.mainHeight = mainHeight;
        this.round = round;
        this.nextProofContextHash = nextProofContextHash;
        this.networkSectionToRoot = networkSectionToRoot;
        this.nid = nid;
        this.updateNumber = updateNumber;
        this.prev = prev;
        this.messageCount = messageCount;
        this.messageRoot = messageRoot;
        this.proof = proof;
    }

    public static BlockUpdate readObject(ObjectReader r) {
        r.beginList();
        var mainHeight = r.readNullable(BigInteger.class);
        var round = r.readNullable(BigInteger.class);
        var nextProofContextHash = r.readNullable(byte[].class);
        r.beginList();
        byte[][] networkSectionToRoot;
        List<byte[]> nstoRootList = new ArrayList<>();
        while(r.hasNext()) {
            nstoRootList.add(r.readByteArray());
        }
        networkSectionToRoot = new byte[nstoRootList.size()][];
        for(int i = 0; i < nstoRootList.size(); i++) {
            networkSectionToRoot[i] = nstoRootList.get(i);
        }
        r.end();
        var nid = r.readBigInteger();
        var updateNumber = r.readBigInteger();
        var prev = r.readNullable(byte[].class);
        var messageCount = r.readBigInteger();
        var messageRoot = r.readNullable(byte[].class);
        var proof = r.readNullable(byte[].class);
        r.end();
        return new BlockUpdate(
                mainHeight,
                round,
                nextProofContextHash,
                networkSectionToRoot,
                nid,
                updateNumber,
                prev,
                messageCount,
                messageRoot,
                proof
        );
    }

    public static BlockUpdate fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return BlockUpdate.readObject(reader);
    }
}

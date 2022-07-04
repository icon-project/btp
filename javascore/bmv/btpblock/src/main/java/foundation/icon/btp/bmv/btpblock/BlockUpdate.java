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
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class BlockUpdate {
    private static final int HASH_LEN = 32;
    private BigInteger mainHeight;
    private BigInteger round;
    private byte[] nextProofContextHash;
    private NetworkSectionToRoot[] networkSectionToRoot;
    private BigInteger nid;
    private BigInteger updateNumber;
    private byte[] prev;
    private BigInteger messageCount;
    private byte[] messageRoot;
    private byte[] proof;
    private byte[] nextProofContext;

    public BigInteger getMainHeight() {
        return mainHeight;
    }

    public BigInteger getRound() {
        return round;
    }

    public byte[] getNextProofContextHash() {
        return nextProofContextHash;
    }

    public NetworkSectionToRoot[] getNetworkSectionToRoot() {
        return networkSectionToRoot;
    }

    public BigInteger getNid() {
        return nid;
    }

    public BigInteger getUpdateNumber() {
        return updateNumber;
    }

    public BigInteger getFirstMessageSn() {
        return updateNumber.shiftRight(1);
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

    public byte[] getNextProofContext() {
        return nextProofContext;
    }

    public BlockUpdate(
            BigInteger mainHeight,
            BigInteger round,
            byte[] nextProofContextHash,
            NetworkSectionToRoot[] networkSectionToRoot,
            BigInteger nid,
            BigInteger updateNumber,
            byte[] prev,
            BigInteger messageCount,
            byte[] messageRoot,
            byte[] proof,
            byte[] nextProofContext
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
        this.nextProofContext = nextProofContext;
    }

    public static BlockUpdate readObject(ObjectReader r) {
        r.beginList();
        var mainHeight = r.readNullable(BigInteger.class);
        var round = r.readNullable(BigInteger.class);
        var nextProofContextHash = r.readNullable(byte[].class);
        r.beginList();
        NetworkSectionToRoot[] networkSectionToRoot;
        List<NetworkSectionToRoot> nstoRootList = new ArrayList<>();
        while(r.hasNext()) {
            nstoRootList.add(r.read(NetworkSectionToRoot.class));
        }
        networkSectionToRoot = new NetworkSectionToRoot[nstoRootList.size()];
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
        var nextProofContext = r.readNullable(byte[].class);
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
                proof,
                nextProofContext
        );
    }

    public static void writeObject(ObjectWriter w, BlockUpdate blockUpdate) {
        w.beginList(11);
        w.writeNullable(blockUpdate.mainHeight);
        w.writeNullable(blockUpdate.round);
        w.writeNullable(blockUpdate.nextProofContextHash);
        w.beginList(blockUpdate.networkSectionToRoot.length);
        for (NetworkSectionToRoot nsr : blockUpdate.networkSectionToRoot) {
            w.write(nsr);
        }
        w.end();
        w.write(blockUpdate.nid);
        w.write(blockUpdate.updateNumber);
        w.writeNullable(blockUpdate.prev);
        w.write(blockUpdate.messageCount);
        w.writeNullable(blockUpdate.messageRoot);
        w.writeNullable(blockUpdate.proof);
        w.writeNullable(blockUpdate.nextProofContext);
        w.end();
    }

    public static BlockUpdate fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return BlockUpdate.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter w = Context.newByteArrayObjectWriter("RLPn");
        writeObject(w, this);
        return w.toByteArray();
    }

    private static byte[] concatAndHash(byte[] b1, byte[] b2) {
        byte[] data = new byte[HASH_LEN * 2];
        System.arraycopy(b1, 0, data, 0, HASH_LEN);
        System.arraycopy(b2, 0, data, HASH_LEN, HASH_LEN);
        return BTPMessageVerifier.hash(data);
    }

    public byte[] getNetworkSectionsRoot(byte[] leaf) {
        byte[] h = leaf;
        for (NetworkSectionToRoot nsRoot : networkSectionToRoot) {
            if (nsRoot.dir == NetworkSectionToRoot.LEFT) {
                h = concatAndHash(nsRoot.value, leaf);
            } else if (nsRoot.dir == NetworkSectionToRoot.RIGHT) {
                h = concatAndHash(leaf, nsRoot.value);
            }
        }
        return h;
    }

    public static class NetworkSectionToRoot {
        final static int LEFT = 0;
        final static int RIGHT = 1;
        private int dir;
        private byte[] value;

        public NetworkSectionToRoot(int dir, byte[] value) {
            this.dir = dir;
            this.value = value;
        }

        public static NetworkSectionToRoot readObject(ObjectReader r) {
            r.beginList();
            NetworkSectionToRoot obj = new NetworkSectionToRoot(r.readInt(), r.readByteArray());
            r.end();
            return obj;
        }

        public static void writeObject(ObjectWriter w, NetworkSectionToRoot networkSectionToRoot) {
            w.beginList(2);
            w.write(networkSectionToRoot.dir);
            w.write(networkSectionToRoot.value);
            w.end();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NetworkSectionToRoot that = (NetworkSectionToRoot) o;
            return dir == that.dir && Arrays.equals(value, that.value);
        }

    }
}

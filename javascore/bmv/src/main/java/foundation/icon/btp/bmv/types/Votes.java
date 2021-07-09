/*
 * Copyright 2021 ICON Foundation
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
package foundation.icon.btp.bmv.types;

import score.*;
import scorex.util.ArrayList;

import java.util.List;

public class Votes {

    final static String RLPn = "RLPn";
    public static int VOTE_TYPE_PRECOMMIT = 1;
    private final PartSetID blockPartSetID;
    private final VoteItem[] voteItems;
    private long round;

    public Votes(long round, PartSetID blockPartSetID, VoteItem[] voteItems) {
        this.round = round;
        this.blockPartSetID = blockPartSetID;
        this.voteItems = voteItems;
    }

    public static Votes fromBytes(byte[] bytes) {
        if (bytes == null)
            return null;
        var reader = Context.newByteArrayObjectReader(RLPn, bytes);
        reader.beginList();

        long round = reader.readLong();
        PartSetID blockPartSetID = new PartSetID(reader);

        reader.beginList();

        List<VoteItem> items = new ArrayList<>();
        while (reader.hasNext())
            items.add(new VoteItem(reader));
        reader.end();

        VoteItem[] voteItems = new VoteItem[items.size()];
        for (int i = 0; i < voteItems.length; i++) {
            voteItems[i] = items.get(i);
        }

        round = items.size();

        while (reader.hasNext()) {
            reader.skip(1);
        }
        reader.end();

        return new Votes(round, blockPartSetID, voteItems);
    }

    public long getRound() {
        return round;
    }

    public PartSetID getBlockPartSetID() {
        return blockPartSetID;
    }

    public VoteItem[] getVoteItems() {
        return voteItems;
    }

    public boolean verify(long height, byte[] blockId, ValidatorList validatorList) {
        List<Address> positiveValidators = new ArrayList<>();
        for (VoteItem voteItem : this.voteItems) {
            byte[] encVoteMessage = encodeVoteMessage(voteItem, height, blockId);
            byte[] voteMessageHash = Context.hash("sha3-256", encVoteMessage);
            byte[] publicKey = Context.recoverKey("sha3-256", voteMessageHash, voteItem.signature, true);
            Address address = Context.getAddressFromKey(publicKey);

          /*  if(validatorList.contains(address) && !positiveValidators.contains(address))
               positiveValidators.add(address);*/
        }

        double r = (validatorList.getValidators().size() * (double) 2 / 3);
        return (positiveValidators.size() <= r);
    }

    private byte[] encodeVoteMessage(VoteItem voteItem, long height, byte[] blockId) {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter(RLPn);
        writer.beginList(6);
        writer.write(height);
        writer.write(getRound());
        writer.write(VOTE_TYPE_PRECOMMIT);
        writer.write(blockId);
        blockPartSetID.writeTo(writer);
        writer.write(voteItem.timestamp);
        writer.end();
        return writer.toByteArray();
    }

    public static class VoteItem {
        private final long timestamp;
        private final byte[] signature;

        public VoteItem(ObjectReader r) {
            r.beginList();
            this.timestamp = r.readLong();
            this.signature = r.readByteArray();
            while (r.hasNext()) {
                r.skip(1);
            }
            r.end();
        }

        public long getTimestamp() {
            return timestamp;
        }

        public byte[] getSignature() {
            return signature;
        }
    }

    public static class PartSetID {
        private final int count;
        private final byte[] hash;

        public PartSetID(ObjectReader r) {
            r.beginList();
            count = r.readInt();
            hash = r.readByteArray();
            while (r.hasNext()) {
                r.skip(1);
            }
            r.end();
        }

        public int getCount() {
            return count;
        }

        public byte[] getHash() {
            return hash;
        }

        public void writeTo(ObjectWriter w) {
            w.beginList(2);
            w.write(count);
            w.write(hash);
            w.end();
        }
    }

}

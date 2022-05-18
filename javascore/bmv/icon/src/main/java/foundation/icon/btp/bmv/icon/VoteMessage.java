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

package foundation.icon.btp.bmv.icon;

import foundation.icon.score.util.StringUtil;
import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

public class VoteMessage {
    public static final long VOTE_TYPE_PRECOMMIT = 1;
    private long height;
    private long round;
    private long voteType;
    private byte[] blockId;
    private PartSetId partSetId;
    private long timestamp;

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    public long getRound() {
        return round;
    }

    public void setRound(long round) {
        this.round = round;
    }

    public long getVoteType() {
        return voteType;
    }

    public void setVoteType(long voteType) {
        this.voteType = voteType;
    }

    public byte[] getBlockId() {
        return blockId;
    }

    public void setBlockId(byte[] blockId) {
        this.blockId = blockId;
    }

    public PartSetId getPartSetId() {
        return partSetId;
    }

    public void setPartSetId(PartSetId partSetId) {
        this.partSetId = partSetId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("VoteMessage{");
        sb.append("height=").append(height);
        sb.append(", round=").append(round);
        sb.append(", voteType=").append(voteType);
        sb.append(", blockId=").append(StringUtil.toString(blockId));
        sb.append(", partSetId=").append(partSetId);
        sb.append(", timestamp=").append(timestamp);
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, VoteMessage obj) {
        obj.writeObject(writer);
    }

    public static VoteMessage readObject(ObjectReader reader) {
        VoteMessage obj = new VoteMessage();
        reader.beginList();
        obj.setHeight(reader.readLong());
        obj.setRound(reader.readLong());
        obj.setVoteType(reader.readLong());
        obj.setBlockId(reader.readNullable(byte[].class));
        obj.setPartSetId(reader.readNullable(PartSetId.class));
        obj.setTimestamp(reader.readLong());
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(6);
        writer.write(this.getHeight());
        writer.write(this.getRound());
        writer.write(this.getVoteType());
        writer.writeNullable(this.getBlockId());
        PartSetId partSetId = this.getPartSetId();
        writer.writeNullable(partSetId);
        writer.write(this.getTimestamp());
        writer.end();
    }

    public static VoteMessage fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return VoteMessage.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        VoteMessage.writeObject(writer, this);
        return writer.toByteArray();
    }
}

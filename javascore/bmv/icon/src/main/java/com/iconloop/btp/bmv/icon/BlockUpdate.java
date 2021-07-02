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

package com.iconloop.btp.bmv.icon;

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

public class BlockUpdate {
    private BlockHeader blockHeader;
    private Votes votes;
    private Validators nextValidators;

    public BlockHeader getBlockHeader() {
        return blockHeader;
    }

    public void setBlockHeader(BlockHeader blockHeader) {
        this.blockHeader = blockHeader;
    }

    public Votes getVotes() {
        return votes;
    }

    public void setVotes(Votes votes) {
        this.votes = votes;
    }

    public Validators getNextValidators() {
        return nextValidators;
    }

    public void setNextValidators(Validators nextValidators) {
        this.nextValidators = nextValidators;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BlockUpdate{");
        sb.append("blockHeader=").append(blockHeader);
        sb.append(", votes=").append(votes);
        sb.append(", nextValidators=").append(nextValidators);
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, BlockUpdate obj) {
        obj.writeObject(writer);
    }

    public static BlockUpdate readObject(ObjectReader reader) {
        BlockUpdate obj = new BlockUpdate();
        reader.beginList();
        byte[] blockHeaderBytes = reader.readNullable(byte[].class);
        if (blockHeaderBytes != null) {
            ObjectReader blockHeaderReader = Context.newByteArrayObjectReader("RLPn",blockHeaderBytes);
            obj.setBlockHeader(blockHeaderReader.read(BlockHeader.class));
        }
        byte[] votesBytes = reader.readNullable(byte[].class);
        if (votesBytes != null) {
            ObjectReader votesReader = Context.newByteArrayObjectReader("RLPn",votesBytes);
            obj.setVotes(votesReader.read(Votes.class));
        }
        byte[] nextValidatorsBytes = reader.readNullable(byte[].class);
        if (nextValidatorsBytes != null) {
            ObjectReader nextValidatorsReader = Context.newByteArrayObjectReader("RLPn",nextValidatorsBytes);
            obj.setNextValidators(nextValidatorsReader.read(Validators.class));
        }
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(3);
        BlockHeader blockHeader = this.getBlockHeader();
        if (blockHeader != null) {
            ByteArrayObjectWriter blockHeaderWriter = Context.newByteArrayObjectWriter("RLPn");
            blockHeaderWriter.writeNullable(blockHeader);
            writer.write(blockHeaderWriter.toByteArray());
        } else {
            writer.writeNull();
        }
        Votes votes = this.getVotes();
        if (votes != null) {
            ByteArrayObjectWriter votesWriter = Context.newByteArrayObjectWriter("RLPn");
            votesWriter.write(votes);
            writer.write(votesWriter.toByteArray());
        } else {
            writer.writeNull();
        }
        Validators nextValidators = this.getNextValidators();
        if (nextValidators != null) {
            ByteArrayObjectWriter nextValidatorsWriter = Context.newByteArrayObjectWriter("RLPn");
            nextValidatorsWriter.writeNullable(nextValidators);
            writer.write(nextValidatorsWriter.toByteArray());
        } else {
            writer.writeNull();
        }
        writer.end();
    }

    public static BlockUpdate fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return BlockUpdate.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        BlockUpdate.writeObject(writer, this);
        return writer.toByteArray();
    }
}

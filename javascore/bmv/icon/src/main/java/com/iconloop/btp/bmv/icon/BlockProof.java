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

public class BlockProof {
    private BlockHeader blockHeader;
    private BlockWitness blockWitness;

    public BlockHeader getBlockHeader() {
        return blockHeader;
    }

    public void setBlockHeader(BlockHeader blockHeader) {
        this.blockHeader = blockHeader;
    }

    public BlockWitness getBlockWitness() {
        return blockWitness;
    }

    public void setBlockWitness(BlockWitness blockWitness) {
        this.blockWitness = blockWitness;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BlockProof{");
        sb.append("blockHeader=").append(blockHeader);
        sb.append(", blockWitness=").append(blockWitness);
        sb.append('}');
        return sb.toString();
    }
    
    public static void writeObject(ObjectWriter writer, BlockProof obj) {
        obj.writeObject(writer);
    }

    public static BlockProof readObject(ObjectReader reader) {
        BlockProof obj = new BlockProof();
        reader.beginList();
        byte[] blockHeaderBytes = reader.readNullable(byte[].class);
        if (blockHeaderBytes != null) {
            ObjectReader blockHeaderReader = Context.newByteArrayObjectReader("RLPn",blockHeaderBytes);
            obj.setBlockHeader(blockHeaderReader.read(BlockHeader.class));
        }
        obj.setBlockWitness(reader.readNullable(BlockWitness.class));
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(2);
        BlockHeader blockHeader = this.getBlockHeader();
        if (blockHeader != null) {
            ByteArrayObjectWriter blockHeaderWriter = Context.newByteArrayObjectWriter("RLPn");
            blockHeaderWriter.write(blockHeader);
            writer.writeNullable(blockHeaderWriter.toByteArray());
        } else {
            writer.writeNull();
        }
        BlockWitness blockWitness = this.getBlockWitness();
        writer.writeNullable(blockWitness);
        writer.end();
    }

    public static BlockProof fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return BlockProof.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        BlockProof.writeObject(writer, this);
        return writer.toByteArray();
    }
}

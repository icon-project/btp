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
import scorex.util.ArrayList;

import java.util.List;

public class RelayMessage {
    private BlockUpdate[] blockUpdates;
    private BlockProof blockProof;
    private ReceiptProof[] receiptProofs;

    public BlockUpdate[] getBlockUpdates() {
        return blockUpdates;
    }

    public void setBlockUpdates(BlockUpdate[] blockUpdates) {
        this.blockUpdates = blockUpdates;
    }

    public BlockProof getBlockProof() {
        return blockProof;
    }

    public void setBlockProof(BlockProof blockProof) {
        this.blockProof = blockProof;
    }

    public ReceiptProof[] getReceiptProofs() {
        return receiptProofs;
    }

    public void setReceiptProofs(ReceiptProof[] receiptProofs) {
        this.receiptProofs = receiptProofs;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RelayMessage{");
        sb.append("blockUpdates=").append(StringUtil.toString(blockUpdates));
        sb.append(", blockProof=").append(blockProof);
        sb.append(", receiptProofs=").append(StringUtil.toString(receiptProofs));
        sb.append('}');
        return sb.toString();
    }

    public static void writeObject(ObjectWriter writer, RelayMessage obj) {
        obj.writeObject(writer);
    }

    public static RelayMessage readObject(ObjectReader reader) {
        RelayMessage obj = new RelayMessage();
        reader.beginList();
        if (reader.beginNullableList()) {
            BlockUpdate[] blockUpdates = null;
            List<BlockUpdate> blockUpdatesList = new ArrayList<>();
            while(reader.hasNext()) {
                byte[] blockUpdatesElementBytes = reader.readNullable(byte[].class);
                if (blockUpdatesElementBytes != null) {
                    ObjectReader blockUpdatesElementReader = Context.newByteArrayObjectReader("RLPn",blockUpdatesElementBytes);
                    blockUpdatesList.add(blockUpdatesElementReader.read(BlockUpdate.class));
                } else {
                    blockUpdatesList.add(null);
                }
            }
            blockUpdates = new BlockUpdate[blockUpdatesList.size()];
            for(int i=0; i<blockUpdatesList.size(); i++) {
                blockUpdates[i] = (BlockUpdate)blockUpdatesList.get(i);
            }
            obj.setBlockUpdates(blockUpdates);
            reader.end();
        }
        byte[] blockProofBytes = reader.readNullable(byte[].class);
        if (blockProofBytes != null) {
            ObjectReader blockProofReader = Context.newByteArrayObjectReader("RLPn",blockProofBytes);
            obj.setBlockProof(blockProofReader.read(BlockProof.class));
        }
        if (reader.beginNullableList()) {
            ReceiptProof[] receiptProofs = null;
            List<ReceiptProof> receiptProofsList = new ArrayList<>();
            while(reader.hasNext()) {
                byte[] receiptProofsElementBytes = reader.readNullable(byte[].class);
                if (receiptProofsElementBytes != null) {
                    ObjectReader receiptProofsElementReader = Context.newByteArrayObjectReader("RLPn",receiptProofsElementBytes);
                    receiptProofsList.add(receiptProofsElementReader.read(ReceiptProof.class));
                } else {
                    receiptProofsList.add(null);
                }
            }
            receiptProofs = new ReceiptProof[receiptProofsList.size()];
            for(int i=0; i<receiptProofsList.size(); i++) {
                receiptProofs[i] = (ReceiptProof)receiptProofsList.get(i);
            }
            obj.setReceiptProofs(receiptProofs);
            reader.end();
        }
        reader.end();
        return obj;
    }

    public void writeObject(ObjectWriter writer) {
        writer.beginList(3);
        BlockUpdate[] blockUpdates = this.getBlockUpdates();
        if (blockUpdates != null) {
            writer.beginNullableList(blockUpdates.length);
            for(BlockUpdate v : blockUpdates) {
                if (v != null) {
                    ByteArrayObjectWriter vWriter = Context.newByteArrayObjectWriter("RLPn");
                    vWriter.write(v);
                    writer.writeNullable(vWriter.toByteArray());
                } else {
                    writer.writeNull();
                }
            }
            writer.end();
        } else {
            writer.writeNull();
        }
        BlockProof blockProof = this.getBlockProof();
        if (blockProof != null) {
            ByteArrayObjectWriter blockProofWriter = Context.newByteArrayObjectWriter("RLPn");
            blockProofWriter.write(blockProof);
            writer.writeNullable(blockProofWriter.toByteArray());
        } else {
            writer.writeNull();
        }
        ReceiptProof[] receiptProofs = this.getReceiptProofs();
        if (receiptProofs != null) {
            writer.beginNullableList(receiptProofs.length);
            for(ReceiptProof v : receiptProofs) {
                if (v != null) {
                    ByteArrayObjectWriter vWriter = Context.newByteArrayObjectWriter("RLPn");
                    vWriter.write(v);
                    writer.writeNullable(vWriter.toByteArray());
                } else {
                    writer.writeNull();
                }
            }
            writer.end();
        } else {
            writer.writeNull();
        }
        writer.end();
    }

    public static RelayMessage fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return RelayMessage.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        RelayMessage.writeObject(writer, this);
        return writer.toByteArray();
    }
}
